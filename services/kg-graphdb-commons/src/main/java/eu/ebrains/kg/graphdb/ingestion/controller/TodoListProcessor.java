/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.graphdb.ingestion.controller;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.InferredJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.TodoItem;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.commons.model.ArangoInstance;
import eu.ebrains.kg.graphdb.ingestion.model.DBOperation;
import eu.ebrains.kg.graphdb.ingestion.model.EdgeResolutionOperation;
import eu.ebrains.kg.graphdb.ingestion.model.RemoveReleaseStateOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TodoListProcessor {

    private final ArangoRepositoryCommons repository;

    private final StructureSplitter splitter;

    private final MainEventTracker eventTracker;

    private final DataController dataController;

    private final IdUtils idUtils;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ReleasingController releasingController;


    public TodoListProcessor(ArangoRepositoryCommons repository, StructureSplitter splitter, MainEventTracker eventTracker, IdUtils idUtils, DataController dataController, ReleasingController releasingController) {
        this.repository = repository;
        this.splitter = splitter;
        this.eventTracker = eventTracker;
        this.idUtils = idUtils;
        this.dataController = dataController;
        this.releasingController = releasingController;
    }

    public void doProcessTodoList(List<TodoItem> todoList, DataStage stage) {
        //TODO can we make this transactional?
        Set<User> handledUsers = new HashSet<>();
        for (TodoItem todoItem : todoList) {
            ArangoDocumentReference rootDocumentReference = ArangoCollectionReference.fromSpace(todoItem.getSpace()).doc(todoItem.getDocumentId());
            if(todoItem.getPayload()!=null && todoItem.getSpace()!=null){
                todoItem.getPayload().put(EBRAINSVocabulary.META_SPACE, todoItem.getSpace());
            }
            switch (todoItem.getType()) {
                case UPDATE:
                case INSERT:
                    logger.info("Upserting a document");
                    upsertDocument(rootDocumentReference, todoItem.getPayload(), stage);
                    break;
                case DELETE:
                    logger.info("Removing an instance");
                    //Since we're going to do a "hard" delete, we also have to remove all instances that have been contributing to it.
                    final List<ArangoDocumentReference> nativeDocumentsByInferredInstance = getNativeDocumentsByInferredInstance(rootDocumentReference);
                    repository.executeTransactional(DataStage.NATIVE, dataController.createDeleteOperations(stage, nativeDocumentsByInferredInstance));
                    deleteDocument(DataStage.IN_PROGRESS, rootDocumentReference);
                    break;
                case UNRELEASE:
                    logger.info("Unreleasing a document");
                    unreleaseDocument(rootDocumentReference);
                    break;
                case RELEASE:
                    logger.info("Releasing a document");
                    releaseDocument(rootDocumentReference, todoItem.getPayload());
                    break;

            }
            logger.debug("Updating last seen event id");
            eventTracker.updateLastSeenEventId(stage, todoItem.getEventId());
        }
    }

    private List<ArangoDocumentReference> getNativeDocumentsByInferredInstance(ArangoDocumentReference rootDocumentReference) {
        final ArangoDocument document = repository.getDocument(DataStage.IN_PROGRESS, rootDocumentReference);
        if(document!=null){
            final NormalizedJsonLd doc = document.asIndexedDoc().getDoc();
            final List<String> inferenceSourceDocs = doc.getAsListOf(InferredJsonLdDoc.INFERENCE_OF, String.class);
            return inferenceSourceDocs.stream().map(inferenceSourceDoc -> new ArangoDocumentReference(document.getId().getArangoCollectionReference(), idUtils.getUUID(new JsonLdId(inferenceSourceDoc)))).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }


    private void unreleaseDocument(ArangoDocumentReference rootDocumentReference) {
        deleteDocument(DataStage.RELEASED, rootDocumentReference);
        repository.executeTransactional(DataStage.IN_PROGRESS, Collections.singletonList(new RemoveReleaseStateOperation(releasingController.getReleaseStatusEdgeId(rootDocumentReference))));
    }

    private void releaseDocument(ArangoDocumentReference rootDocumentReference, NormalizedJsonLd payload) {
        // Releasing a specific revision
        upsertDocument(rootDocumentReference, payload, DataStage.RELEASED);
        repository.executeTransactional(DataStage.IN_PROGRESS, Collections.singletonList(releasingController.getReleaseStatusUpdateOperation(rootDocumentReference, true)));
    }


    private boolean hasChangedReleaseStatus(DataStage stage, ArangoDocumentReference documentReference) {
        //TODO analyze payload for change by comparison with current instance - ignore alternatives
        return stage == DataStage.IN_PROGRESS;
    }

    public ArangoDocumentReference upsertDocument(ArangoDocumentReference rootDocumentRef, NormalizedJsonLd payload, DataStage stage) {
        List<ArangoInstance> arangoInstances = splitter.extractRelations(rootDocumentRef, payload);
        List<DBOperation> upsertOperationsForDocument = dataController.createUpsertOperations(rootDocumentRef, stage, arangoInstances, hasChangedReleaseStatus(stage, rootDocumentRef));
        repository.executeTransactional(stage, upsertOperationsForDocument);
        List<EdgeResolutionOperation> lazyIdResolutionOperations;
        if (stage != DataStage.NATIVE) {
            //We don't need to resolve links in NATIVE and neither do META structures... it is sufficient if we do this in IN_PROGRESS and RELEASED
            lazyIdResolutionOperations = dataController.createResolutionsForPreviouslyUnresolved(stage, rootDocumentRef, payload.allIdentifiersIncludingId());
            repository.executeTransactional(stage, lazyIdResolutionOperations);
        }
        return rootDocumentRef;
    }


    public void deleteDocument(DataStage stage, ArangoDocumentReference documentReference) {
        if (repository.doesDocumentExist(stage, documentReference)) {
            final List<DBOperation> deleteOperations = dataController.createDeleteOperations(stage, Collections.singletonList(documentReference));
            repository.executeTransactional(stage, deleteOperations);
        } else {
            logger.warn(String.format("Tried to remove non-existent document with id %s in stage %s", documentReference.getId(), stage.name()));
        }
    }
}

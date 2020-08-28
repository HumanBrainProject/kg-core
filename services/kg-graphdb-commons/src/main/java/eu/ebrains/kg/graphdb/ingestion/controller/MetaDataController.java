/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ebrains.kg.graphdb.ingestion.controller;


import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.commons.model.ArangoEdge;
import eu.ebrains.kg.graphdb.commons.model.ArangoInstance;
import eu.ebrains.kg.graphdb.ingestion.controller.structure.StructureTracker;
import eu.ebrains.kg.graphdb.ingestion.model.DBOperation;
import eu.ebrains.kg.graphdb.ingestion.model.DeleteOperation;
import eu.ebrains.kg.graphdb.ingestion.model.EdgeResolutionOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MetaDataController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SemanticsController semanticsController;

    private final StructureTracker structureTracker;
    private final ArangoRepositoryCommons repository;


    public MetaDataController(SemanticsController semanticsController, StructureTracker structureTracker, ArangoRepositoryCommons repository) {
        this.semanticsController = semanticsController;
        this.structureTracker = structureTracker;
        this.repository = repository;
    }

    @Async
    public void handleMetaData(DataStage stage, ArangoDocumentReference rootDocumentRef, NormalizedJsonLd payload, List<ArangoInstance> arangoInstances, List<EdgeResolutionOperation> lazyIdResolutionOperations, List<UUID> mergeIds) {
        repository.executeTransactionalOnMeta(stage, createUpsertOperations(stage, rootDocumentRef, payload.getTypes(), arangoInstances, lazyIdResolutionOperations, mergeIds));
    }

    public List<DBOperation> createUpsertOperations(DataStage stage, ArangoDocumentReference rootDocumentRef, List<String> types, List<ArangoInstance> arangoInstances, List<EdgeResolutionOperation> resolvedEdges, List<UUID> mergedIds) {
        List<DBOperation> operations = new ArrayList<>();
        logger.debug("Looking for meta data operations for documents");
        List<DBOperation> metaOperationsForDocuments = arangoInstances.stream().filter(a -> a instanceof ArangoDocument).map(a -> (ArangoDocument) a).map(a -> createUpsertOperationsForDocument(stage, rootDocumentRef, a)).flatMap(List::stream).collect(Collectors.toList());
        operations.addAll(metaOperationsForDocuments);
        logger.debug("Looking for meta data operations for edges");
        List<DBOperation> metaOperationsForEdges = arangoInstances.stream().filter(a -> a instanceof ArangoEdge).map(a -> (ArangoEdge) a).map(e -> createUpsertOperationsForEdge(stage, rootDocumentRef, types, e)).flatMap(List::stream).collect(Collectors.toList());
        operations.addAll(metaOperationsForEdges);
        logger.debug("Looking for meta data operations for resolved edges");
        List<DBOperation> metaOperationsForResolvedEdges = resolvedEdges != null ? resolvedEdges.stream().map(r -> getMetaUpsertOperationsForResolvedInstance(stage, r, arangoInstances)).flatMap(List::stream).collect(Collectors.toList()) : Collections.emptyList();
        operations.addAll(metaOperationsForResolvedEdges);
        if (mergedIds != null) {
            logger.debug("Looking for mergeIds (they have to be removed from the statistics)");
            //Note that merges can only happen in the same space, so we can "borrow" the space from the rootDocument.
            mergedIds.forEach(mergedId -> operations.add(new DeleteOperation(rootDocumentRef.getArangoCollectionReference().doc(mergedId))));
        }
        logger.debug(String.format("All operations: %d", operations.size()));
        return operations;
    }

    public List<DBOperation> createDeleteOperations(DataStage stage, ArangoDocumentReference rootDocumentRef) {
        List<DBOperation> genericOperations = Collections.singletonList(new DeleteOperation(rootDocumentRef));
        List<DBOperation> structuralDeleteOperations = this.structureTracker.createDeleteOperations(stage, rootDocumentRef);
        return Stream.concat(genericOperations.stream(), structuralDeleteOperations.stream()).collect(Collectors.toList());
    }

    private List<DBOperation> createUpsertOperationsForDocument(DataStage stage, ArangoDocumentReference rootDocumentRef, ArangoDocument document) {
        List<DBOperation> structureOps = structureTracker.createUpsertOperations(stage, rootDocumentRef, document);
        List<DBOperation> semanticsOps = semanticsController.createUpsertOperations(stage, rootDocumentRef, document);
        return Stream.concat(structureOps.stream(), semanticsOps.stream()).collect(Collectors.toList());
    }

    private List<DBOperation> createUpsertOperationsForEdge(DataStage stage, ArangoDocumentReference originalDocumentRef, List<String> types, ArangoEdge edge) {
        return structureTracker.createUpsertOperations(stage, originalDocumentRef, types, edge);
    }

    private List<DBOperation> getMetaUpsertOperationsForResolvedInstance(DataStage stage, EdgeResolutionOperation resolutionOperation, List<ArangoInstance> instances) {
        return instances.stream().filter(i -> i instanceof ArangoDocument).map(i -> (ArangoDocument) i).filter(i -> !i.asIndexedDoc().isEmbedded()).map(i -> structureTracker.createUpsertOperations(stage, i, resolutionOperation)).flatMap(Collection::stream).collect(Collectors.toList());
    }

}

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


import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.controller.ArangoUtils;
import eu.ebrains.kg.graphdb.commons.controller.EntryHookDocuments;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.commons.model.ArangoEdge;
import eu.ebrains.kg.graphdb.commons.model.ArangoInstance;
import eu.ebrains.kg.graphdb.commons.serviceCall.GraphDBToIds;
import eu.ebrains.kg.graphdb.ingestion.model.DBOperation;
import eu.ebrains.kg.graphdb.ingestion.model.DeleteOperation;
import eu.ebrains.kg.graphdb.ingestion.model.EdgeResolutionOperation;
import eu.ebrains.kg.graphdb.ingestion.model.UpsertOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DataController {
    @Autowired
    private IdUtils idUtils;

    @Autowired
    private ArangoRepositoryCommons repository;


    @Autowired
    private EntryHookDocuments entryHookDocuments;

    @Autowired
    private ArangoUtils arangoUtils;

    @Autowired
    private GraphDBToIds idLookupSvc;

    @Autowired
    private ReleasingController releasingController;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public List<DBOperation> createUpsertOperations(ArangoDocumentReference rootDocumentRef, NormalizedJsonLd payload, DataStage stage, List<ArangoInstance> arangoInstances, List<UUID> mergedIds, boolean changedReleaseStatus) {
        logger.trace("Finding upsert operations for document");
        List<DBOperation> operations = new ArrayList<>();
        operations.addAll(createTypeOperations(rootDocumentRef, stage, arangoInstances));
        operations.addAll(createIdMergeOperations(stage, rootDocumentRef, mergedIds));
        arangoInstances.stream().filter(i -> i instanceof ArangoDocument).forEach(i -> ((ArangoDocument) i).setKeyBasedOnId());
        if (changedReleaseStatus) {
            defineChangedReleaseStatusIfApplicable(rootDocumentRef, operations);
        }
        //Resolve all edges with to == null
        Set<ArangoEdge> resolvedEdges = resolveEdges(stage, arangoInstances.stream().filter(i -> i instanceof ArangoEdge).map(i -> (ArangoEdge) i).filter(i -> i.getTo() == null).collect(Collectors.toSet()));
        arangoInstances.stream().filter(i -> i instanceof ArangoDocument).map(i -> (ArangoDocument)i).forEach(d -> d.applyResolvedEdges(resolvedEdges));
        operations.addAll(createDBUpsertOperations(rootDocumentRef, arangoInstances));
        return operations;
    }

    public List<DBOperation> createDeleteOperations(DataStage stage, ArangoDocumentReference documentRef) {
        return Collections.singletonList(new DeleteOperation(documentRef));
    }


    public List<EdgeResolutionOperation> createResolutionsForPreviouslyUnresolved(DataStage stage, ArangoDocumentReference rootDocumentRef, Set<String> allIdentifiers) {
        logger.trace("Resolve unresolved");
        List<EdgeResolutionOperation> result = new ArrayList<>();
        List<ArangoEdge> unresolvedEdgesForIds = repository.findUnresolvedEdgesForIds(stage, allIdentifiers);
        for (ArangoEdge arangoEdge : unresolvedEdgesForIds) {
            ArangoDocumentReference oldEdgeRef = arangoEdge.getId();
            //Direct the found edges to the new document
            arangoEdge.setTo(rootDocumentRef);
            //Relocate the edge to the correct edge collection (according to the property label)
            ArangoCollectionReference propertyEdgeCollection = ArangoCollectionReference.fromSpace(new Space(arangoEdge.getOriginalLabel()), true);
            arangoEdge.redefineId(propertyEdgeCollection.doc(arangoEdge.getKey()));
            arangoEdge.setResolvedTargetId(idUtils.buildAbsoluteUrl(rootDocumentRef.getDocumentId()));
            result.add(new EdgeResolutionOperation(oldEdgeRef, arangoEdge));
        }
        return result;
    }


    private List<DBOperation> createTypeOperations(ArangoDocumentReference rootDocumentRef, DataStage stage, List<ArangoInstance> arangoInstances) {
        logger.trace("Handle types");
        List<DBOperation> operations = new ArrayList<>();
        for (ArangoInstance arangoInstance : arangoInstances) {
            if (arangoInstance instanceof ArangoDocument) {
                ArangoDocument arangoDocument = (ArangoDocument) arangoInstance;
                operations.addAll(arangoDocument.getDoc().getTypes().stream().map(t -> {
                    ArangoEdge edge = entryHookDocuments.createEdgeFromHookDocument(InternalSpace.TYPE_EDGE_COLLECTION, arangoDocument.getId(), entryHookDocuments.getOrCreateTypeHookDocument(stage, t), null);
                    return new UpsertOperation(rootDocumentRef, edge.dumpPayload(), new ArangoDocumentReference(InternalSpace.TYPE_EDGE_COLLECTION, edge.getKey()));
                }).collect(Collectors.toList()));
            }
        }
        return operations;
    }


    private List<DBOperation> createIdMergeOperations(DataStage stage, ArangoDocumentReference targetDocumentRef, List<UUID> mergedIds) {
        logger.trace("Handle merges");
        if (mergedIds == null || mergedIds.isEmpty()) {
            logger.trace("No merges");
            return Collections.emptyList();
        } else {
            logger.trace("Merges available");
            List<DBOperation> result = new ArrayList<>();
            //Find all relations pointing to any of the merged ids...
            for (UUID mergedDocumentId : mergedIds) {
                //Because merges can only happen in the same space, we can reuse the collection of the target document.
                ArangoDocumentReference mergedDocumentRef = targetDocumentRef.getArangoCollectionReference().doc(mergedDocumentId);
                List<ArangoEdge> incomingRelationsForDocument = repository.getIncomingRelationsForDocument(stage, mergedDocumentRef);
                for (ArangoEdge arangoEdge : incomingRelationsForDocument) {
                    if (!arangoUtils.isInternalCollection(arangoEdge.getId().getArangoCollectionReference())) {
                        //redirect them to the new targetId
                        logger.debug(String.format("I'm redirecting the link from %s to %s due to a merge", arangoEdge.getTo().getId(), targetDocumentRef.getId()));
                        arangoEdge.setTo(targetDocumentRef);
                        result.add(new UpsertOperation(targetDocumentRef, arangoEdge.dumpPayload(), arangoEdge.getId()));
                    }
                }
                //We're going to merge the instance - therefore we also need to remove the previous documents and all their dependent documents.
                result.add(new DeleteOperation(mergedDocumentRef));
            }
            return result;
        }
    }


    private void defineChangedReleaseStatusIfApplicable(ArangoDocumentReference documentReference, List<DBOperation> operations) {
        logger.trace("set release status");
        UpsertOperation releaseStatusOperation = releasingController.getReleaseStatusUpdateOperation(documentReference, false);
        if (repository.doesDocumentExist(DataStage.IN_PROGRESS, releaseStatusOperation.getDocumentReference())) {
            //There is already a release status assignment -> regardless if it was "changed" or "released" beforehand -> we are going to update it to "changed"
            operations.add(releaseStatusOperation);
        }
    }


    private Set<ArangoEdge> resolveEdges(DataStage stage, Set<ArangoEdge> edges) {
        logger.trace("Resolve edges");
        Map<JsonLdId, UUID> edgeToRequestId = new HashMap<>();
        List<IdWithAlternatives> ids = edges.stream().map(e -> {
            UUID requestId = UUID.randomUUID();
            edgeToRequestId.put(e.getOriginalTo(), requestId);
            return new IdWithAlternatives().setId(requestId).setAlternatives(Collections.singleton(e.getOriginalTo().getId()));
        }).collect(Collectors.toList());
        List<JsonLdIdMapping> resolvedIds = idLookupSvc.resolveIds(stage, ids);
        Set<ArangoEdge> resolvedEdges = new HashSet<>();
        Set<JsonLdId> resolvedJsonLdIds = new HashSet<>();

        for (ArangoEdge edge : edges) {
            ArangoDocumentReference unknownTarget = ArangoDocumentReference.fromArangoId("unknown/" + UUID.nameUUIDFromBytes("unknown".getBytes(StandardCharsets.UTF_8)).toString(), false);
            if (stage == DataStage.NATIVE || isEdgeOf(edge.getId(), InternalSpace.INFERENCE_OF_SPACE, new Space(EBRAINSVocabulary.META_USER))) {
                //We are either in NATIVE stage or have a relation to inference of or user - we already know that we won't be able to resolve it (since the target instance is in a different database), so we shortcut the process.
                logger.trace(String.format("Not resolving edge pointing to %s", edge.getOriginalTo()));
                edge.setTo(unknownTarget);
            } else {
                JsonLdIdMapping jsonLdIdMapping = resolvedIds.stream().filter(resolved -> resolved.getRequestedId().equals(edgeToRequestId.get(edge.getOriginalTo()))).findFirst().orElse(null);
                if (jsonLdIdMapping != null && jsonLdIdMapping.getResolvedIds() != null && jsonLdIdMapping.getResolvedIds().size() == 1) {
                    JsonLdId resolvedId = jsonLdIdMapping.getResolvedIds().iterator().next();
                    UUID uuid = idUtils.getUUID(resolvedId);
                    if (uuid == null) {
                        throw new IllegalArgumentException(String.format("Resolved %s to a non-internal documentId: %s", edge.getOriginalTo().getId(), resolvedId.getId()));
                    } else if (jsonLdIdMapping.getSpace() == null) {
                        throw new IllegalArgumentException(String.format("The resolution for the id %s didn't provide the according space", resolvedId.getId()));
                    } else {
                        logger.debug(String.format("I have resolved the id %s to %s - redirecting the edge", edge.getOriginalTo().getId(), resolvedId.getId()));
                        edge.setTo(ArangoCollectionReference.fromSpace(jsonLdIdMapping.getSpace()).doc(uuid));
                        edge.setResolvedTargetId(resolvedId);
                        if(resolvedJsonLdIds.add(resolvedId)) {
                            resolvedEdges.add(edge);
                        }
                    }
                } else {
                    logger.info("Was not able to resolve link -> we postpone it.");
                    ArangoDocumentReference unresolvedEdge = ArangoCollectionReference.fromSpace(InternalSpace.UNRESOLVED_SPACE, true).doc(edge.getId().getDocumentId());
                    edge.redefineId(unresolvedEdge);
                    edge.setTo(unknownTarget);
                }
            }
        }
        logger.trace("Edges resolved");
        return resolvedEdges;
    }


    public List<DBOperation> createDBUpsertOperations(ArangoDocumentReference rootDocumentRef, List<ArangoInstance> arangoDocuments) {
        logger.trace("Creating upsert operations");
        return arangoDocuments.stream().map(i -> new UpsertOperation(rootDocumentRef, i.dumpPayload(), i.getId())).collect(Collectors.toList());
    }

    private boolean isEdgeOf(ArangoDocumentReference edge, Space... spaces) {
        if(edge == null || edge.getArangoCollectionReference() == null || edge.getArangoCollectionReference().getCollectionName() == null){
            return false;
        }
        for (Space space : spaces) {
            if(edge.getArangoCollectionReference().getCollectionName().equals(ArangoCollectionReference.fromSpace(space, true).getCollectionName())){
                return true;
            }
        }
        return false;
    }


}

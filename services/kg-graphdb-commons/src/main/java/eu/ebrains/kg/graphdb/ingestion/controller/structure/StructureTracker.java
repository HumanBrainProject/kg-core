/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.graphdb.ingestion.controller.structure;

import com.arangodb.model.AqlQueryOptions;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.TypeUtils;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.commons.model.ArangoEdge;
import eu.ebrains.kg.graphdb.commons.model.ArangoInstance;
import eu.ebrains.kg.graphdb.ingestion.controller.IdFactory;
import eu.ebrains.kg.graphdb.ingestion.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class StructureTracker {

    private final ArangoRepositoryCommons arangoRepositoryCommons;

    private final ArangoDatabases databases;

    private final StaticStructureController staticStructureController;

    private final TypeUtils typeUtils;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public StructureTracker(ArangoRepositoryCommons arangoRepositoryCommons, ArangoDatabases databases, StaticStructureController staticStructureController, TypeUtils typeUtils) {
        this.arangoRepositoryCommons = arangoRepositoryCommons;
        this.databases = databases;
        this.staticStructureController = staticStructureController;
        this.typeUtils = typeUtils;
    }

    public List<DBOperation> createUpsertOperations(DataStage stage, ArangoDocumentReference originalDocumentRef, ArangoDocument arangoDocument) {
        logger.debug("Creating upsert operations for structure");
        List<DBOperation> allOperations = new ArrayList<>();
        List<ArangoInstance> arangoInstances = staticStructureController.ensureStaticElementsAndCleanDocumentStructure(stage, arangoDocument, originalDocumentRef, allOperations);
        logger.debug("Handle updated types");
        allOperations.addAll(handleUpdatedTypes(originalDocumentRef.getArangoCollectionReference(), arangoDocument, stage));
        logger.debug("Add new edges of document contributions");
        List<DBOperation> newDocumentContributionsToBeAdded = addNewEdgesOfDocumentContributions(originalDocumentRef, arangoInstances, arangoDocument.getDoc().types(), null, true);
        allOperations.addAll(newDocumentContributionsToBeAdded);
        logger.debug(String.format("Found %d operations for structure", allOperations.size()));
        return allOperations;
    }

    /**
     * If the type list has changed when updating a document, the structural information has to be updated accordingly for incoming links.
     */
    private List<DBOperation> handleUpdatedTypes(ArangoCollectionReference collection, ArangoDocument arangoDocument, DataStage stage) {
        List<DBOperation> allOperations = new ArrayList<>();
        if (!arangoDocument.asIndexedDoc().isEmbedded()) {
            List<String> newSpaceTypeIds = arangoDocument.getDoc().types().stream().map(type -> IdFactory.createDocumentRefForSpaceToTypeEdge(collection, type).getId()).collect(Collectors.toList());
            Set<String> existingSpaceTypeIds = findIncomingSpaceTypeIds(arangoDocument.getOriginalDocument(), stage);
            cleanUpRemovedLinks(arangoDocument.getId(), stage, allOperations, newSpaceTypeIds, existingSpaceTypeIds);
            createLinksToNewTypes(arangoDocument.getId(), stage, newSpaceTypeIds, existingSpaceTypeIds, allOperations);
        }
        return allOperations;
    }

    private void createLinksToNewTypes(ArangoDocumentReference targetDocumentRef, DataStage stage, List<String> newSpaceTypeIds, Set<String> existingSpaceTypeIds, List<DBOperation> allOperations) {
        List<String> spaceTypesToBeLinked = newSpaceTypeIds.stream().filter(t -> !existingSpaceTypeIds.contains(t)).distinct().collect(Collectors.toList());
        if (!spaceTypesToBeLinked.isEmpty()) {
            Set<IncomingProperty> incomingProperties = findIncomingProperties(targetDocumentRef, stage);
            incomingProperties.forEach(incomingProperty -> {
                ArangoDocumentReference rootDocumentRef = ArangoDocumentReference.fromArangoId(incomingProperty.getOrigin(), false);
                ArangoDocument originalDoc = arangoRepositoryCommons.getDocument(stage, rootDocumentRef);
                List<ArangoInstance> arangoInstances = staticStructureController.ensureStaticPropertyToTypeEdges(stage, rootDocumentRef.getArangoCollectionReference(), targetDocumentRef.getArangoCollectionReference(), incomingProperty.getProperty(), originalDoc.getDoc().types(), spaceTypesToBeLinked, allOperations);
                allOperations.addAll(addNewEdgesOfDocumentContributions(rootDocumentRef, arangoInstances, incomingProperty.getDocTypes(), targetDocumentRef, false));
            });
        }
    }

    private void cleanUpRemovedLinks(ArangoDocumentReference rootDocumentRef, DataStage stage, List<DBOperation> allOperations, List<String> newTypeList, Set<String> existingTypes) {
        Set<String> typesToBeRemoved = existingTypes.stream().filter(t -> !newTypeList.contains(t)).collect(Collectors.toSet());
        if (!typesToBeRemoved.isEmpty()) {
            Set<ArangoDocumentReference> incomingSpaceTypeLinksToBeRemoved = findIncomingSpaceTypeLinks(rootDocumentRef, stage, typesToBeRemoved);
            allOperations.addAll(incomingSpaceTypeLinksToBeRemoved.stream().map(DeleteInstanceOperation::new).collect(Collectors.toList()));
        }
    }

    public List<DBOperation> createUpsertOperations(DataStage stage, ArangoDocument targetDocument, EdgeResolutionOperation resolutionOperation) {
        List<DBOperation> dbOperations = new ArrayList<>();
        String propertyName = resolutionOperation.getUpdatedEdge().getOriginalLabel();
        logger.trace(String.format("Create structural upsert operation for property %s in document %s", propertyName, resolutionOperation.getUpdatedEdge().getFrom().getId()));
        ArangoDocument fromDocument = arangoRepositoryCommons.getDocument(stage, resolutionOperation.getUpdatedEdge().getFrom());
        if (fromDocument == null) {
            throw new RuntimeException(String.format("Was not able to find original document based on resolved instance (%s) in stage %s", resolutionOperation.getUpdatedEdge().getFrom().getId(), stage.name()));
        }
        ArangoDocument fromRootDocument = fromDocument;
        if (fromDocument.asIndexedDoc().isEmbedded()) {
            fromRootDocument = arangoRepositoryCommons.getDocument(stage, fromDocument.getOriginalDocument());
        }
        List<ArangoInstance> arangoInstances = staticStructureController.ensureStaticPropertyToTypeEdges(stage, fromRootDocument.getId().getArangoCollectionReference(), targetDocument.getId().getArangoCollectionReference(), propertyName, fromRootDocument.getDoc().types(), targetDocument.getDoc().types(), dbOperations);
        dbOperations.addAll(addNewEdgesOfDocumentContributions(fromRootDocument.getOriginalDocument(), arangoInstances, fromRootDocument.getDoc().types(), targetDocument.getOriginalDocument(), false));
        return dbOperations;
    }

    public List<DBOperation> createUpsertOperations(DataStage stage, ArangoDocumentReference originalDocumentRef, List<String> types, ArangoEdge edge) {
        //There is an edge added. We want to register the outgoing link per property
        List<DBOperation> allOperations = new ArrayList<>();
        ArangoDocument toDocument = arangoRepositoryCommons.getDocument(stage, edge.getTo());
        if (toDocument != null) {
            List<String> targetTypes = toDocument.getDoc().types();
            if (types != null) {
                //Create all edges from document to the structural edges it contributes to.
                List<ArangoInstance> arangoInstances = staticStructureController.ensureStaticPropertyToTypeEdges(stage, originalDocumentRef.getArangoCollectionReference(), toDocument.getId().getArangoCollectionReference(), edge.getOriginalLabel(), types, targetTypes, allOperations);
                allOperations.addAll(addNewEdgesOfDocumentContributions(originalDocumentRef, arangoInstances, arangoRepositoryCommons.getDocument(stage, edge.getFrom()).getDoc().types(), toDocument.getOriginalDocument(), true));
            }
        }
        return allOperations;
    }

    public List<DBOperation> createDeleteOperations(DataStage stage, ArangoDocumentReference documentRef) {
        //We remove all type links pointing to this document (they are unresolved after the removal of the instance).
        return findIncomingSpaceTypeLinks(documentRef, stage, null).stream().map(DeleteInstanceOperation::new).collect(Collectors.toList());
    }


    private List<DBOperation> addNewEdgesOfDocumentContributions(ArangoDocumentReference rootDocumentRef, List<ArangoInstance> arangoInstances, List<String> docTypes, ArangoDocumentReference targetOriginalDocumentRef, boolean attachToOriginalDocument) {
        return arangoInstances.stream().filter(arangoInstance -> arangoInstance instanceof ArangoEdge).map(i -> {
            ArangoEdge structuralEdge = (ArangoEdge) i;
            DocumentRelation edgeFromDocumentToStructuralEdge = new DocumentRelation();
            edgeFromDocumentToStructuralEdge.setFrom(ArangoDocumentReference.fromArangoId("internalunresolved/" + UUID.nameUUIDFromBytes("unspecified".getBytes(StandardCharsets.UTF_8)), true));
            edgeFromDocumentToStructuralEdge.setTo(structuralEdge.getId());
            edgeFromDocumentToStructuralEdge.setOriginalDocument(rootDocumentRef);
            edgeFromDocumentToStructuralEdge.setDocCollection(rootDocumentRef.getArangoCollectionReference().getCollectionName());
            edgeFromDocumentToStructuralEdge.setDocTypes(docTypes);
            edgeFromDocumentToStructuralEdge.setTargetDocument(targetOriginalDocumentRef);
            UUID edgeFromDocId = UUID.randomUUID();
            ArangoDocumentReference edgeDocumentReference = InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION.doc(edgeFromDocId);
            edgeFromDocumentToStructuralEdge.redefineId(edgeDocumentReference);
            return new UpsertOperation(rootDocumentRef, typeUtils.translate(edgeFromDocumentToStructuralEdge.getPayload(), NormalizedJsonLd.class), edgeDocumentReference, true, attachToOriginalDocument);
        }).collect(Collectors.toList());
    }

    public static class IncomingProperty {
        private String origin;
        private String property;
        private List<String> docTypes;

        public String getOrigin() {
            return origin;
        }

        public void setOrigin(String origin) {
            this.origin = origin;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public List<String> getDocTypes() {
            return docTypes;
        }

        public void setDocTypes(List<String> docTypes) {
            this.docTypes = docTypes;
        }
    }


    private Set<IncomingProperty> findIncomingProperties(ArangoDocumentReference targetOriginalDocumentRef, DataStage stage) {
        if (hasCollection(stage, InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION) && hasCollection(stage, InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION)) {
            Map<String, Object> bindVars = new HashMap<>();
            AQL aql = new AQL();
            aql.addLine(AQL.trust("FOR edge IN @@documentRelation"));
            bindVars.put("@documentRelation", InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION.getCollectionName());
            aql.addLine(AQL.trust("FILTER edge." + DocumentRelation.TARGET_ORIGINAL_DOCUMENT + " == @targetOriginalDocument"));
            bindVars.put("targetOriginalDocument", targetOriginalDocumentRef.getId());
            aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION(@propertyToType, edge._to)"));
            bindVars.put("propertyToType", InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION.getCollectionName());
            aql.addLine(AQL.trust("RETURN DISTINCT {\"origin\" : edge." + IndexedJsonLdDoc.ORIGINAL_DOCUMENT + ", \"docTypes\": TO_ARRAY(edge." + IndexedJsonLdDoc.DOC_TYPES + "), \"property\": DOCUMENT(DOCUMENT(edge._to)._from).`" + SchemaOrgVocabulary.IDENTIFIER + "`}"));
            return new HashSet<>(databases.getMetaByStage(stage).query(aql.build().getValue(), bindVars, new AqlQueryOptions(), IncomingProperty.class).asListRemaining());
        }
        return Collections.emptySet();
    }


    private boolean hasCollection(DataStage stage, ArangoCollectionReference reference) {
        return databases.getMetaByStage(stage).collection(reference.getCollectionName()).exists();
    }

    private Set<String> findIncomingSpaceTypeIds(ArangoDocumentReference targetOriginalDocumentRef, DataStage stage) {
        if (hasCollection(stage, InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION) && hasCollection(stage, InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION) && targetOriginalDocumentRef != null) {
            AQL aql = new AQL();
            Map<String, Object> bindVars = new HashMap<>();
            aql.addLine(AQL.trust("FOR d IN @@documentRelation"));
            bindVars.put("@documentRelation", InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION.getCollectionName());
            aql.addLine(AQL.trust("FILTER d." + DocumentRelation.TARGET_ORIGINAL_DOCUMENT + " == @targetDocument"));
            bindVars.put("targetDocument", targetOriginalDocumentRef.getId());
            aql.addLine(AQL.trust("LET doc = DOCUMENT(d._to)"));
            aql.addLine(AQL.trust("FILTER doc." + ArangoVocabulary.COLLECTION + " == @propertyToTypeSpaceName"));
            bindVars.put("propertyToTypeSpaceName", InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION.getCollectionName());
            aql.addLine(AQL.trust("LET linkedSpaceType = DOCUMENT(doc._to)"));
            aql.addLine(AQL.trust("RETURN DISTINCT linkedSpaceType._id"));
            return new HashSet<>(databases.getMetaByStage(stage).query(aql.build().getValue(), bindVars, new AqlQueryOptions(), String.class).asListRemaining());
        }
        return Collections.emptySet();
    }


    private Set<ArangoDocumentReference> findIncomingSpaceTypeLinks(ArangoDocumentReference targetOriginalDocumentRef, DataStage stage, Set<String> spaceTypeIdFilterList) {
        if (hasCollection(stage, InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION) && hasCollection(stage, InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION)) {
            AQL aql = new AQL();
            Map<String, Object> bindVars = new HashMap<>();
            aql.addLine(AQL.trust("FOR d IN @@documentRelation"));
            bindVars.put("@documentRelation", InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION.getCollectionName());
            aql.addLine(AQL.trust("FILTER d." + DocumentRelation.TARGET_ORIGINAL_DOCUMENT + " == @targetDocument"));
            bindVars.put("targetDocument", targetOriginalDocumentRef.getId());
            aql.addLine(AQL.trust("LET doc = DOCUMENT(d._to)"));
            aql.addLine(AQL.trust("FILTER doc." + ArangoVocabulary.COLLECTION + " == @propertyToTypeSpaceName"));
            bindVars.put("propertyToTypeSpaceName", InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION.getCollectionName());
            aql.addLine(AQL.trust("LET linkedSpaceType = DOCUMENT(doc._to)"));
            if (spaceTypeIdFilterList != null) {
                aql.addLine(AQL.trust("FILTER linkedSpaceType._id IN @typeList"));
                bindVars.put("typeList", spaceTypeIdFilterList);
            }
            aql.addLine(AQL.trust("RETURN d._id"));
            List<String> typesWithLinks = databases.getMetaByStage(stage).query(aql.build().getValue(), bindVars, new AqlQueryOptions(), String.class).asListRemaining();
            return typesWithLinks.stream().map(m -> ArangoDocumentReference.fromArangoId(m, true)).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }


}

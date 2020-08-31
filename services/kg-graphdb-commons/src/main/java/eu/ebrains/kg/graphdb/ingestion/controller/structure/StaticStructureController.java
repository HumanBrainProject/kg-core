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

package eu.ebrains.kg.graphdb.ingestion.controller.structure;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.Tuple;
import eu.ebrains.kg.commons.TypeUtils;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.model.Type;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.commons.model.ArangoEdge;
import eu.ebrains.kg.graphdb.commons.model.ArangoInstance;
import eu.ebrains.kg.graphdb.commons.model.MetaRepresentation;
import eu.ebrains.kg.graphdb.ingestion.controller.IdFactory;
import eu.ebrains.kg.graphdb.ingestion.model.DBOperation;
import eu.ebrains.kg.graphdb.ingestion.model.DeleteOperation;
import eu.ebrains.kg.graphdb.ingestion.model.UpsertOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class StaticStructureController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ArangoRepositoryCommons arangoRepositoryCommons;

    public StaticStructureController(ArangoRepositoryCommons arangoRepositoryCommons) {
        this.arangoRepositoryCommons = arangoRepositoryCommons;
    }

    List<ArangoInstance> ensureStaticPropertyToTypeEdges(DataStage stage, ArangoCollectionReference originCollection, ArangoCollectionReference targetCollection, String property, List<String> originTypes, List<String> targetTypes, List<DBOperation> allOperations) {
        List<ArangoInstance> allInstances = new ArrayList<>();
        //We can skip the type and property creation since we know that they already have been created due to the document insertion process
        originTypes.forEach(originType -> {
            targetTypes.forEach(t -> {
                        ArangoDocumentReference targetSpaceType = IdFactory.createDocumentRefForSpaceToTypeEdge(targetCollection.getCollectionName(), t);
                        ArangoDocumentReference originType2Property = IdFactory.createDocumentRefForSpaceTypeToPropertyEdge(originCollection.getCollectionName(), originType, property);
                        ArangoDocumentReference property2targetType = IdFactory.createDocumentRefForSpaceTypePropertyToTypeEdge(originCollection.getCollectionName(), originType, property, targetCollection.getCollectionName(), t);
                        ArangoEdge edge = new ArangoEdge();
                        edge.setFrom(originType2Property);
                        edge.setTo(targetSpaceType);
                        edge.redefineId(property2targetType);
                        allInstances.add(edge);
                    }
            );
        });
        allOperations.addAll(createNonExistingGlobalVerticesAndStructuralEdges(stage, allInstances));
        return allInstances;
    }

    List<ArangoInstance> ensureStaticElementsAndCleanDocumentStructure(DataStage stage, ArangoDocument arangoDocument, ArangoDocumentReference originalDocumentRef, List<DBOperation> allOperations) {
        //find all instances which are required
        logger.debug("prepare global vertices and edges");
        List<ArangoInstance> requiredGlobalArangoInstances = prepareNonExistingGlobalVerticesAndEdges(arangoDocument);

        // ensure representations of found vertices and edges in database
        //Create all non-existing "structural" (document-independent) edges between vertices
        logger.debug("create global vertices and edges");
        List<DBOperation> instancesAndStructuralEdgesToBeCreated = createNonExistingGlobalVerticesAndStructuralEdges(stage, requiredGlobalArangoInstances);
        allOperations.addAll(instancesAndStructuralEdgesToBeCreated);

        if (!arangoRepositoryCommons.doesDocumentExist(stage, arangoDocument.getId())) {
            logger.debug("create document representation");
            MetaRepresentation metaRepresentation = createMetaRepresentation(arangoDocument.getId().toString(), ArangoCollectionReference.fromSpace(InternalSpace.DOCUMENT_SPACE));
            allOperations.add(new UpsertOperation(originalDocumentRef, new NormalizedJsonLd(TypeUtils.translate(metaRepresentation, Map.class)), arangoDocument.getId()));
        } else {
            //Remove all existing edges from the document to the structural edges it has contributed to (e.g. of a previous insertion/update) - this includes outgoing links
            logger.debug("remove existing edges for updating");
            List<DBOperation> previousDocumentContributionsToBeRemoved = removePreviouslyExistingEdgesOfDocumentContributions(stage, originalDocumentRef);
            allOperations.addAll(previousDocumentContributionsToBeRemoved);
        }
        logger.debug("done for static elements and document structure");
        return requiredGlobalArangoInstances;
    }

    private List<DBOperation> createNonExistingGlobalVerticesAndStructuralEdges(DataStage stage, List<ArangoInstance> requiredArangoInstances) {
        Set<ArangoInstance> missingDocuments = arangoRepositoryCommons.checkExistenceOfInstances(stage, requiredArangoInstances, false);
        return missingDocuments.stream().map(d -> new UpsertOperation(null, d.dumpPayload(), d.getId(), false, false)).
                collect(Collectors.toList());
    }

    private List<ArangoInstance> prepareNonExistingGlobalVerticesAndEdges(ArangoDocument arangoDocument) {
        //Find vertices in arango document...
        List<MetaRepresentation> allVertices = new ArrayList<>();
        List<ArangoEdge> allEdges = new ArrayList<>();

        //... find types
        List<String> types = findTypesInDocument(arangoDocument);
        List<MetaRepresentation> typeRepresentations = types.stream().map(t -> createMetaRepresentation(t, ArangoCollectionReference.fromSpace(InternalSpace.TYPES_SPACE))).collect(Collectors.toList());
        allVertices.addAll(typeRepresentations);

        //... find space
        String collectionName = arangoDocument.getId().getArangoCollectionReference().getCollectionName();
        MetaRepresentation spaceRepresentation = createMetaRepresentation(new Space(collectionName).getName(), ArangoCollectionReference.fromSpace(InternalSpace.SPACES_SPACE));
        allVertices.add(spaceRepresentation);


        Map<ArangoDocumentReference, Tuple<String, String>> spaceTypes = new HashMap<>();
        //... link spaces with types
        typeRepresentations.forEach(type -> {
            ArangoEdge edge = new ArangoEdge();
            edge.setFrom(spaceRepresentation.getId());
            edge.setTo(type.getId());
            ArangoDocumentReference documentRefForSpaceToTypeEdge = IdFactory.createDocumentRefForSpaceToTypeEdge(spaceRepresentation.getIdentifier(), type.getIdentifier());
            edge.redefineId(documentRefForSpaceToTypeEdge);
            Tuple<String, String> tuple = new Tuple<>();
            tuple.setA(spaceRepresentation.getIdentifier());
            tuple.setB(type.getIdentifier());
            spaceTypes.put(documentRefForSpaceToTypeEdge, tuple);
            allEdges.add(edge);
        });


        //... find properties
        Map<MetaRepresentation, MetaRepresentation> propertiesWithValueTypes = findPropertiesWithValueTypes(arangoDocument);
        allVertices.addAll(propertiesWithValueTypes.keySet());

        //... find property value data types
        List<MetaRepresentation> propertyValueDataTypeRepresentation = new ArrayList<>(propertiesWithValueTypes.values());
        allVertices.addAll(propertyValueDataTypeRepresentation);

        //Create property representation by type and space of the document
        spaceTypes.keySet().forEach(spaceType -> propertiesWithValueTypes.forEach((property, propertyValueType) -> {
            ArangoEdge propertyInSpaceType = new ArangoEdge();
            Tuple<String, String> tuple = spaceTypes.get(spaceType);
            propertyInSpaceType.redefineId(IdFactory.createDocumentRefForSpaceTypeToPropertyEdge(tuple.getA(), tuple.getB(), property.getIdentifier()));
            propertyInSpaceType.setFrom(spaceType);
            propertyInSpaceType.setTo(property.getId());
            allEdges.add(propertyInSpaceType);

            ArangoEdge property2propertyValueType = new ArangoEdge();
            property2propertyValueType.setFrom(propertyInSpaceType.getId());
            property2propertyValueType.setTo(propertyValueType.getId());
            property2propertyValueType.redefineId(IdFactory.createDocumentRefForPropertyInSpaceTypeToPropertyValueTypeEdge(propertyInSpaceType.getId().getId(), propertyValueType.getIdentifier()));
            allEdges.add(property2propertyValueType);
        }));

        List<ArangoDocument> verticesAsArangoDocs = allVertices.stream().map(v -> ArangoDocument.from(new NormalizedJsonLd(TypeUtils.translate(v, Map.class)))).collect(Collectors.toList());
        List<ArangoInstance> arangoInstances = new ArrayList<>();
        arangoInstances.addAll(verticesAsArangoDocs);
        arangoInstances.addAll(allEdges);
        return arangoInstances;
    }

    private Map<MetaRepresentation, MetaRepresentation> findPropertiesWithValueTypes(ArangoDocument arangoDocument) {
        NormalizedJsonLd doc = arangoDocument.getDoc();
        return doc.keySet().stream().filter(k -> !NormalizedJsonLd.isInternalKey(k)).collect(Collectors.toMap(
                k -> createMetaRepresentation(k, ArangoCollectionReference.fromSpace(InternalSpace.PROPERTIES_SPACE)),
                k -> createMetaRepresentation(getJsonTypeForObject(doc.get(k)), ArangoCollectionReference.fromSpace(InternalSpace.PROPERTY_VALUE_TYPE_SPACE))
        ));
    }

    private List<String> findTypesInDocument(ArangoDocument arangoDocument) {
        return arangoDocument.getDoc().getTypes();
    }

    public static ArangoDocumentReference createDocumentRefForMetaRepresentation(String name, ArangoCollectionReference collection) {
        return collection.doc(IdUtils.createMetaRepresentationUUID(name));
    }

    public static MetaRepresentation createMetaRepresentation(String t, ArangoCollectionReference collectionReference) {
        MetaRepresentation representation = new MetaRepresentation();
        representation.setId(createDocumentRefForMetaRepresentation(t, collectionReference));
        representation.setIdentifier(t);
        representation.setName(Type.labelFromName(t));
        return representation;
    }

    private List<DBOperation> removePreviouslyExistingEdgesOfDocumentContributions(DataStage stage, ArangoDocumentReference originalDocumentRef) {
        List<DBOperation> ops = new ArrayList<>();
        if (arangoRepositoryCommons.doesDocumentExist(stage, originalDocumentRef)) {
            List<ArangoDocumentReference> edgesFromDocument = arangoRepositoryCommons.getEdgesFrom(stage, InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION, originalDocumentRef);
            edgesFromDocument.forEach(edgeFromDocument -> ops.add(new DeleteOperation(edgeFromDocument)));
        }
        return ops;
    }

    private String getJsonTypeForObject(Object o) {
        if (o instanceof List<?>) {
            if (((List) o).isEmpty()) {
                return "string_array";
            } else {
                Object head = ((List) o).get(0);
                if (head instanceof String) {
                    return "string_array";
                } else if (head instanceof Number) {
                    return "number_array";
                } else if (head instanceof JsonLdId) {
                    return "iri_array";
                }
            }
        } else if (o instanceof JsonLdId) {
            return "iri";
        } else if (o instanceof Map) {
            if (((Map) o).containsKey(JsonLdConsts.ID)) {
                return "iri";
            }
            return "object";
        } else if (o instanceof String) {
            try {
                ZonedDateTime.parse((String) o);
            } catch (DateTimeParseException e) {
                return "string";
            }
            return "date";
        } else if (o instanceof Number) {
            return "number";
        } else if (o instanceof Boolean) {
            return "boolean";
        } else if (o == null) {
            return "null";
        }
        return String.format("unknown (%s)", o.getClass());
    }

}

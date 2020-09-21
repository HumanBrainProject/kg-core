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

package eu.ebrains.kg.graphdb.ingestion.controller.semantics;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.TypeUtils;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.commons.model.ArangoEdge;
import eu.ebrains.kg.graphdb.commons.model.MetaRepresentation;
import eu.ebrains.kg.graphdb.ingestion.controller.structure.StaticStructureController;
import eu.ebrains.kg.graphdb.ingestion.model.DBOperation;
import eu.ebrains.kg.graphdb.ingestion.model.UpsertOperation;

import java.util.*;
import java.util.stream.Collectors;

public abstract class SemanticsHandler {

    protected TypeUtils typeUtils;

    public SemanticsHandler(TypeUtils typeUtils) {
        this.typeUtils = typeUtils;
    }

    public List<DBOperation> createUpsertOperations(DataStage stage, ArangoDocumentReference rootDocumentRef, ArangoDocument document){
        return Collections.emptyList();
    }

    List<DBOperation> handleOverrideReference(ArangoDocumentReference rootDocumentRef, ArangoDocument document, JsonLdId originalTo, ArangoDocumentReference targetEdge, ArangoCollectionReference edgeCollection) {
        ArangoEdge edge = new ArangoEdge();
        edge.setTo(targetEdge);
        edge.setFrom(document.getId());
        edge.setOriginalTo(originalTo);
        edge.setOriginalDocument(rootDocumentRef);
        ArangoDocumentReference edgeId = edgeCollection.doc(UUID.randomUUID());
        edge.redefineId(edgeId);
        UpsertOperation edgeOperation = new UpsertOperation(rootDocumentRef, typeUtils.translate(edge.getPayload(), NormalizedJsonLd.class), edgeId);
        UpsertOperation definition = new UpsertOperation(rootDocumentRef, document.getDoc(), document.getId());
        return Arrays.asList(definition, edgeOperation);
    }

    List<DBOperation> handleExplicitlyStatedTargetTypes(ArangoDocument document, ArangoDocumentReference propertyRef){
        List<String> types = document.getDoc().getAsListOf(EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES, String.class);
        if(types!=null){
            //explicitly stated type connections
            return types.stream().map(t -> {
                ArangoCollectionReference typeReference = ArangoCollectionReference.fromSpace(InternalSpace.TYPES_SPACE);
                MetaRepresentation metaRepresentation = StaticStructureController.createMetaRepresentation(t, typeReference);
                UpsertOperation upsertTypeOperation = new UpsertOperation(null, new NormalizedJsonLd(typeUtils.translate(metaRepresentation, Map.class)), metaRepresentation.getIdRef(), false, false);
                ArangoEdge edge = new ArangoEdge();
                edge.setTo(metaRepresentation.getIdRef());
                edge.setFrom(propertyRef);
                ArangoDocumentReference edgeId =  InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION.doc(UUID.randomUUID());
                edge.redefineId(edgeId);
                return Arrays.asList(upsertTypeOperation, new UpsertOperation(null, typeUtils.translate(edge, NormalizedJsonLd.class), edgeId, false, false));
            }).flatMap(List::stream).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}

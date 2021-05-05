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
import eu.ebrains.kg.arango.commons.model.InternalSpace;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class IdFactory {

    public static ArangoDocumentReference createDocumentRefForSpaceTypeToPropertyEdge(ArangoCollectionReference collectionReference, String typeName, String propertyName) {
        return InternalSpace.TYPE_TO_PROPERTY_EDGE_COLLECTION.doc(UUID.nameUUIDFromBytes((collectionReference.getCollectionName() + "_" + typeName + "_" + propertyName).getBytes(StandardCharsets.UTF_8)));
    }

    public static ArangoDocumentReference createDocumentRefForGlobalTypeToPropertyEdge(String typeName, String propertyName) {
        return InternalSpace.GLOBAL_TYPE_TO_PROPERTY_EDGE_COLLECTION.doc(UUID.nameUUIDFromBytes((typeName + "_" + propertyName).getBytes(StandardCharsets.UTF_8)));
    }

    public static ArangoDocumentReference createDocumentRefForSpaceTypePropertyToTypeEdge(ArangoCollectionReference originCollection, String originTypeName, String propertyName, ArangoCollectionReference targetCollection, String targetTypeName) {
        return InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION.doc(UUID.nameUUIDFromBytes((originCollection.getCollectionName() + "_" + originTypeName + "_" + propertyName + "_" + targetCollection.getCollectionName() + "_" + targetTypeName).getBytes(StandardCharsets.UTF_8)));
    }

    public static ArangoDocumentReference createDocumentRefForPropertyInSpaceTypeToPropertyValueTypeEdge(String propertyInSpaceType, String propertyValueTypeName) {
        return InternalSpace.PROPERTY_TO_PROPERTY_VALUE_TYPE_EDGE_COLLECTION.doc(UUID.nameUUIDFromBytes((propertyInSpaceType + "_" + propertyValueTypeName).getBytes(StandardCharsets.UTF_8)));
    }

    public static ArangoDocumentReference createDocumentRefForSpaceToTypeEdge(ArangoCollectionReference collectionReference, String typeName) {
        return InternalSpace.SPACE_TO_TYPE_EDGE_COLLECTION.doc(UUID.nameUUIDFromBytes((collectionReference.getCollectionName() + "_" + typeName).getBytes(StandardCharsets.UTF_8)));
    }
}

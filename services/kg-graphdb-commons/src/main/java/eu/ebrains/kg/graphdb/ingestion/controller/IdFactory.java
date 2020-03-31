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
import eu.ebrains.kg.arango.commons.model.InternalSpace;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class IdFactory {

    public static ArangoDocumentReference createDocumentRefForSpaceTypeToPropertyEdge(String collectionName, String typeName, String propertyName) {
        return InternalSpace.TYPE_TO_PROPERTY_EDGE_COLLECTION.doc(UUID.nameUUIDFromBytes((collectionName + "_" + typeName + "_" + propertyName).getBytes(StandardCharsets.UTF_8)));
    }

    public static ArangoDocumentReference createDocumentRefForSpaceTypePropertyToTypeEdge(String originCollectionName, String originTypeName, String propertyName, String targetCollectionName, String targetTypeName) {
        return InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION.doc(UUID.nameUUIDFromBytes((originCollectionName + "_" + originTypeName + "_" + propertyName + "_" + targetCollectionName + "_" + targetTypeName).getBytes(StandardCharsets.UTF_8)));
    }

    public static ArangoDocumentReference createDocumentRefForPropertyInSpaceTypeToPropertyValueTypeEdge(String propertyInSpaceType, String propertyValueTypeName) {
        return InternalSpace.PROPERTY_TO_PROPERTY_VALUE_TYPE_EDGE_COLLECTION.doc(UUID.nameUUIDFromBytes((propertyInSpaceType + "_" + propertyValueTypeName).getBytes(StandardCharsets.UTF_8)));
    }

    public static ArangoDocumentReference createDocumentRefForSpaceToTypeEdge(String spaceName, String typeName) {
        return InternalSpace.SPACE_TO_TYPE_EDGE_COLLECTION.doc(UUID.nameUUIDFromBytes((spaceName + "_" + typeName).getBytes(StandardCharsets.UTF_8)));
    }
}

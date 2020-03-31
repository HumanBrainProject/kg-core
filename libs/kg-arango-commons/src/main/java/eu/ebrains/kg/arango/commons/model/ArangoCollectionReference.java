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

package eu.ebrains.kg.arango.commons.model;

import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoKey;
import eu.ebrains.kg.commons.model.Space;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class ArangoCollectionReference {

    private String collectionName;

    private Boolean edge;

    public String getCollectionName() {
        return collectionName;
    }

    public Boolean isEdge() {
        return edge;
    }



    protected ArangoCollectionReference() {
    }

    public ArangoCollectionReference(String collectionName, Boolean edge) {
        this.collectionName = new ArangoKey(collectionName).getValue();
        this.edge = edge;
    }

    public ArangoDocumentReference docWithStableId(String stableId){
        return doc(UUID.nameUUIDFromBytes((getCollectionName()+"-"+stableId).getBytes(StandardCharsets.UTF_8)));
    }

    public ArangoDocumentReference doc(UUID uuid){
        return new ArangoDocumentReference(this, uuid);
    }

    public static ArangoCollectionReference fromSpace(Space space) {
        return fromSpace(space, false);
    }

    public static ArangoCollectionReference fromSpace(Space space, boolean edge) {
        return space == null ? null : new ArangoCollectionReference(space.getName(), edge);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArangoCollectionReference that = (ArangoCollectionReference) o;
        //The edge boolean is explicitly excluded from the equals, since sometimes we don't have this information available.
        return Objects.equals(collectionName, that.collectionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collectionName);
    }
}

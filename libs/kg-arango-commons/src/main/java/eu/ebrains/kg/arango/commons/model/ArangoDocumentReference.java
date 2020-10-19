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

import eu.ebrains.kg.commons.jsonld.InstanceId;

import java.util.Objects;
import java.util.UUID;

public class ArangoDocumentReference {

    private final ArangoCollectionReference arangoCollectionReference;
    private final UUID key;

    public ArangoDocumentReference(ArangoCollectionReference arangoCollectionReference, UUID key) {
        this.arangoCollectionReference = arangoCollectionReference;
        this.key = key;
    }

    public ArangoCollectionReference getArangoCollectionReference() {
        return arangoCollectionReference;
    }

    public UUID getDocumentId() {
        return key;
    }

    public String getId() {
        return String.format("%s/%s", arangoCollectionReference.getCollectionName(),  getDocumentId());
    }

    public static ArangoDocumentReference fromArangoId(String arangoId, Boolean isEdge) {
        if(arangoId!=null) {
            String[] split = arangoId.split("/");
            if (split.length == 2) {
                return new ArangoDocumentReference(new ArangoCollectionReference(split[0], isEdge), UUID.fromString(split[1]));
            }
        }
        return null;
    }

    public static ArangoDocumentReference fromInstanceId(InstanceId instanceId) {
        return instanceId != null ? new ArangoDocumentReference(ArangoCollectionReference.fromSpace(instanceId.getSpace()), instanceId.getUuid()) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArangoDocumentReference that = (ArangoDocumentReference) o;
        return Objects.equals(arangoCollectionReference, that.arangoCollectionReference) &&
                Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arangoCollectionReference, key);
    }
}

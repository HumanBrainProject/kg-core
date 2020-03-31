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

package eu.ebrains.kg.commons.jsonld;

import eu.ebrains.kg.commons.model.Space;

import java.util.Objects;
import java.util.UUID;

/**
 * This is the id of an instance. An instance represents an entity in a specific space and can be created out of multiple documents
 */
public class InstanceId {
    public InstanceId() {
    }

    public InstanceId(UUID id, Space space){
        this(id, space, false);
    }

    public InstanceId(UUID id, Space space, boolean deprecated) {
        this.uuid = id;
        this.space = space;
        this.deprecated = deprecated;
    }

    private UUID uuid;
    private Space space;
    private boolean deprecated;
    private transient boolean  unresolved;

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public boolean isUnresolved() {
        return unresolved;
    }

    public void setUnresolved(boolean unresolved) {
        this.unresolved = unresolved;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Space getSpace() {
        return space;
    }

    public String serialize(){
        return (space != null ? space.getName() : "")+"/"+uuid;
    }

    public static InstanceId deserialize(String instanceId){
        if(instanceId!=null && !instanceId.trim().isEmpty()) {
            int i = instanceId.lastIndexOf("/");
            if (i != -1) {
                String space = instanceId.substring(0, i).trim();
                String uuid = instanceId.substring(i + 1).trim();
                return new InstanceId(UUID.fromString(uuid), new Space(space));
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstanceId that = (InstanceId) o;
        return Objects.equals(uuid, that.uuid) &&
                Objects.equals(space, that.space);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, space);
    }
}

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

package eu.ebrains.kg.commons.jsonld;

import eu.ebrains.kg.commons.model.SpaceName;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * This is the id of an instance. An instance represents an entity in a specific space and can be created out of multiple documents
 */
public class InstanceId implements Serializable {
    public InstanceId() {
    }


    public InstanceId(UUID id, SpaceName space) {
        this.uuid = id;
        this.space = space;
    }

    private UUID uuid;
    private SpaceName space;
    private transient boolean unresolved;

    public boolean isUnresolved() {
        return unresolved;
    }

    public void setUnresolved(boolean unresolved) {
        this.unresolved = unresolved;
    }

    public UUID getUuid() {
        return uuid;
    }

    public SpaceName getSpace() {
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
                try {
                    return new InstanceId(UUID.fromString(uuid), new SpaceName(space));
                }
                catch(IllegalArgumentException e){
                    //If the uuid is not a real UUID, this is not a valid instance id
                    return null;
                }
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

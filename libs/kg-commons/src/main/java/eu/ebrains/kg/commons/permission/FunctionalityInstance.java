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

package eu.ebrains.kg.commons.permission;

import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.SpaceName;

import java.util.Objects;
import java.util.UUID;

/**
 * A functionality instance carries the functionality which can be executed either globally (neither space nor id) for a specific space or for a specific instance (id)
 */
public class FunctionalityInstance {
    private Functionality functionality;
    private SpaceName space;
    private UUID id;

    private FunctionalityInstance(){}

    public boolean appliesTo(SpaceName space, UUID id){
        if(this.space == null && this.id == null){
            return true;
        }
        else if(this.space!=null && this.id == null && this.space.equals(space)){
            return true;
        }
        else if(this.space!=null && this.id != null && this.space.equals(space) && this.id.equals(id)){
            return true;
        }
        return false;
    }

    public FunctionalityInstance(Functionality functionality, SpaceName space, UUID id) {
        this.functionality = functionality;
        this.space = space;
        this.id = id;
    }

    public static String getRolePatternForSpace(SpaceName space) {
        return String.format("%s\\:.*", space.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionalityInstance that = (FunctionalityInstance) o;
        return functionality == that.functionality &&
                Objects.equals(space, that.space) &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionality, space, id);
    }

    public Functionality getFunctionality() {
        return functionality;
    }

    public SpaceName getSpace() {
        return space;
    }

    public UUID getId() {
        return id;
    }

    public InstanceId getInstanceId() {
        if (space != null && id != null) {
            return new InstanceId(id, space);
        }
        return null;
    }
}

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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.commons.model;

import java.util.Set;
import java.util.UUID;

public class IdWithAlternatives {

    private String requestKey;
    private UUID id;
    private String space;
    private Set<String> alternatives;
    private transient boolean found;

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public IdWithAlternatives() {
    }

    public IdWithAlternatives(UUID id, SpaceName space, Set<String> alternatives) {
        this.id = id;
        this.space = space != null ? space.getName() : null;
        this.alternatives = alternatives;
        this.requestKey = requestKey;
    }

    public String getSpace() {
        return space;
    }

    public IdWithAlternatives setSpace(String space) {
        this.space = space;
        return this;
    }

    public UUID getId() {
        return id;
    }

    public IdWithAlternatives setId(UUID id) {
        this.id = id;
        return this;
    }

    public Set<String> getAlternatives() {
        return alternatives;
    }

    public IdWithAlternatives setAlternatives(Set<String> alternatives) {
        this.alternatives = alternatives;
        return this;
    }
}

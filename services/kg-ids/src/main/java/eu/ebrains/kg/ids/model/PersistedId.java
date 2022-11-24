/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

package eu.ebrains.kg.ids.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.ebrains.kg.arango.commons.aqlbuilder.ArangoVocabulary;
import eu.ebrains.kg.commons.model.SpaceName;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PersistedId {

    @JsonProperty(ArangoVocabulary.KEY)
    private String key;

    @JsonProperty(ArangoVocabulary.SPACE)
    private String space;

    private Set<String> alternativeIds = new HashSet<>();


    public String getKey() {
        return key;
    }

    public UUID getUUID() {
        return key!=null ? UUID.fromString(key) : null;
    }

    public PersistedId setUUID(UUID id) {
        this.key = id !=null ? id.toString() : null;
        return this;
    }

    public SpaceName getSpace() {
        return space != null ? new SpaceName(space) : null;
    }

    public void setSpace(SpaceName space) {
        this.space = space != null ? space.getName() : null;
    }

    public Set<String> getAlternativeIds() {
        return alternativeIds;
    }

    public PersistedId setAlternativeIds(Set<String> alternativeIds) {
        this.alternativeIds = alternativeIds;
        return this;
    }
}

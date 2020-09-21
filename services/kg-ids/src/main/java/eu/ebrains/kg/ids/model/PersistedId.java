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

package eu.ebrains.kg.ids.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.commons.model.Space;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PersistedId {

    @JsonProperty(ArangoVocabulary.KEY)
    private String key;

    @JsonProperty(ArangoVocabulary.SPACE)
    private String space;

    private Set<String> alternativeIds = new HashSet<>();

    private boolean deprecated;

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

    public Space getSpace() {
        return space != null ? new Space(space) : null;
    }

    public void setSpace(Space space) {
        this.space = space != null ? space.getName() : null;
    }

    public Set<String> getAlternativeIds() {
        return alternativeIds;
    }

    public PersistedId setAlternativeIds(Set<String> alternativeIds) {
        this.alternativeIds = alternativeIds;
        return this;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }
}

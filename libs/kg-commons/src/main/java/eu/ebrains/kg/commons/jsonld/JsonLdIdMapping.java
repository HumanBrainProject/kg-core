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

import java.util.Set;
import java.util.UUID;

public class JsonLdIdMapping {
    private String space;
    private UUID requestedId;
    private boolean deprecated;
    private Set<JsonLdId> resolvedIds;

    public JsonLdIdMapping() {
    }

    public JsonLdIdMapping(UUID requestedId, Set<JsonLdId> resolvedIds, SpaceName space, boolean deprecated) {
        this.requestedId = requestedId;
        this.resolvedIds = resolvedIds;
        this.deprecated = deprecated;
        this.space = space != null ? space.getName() : null;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public SpaceName getSpace(){
        return space!=null ? new SpaceName(space) : null;
    }

    public UUID getRequestedId() {
        return requestedId;
    }

    public Set<JsonLdId> getResolvedIds() {
        return resolvedIds;
    }

}


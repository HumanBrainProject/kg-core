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

import java.util.Set;
import java.util.UUID;

public class JsonLdIdMapping {
    private String space;
    private UUID requestedId;
    private boolean deprecated;
    private Set<JsonLdId> resolvedIds;

    public JsonLdIdMapping() {
    }

    public JsonLdIdMapping(UUID requestedId, Set<JsonLdId> resolvedIds, Space space, boolean deprecated) {
        this.requestedId = requestedId;
        this.resolvedIds = resolvedIds;
        this.deprecated = deprecated;
        this.space = space != null ? space.getName() : null;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public Space getSpace(){
        return space!=null ? new Space(space) : null;
    }

    public UUID getRequestedId() {
        return requestedId;
    }

    public Set<JsonLdId> getResolvedIds() {
        return resolvedIds;
    }

}


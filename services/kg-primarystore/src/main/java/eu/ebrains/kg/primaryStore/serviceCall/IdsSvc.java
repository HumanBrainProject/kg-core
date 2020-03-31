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

package eu.ebrains.kg.primaryStore.serviceCall;

import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import eu.ebrains.kg.commons.model.Space;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class IdsSvc {

    private final ServiceCall serviceCall;
    private final IdUtils idUtils;

    public IdsSvc(ServiceCall serviceCall, IdUtils idUtils) {
        this.serviceCall = serviceCall;
        this.idUtils = idUtils;
    }

    private final static String SERVICE_URL = "http://kg-ids";

    public List<JsonLdId> deprecateInstance(UUID instance, AuthTokens authTokens) {
        JsonLdId[] result = serviceCall.delete(String.format("%s/ids/%s/%s", SERVICE_URL, DataStage.LIVE.name(), instance), authTokens, JsonLdId[].class);
        if (result != null) {
            return Arrays.asList(result);
        }
        return Collections.emptyList();
    }

    public List<JsonLdId> upsert(DataStage stage, IdWithAlternatives idWithAlternatives, AuthTokens authTokens) {
        JsonLdId[] result = serviceCall.post(String.format("%s/ids/%s", SERVICE_URL, stage.name()), idWithAlternatives, authTokens, JsonLdId[].class);
        if (result != null) {
            return Arrays.asList(result);
        }
        return Collections.emptyList();
    }

    public Set<JsonLdId> resolveIds(DataStage stage, UUID id, Set<String> alternativeIds, Space space, AuthTokens authTokens) {
        IdWithAlternatives idWithAlternatives = new IdWithAlternatives();
        idWithAlternatives.setId(id);
        idWithAlternatives.setSpace(space.getName());
        idWithAlternatives.setAlternatives(alternativeIds);
        JsonLdIdMapping[] result = serviceCall.post(String.format("%s/ids/%s/resolved", SERVICE_URL, stage.name()), Collections.singletonList(idWithAlternatives), authTokens, JsonLdIdMapping[].class);
        if (result == null || result.length == 0) {
            return null;
        }
        if (result.length > 1) {
            throw new RuntimeException("Received multiple responses although I was only asking for a single id. There is something totally wrong!");
        }
        JsonLdIdMapping jsonLdIdMapping = result[0];
        String absoluteId = idUtils.buildAbsoluteUrl(id).getId();
        if (jsonLdIdMapping.getRequestedId() == null || !jsonLdIdMapping.getRequestedId().equals(id)) {
            throw new RuntimeException(String.format("Did receive a result - but instead of id %s, I received a value for %s", absoluteId, jsonLdIdMapping.getRequestedId()));
        } else {
            if (jsonLdIdMapping.getResolvedIds() == null || jsonLdIdMapping.getResolvedIds().isEmpty()) {
                return null;
            }
            return jsonLdIdMapping.getResolvedIds();
        }
    }
}
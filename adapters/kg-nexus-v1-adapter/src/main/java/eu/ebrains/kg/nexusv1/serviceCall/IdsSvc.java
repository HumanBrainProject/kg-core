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

package eu.ebrains.kg.nexusv1.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

@Component
public class IdsSvc {

    private final ServiceCall serviceCall;
    private final AuthContext authContext;
    private final IdUtils idUtils;

    public IdsSvc(ServiceCall serviceCall, AuthContext authContext, IdUtils idUtils) {
        this.serviceCall = serviceCall;
        this.authContext = authContext;
        this.idUtils = idUtils;
    }

    private final static String SERVICE_URL = "http://kg-ids";


    public JsonLdIdMapping resolveId(DataStage stage, String idToLookup) {
        IdWithAlternatives idWithAlternatives = new IdWithAlternatives();
        idWithAlternatives.setId(UUID.randomUUID());
        idWithAlternatives.setAlternatives(Collections.singleton(idToLookup));
        JsonLdIdMapping[] result = serviceCall.post(String.format("%s/ids/%s/resolved", SERVICE_URL, stage.name()), Collections.singletonList(idWithAlternatives), authContext.getAuthTokens(), JsonLdIdMapping[].class);
        if (result == null || result.length == 0) {
            return null;
        }
        if (result.length > 1) {
            throw new RuntimeException("Received multiple responses although I was only asking for a single id. There is something totally wrong!");
        }
        return result[0];
    }
}
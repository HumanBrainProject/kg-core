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

package eu.ebrains.kg.graphdb.commons.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class IdsSvc {

    private final ServiceCall serviceCall;
    private final AuthContext authContext;

    public IdsSvc(ServiceCall serviceCall, AuthContext authContext) {
        this.serviceCall = serviceCall;
        this.authContext = authContext;
    }

    private final static String SERVICE_URL = "http://kg-ids";

    public List<JsonLdIdMapping> resolveIds(DataStage stage, List<IdWithAlternatives> ids) {
        if(ids.isEmpty()){
            return Collections.emptyList();
        }
        JsonLdIdMapping[] result = serviceCall.post(String.format("%s/ids/%s/resolved", SERVICE_URL, stage.name()), ids, authContext.getAuthTokens(), JsonLdIdMapping[].class);
        if (result == null || result.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(result);
    }
}

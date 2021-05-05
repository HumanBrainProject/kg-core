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

package eu.ebrains.kg.coreToQueryComparison.serviceCall;

import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.ServiceCallWithClientSecret;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CoreSvc {

    private final ServiceCallWithClientSecret serviceCallWithClientSecret;
    private final static String SERVICE_URL = "http://kg-core-api/"+ Version.API;

    public CoreSvc(ServiceCallWithClientSecret serviceCallWithClientSecret, @Value("${eu.ebrains.kg.test.queryEndpoint}") String queryEndpoint) {
        this.serviceCallWithClientSecret = serviceCallWithClientSecret;
    }

    public PaginatedResultOfDocuments getInstances(Type type, DataStage stage) {
        return serviceCallWithClientSecret.get(String.format("%s/instances?type=%s&stage=%s", SERVICE_URL, type.getEncodedName(), stage.name()), new AuthTokens(), PaginatedResultOfDocuments.class);
    }

    public Result<List<NormalizedJsonLd>> getInstancesByIdentifier(String identifier){
        return serviceCallWithClientSecret.post(String.format("%s/instancesByIdentifiers?stage=LIVE&returnEmbedded=true", SERVICE_URL), Collections.singleton(identifier), new AuthTokens(), ResultOfDocuments.class);
    }

}

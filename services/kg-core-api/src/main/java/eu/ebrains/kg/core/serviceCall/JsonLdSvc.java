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

package eu.ebrains.kg.core.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import org.springframework.stereotype.Component;

@Component
public class JsonLdSvc {
    private final ServiceCall serviceCall;
    private final AuthContext authContext;

    public JsonLdSvc(ServiceCall serviceCall, AuthContext authContext) {
        this.serviceCall = serviceCall;
        this.authContext = authContext;
    }

    public NormalizedJsonLd toNormalizedJsonLd(JsonLdDoc payload) {
        return serviceCall.post("http://kg-jsonld/jsonld", payload, authContext.getAuthTokens(), NormalizedJsonLd.class);
    }

}

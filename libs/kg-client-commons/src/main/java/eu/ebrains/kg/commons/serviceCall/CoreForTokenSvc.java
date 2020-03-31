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

package eu.ebrains.kg.commons.serviceCall;

import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.ResultOfDocument;
import org.springframework.stereotype.Component;

@Component
public class CoreForTokenSvc {

    private final ServiceCall serviceCall;
    private final static String SERVICE_URL = "http://kg-core-api/"+ Version.API;

    public CoreForTokenSvc(ServiceCall serviceCall) {
        this.serviceCall = serviceCall;
    }

    public String getTokenEndpoint(){
        Result<NormalizedJsonLd> resultOfDocument = serviceCall.get(String.format("%s/users/authorization/tokenEndpoint", SERVICE_URL), null, ResultOfDocument.class);
        return resultOfDocument!=null && resultOfDocument.getData()!=null ? resultOfDocument.getData().getAs("endpoint", String.class) : null;
    }
}

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

import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.Space;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class PrimaryStoreSvc {

    private final ServiceCall serviceCall;

    public PrimaryStoreSvc(ServiceCall serviceCall) {
        this.serviceCall = serviceCall;
    }

    public List<InstanceId> postEvent(Event event, boolean deferInference, AuthTokens authTokens) {
        return Arrays.asList(serviceCall.post(String.format("http://kg-primarystore/events?deferInference=%b", deferInference), event, authTokens, InstanceId[].class));
    }

    public void inferInstance(Space space, UUID id, AuthTokens authTokens) {
        serviceCall.post(String.format("http://kg-primarystore/events/inference/%s/%s", space.getName(), id), null, authTokens, String.class);
    }

    public void inferDeferred(AuthTokens authTokens) {
        serviceCall.post("http://kg-primarystore/events/inference/deferred", null, authTokens, String.class);
    }

}

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

package eu.ebrains.kg.admin.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.Event;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class AdminToPrimaryStore {

    private final ServiceCall serviceCall;

    private final AuthContext authContext;

    public AdminToPrimaryStore(ServiceCall serviceCall, AuthContext authContext) {
        this.serviceCall = serviceCall;
        this.authContext = authContext;
    }

    private final static String EVENTS_ENDPOINT = "http://kg-primarystore/internal/primaryStore/events";

    public List<InstanceId> postEvent(Event event, boolean deferInference) {
        InstanceId[] result = serviceCall.post(String.format("%s?deferInference=%b", EVENTS_ENDPOINT, deferInference), event, authContext.getAuthTokens(), InstanceId[].class);
        return result!=null ? Arrays.asList(result) : Collections.emptyList();
    }
}

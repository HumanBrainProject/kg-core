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

import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.SSEService;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.models.EventStoreSvc;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component("nexus-v1-event-store")
public class PrimaryStoreSvc implements EventStoreSvc {

    @Autowired
    SSEService sseService;

    @Autowired
    ServiceCall serviceCall;

    @Value("${eu.ebrains.kg.nexus.endpoint}")
    private String EXTERNAL_SSE;

    private String SERVICE_ENDPOINT = "http://kg-primarystore";

    public void pushToStore(Event event, Space space) {
        serviceCall.post(String.format("%s/events/%s", SERVICE_ENDPOINT, space.getName()), event, new AuthTokens(), Void.class);
    }

    @Override
    public Flux<ServerSentEvent> connectToSSE(String lastEventId, ClientAuthToken authToken) {
        return sseService.sse(EXTERNAL_SSE + "/resources/events", lastEventId, false, authToken);
    }
}

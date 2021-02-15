/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.commons.api.rest;

import eu.ebrains.kg.commons.AuthTokenContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.api.PrimaryStoreEvents;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.PersistedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.*;

@RestClient
public class PrimaryStoreEventsRestClient implements PrimaryStoreEvents.Client {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static String SERVICE_URL = "http://kg-primarystore/internal/primaryStore";
    private final ServiceCall serviceCall;
    private final AuthTokenContext authTokenContext;

    public PrimaryStoreEventsRestClient(ServiceCall serviceCall, AuthTokenContext authTokenContext) {
        this.serviceCall = serviceCall;
        this.authTokenContext = authTokenContext;
    }

    @Override
    public Flux<ServerSentEvent<PersistedEvent>> streamEvents(DataStage stage, String lastEventId, String lastEventIdFromQuery) {
        return null;
    }

    @Override
    public Long getNumberOfRegisteredEvents(DataStage stage) {
        return null;
    }

    @Override
    public List<PersistedEvent> getEventsSince(DataStage stage, String lastEventId) {
        return null;
    }

    @Override
    public Set<InstanceId> postEvent(Event event, boolean deferInference) {
        return new HashSet<>(Arrays.asList(serviceCall.post(String.format("%s/events", SERVICE_URL), event, authTokenContext.getAuthTokens(), InstanceId[].class)));
    }

    @Override
    public void inferDeferred(String space, boolean sync) {
        serviceCall.post(String.format("%s/events/inference/deferred/%s?sync=%b",
                SERVICE_URL, space, sync),
                null, authTokenContext.getAuthTokens(), String.class);
    }

    @Override
    public void infer(String space, UUID id) {
        serviceCall.post(String.format("%s/events/inference/%s/%s",
                SERVICE_URL, space, id), null, authTokenContext.getAuthTokens(),
                String.class);
    }
}

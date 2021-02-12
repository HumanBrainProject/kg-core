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
import eu.ebrains.kg.commons.api.Indexing;
import eu.ebrains.kg.commons.model.PersistedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestClient
public class IndexingRestClient implements Indexing.Client {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static String SERVICE_URL = "http://kg-indexing/internal/indexing";
    private final ServiceCall serviceCall;
    private final AuthTokenContext authTokenContext;

    public IndexingRestClient(ServiceCall serviceCall, AuthTokenContext authTokenContext) {
        this.serviceCall = serviceCall;
        this.authTokenContext = authTokenContext;
    }

    @Override
    public void indexEvent(PersistedEvent event) {
        logger.trace(String.format("Sending event %s to indexing", event.getEventId()));
        serviceCall.post(String.format("%s/event", SERVICE_URL), event, authTokenContext.getAuthTokens(), Void.class);
    }
}

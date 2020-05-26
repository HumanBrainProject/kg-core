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

package eu.ebrains.kg.primaryStore.serviceCall;

import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.model.PersistedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PrimaryStoreToIndexing {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServiceCall serviceCall;

    public PrimaryStoreToIndexing(ServiceCall serviceCall) {
        this.serviceCall = serviceCall;
    }

    public void indexEvent(PersistedEvent event, AuthTokens authTokens) {
        logger.trace(String.format("Sending event %s to indexing", event.getEventId()));
        serviceCall.post("http://kg-indexing/internal/indexing/event", event, authTokens, Void.class);
    }

}

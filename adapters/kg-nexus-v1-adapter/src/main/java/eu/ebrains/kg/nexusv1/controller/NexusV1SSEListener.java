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

package eu.ebrains.kg.nexusv1.controller;

import com.google.gson.Gson;
import eu.ebrains.kg.commons.SSEListener;
import eu.ebrains.kg.commons.models.EventProcessor;
import eu.ebrains.kg.commons.models.EventStoreSvc;
import eu.ebrains.kg.commons.models.EventTracker;
import eu.ebrains.kg.nexusv1.models.NexusV1Event;
import eu.ebrains.kg.nexusv1.serviceCall.JsonLdSvc;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;

@Component
public class NexusV1SSEListener extends SSEListener<NexusV1Event> {

    private final Gson gson;
    private final JsonLdSvc jsonLdSvc;

    public NexusV1SSEListener(@Qualifier("nexus-v1-event-store") EventStoreSvc primaryStoreSvc, @Qualifier("nexus-v1-event-processor") EventProcessor eventProcessor, @Qualifier("nexus-v1-event-tracker") EventTracker eventTracker, Gson gson, JsonLdSvc jsonLdSvc) {
        super(primaryStoreSvc, eventProcessor, eventTracker, gson);
        this.gson = gson;
        this.jsonLdSvc = jsonLdSvc;
    }

    @Override
    protected NexusV1Event fromServerSentEvent(ServerSentEvent serverSentEvent) {
        NexusV1Event nexusV1Event = gson.fromJson(gson.toJson(serverSentEvent), NexusV1Event.class);
        return nexusV1Event;

    }
}

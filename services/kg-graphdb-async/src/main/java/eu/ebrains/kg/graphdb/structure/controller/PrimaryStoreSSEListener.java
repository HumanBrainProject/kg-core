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

package eu.ebrains.kg.graphdb.structure.controller;

import com.google.gson.Gson;
import eu.ebrains.kg.commons.SSEListener;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.models.EventProcessor;
import eu.ebrains.kg.commons.models.EventStoreSvc;
import eu.ebrains.kg.commons.models.EventTracker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;

@Component
public class PrimaryStoreSSEListener extends SSEListener<PersistedEvent> {

    private final Gson gson;

    public PrimaryStoreSSEListener(@Qualifier("main-event-store") EventStoreSvc primaryStoreSvc, @Qualifier("main-event-processor") EventProcessor eventProcessor, @Qualifier("main-event-tracker") EventTracker eventTracker, Gson gson) {
        super(primaryStoreSvc, eventProcessor, eventTracker, gson);
        this.gson = gson;
    }

    @Override
    protected PersistedEvent fromServerSentEvent(ServerSentEvent serverSentEvent) {
         return gson.fromJson(gson.toJson(serverSentEvent.data()), PersistedEvent.class);
    }
}

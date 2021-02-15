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

package eu.ebrains.kg.primaryStore.api;

import eu.ebrains.kg.commons.api.PrimaryStoreEvents;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.primaryStore.controller.EventProcessor;
import eu.ebrains.kg.primaryStore.controller.EventRepository;
import eu.ebrains.kg.primaryStore.controller.InferenceProcessor;
import eu.ebrains.kg.primaryStore.controller.SSEProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class PrimaryStoreEventsAPI implements PrimaryStoreEvents.Client {

    private final EventProcessor eventProcessor;

    private final InferenceProcessor inferenceProcessor;

    private final SSEProducer sseProducer;

    private final EventRepository eventRepository;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PrimaryStoreEventsAPI(EventProcessor eventProcessor, SSEProducer sseProducer, EventRepository eventRepository, InferenceProcessor inferenceProcessor) {
        this.eventProcessor = eventProcessor;
        this.sseProducer = sseProducer;
        this.eventRepository = eventRepository;
        this.inferenceProcessor = inferenceProcessor;
    }

    @Override
    public Flux<ServerSentEvent<PersistedEvent>> streamEvents(DataStage stage, String lastEventId, String lastEventIdFromQuery) {
        return sseProducer.initiateNewFlux(stage, lastEventId != null ? lastEventId : lastEventIdFromQuery);
    }

    @Override
    public Long getNumberOfRegisteredEvents(DataStage stage) {
        return eventRepository.count(stage);
    }

    @Override
    public List<PersistedEvent> getEventsSince(DataStage stage, String lastEventId) {
        return eventRepository.eventsByLastEventId(stage, lastEventId);
    }

    @Override
    public Set<InstanceId> postEvent(Event event, boolean deferInference) {
        return eventProcessor.postEvent(event, deferInference);
    }

    @Override
    public void inferDeferred(String space, boolean sync) {
        logger.info("Received request for deferred inference");
        if(sync) {
            eventProcessor.syncDeferredInference(new SpaceName(space));
        }
        else {
            eventProcessor.asyncDeferredInference(new SpaceName(space));
        }
    }

    @Override
    public void infer(String space, UUID id) {
        logger.info("Received request for deferred inference");
        eventProcessor.autoRelease(inferenceProcessor.triggerInference(new SpaceName(space), id));
    }

}

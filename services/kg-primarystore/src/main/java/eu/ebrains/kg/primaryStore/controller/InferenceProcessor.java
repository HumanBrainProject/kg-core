/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.primaryStore.controller;

import eu.ebrains.kg.commons.api.Indexing;
import eu.ebrains.kg.commons.api.Inference;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.primaryStore.model.DeferredInference;
import eu.ebrains.kg.primaryStore.model.ExecutedDeferredInference;
import eu.ebrains.kg.primaryStore.model.FailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class InferenceProcessor {

    private final Indexing.Client indexing;

    private final SSEProducer sseProducer;

    private final EventRepository eventRepository;

    private final EventController eventController;

    private final Inference.Client inference;

    private static final int LIMIT_DEFERRED_INFERENCE_RETRIES = 10;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public InferenceProcessor(Indexing.Client indexing, SSEProducer sseProducer, EventRepository eventRepository, EventController eventController, Inference.Client inference) {
        this.indexing = indexing;
        this.sseProducer = sseProducer;
        this.eventRepository = eventRepository;
        this.eventController = eventController;
        this.inference = inference;
    }

    @Async
    public CompletableFuture<ExecutedDeferredInference> asyncDoDeferInference(DeferredInference deferredInference){
        return CompletableFuture.completedFuture(doDeferInference(deferredInference,  0));
    }

    private ExecutedDeferredInference doDeferInference(DeferredInference deferredInference, int retry){
        try {
            List<PersistedEvent> persistedEvents = triggerInference(deferredInference.getSpace(), deferredInference.getUuid());
            eventRepository.removeDeferredInference(deferredInference);
            return new ExecutedDeferredInference(persistedEvents, deferredInference, true, null);
        } catch (Exception e) {
            if(LIMIT_DEFERRED_INFERENCE_RETRIES>retry) {
                logger.error(String.format("Was not able to infer deferred instance %s", deferredInference.getKey()), e);
                return new ExecutedDeferredInference(null, deferredInference, false, e);
            }
            else{
                int retryInSecs = retry*retry;
                logger.warn(String.format("Was not able to infer deferred instance %s - I will retry for the %d time in %d seconds", deferredInference.getKey(), retry, retryInSecs), e);
                try {
                    Thread.sleep(retryInSecs * 1000L);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                return doDeferInference(deferredInference, ++retry);
            }
        }
    }

    public List<PersistedEvent> triggerInference(SpaceName space, UUID documentId){
        List<PersistedEvent> events = inference.infer(space.getName(), documentId).stream().map(e -> eventController.persistEvent(e, DataStage.IN_PROGRESS)).collect(Collectors.toList());
        events.forEach(evt -> {
            try {
                indexing.indexEvent(evt);
            } catch (Exception e) {
                eventRepository.recordFailedEvent(new FailedEvent(evt, e, ZonedDateTime.now()));
                throw e;
            }
            sseProducer.emit(evt);
        });
        return events;
    }

}

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

package eu.ebrains.kg.primaryStore.controller;

import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.primaryStore.model.DeferredInference;
import eu.ebrains.kg.primaryStore.model.ExecutedDeferredInference;
import eu.ebrains.kg.primaryStore.model.FailedEvent;
import eu.ebrains.kg.primaryStore.serviceCall.PrimaryStoreToIndexing;
import eu.ebrains.kg.primaryStore.serviceCall.PrimaryStoreToInference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class EventProcessor {
    private final PrimaryStoreToIndexing indexingSvc;

    private final SSEProducer sseProducer;

    private final EventRepository eventRepository;

    private final EventController eventController;

    private final PrimaryStoreToInference inferenceSvc;

    private static final int LIMIT_DEFERRED_INFERENCE_RETRIES = 10;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EventProcessor(PrimaryStoreToIndexing indexingSvc, SSEProducer sseProducer, EventRepository eventRepository, EventController eventController, PrimaryStoreToInference inferenceSvc) {
        this.indexingSvc = indexingSvc;
        this.sseProducer = sseProducer;
        this.eventRepository = eventRepository;
        this.eventController = eventController;
        this.inferenceSvc = inferenceSvc;
    }

    public List<PersistedEvent> processEvent(AuthTokens authTokens, PersistedEvent persistedEvent, UserWithRoles userWithRoles, boolean deferInference) {
        try {
            indexingSvc.indexEvent(persistedEvent, authTokens);
        } catch (Exception e) {
            eventRepository.recordFailedEvent(new FailedEvent(persistedEvent, e, ZonedDateTime.now()));
            throw e;
        }
        sseProducer.emit(persistedEvent);
        if (persistedEvent.getDataStage() == DataStage.NATIVE) {
            if (deferInference) {
                DeferredInference deferredInference = new DeferredInference();
                deferredInference.setKey(persistedEvent.getDocumentId().toString());
                deferredInference.setSpace(persistedEvent.getSpace());
                deferredInference.setUuid(persistedEvent.getDocumentId());
                eventRepository.recordDeferredInference(deferredInference);
            } else {
                return triggerInference(persistedEvent.getSpace(), persistedEvent.getDocumentId(), userWithRoles, authTokens);
            }
        }
        return Collections.emptyList();
    }

    public void syncDeferredInference(AuthTokens authTokens, Space space, UserWithRoles userWithRoles){
        List<ExecutedDeferredInference> results = deferInference(authTokens, space, userWithRoles);
        List<String> unsuccessful = results.stream().filter(d -> !d.isSuccessful()).map(d -> String.format("%s/%s: %s", d.getDeferredInference().getSpace().getName(), d.getDeferredInference().getUuid(), d.getException().getMessage())).collect(Collectors.toList());
        if(!unsuccessful.isEmpty()){
            logger.error(String.format("Was not able to infer all deferred instances: %s", String.join(", ", unsuccessful)));
        }
    }

    @Async
    public void asyncDeferredInference(AuthTokens authTokens, Space space, UserWithRoles userWithRoles){
        syncDeferredInference(authTokens, space, userWithRoles);
    }

    private synchronized List<ExecutedDeferredInference> deferInference(AuthTokens authTokens, Space space, UserWithRoles userWithRoles){
        DeferredInference deferredInference;
        int skipped = 0;
        List<ExecutedDeferredInference> results = new ArrayList<>();
        while((deferredInference = eventRepository.getNextDeferredInference(space, skipped)) != null){
            ExecutedDeferredInference executedDeferredInference = doDeferInference(authTokens, deferredInference, userWithRoles, 0);
            results.add(executedDeferredInference);
            if(!executedDeferredInference.isSuccessful()){
                skipped++;
            }
        }
        return results;
    }

    private ExecutedDeferredInference doDeferInference(AuthTokens authTokens, DeferredInference deferredInference, UserWithRoles userWithRoles, int retry){
        try {
            triggerInference(deferredInference.getSpace(), deferredInference.getUuid(), userWithRoles, authTokens);
            eventRepository.removeDeferredInference(deferredInference);
            return new ExecutedDeferredInference(deferredInference, true, null);
        } catch (Exception e) {
            if(LIMIT_DEFERRED_INFERENCE_RETRIES>retry) {
                logger.error(String.format("Was not able to infer deferred instance %s", deferredInference.getKey()), e);
                return new ExecutedDeferredInference(deferredInference, false, e);
            }
            else{
                int retryInSecs = retry*retry;
                logger.warn(String.format("Was not able to infer deferred instance %s - I will retry for the %d time in %d seconds", deferredInference.getKey(), retry, retryInSecs), e);
                try {
                    Thread.sleep(retryInSecs *1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                return doDeferInference(authTokens, deferredInference, userWithRoles, ++retry);
            }
        }
    }

    public List<PersistedEvent> triggerInference(Space space, UUID documentId, UserWithRoles userWithRoles, AuthTokens authTokens){
        List<PersistedEvent> events = inferenceSvc.infer(space, documentId, authTokens).stream().map(e -> eventController.persistEvent(authTokens, e, DataStage.IN_PROGRESS, userWithRoles, null)).collect(Collectors.toList());
        events.forEach(evt -> {
            try {
                indexingSvc.indexEvent(evt, authTokens);
            } catch (Exception e) {
                eventRepository.recordFailedEvent(new FailedEvent(evt, e, ZonedDateTime.now()));
                throw e;
            }
            sseProducer.emit(evt);
        });
        return events;
    }



}

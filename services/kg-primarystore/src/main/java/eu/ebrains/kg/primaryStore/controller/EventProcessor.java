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
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Component
public class EventProcessor {
    private final PrimaryStoreToIndexing indexingSvc;

    private final SSEProducer sseProducer;

    private final EventRepository eventRepository;

    private final EventController eventController;

    private final PrimaryStoreToInference inferenceSvc;

    private InferenceProcessor inferenceProcessor;

    private final UserResolver userResolver;

    private static final int LIMIT_DEFERRED_INFERENCE_RETRIES = 10;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EventProcessor(PrimaryStoreToIndexing indexingSvc, SSEProducer sseProducer, EventRepository eventRepository, EventController eventController, PrimaryStoreToInference inferenceSvc, UserResolver userResolver, InferenceProcessor inferenceProcessor) {
        this.indexingSvc = indexingSvc;
        this.sseProducer = sseProducer;
        this.eventRepository = eventRepository;
        this.eventController = eventController;
        this.inferenceSvc = inferenceSvc;
        this.userResolver = userResolver;
        this.inferenceProcessor = inferenceProcessor;
    }

    public Set<InstanceId> postEvent(UserWithRoles userWithRoles, AuthTokens authTokens, Event event, boolean deferInference) {
        logger.info(String.format("Received event of type %s for instance %s in space %s by user %s via client %s", event.getType().name(), event.getDocumentId(), event.getSpace().getName(), userWithRoles != null && userWithRoles.getUser() != null ? userWithRoles.getUser().getUserName() : "anonymous", userWithRoles != null && userWithRoles.getClientId() != null ? userWithRoles.getClientId() : "unknown"));
        PersistedEvent persistedEvent = eventController.persistEvent(authTokens, event, event.getType().getStage(), userWithRoles,  userResolver.resolveUser(event, userWithRoles));
        List<PersistedEvent> inferredEvents = processEvent(authTokens, persistedEvent, userWithRoles, deferInference);
        return inferredEvents.stream().map(e -> new InstanceId(e.getDocumentId(), e.getSpace())).collect(Collectors.toSet());
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
                return autoRelease(inferenceProcessor.triggerInference(persistedEvent.getSpace(), persistedEvent.getDocumentId(), userWithRoles, authTokens), userWithRoles, authTokens);
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
        List<DeferredInference> deferredInferences;
        List<ExecutedDeferredInference> results = new ArrayList<>();
        int pageSize = 20;
        while((deferredInferences = eventRepository.getDeferredInferences(space, pageSize)) != null && !deferredInferences.isEmpty()){
            List<CompletableFuture<ExecutedDeferredInference>> completableFutures = deferredInferences.stream().map(i -> inferenceProcessor.asyncDoDeferInference(authTokens, i, userWithRoles)).collect(Collectors.toList());
            // Wait until they are all done
            CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new)).join();
            List<ExecutedDeferredInference> executedDeferredInferences = completableFutures.stream().map(c -> {
                try {
                    return c.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            executedDeferredInferences.stream().filter(ExecutedDeferredInference::isSuccessful).forEach(e -> autoRelease(e.getPersistedEvents(), userWithRoles, authTokens));
            results.addAll(executedDeferredInferences);
        }
        return results;
    }

    public List<PersistedEvent> autoRelease(List<PersistedEvent> events, UserWithRoles userWithRoles, AuthTokens authTokens){
        events.forEach(e -> {
            if(e.getSpace().isAutoRelease()){
                postEvent(userWithRoles, authTokens, new Event(e.getSpace(), e.getDocumentId(), new NormalizedJsonLd(e.getData()).removeAllInternalProperties().removeAllFieldsFromNamespace(EBRAINSVocabulary.META), Event.Type.RELEASE, new Date()), false);
            }
        });
        return events;
    }

}

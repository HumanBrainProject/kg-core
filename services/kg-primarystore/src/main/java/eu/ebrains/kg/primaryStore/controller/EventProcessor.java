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
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.primaryStore.model.DeferredInference;
import eu.ebrains.kg.primaryStore.model.ExecutedDeferredInference;
import eu.ebrains.kg.primaryStore.model.FailedEvent;
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
    private final Indexing.Client indexing;

    private final EventRepository eventRepository;

    private final EventController eventController;

    private final Inference.Client inference;

    private InferenceProcessor inferenceProcessor;

    private final UserResolver userResolver;

    private static final int LIMIT_DEFERRED_INFERENCE_RETRIES = 10;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EventProcessor(Indexing.Client indexing, EventRepository eventRepository, EventController eventController, Inference.Client inference, UserResolver userResolver, InferenceProcessor inferenceProcessor) {
        this.indexing = indexing;
        this.eventRepository = eventRepository;
        this.eventController = eventController;
        this.inference = inference;
        this.userResolver = userResolver;
        this.inferenceProcessor = inferenceProcessor;
    }

    public Set<InstanceId> postEvent(Event event, boolean deferInference) {
        PersistedEvent persistedEvent = eventController.persistEvent(event, event.getType().getStage());
        List<PersistedEvent> inferredEvents = processEvent(persistedEvent, deferInference);
        return inferredEvents.stream().map(e -> new InstanceId(e.getDocumentId(), e.getSpaceName())).collect(Collectors.toSet());
    }

    public List<PersistedEvent> processEvent(PersistedEvent persistedEvent, boolean deferInference) {
        try {
            indexing.indexEvent(persistedEvent);
        } catch (Exception e) {
            eventRepository.recordFailedEvent(new FailedEvent(persistedEvent, e, ZonedDateTime.now()));
            throw e;
        }
        if (persistedEvent.getDataStage() == DataStage.NATIVE) {
            if (deferInference) {
                DeferredInference deferredInference = new DeferredInference();
                deferredInference.setKey(persistedEvent.getDocumentId().toString());
                deferredInference.setSpace(persistedEvent.getSpaceName());
                deferredInference.setUuid(persistedEvent.getDocumentId());
                eventRepository.recordDeferredInference(deferredInference);
            } else {
                return autoRelease(inferenceProcessor.triggerInference(persistedEvent.getSpaceName(), persistedEvent.getDocumentId()));
            }
        }
        return Collections.emptyList();
    }

    public void syncDeferredInference(SpaceName space){
        List<ExecutedDeferredInference> results = deferInference(space);
        List<String> unsuccessful = results.stream().filter(d -> !d.isSuccessful()).map(d -> String.format("%s/%s: %s", d.getDeferredInference().getSpace().getName(), d.getDeferredInference().getUuid(), d.getException().getMessage())).collect(Collectors.toList());
        if(!unsuccessful.isEmpty()){
            logger.error(String.format("Was not able to infer all deferred instances: %s", String.join(", ", unsuccessful)));
        }
    }

    @Async
    public void asyncDeferredInference(SpaceName space){
        syncDeferredInference(space);
    }

    private synchronized List<ExecutedDeferredInference> deferInference(SpaceName space){
        List<DeferredInference> deferredInferences;
        List<ExecutedDeferredInference> results = new ArrayList<>();
        int pageSize = 20;
        while((deferredInferences = eventRepository.getDeferredInferences(space, pageSize)) != null && !deferredInferences.isEmpty()){
            List<CompletableFuture<ExecutedDeferredInference>> completableFutures = deferredInferences.stream().map(i -> inferenceProcessor.asyncDoDeferInference(i)).collect(Collectors.toList());
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
            executedDeferredInferences.stream().filter(ExecutedDeferredInference::isSuccessful).forEach(e -> autoRelease(e.getPersistedEvents()));
            results.addAll(executedDeferredInferences);
        }
        return results;
    }

    public List<PersistedEvent> autoRelease(List<PersistedEvent> events){
        events.forEach(e -> {
            if(e.getSpace() != null && e.getSpace().isAutoRelease()){
                postEvent(new Event(e.getSpaceName(), e.getDocumentId(), new NormalizedJsonLd(e.getData()).removeAllInternalProperties().removeAllFieldsFromNamespace(EBRAINSVocabulary.META), Event.Type.RELEASE, new Date()), false);
            }
        });
        return events;
    }

}

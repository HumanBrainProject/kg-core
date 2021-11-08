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
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.primaryStore.model.FailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EventProcessor {
    private final Indexing.Client indexing;

    private final EventRepository eventRepository;

    private final EventController eventController;

    private final InferenceProcessor inferenceProcessor;


    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EventProcessor(Indexing.Client indexing, EventRepository eventRepository, EventController eventController, InferenceProcessor inferenceProcessor) {
        this.indexing = indexing;
        this.eventRepository = eventRepository;
        this.eventController = eventController;
        this.inferenceProcessor = inferenceProcessor;
    }

    public void rerunEvents(SpaceName spaceName){
        eventController.checkPermissionsForRerunEvents();
        final List<PersistedEvent> events = eventRepository.queryAllEvents(DataStage.NATIVE, spaceName);
        events.forEach(e -> {
            eventController.handleIds(DataStage.NATIVE, e);
            processEvent(e);
        });
    }

    public Set<InstanceId> postEvent(Event event) {
        PersistedEvent persistedEvent = eventController.persistEvent(event, event.getType().getStage());
        List<PersistedEvent> inferredEvents = processEvent(persistedEvent);
        return inferredEvents.stream().map(e -> new InstanceId(e.getDocumentId(), e.getSpaceName())).collect(Collectors.toSet());
    }

    public List<PersistedEvent> processEvent(PersistedEvent persistedEvent) {
        try {
            indexing.indexEvent(persistedEvent);
        } catch (Exception e) {
            eventRepository.recordFailedEvent(new FailedEvent(persistedEvent, e, ZonedDateTime.now()));
            throw e;
        }
        if (persistedEvent.getDataStage() == DataStage.NATIVE) {
            return autoRelease(inferenceProcessor.triggerInference(persistedEvent.getSpaceName(), persistedEvent.getDocumentId()));
        }
        return Collections.emptyList();
    }

    public List<PersistedEvent> autoRelease(List<PersistedEvent> events) {
        events.forEach(e -> {
            if (e.getSpace() != null && e.getSpace().isAutoRelease()) {
                final NormalizedJsonLd normalizedJsonLd = new NormalizedJsonLd(e.getData());
                normalizedJsonLd.removeAllInternalProperties();
                normalizedJsonLd.removeAllFieldsFromNamespace(EBRAINSVocabulary.META);
                postEvent(new Event(e.getSpaceName(), e.getDocumentId(), normalizedJsonLd, Event.Type.RELEASE, new Date()));
            }
        });
        return events;
    }

}

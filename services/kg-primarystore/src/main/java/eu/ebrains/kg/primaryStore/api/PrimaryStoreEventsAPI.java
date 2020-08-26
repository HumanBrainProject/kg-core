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

package eu.ebrains.kg.primaryStore.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.primaryStore.controller.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RestController
@RequestMapping("/internal/primaryStore/events")
public class PrimaryStoreEventsAPI {

    private final EventProcessor eventProcessor;

    private final InferenceProcessor inferenceProcessor;

    private final EventController eventController;

    private final SSEProducer sseProducer;

    private final EventRepository eventRepository;

    private final AuthContext authContext;

    private final UserResolver userResolver;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PrimaryStoreEventsAPI(EventProcessor eventProcessor, EventController eventController, SSEProducer sseProducer, EventRepository eventRepository, AuthContext authContext, UserResolver userResolver, InferenceProcessor inferenceProcessor) {
        this.eventProcessor = eventProcessor;
        this.eventController = eventController;
        this.sseProducer = sseProducer;
        this.eventRepository = eventRepository;
        this.authContext = authContext;
        this.userResolver = userResolver;
        this.inferenceProcessor = inferenceProcessor;
    }

    @GetMapping(value = "/stream/{stage}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PersistedEvent>> streamEvents(@PathVariable("stage") DataStage stage, @RequestHeader(value = "Last-Event-Id", required = false) String lastEventId, @RequestParam(value = "lastEventId", required = false) String lastEventIdFromQuery) {
        return sseProducer.initiateNewFlux(stage, lastEventId != null ? lastEventId : lastEventIdFromQuery);
    }

    @GetMapping("/count/{stage}")
    public Long getNumberOfRegisteredEvents(@PathVariable("stage") DataStage stage) {
        return eventRepository.count(stage);
    }

    @GetMapping("/{stage}")
    public List<PersistedEvent> getEventsSince(@PathVariable("stage") DataStage stage, @RequestParam(value = "lastEventId", required = false) String lastEventId) {
        return eventRepository.eventsByLastEventId(stage, lastEventId);
    }

    @PostMapping
    public Set<InstanceId> postEvent(@RequestBody Event event, @RequestParam(value = "deferInference", required = false, defaultValue = "false") boolean deferInference) {
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        return eventProcessor.postEvent(userWithRoles, authContext.getAuthTokens(), event, deferInference);
    }


    @PostMapping("/inference/deferred/{space}")
    public void inferDeferred(@PathVariable("space") String space, @RequestParam(value = "sync", required = false, defaultValue = "false") boolean sync) {
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        logger.info("Received request for deferred inference");
        if(sync) {
            eventProcessor.syncDeferredInference(authContext.getAuthTokens(), new Space(space), userWithRoles);
        }
        else {
            eventProcessor.asyncDeferredInference(authContext.getAuthTokens(), new Space(space), userWithRoles);
        }
    }


    @PostMapping("/inference/{space}/{id}")
    public void infer(@PathVariable("space") String space, @PathVariable("id") UUID id) {
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        logger.info("Received request for deferred inference");
        eventProcessor.autoRelease(inferenceProcessor.triggerInference(new Space(space), id, userWithRoles, authContext.getAuthTokens()), userWithRoles, authContext.getAuthTokens());
    }

}

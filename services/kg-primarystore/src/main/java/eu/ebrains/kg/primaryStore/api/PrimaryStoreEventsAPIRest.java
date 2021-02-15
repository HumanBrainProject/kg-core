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
public class PrimaryStoreEventsAPIRest implements PrimaryStoreEvents {

    private final PrimaryStoreEventsAPI primaryStoreEventsAPI;

    public PrimaryStoreEventsAPIRest(PrimaryStoreEventsAPI primaryStoreEventsAPI) {
        this.primaryStoreEventsAPI = primaryStoreEventsAPI;
    }

    @GetMapping(value = "/stream/{stage}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PersistedEvent>> streamEvents(@PathVariable("stage") DataStage stage, @RequestHeader(value = "Last-Event-Id", required = false) String lastEventId, @RequestParam(value = "lastEventId", required = false) String lastEventIdFromQuery) {
        return primaryStoreEventsAPI.streamEvents(stage, lastEventId, lastEventIdFromQuery);
    }

    @GetMapping("/count/{stage}")
    public Long getNumberOfRegisteredEvents(@PathVariable("stage") DataStage stage) {
        return primaryStoreEventsAPI.getNumberOfRegisteredEvents(stage);
    }

    @GetMapping("/{stage}")
    public List<PersistedEvent> getEventsSince(@PathVariable("stage") DataStage stage, @RequestParam(value = "lastEventId", required = false) String lastEventId) {
        return primaryStoreEventsAPI.getEventsSince(stage, lastEventId);
    }

    @PostMapping
    public Set<InstanceId> postEvent(@RequestBody Event event, @RequestParam(value = "deferInference", required = false, defaultValue = "false") boolean deferInference) {
        return primaryStoreEventsAPI.postEvent(event, deferInference);
    }


    @PostMapping("/inference/deferred/{space}")
    public void inferDeferred(@PathVariable("space") String space, @RequestParam(value = "sync", required = false, defaultValue = "false") boolean sync) {
       primaryStoreEventsAPI.inferDeferred(space, sync);
    }


    @PostMapping("/inference/{space}/{id}")
    public void infer(@PathVariable("space") String space, @PathVariable("id") UUID id) {
        primaryStoreEventsAPI.infer(space, id);
    }

}

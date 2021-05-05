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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.primaryStore.controller;

import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.PersistedEvent;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.UnicastProcessor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SSEProducer {

    private final EventRepository eventRepository;
    private final Map<DataStage, EmitterProcessor<ServerSentEvent<PersistedEvent>>> emitterByStage;
    private final Map<DataStage, Object> emitterLock;

    public SSEProducer(EventRepository eventRepository){
        this.eventRepository = eventRepository;
        Map<DataStage, EmitterProcessor<ServerSentEvent<PersistedEvent>>> emitters = new HashMap<>();
        Map<DataStage, Object> locks = new HashMap<>();
        for (DataStage stage : DataStage.values()) {
            emitters.put(stage, EmitterProcessor.create());
            locks.put(stage, new Object());
        }
        this.emitterByStage = Collections.unmodifiableMap(emitters);
        this.emitterLock = Collections.unmodifiableMap(locks);
    }


    ServerSentEvent<PersistedEvent> wrapAsSSE(PersistedEvent e) {
        return ServerSentEvent.builder(e).build();
    }

    public Flux<ServerSentEvent<PersistedEvent>> initiateNewFlux(DataStage stage, String lastEventId) {
        Flux<ServerSentEvent<PersistedEvent>> historyLog = getHistoryFlux(stage, lastEventId);
        Flux<ServerSentEvent<PersistedEvent>> realtimeLog = emitterByStage.get(stage).log();
        return Flux.mergeSequential(historyLog, realtimeLog);
    }

    @Async
    public void emit(PersistedEvent event) {
        //TODO reenable SSE emission
//        synchronized (emitterLock.get(event.getDataStage())) {
//            emitterByStage.get(event.getDataStage()).onNext(wrapAsSSE(event));
//        }
    }


    private Flux<ServerSentEvent<PersistedEvent>> getHistoryFlux(DataStage stage, String lastEventId) {
        UnicastProcessor<ServerSentEvent<PersistedEvent>> unicastProcessor = UnicastProcessor.create();
        List<PersistedEvent> persistedEvents = eventRepository.eventsByLastEventId(stage, lastEventId);
        persistedEvents.stream().map(this::wrapAsSSE).forEachOrdered(unicastProcessor::onNext);
        unicastProcessor.onComplete();
        return unicastProcessor.log();
    }

}

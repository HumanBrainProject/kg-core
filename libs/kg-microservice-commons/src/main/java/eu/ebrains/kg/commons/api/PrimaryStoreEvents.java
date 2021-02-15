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

package eu.ebrains.kg.commons.api;

import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.PersistedEvent;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PrimaryStoreEvents {

    interface Client extends PrimaryStoreEvents {}

    Flux<ServerSentEvent<PersistedEvent>> streamEvents(DataStage stage, String lastEventId, String lastEventIdFromQuery);

    Long getNumberOfRegisteredEvents(DataStage stage);

    List<PersistedEvent> getEventsSince(DataStage stage, String lastEventId);

    Set<InstanceId> postEvent(Event event, boolean deferInference);

    void inferDeferred(String space, boolean sync);

    void infer(String space, UUID id);
}

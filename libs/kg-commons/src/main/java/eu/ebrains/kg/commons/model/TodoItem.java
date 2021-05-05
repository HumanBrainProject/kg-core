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

package eu.ebrains.kg.commons.model;

import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;

import java.util.List;
import java.util.UUID;

public class TodoItem {

    private UUID documentId;
    private String eventId;
    private Event.Type type;
    private NormalizedJsonLd payload;
    private SpaceName space;
    private List<JsonLdId> mergeIds;
    private User user;

    public TodoItem() {
    }

    public static TodoItem fromEvent(PersistedEvent event){
        return new TodoItem(event.getEventId(), event.getDocumentId(), event.getSpaceName(), event.getType(), event.getData(), event.getMergedIds(), event.getUser());
    }

    public TodoItem(String eventId, UUID documentId, SpaceName space, Event.Type type, NormalizedJsonLd payload, List<JsonLdId> mergeIds, User user) {
        this.documentId = documentId;
        this.eventId = eventId;
        this.space = space;
        this.type = type;
        this.payload = payload;
        this.mergeIds = mergeIds;
        this.user = user;
    }

    public Event.Type getType() {
        return type;
    }

    public NormalizedJsonLd getPayload() {
        return payload;
    }

    public String getEventId() {
        return eventId;
    }

    public List<JsonLdId> getMergeIds() {
        return mergeIds;
    }

    public SpaceName getSpace() {
        return space;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public User getUser() {
        return user;
    }
}

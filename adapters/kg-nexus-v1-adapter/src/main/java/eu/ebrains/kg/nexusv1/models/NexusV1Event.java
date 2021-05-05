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

package eu.ebrains.kg.nexusv1.models;

import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.EventId;
import eu.ebrains.kg.commons.model.Space;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NexusV1Event extends Event implements EventId {

    private Type event;
    private String id;
    private Type eventType;
    private String eventId;

    public String getEventId() {
        return this.id;
    }

    public Date getDate() throws ParseException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
        String dateStr = (String)this.data.get("_instant");
        return dateFormatter.parse(dateStr);
    }

    public JsonLdDoc getEventSourcePayload(){
         Map<String, Object> source = (Map) this.data.getOrDefault("_source", new HashMap());
         return new JsonLdDoc(source);
    }

    public String getUserId(){
        return (String) this.data.get("_subject");
    }

    public String getOrganization(){
        return (String) this.data.get("_organizationUuid");
    }

    public String getResourceId(){
       return (String) this.data.get("_resourceId");
    }

    public enum Type{
        Created, Updated, Deprecated, TagAdded, FileCreated, FileUpdated
    }

    public Event.Type toEventType() throws UnHandleEventTypeException {
        switch(this.event){
            case Created:
            case FileCreated:
                return Event.Type.INSERT;
            case Updated:
            case FileUpdated:
                return Event.Type.UPDATE;
            case Deprecated:
                return Event.Type.DELETE;
            default:
                throw new UnHandleEventTypeException("Could not interprete the nexus Event type");
        }
    }

    public NexusV1Event(Type eventType, String eventId){
        this.event = eventType;
        this.id = eventId;
    }

    public NexusV1Event(Space space, UUID id, NormalizedJsonLd data, Event.Type type, Date timeStamp, Type eventType, String eventId){
        super(space, id, data, type, timeStamp);
        this.event = eventType;
        this.id = eventId;
    }

    public class UnHandleEventTypeException extends Exception {
         public UnHandleEventTypeException(String message) {
            super(message);
        }
    }
}

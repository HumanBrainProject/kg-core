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

package eu.ebrains.kg.commons.model;

import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;

import java.util.Date;
import java.util.UUID;

public class Event {
    public enum Type{
        INSERT(DataStage.NATIVE), UPDATE(DataStage.NATIVE), DELETE(DataStage.IN_PROGRESS), RELEASE(DataStage.RELEASED), UNRELEASE(DataStage.RELEASED), META_DEPRECATION(DataStage.IN_PROGRESS);
        DataStage stage;
        Type(DataStage targetStage){
            this.stage = targetStage;
        }
        public DataStage getStage() {
            return stage;
        }
    }

    protected SpaceName spaceName;

    protected UUID documentId;

    protected NormalizedJsonLd data;

    private Type type;

    private String userId;

    private Long reportedTimeStampInMs;

    private boolean undeprecate;

    public Event() {
    }

    public Event(SpaceName spaceName, UUID documentId, NormalizedJsonLd data, Type type, Date timeStamp) {
        this(spaceName, documentId, data, type, timeStamp!=null ? timeStamp.getTime() : null);
    }

    protected Event(SpaceName spaceName, UUID documentId, NormalizedJsonLd data, Type type, Long timeStampInMs) {
        this.spaceName = spaceName;
        this.documentId = documentId;
        this.data = data;
        this.type = type;
        this.reportedTimeStampInMs = timeStampInMs;
    }

    public NormalizedJsonLd getData() {
        return data;
    }

    public Type getType() {
        return type;
    }

    public String getUserId() {
        return userId;
    }

    public Long getReportedTimeStampInMs() {
        return reportedTimeStampInMs;
    }

    public static Event createDeleteEvent(SpaceName space, UUID id, JsonLdId absoluteId){
        NormalizedJsonLd normalizedJsonLd = new NormalizedJsonLd();
        normalizedJsonLd.setId(absoluteId);
        return new Event(space, id, normalizedJsonLd, Event.Type.DELETE, new Date().getTime());
    }

    public static Event createUpsertEvent(SpaceName space, UUID id, Event.Type type, NormalizedJsonLd payload){
        return new Event(space, id, payload, type, new Date().getTime());
    }

    public void setInstance(SpaceName space, UUID uuid){
        this.spaceName = space;
        this.documentId = uuid;
    }

    public SpaceName getSpaceName() {
        return spaceName;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setReportedTimeStampInMs(Long reportedTimeStampInMs) {
        this.reportedTimeStampInMs = reportedTimeStampInMs;
    }
}

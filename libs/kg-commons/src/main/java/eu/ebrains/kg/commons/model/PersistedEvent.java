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


import com.google.gson.annotations.SerializedName;
import eu.ebrains.kg.commons.jsonld.JsonLdId;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PersistedEvent extends Event implements EventId {
    private User user;
    private String ingestionUserId;
    private Long indexedTimestamp;
    private String eventId;
    private DataStage dataStage;
    private List<JsonLdId> mergedIds;
    private boolean suggestion;

    @SerializedName("_key")
    private String key;

    public PersistedEvent() {
        super();
    }

    public PersistedEvent(Event event, DataStage dataStage, User user) {
        super(event.getSpace(), event.getDocumentId(), event.getData(), event.getType(), event.getReportedTimeStampInMs());
        this.ingestionUserId = event.getUserId();
        this.indexedTimestamp = new Date().getTime();
        this.eventId = UUID.randomUUID().toString();
        this.key = this.eventId;
        this.dataStage = dataStage;
        this.user = user;
    }

    public Long getIndexedTimestamp() {
        return indexedTimestamp;
    }

    public String getEventId() {
        return eventId;
    }

    public DataStage getDataStage() {
        return dataStage;
    }

    public List<JsonLdId> getMergedIds() {
        return mergedIds;
    }

    public void setMergedIds(List<JsonLdId> mergedIds) {
        this.mergedIds = mergedIds;
    }

    public User getUser() {
        return user;
    }

    public void setSuggestion(boolean suggestion) {
        this.suggestion = suggestion;
    }
}

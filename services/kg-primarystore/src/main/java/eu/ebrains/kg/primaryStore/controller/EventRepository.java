/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

import com.arangodb.ArangoCollection;
import com.arangodb.model.HashIndexOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.arangodb.model.SkiplistIndexOptions;
import eu.ebrains.kg.arango.commons.aqlbuilder.AQL;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.exception.AmbiguousException;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.primaryStore.model.FailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class EventRepository {
    private final ArangoDatabaseProxy arangoDatabase;
    private final PrimaryStoreDBUtils primaryStoreDBUtils;

    private final JsonAdapter jsonAdapter;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EventRepository(@Qualifier("primaryStoreDB") ArangoDatabaseProxy arangoDatabase, JsonAdapter jsonAdapter, PrimaryStoreDBUtils primaryStoreDBUtils) {
        this.primaryStoreDBUtils = primaryStoreDBUtils;
        this.arangoDatabase = arangoDatabase;
        this.jsonAdapter = jsonAdapter;
    }

    void recordFailedEvent(FailedEvent e) {
        try {
            ArangoCollection events = getOrCreateFailuresCollection(e.getPersistedEvent().getDataStage());
            events.insertDocument(jsonAdapter.toJson(e));
        } catch (Exception recordingException) {
            //We don't want any failure recording issue to abort the rest of the logic - but we need to be notified about these events nevertheless...
            logger.error(String.format("Was not able to record failed event for %s! ", e.getPersistedEvent().getEventId()), recordingException);
        }
    }

    void insert(PersistedEvent e) {
        ArangoCollection events = getOrCreateCollection(e.getDataStage());
        events.insertDocument(jsonAdapter.toJson(e));
    }

    public long count(DataStage stage) {
        return getOrCreateCollection(stage).count().getCount();
    }

    private ArangoCollection getOrCreateCollection(DataStage stage) {
        return getOrCreateCollection(new ArangoCollectionReference(getCollectionName(stage), false));
    }

    private ArangoCollection getOrCreateFailuresCollection(DataStage stage) {
        return getOrCreateCollection(new ArangoCollectionReference(getCollectionName(stage) + "_failures", false));
    }

    private ArangoCollection getOrCreateCollection(ArangoCollectionReference collectionReference) {
        ArangoCollection events = primaryStoreDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), collectionReference);
        events.ensurePersistentIndex(Arrays.asList("indexedTimestamp", "eventId"), new PersistentIndexOptions());
        events.ensureHashIndex(Collections.singleton("eventId"), new HashIndexOptions());
        events.ensureSkiplistIndex(Arrays.asList("documentId", "type", "indexedTimestamp"), new SkiplistIndexOptions());
        return events;
    }

    public String getFirstRelease(UUID documentId){
        getOrCreateCollection(DataStage.RELEASED); //ensure the collection exists.
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("RETURN FIRST(FOR r IN "+getCollectionName(DataStage.RELEASED)));
        aql.addLine(AQL.trust("FILTER r.type==\""+ Event.Type.RELEASE.name()+"\" AND r.documentId==@documentId"));
        bindVars.put("documentId", documentId);
        aql.addLine(AQL.trust("SORT r.indexedTimestamp ASC"));
        aql.addLine(AQL.trust("RETURN r.indexedTimestamp)"));
        final List<Long> timestamps = arangoDatabase.getOrCreate().query(aql.build().getValue(), bindVars, Long.class).asListRemaining();
        if(timestamps.isEmpty()){
            return null;
        }
        else if(timestamps.size()==1){
            final Long timestamp = timestamps.get(0);
            if(timestamp!=null){
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT);
            }
        }
        else {
            throw new AmbiguousException(String.format("Unexpected number of results when querying for first release of document %s", documentId));
        }
        return null;

    }

   public List<PersistedEvent> queryAllEvents(DataStage stage, SpaceName spaceName) {
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR doc IN `" + stage.name().toLowerCase() + "_events`"));
        if(spaceName!=null){
            aql.addLine(AQL.trust(" FILTER doc.spaceName == @spaceName"));
            bindVars.put("spaceName", spaceName.getName());
        }
        aql.addLine(AQL.trust(" SORT doc.`indexedTimestamp` ASC"));
        aql.addLine(AQL.trust("RETURN doc"));
        return arangoDatabase.get().query(aql.build().getValue(), bindVars, PersistedEvent.class).asListRemaining();
    }
    private String getCollectionName(DataStage stage) {
        return stage.name().toLowerCase() + "_events";
    }

}

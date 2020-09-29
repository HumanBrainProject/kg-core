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

package eu.ebrains.kg.primaryStore.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.model.*;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.primaryStore.model.DeferredInference;
import eu.ebrains.kg.primaryStore.model.FailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class EventRepository {
    private final ArangoDatabaseProxy arangoDatabase;

    private final JsonAdapter jsonAdapter;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EventRepository(@Qualifier("primaryStoreDB") ArangoDatabaseProxy arangoDatabase, JsonAdapter jsonAdapter) {
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

    void recordDeferredInference(DeferredInference e) {
        ArangoCollection events = getOrCreateDeferredInferenceCollection(DataStage.IN_PROGRESS);
        events.insertDocument(jsonAdapter.toJson(e), new DocumentCreateOptions().overwrite(true));
    }

    List<DeferredInference> getDeferredInferences(SpaceName space, int pageSize) {
        HashMap<String, Object> bindVars = new HashMap<>();
        bindVars.put("@collection", getOrCreateDeferredInferenceCollection(DataStage.IN_PROGRESS).name());
        bindVars.put("space", space.getName());
        bindVars.put("pageSize", pageSize);
        AQL aql = new AQL();
        aql.addLine(AQL.trust("FOR doc IN @@collection FILTER doc.space.name == @space LIMIT @pageSize RETURN doc"));
        return arangoDatabase.getOrCreate().query(aql.build().getValue(), bindVars, new AqlQueryOptions(), String.class).asListRemaining().stream().map(d -> jsonAdapter.fromJson(d, DeferredInference.class)).collect(Collectors.toList());
    }

    void removeDeferredInference(DeferredInference inference) {
        getOrCreateDeferredInferenceCollection(DataStage.IN_PROGRESS).deleteDocument(inference.getKey());
    }

    void insert(PersistedEvent e) {
        ArangoCollection events = getOrCreateCollection(e.getDataStage());
        events.insertDocument(jsonAdapter.toJson(e));
    }

    public long count(DataStage stage) {
        return getOrCreateCollection(stage).count().getCount();
    }

    private ArangoCollection getOrCreateCollection(DataStage stage) {
        return getOrCreateCollection(getCollectionName(stage), stage);
    }

    private ArangoCollection getOrCreateFailuresCollection(DataStage stage) {
        return getOrCreateCollection(getCollectionName(stage) + "_failures", stage);
    }

    private ArangoCollection getOrCreateDeferredInferenceCollection(DataStage stage) {
        return getOrCreateCollection(getCollectionName(stage) + "_deferred", stage);
    }

    private ArangoCollection getOrCreateCollection(String collectionName, DataStage stage) {
        ArangoCollection events = arangoDatabase.getOrCreate().collection(collectionName);
        if (!events.exists()) {
            events.create();
        }
        events.ensurePersistentIndex(Arrays.asList("indexedTimestamp", "eventId"), new PersistentIndexOptions());
        events.ensureHashIndex(Collections.singleton("eventId"), new HashIndexOptions());
        events.ensureSkiplistIndex(Arrays.asList("resourceId", "indexedTimestamp"), new SkiplistIndexOptions());
        return events;
    }

    public List<PersistedEvent> eventsByLastEventId(DataStage stage, String lastEventId) {
        getOrCreateCollection(stage);
        String query;
        Map<String, Object> bindVars = new HashMap<>();
        if (lastEventId == null) {
            query = queryAllEvents(stage);
        } else {
            query = queryEventsByLastEventId(stage);
            bindVars.put("documentId", lastEventId);
        }
        return arangoDatabase.getOrCreate().query(query, bindVars, new AqlQueryOptions(), PersistedEvent.class).asListRemaining();
    }

    private String queryAllEvents(DataStage stage) {
        return "FOR doc IN `" + stage.name().toLowerCase() + "_events`\n" +
                "    SORT doc.`indexedTimestamp` ASC\n" +
                "    RETURN doc\n";
    }

    private String getCollectionName(DataStage stage) {
        return stage.name().toLowerCase() + "_events";
    }

    private String queryEventsByLastEventId(DataStage stage) {
        return "LET last_seen = DOCUMENT(\"" + getCollectionName(stage) + "\", @documentId)\n" +
                "\n" +
                "FOR doc IN `" + getCollectionName(stage) + "`\n" +
                "    FILTER doc.`indexedTimestamp`>last_seen.`indexedTimestamp` OR (doc.`indexedTimestamp` == last_seen.`indexedTimestamp` AND doc.`_key`>last_seen.`_key`)\n" +
                "    SORT doc.`indexedTimestamp` ASC\n" +
                "    RETURN doc\n";
    }

}

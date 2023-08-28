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

package eu.ebrains.kg.graphdb.health.controller;


import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.CollectionsReadOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.OverwriteMode;
import eu.ebrains.kg.arango.commons.aqlbuilder.AQL;
import eu.ebrains.kg.arango.commons.aqlbuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.jsonld.DynamicJson;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.GraphDBArangoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class HealthController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RelationConsistency relationConsistency;
    private final ArangoDatabases arangoDatabases;
    private final GraphDBArangoUtils utils;

    public HealthController(RelationConsistency relationConsistency, ArangoDatabases arangoDatabases, GraphDBArangoUtils utils) {
        this.relationConsistency = relationConsistency;
        this.arangoDatabases = arangoDatabases;
        this.utils = utils;
    }

    public enum Progress {
        STARTED, IN_PROGRESS, DONE
    }

    @Async
    public void analyze(){
        for (DataStage stage : List.of(DataStage.IN_PROGRESS, DataStage.RELEASED)) {
            final ArangoDatabase database = arangoDatabases.getByStage(stage);
            final Collection<CollectionEntity> collections = database.getCollections(new CollectionsReadOptions().excludeSystem(true));
            collections.forEach(collectionEntity -> {
                checkRelationConsistency(database, collectionEntity, stage);
            });
        }
    }

    private enum ConsistencyChecks{
        RELATION_CONSISTENCY("relationConsistency");

        private final String identifier;

        ConsistencyChecks(String identifier) {
            this.identifier = identifier;
        }
    }

    public List<String> getAvailableChecks(){
        return Arrays.stream(ConsistencyChecks.values()).map(v -> v.identifier).sorted().collect(Collectors.toList());
    }


    private void checkRelationConsistency(ArangoDatabase database, CollectionEntity collectionEntity, DataStage stage){
        if(collectionEntity.getType() == CollectionType.DOCUMENT) {
            final ArangoDatabase consistencyChecksDB = arangoDatabases.getConsistencyChecksDB();
            String name = ConsistencyChecks.RELATION_CONSISTENCY.identifier;
            final ArangoCollectionReference collection = ArangoCollectionReference.fromSpace(new SpaceName(String.format("%s_%s", name, stage.name())));
            final ArangoCollection targetCollection = utils.getOrCreateArangoCollection(consistencyChecksDB, collection);
            if(targetCollection.documentExists(collectionEntity.getName())){
                logger.info(String.format("Skipping %s because report already exists", collectionEntity.getName()));
            }
            else {
                final String started = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                final Map<String, List<String>> results = relationConsistency.checkRelationConsistency(database, collectionEntity);
                if(results!=null) {
                    final Map<String, Object> wrapper = createConsistencyResult(started, collectionEntity.getName(), results);
                    targetCollection.insertDocument(wrapper, new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
                }
            }
        }
    }



    private Map<String, Object> createConsistencyResult(String started, String name, Map<String, ?> data){
        Map<String, Object> result = new HashMap<>();
        result.put(ArangoVocabulary.KEY, name);
        result.put("data", data);
        result.put("started", started);
        result.put("ended", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        return result;
    }

    public List<DynamicJson> getReport(DataStage stage, String name){
        AQL aql = new AQL();
        aql.add(AQL.trust(""" 
                FOR d IN @@collection
                FILTER d.data != {}
                RETURN d"""));
        return arangoDatabases.getConsistencyChecksDB().query(aql.build().getValue(), Map.of("@collection", String.format("%s_%s", name, stage.name()).toLowerCase()), DynamicJson.class).asListRemaining();
    }


}

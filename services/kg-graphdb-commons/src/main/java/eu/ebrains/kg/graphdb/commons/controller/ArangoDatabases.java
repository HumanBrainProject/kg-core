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

package eu.ebrains.kg.graphdb.commons.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.CollectionsReadOptions;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.graphdb.structure.controller.StructureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ArangoDatabases {

    private static final Logger logger = LoggerFactory.getLogger(ArangoDatabases.class);

    final ArangoDatabaseProxy structureDB;
    final ArangoDatabaseProxy releasedDB;
    final ArangoDatabaseProxy nativeDB;
    final ArangoDatabaseProxy inProgressDB;

    final ArangoDatabaseProxy consistencyChecks;

    public ArangoDatabases(@Qualifier("structure") ArangoDatabaseProxy structureDB, @Qualifier("released") ArangoDatabaseProxy releasedDB, @Qualifier("native") ArangoDatabaseProxy nativeDB, @Qualifier("inProgress") ArangoDatabaseProxy inProgressDB, @Qualifier("consistencyChecks") ArangoDatabaseProxy consistencyChecks) {
        this.releasedDB = releasedDB;
        this.nativeDB = nativeDB;
        this.inProgressDB = inProgressDB;
        this.structureDB = structureDB;
        this.consistencyChecks = consistencyChecks;
    }

    /**
     * Attention! This method wipes all databases and is accordingly meant to be used very carefully!
     */
    public void clearAll(){
        structureDB.removeDatabase();
        releasedDB.removeDatabase();
        nativeDB.removeDatabase();
        inProgressDB.removeDatabase();
        consistencyChecks.removeDatabase();
    }


    @Async
    public void setup(){
        structureDB.createIfItDoesntExist();
        inProgressDB.createIfItDoesntExist();
        releasedDB.createIfItDoesntExist();
        consistencyChecks.createIfItDoesntExist();
        StructureRepository.setupCollections(structureDB);
        logger.debug("Setting up in progress db... ");
        ArangoDatabase inProgress = inProgressDB.get();
        inProgress.getCollections(new CollectionsReadOptions().excludeSystem(true)).parallelStream().forEach(c -> {
            logger.debug(String.format("Ensuring configuration of collection \"%s\" in \"in progress\"", c.getName()));
            ArangoCollection collection = inProgress.collection(c.getName());
            ArangoDatabaseProxy.ensureIndicesOnCollection(collection);
        });
        ArangoDatabase released = releasedDB.get();
        released.getCollections(new CollectionsReadOptions().excludeSystem(true)).parallelStream().forEach(c -> {
            logger.debug(String.format("Ensuring configuration of collection \"%s\" in \"released\"", c.getName()));
            ArangoCollection collection = released.collection(c.getName());
            ArangoDatabaseProxy.ensureIndicesOnCollection(collection);
        });
    }


    public ArangoDatabase getStructureDB(){
        return this.structureDB.getOrCreate();
    }

    public ArangoDatabase getConsistencyChecksDB(){
        return this.consistencyChecks.getOrCreate();
    }

    public ArangoDatabase getByStage(DataStage stage) {
        switch (stage) {
            case NATIVE:
                return nativeDB.getOrCreate();
            case IN_PROGRESS:
                return inProgressDB.getOrCreate();
            case RELEASED:
                return releasedDB.getOrCreate();
            default:
                throw new IllegalArgumentException("Unknown data stage requested: " + stage.name());
        }
    }

}

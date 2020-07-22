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

package eu.ebrains.kg.graphdb.commons.controller;

import com.arangodb.ArangoDatabase;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.commons.model.DataStage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ArangoDatabases {

    @Autowired
    @Qualifier("released")
    ArangoDatabaseProxy releasedDB;

    @Autowired
    @Qualifier("native")
    ArangoDatabaseProxy nativeDB;

    @Autowired
    @Qualifier("inProgress")
    ArangoDatabaseProxy inProgressDB;

    @Autowired
    @Qualifier("releasedMeta")
    ArangoDatabaseProxy releasedMetaDB;

    @Autowired
    @Qualifier("nativeMeta")
    ArangoDatabaseProxy nativeMetaDB;

    @Autowired
    @Qualifier("inProgressMeta")
    ArangoDatabaseProxy inProgressMetaDB;

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

    public ArangoDatabase getMetaByStage(DataStage stage) {
        switch (stage) {
            //The native space doesn't have its own meta data stage -> so we're reflecting on inProgress.
            case NATIVE:
            case IN_PROGRESS:
                return inProgressMetaDB.getOrCreate();
            case RELEASED:
                return releasedMetaDB.getOrCreate();
            default:
                throw new IllegalArgumentException("Unknown data stage requested: " + stage.name());
        }
    }


}

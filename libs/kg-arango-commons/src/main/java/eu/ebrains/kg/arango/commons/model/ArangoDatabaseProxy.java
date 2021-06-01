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

package eu.ebrains.kg.arango.commons.model;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;

public class ArangoDatabaseProxy {

    public final static int ARANGO_TIMEOUT = 10 * 60 * 1000;
    public final static int ARANGO_MAX_CONNECTIONS = 10;

    private final ArangoDB arangoDB;
    private final String databaseName;
    private boolean exists;

    public ArangoDatabaseProxy(ArangoDB arangoDB, String databaseName) {
        this.arangoDB = arangoDB;
        this.databaseName = databaseName;
    }

    public synchronized void createIfItDoesntExist(){
        ArangoDatabase db = arangoDB.db(databaseName);
        if(!db.exists()){
            db.create();
        }
    }

    public ArangoDatabase get(){
        return arangoDB.db(databaseName);
    }

    /**
     * Use {@link #createIfItDoesntExist()} in post construct and {@link #get()} whenever possible to not suffer from the lookup overhead.
     * @return
     */
    public ArangoDatabase getOrCreate(){
        ArangoDatabase db = arangoDB.db(databaseName);
        if(!exists){
            //If the database is flagged to not exist yet, we're asking the database to be sure.
            exists = db.exists();
        }
        if(!exists){
            //The database really doesn't exist -> let's create it.
            db.create();
            exists = true;
        }
        return db;
    }


    public synchronized void createCollectionIfItDoesntExist(String collection){
        ArangoCollection c = get().collection(collection);
        if (!c.exists()) {
            c.create();
        }
    }
}

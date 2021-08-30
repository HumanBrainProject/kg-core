/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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
import com.arangodb.ArangoDatabase;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class PrimaryStoreDBUtils {

    @Cacheable(value="primaryStoreCollection", key="{#db.name(), #c.collectionName}")
    public ArangoCollection getOrCreateArangoCollection(ArangoDatabase db, ArangoCollectionReference c) {
        return ArangoDatabaseProxy.getOrCreateArangoCollection(db, c);
    }

}

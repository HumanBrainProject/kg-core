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

package eu.ebrains.kg.admin.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import com.google.gson.Gson;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ArangoRepository {

    @Autowired
    ArangoDatabaseProxy databaseProxy;

    @Autowired
    Gson gson;

    private static final AqlQueryOptions QUERY_OPTIONS = new AqlQueryOptions();

    public List<JsonLdDoc> getDocuments(ArangoCollectionReference collectionReference) {
        ArangoDatabase arangoDatabase = databaseProxy.getOrCreate();
        ArangoCollection collection = arangoDatabase.collection(collectionReference.getCollectionName());
        if(collection.exists()) {
            String aql = "FOR doc IN  @@space RETURN doc";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("@space", collection.name());
            return arangoDatabase.query(aql, bindVars, QUERY_OPTIONS, JsonLdDoc.class).asListRemaining();
        }
        return Collections.emptyList();
    }

    public <E extends Object> List<E> getEntities(ArangoCollectionReference arangoCollectionReference, Class<E> cl) {
        List<JsonLdDoc> documents = getDocuments(arangoCollectionReference);
        return documents.stream().map(d -> gson.fromJson(gson.toJson(d), cl)).collect(Collectors.toList());
    }

    public <E extends Object> E getEntity(ArangoCollectionReference arangoCollectionReference, String id, Class<E> cl) {
        JsonLdDoc document = databaseProxy.getOrCreate().collection(arangoCollectionReference.getCollectionName()).getDocument(id, JsonLdDoc.class);
        if (document == null) {
            throw new ArangoDBException("Document not found.");
        }
        return gson.fromJson(gson.toJson(document), cl);
    }

    public <E extends Object> void setEntity(E e, String col, String name) {
        ArangoDatabase database = databaseProxy.getOrCreate();
        JsonLdDoc jsonLdDoc = gson.fromJson(gson.toJson(e), JsonLdDoc.class);
        jsonLdDoc.put("_key", name);
        if (database.collection(col).documentExists(name)) {
            database.collection(col).updateDocument(name, jsonLdDoc);
        } else {
            database.collection(col).insertDocument(jsonLdDoc);
        }
    }

    public void deleteEntity(String id, String col) {
        databaseProxy.getOrCreate().collection(col).deleteDocument(id);
    }


}

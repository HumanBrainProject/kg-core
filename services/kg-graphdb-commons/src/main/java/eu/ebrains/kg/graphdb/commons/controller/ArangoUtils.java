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

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.CollectionsReadOptions;
import com.arangodb.model.HashIndexOptions;
import com.arangodb.model.SkiplistIndexOptions;
import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.graphdb.ingestion.model.DocumentRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class ArangoUtils {

    @Autowired
    ArangoDatabases arangoDatabases;

    @Autowired
    IdUtils idUtils;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final CollectionsReadOptions collectionReadOptions = new CollectionsReadOptions().excludeSystem(true);

    public ArangoCollection getOrCreateArangoCollection(ArangoDatabase db, ArangoCollectionReference c) {
        ArangoCollection collection = db.collection(c.getCollectionName());
        if (!collection.exists()) {
            logger.debug(String.format("Creating collection %s", c.getCollectionName()));
            db.createCollection(c.getCollectionName(), new CollectionCreateOptions().waitForSync(true).type(c.isEdge() != null && c.isEdge() ? CollectionType.EDGES : CollectionType.DOCUMENT));
            collection.ensureHashIndex(Collections.singleton(ArangoVocabulary.COLLECTION), new HashIndexOptions());
        }
        collection.ensureSkiplistIndex(Collections.singletonList(JsonLdConsts.ID), new SkiplistIndexOptions());
        if (collection.getInfo().getType() == CollectionType.EDGES) {
            collection.ensureSkiplistIndex(Collections.singletonList(IndexedJsonLdDoc.ORIGINAL_TO), new SkiplistIndexOptions());
        }
        else{
            collection.ensureSkiplistIndex(Collections.singletonList(IndexedJsonLdDoc.IDENTIFIERS+"[*]"), new SkiplistIndexOptions());
            collection.ensureSkiplistIndex(Collections.singletonList(IndexedJsonLdDoc.EMBEDDED), new SkiplistIndexOptions());
        }
        if(c.equals(InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION)){
            collection.ensureSkiplistIndex(Collections.singletonList(DocumentRelation.TARGET_ORIGINAL_DOCUMENT), new SkiplistIndexOptions());
        }
        return collection;
    }

    public boolean isInternalCollection(ArangoCollectionReference collectionReference) {
        return collectionReference.getCollectionName().startsWith("internal");
    }
}

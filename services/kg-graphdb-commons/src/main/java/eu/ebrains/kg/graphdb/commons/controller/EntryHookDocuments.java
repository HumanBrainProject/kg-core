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
import eu.ebrains.kg.arango.commons.conventions.InternalCollections;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.ReleaseStatus;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.commons.model.ArangoEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EntryHookDocuments {


    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ArangoDatabases databases;
    private final IdUtils idUtils;
    private final ArangoUtils utils;

    public EntryHookDocuments(ArangoDatabases databases, ArangoUtils utils, IdUtils idUtils) {
        this.databases = databases;
        this.utils = utils;
        this.idUtils = idUtils;
    }

    public ArangoDocumentReference getOrCreateDocumentIdHookDocument(ArangoDocumentReference documentRef, ArangoDatabase database) {
        return getOrCreateSingleDocumentWithNamePayload(ArangoCollectionReference.fromSpace(InternalSpace.DOCUMENT_ID_SPACE).doc(documentRef.getDocumentId()), documentRef.getId(), database);
    }

    public ArangoDocumentReference getOrCreateTypeHookDocument(DataStage stage, String type) {
        return getOrCreateSingleDocumentWithNamePayload(ArangoCollectionReference.fromSpace(InternalCollections.TYPE_SPACE).docWithStableId(type), type, databases.getByStage(stage));
    }

    public ArangoDocumentReference getOrCreateReleaseStatusHookDocument(boolean doRelease) {
        return getOrCreateSingleDocumentWithNamePayload(ArangoCollectionReference.fromSpace(InternalSpace.RELEASE_STATUS_SPACE).docWithStableId(doRelease ? ReleaseStatus.RELEASED.name() :  ReleaseStatus.HAS_CHANGED.name()), doRelease ? ReleaseStatus.RELEASED.name() : ReleaseStatus.HAS_CHANGED.name(), databases.getByStage(DataStage.LIVE));
    }


    private ArangoDocumentReference getOrCreateSingleDocumentWithNamePayload(ArangoDocumentReference documentId, String name, ArangoDatabase db) {
        logger.trace(String.format("Ensure unresolved document %s exists", name));
        ArangoCollection collection = utils.getOrCreateArangoCollection(db, documentId.getArangoCollectionReference());
        if (!collection.documentExists(documentId.getDocumentId().toString())) {
            ArangoDocument newDoc = ArangoDocument.create();
            newDoc.setReference(documentId);
            newDoc.getDoc().addProperty(SchemaOrgVocabulary.NAME, name);
            collection.insertDocument(newDoc.getDoc());
        }
        return documentId;
    }

    public ArangoEdge createEdgeFromHookDocument(ArangoCollectionReference edgeCollection, ArangoDocumentReference targetDocument, ArangoDocumentReference hookDocument, JsonLdId originalTo) {
        ArangoEdge arangoEdge = new ArangoEdge();
        arangoEdge.setFrom(hookDocument);
        arangoEdge.setTo(targetDocument);
        arangoEdge.setOriginalTo(originalTo);
        arangoEdge.redefineId(new ArangoDocumentReference(edgeCollection,  UUID.randomUUID()));
        return arangoEdge;
    }
}

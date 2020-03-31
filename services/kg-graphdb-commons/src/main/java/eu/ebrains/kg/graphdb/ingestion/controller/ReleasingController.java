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

package eu.ebrains.kg.graphdb.ingestion.controller;


import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.graphdb.commons.controller.EntryHookDocuments;
import eu.ebrains.kg.graphdb.commons.model.ArangoEdge;
import eu.ebrains.kg.graphdb.ingestion.model.UpsertOperation;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class ReleasingController {

    private final EntryHookDocuments entryHookDocuments;

    public ReleasingController(EntryHookDocuments entryHookDocuments) {
        this.entryHookDocuments = entryHookDocuments;
    }

    UpsertOperation getReleaseStatusUpdateOperation(ArangoDocumentReference documentReference, boolean doRelease) {
        ArangoDocumentReference releaseDocument = entryHookDocuments.getOrCreateReleaseStatusHookDocument(doRelease);
        ArangoEdge arangoEdge = entryHookDocuments.createEdgeFromHookDocument(InternalSpace.RELEASE_STATUS_EDGE_COLLECTION, documentReference, releaseDocument, null);
        ArangoDocumentReference releaseInstanceId = getReleaseStatusEdgeId(documentReference);
        arangoEdge.redefineId(releaseInstanceId);
        return new UpsertOperation(documentReference, arangoEdge.dumpPayload(), releaseInstanceId, true, false);
    }

    public ArangoDocumentReference getReleaseStatusEdgeId(ArangoDocumentReference documentReference) {
        return InternalSpace.RELEASE_STATUS_EDGE_COLLECTION.doc(UUID.nameUUIDFromBytes(("releaseStatusInstance"+documentReference.getDocumentId().toString()).getBytes(StandardCharsets.UTF_8)));
    }

}

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

package eu.ebrains.kg.graphdb.ingestion.model;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;

import java.util.List;
import java.util.UUID;

public class ArangoUpsertItem {

    final DataStage stage;
    final NormalizedJsonLd payload;
    final ArangoDocumentReference arangoDocumentReference;
    final List<JsonLdId> mergeIds;


    public ArangoUpsertItem(UUID originalDocumentId, SpaceName originalSpace, NormalizedJsonLd payload, List<JsonLdId> mergeIds, DataStage stage) {
        this.payload = payload;
        this.arangoDocumentReference = ArangoCollectionReference.fromSpace(originalSpace).doc(originalDocumentId);
        this.mergeIds = mergeIds;
        this.stage = stage;
    }

    public List<JsonLdId> getMergeIds() {
        return mergeIds;
    }

    public NormalizedJsonLd getPayload() {
        return payload;
    }

    public ArangoDocumentReference getArangoDocumentReference() {
        return arangoDocumentReference;
    }

    public DataStage getStage() {
        return stage;
    }
}

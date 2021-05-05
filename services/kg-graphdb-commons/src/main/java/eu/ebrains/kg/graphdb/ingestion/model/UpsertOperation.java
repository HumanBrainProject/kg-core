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

import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;

import java.util.Objects;

public class UpsertOperation implements DBOperation {

    private final ArangoDocumentReference lifecycleDocumentId;
    private final NormalizedJsonLd payload;
    private final ArangoDocumentReference documentReference;
    private final boolean overrideIfExists;
    private final boolean attachToOriginalDocument;

    public UpsertOperation(ArangoDocumentReference lifecycleDocumentId, NormalizedJsonLd payload, ArangoDocumentReference documentReference) {
        this(lifecycleDocumentId, payload, documentReference, true, true);
    }

    public UpsertOperation(ArangoDocumentReference originalDocumentId, NormalizedJsonLd payload, ArangoDocumentReference documentReference, boolean overrideIfExists, boolean attachToOriginalDocument) {
        this.lifecycleDocumentId = originalDocumentId;
        this.payload = payload;
        this.documentReference = documentReference;
        this.overrideIfExists = overrideIfExists;
        this.attachToOriginalDocument = attachToOriginalDocument;
    }

    public ArangoDocumentReference getDocumentReference() {
        return documentReference;
    }

    public NormalizedJsonLd getPayload() {
        return payload;
    }

    public ArangoDocumentReference getLifecycleDocumentId() {
        return lifecycleDocumentId;
    }

    public boolean isOverrideIfExists() {
        return overrideIfExists;
    }

    public boolean isAttachToOriginalDocument() {
        return attachToOriginalDocument;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpsertOperation that = (UpsertOperation) o;
        return attachToOriginalDocument == that.attachToOriginalDocument &&
                Objects.equals(lifecycleDocumentId, that.lifecycleDocumentId) &&
                Objects.equals(payload, that.payload) &&
                Objects.equals(documentReference, that.documentReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lifecycleDocumentId, payload, documentReference, attachToOriginalDocument);
    }
}

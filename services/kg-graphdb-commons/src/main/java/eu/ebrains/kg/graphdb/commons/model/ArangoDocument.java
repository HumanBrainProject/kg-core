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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.graphdb.commons.model;

import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An arango document is a JSON-LD with specific properties:
 * <p>
 * 1. It's flat - meaning, there are no nested constructs except for lists on the first level as well as edge-maps ("@id") indicating links.
 * All originally nested structures have been extracted into other Arango documents and have been linked.
 * They share the common @documentId property though for easy lookup and lifecycle management.
 * <p>
 * 2. Its compacted and contextless.
 * <p>
 * 3. It contains additional, arango-internal fields (such as "_key" or "_id")
 */
public class ArangoDocument implements ArangoInstance {
    private final IndexedJsonLdDoc indexedDoc;

    private ArangoDocument(IndexedJsonLdDoc indexedDoc) {
        this.indexedDoc = indexedDoc;
    }

    public static ArangoDocument from(IndexedJsonLdDoc indexedDoc) {
        return indexedDoc != null ? new ArangoDocument(indexedDoc) : null;
    }

    public static ArangoDocument from(NormalizedJsonLd jsonLdDoc) {
        return from(IndexedJsonLdDoc.from(jsonLdDoc));
    }

    public static ArangoDocument create() {
        return from(new NormalizedJsonLd());
    }

    public IndexedJsonLdDoc asIndexedDoc() {
        return indexedDoc;
    }

    public NormalizedJsonLd getDoc() {
        return indexedDoc.getDoc();
    }

    @Override
    public ArangoDocumentReference getId() {
        return ArangoDocumentReference.fromArangoId(indexedDoc.getDoc().getAs(ArangoVocabulary.ID, String.class), false);
    }

    public void setKeyBasedOnId() {
        indexedDoc.getDoc().put(ArangoVocabulary.KEY, getId() != null && getId().getDocumentId() != null ? getId().getDocumentId().toString() : null);
    }

    public void setReference(ArangoDocumentReference reference) {
        indexedDoc.getDoc().put(ArangoVocabulary.ID, reference != null ? reference.getId() : null);
        setKeyBasedOnId();
    }

    public ArangoDocumentReference getOriginalDocument() {
        String originalDocument = indexedDoc.getDoc().getAs(IndexedJsonLdDoc.ORIGINAL_DOCUMENT, String.class);
        return originalDocument != null ? ArangoDocumentReference.fromArangoId(originalDocument, false) : null;
    }

    public void setOriginalDocument(ArangoDocumentReference originalDocument) {
        indexedDoc.getDoc().put(IndexedJsonLdDoc.ORIGINAL_DOCUMENT, originalDocument.getId());
    }

    @Override
    public Object getPayload() {
        return getDoc();
    }

    public void applyResolvedEdges(Set<ArangoEdge> resolvedEdges) {
        Map<String, JsonLdId> oldToNew = resolvedEdges.stream().collect(Collectors.toMap(k -> k.getOriginalTo().getId(), ArangoEdge::getResolvedTargetId));
        NormalizedJsonLd doc = indexedDoc.getDoc();
        for (String key : doc.keySet()) {
            List<JsonLdId> jsonldIds = doc.getAsListOf(key, JsonLdId.class, true);
            if (!jsonldIds.isEmpty()) {
                List<JsonLdId> newJsonLds = jsonldIds.stream().map(jsonLdId -> {
                    JsonLdId newValue = oldToNew.get(jsonLdId.getId());
                    return newValue != null ? newValue : jsonLdId;
                }).collect(Collectors.toList());
                doc.put(key, newJsonLds);
            }
        }
    }

}

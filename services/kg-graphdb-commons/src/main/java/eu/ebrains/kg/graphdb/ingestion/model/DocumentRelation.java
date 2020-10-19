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

package eu.ebrains.kg.graphdb.ingestion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.graphdb.commons.model.ArangoEdge;

import java.util.List;

public class DocumentRelation extends ArangoEdge {

    public static final String TARGET_ORIGINAL_DOCUMENT = "_targetOriginalDocument";

    @JsonProperty("_docCollection")
    private String docCollection;

    @JsonProperty("_docTypes")
    private List<String> docTypes;

    @JsonProperty(TARGET_ORIGINAL_DOCUMENT)
    private String targetDocument;

    public String getDocCollection() {
        return docCollection;
    }

    public void setDocCollection(String docCollection) {
        this.docCollection = docCollection;
    }

    public void setTargetDocument(ArangoDocumentReference targetOriginalDocument) {
        this.targetDocument = targetOriginalDocument != null ? targetOriginalDocument.getId() : null;
    }

    public List<String> getDocTypes() {
        return docTypes;
    }

    public void setDocTypes(List<String> docTypes) {
        this.docTypes = docTypes;
    }
}

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

package eu.ebrains.kg.commons.jsonld;

import eu.ebrains.kg.commons.semantics.vocabularies.HBPVocabulary;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class NormalizedJsonLdDocWithMeta {

    private final NormalizedJsonLd document;

    private final Map<String, Object> metadata = new TreeMap<>();

    public NormalizedJsonLd getDoc() {
        return document;
    }

    protected NormalizedJsonLdDocWithMeta(NormalizedJsonLd document) {
        this.document = document;
        this.document.addProperty(HBPVocabulary.NAMESPACE + "metadata", metadata);
    }

    public static NormalizedJsonLdDocWithMeta create() {
        return new NormalizedJsonLdDocWithMeta(new NormalizedJsonLd());
    }

    public void setCreatedBy(JsonLdId createdBy) {
        this.metadata.put(HBPVocabulary.NAMESPACE + "createdBy", createdBy);
    }

    public void setLastUpdateBy(JsonLdId lastUpdateBy) {
        this.metadata.put(HBPVocabulary.NAMESPACE + "lastUpdateBy", lastUpdateBy);
    }

    public void setCreatedAt(Date createdAt) {
        this.metadata.put(HBPVocabulary.NAMESPACE + "createdAt", createdAt);
    }

    public void setLastUpdateAt(Date lastUpdateAt) {
        this.metadata.put(HBPVocabulary.NAMESPACE + "lastUpdateAt", lastUpdateAt);
    }

    public void setNumberOfEdits(Integer numberOfEdits) {
        this.metadata.put(HBPVocabulary.NAMESPACE + "numberOfEdits", numberOfEdits);
    }

}

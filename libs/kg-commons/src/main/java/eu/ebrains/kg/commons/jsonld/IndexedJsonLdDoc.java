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

package eu.ebrains.kg.commons.jsonld;

import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 *
 */
public class IndexedJsonLdDoc {
    public static final String COLLECTION = "_collection";
    private static final String USER_ID = "_userId";
    private static final String TIMESTAMP = "_timestamp";
    private static final String INDEX_TIMESTAMP = "_indexTimestamp";
    private static final String REV = "_rev";
    public static final String EMBEDDED = "_embedded";
    public static final String DOCUMENT_ID = "_documentId";
    public static final String ALTERNATIVE = "_alternative";
    public static final String ORIGINAL_DOCUMENT = "_originalDocument";
    public static final String DOC_COLLECTION = "_docCollection";
    public static final String DOC_TYPES = "_docTypes";
    public static final String ORIGINAL_TO = "_originalTo";
    public static final String IDENTIFIERS = "_identifiers";

    public static List<String> INTERNAL_FIELDS = Arrays.asList(COLLECTION, USER_ID, TIMESTAMP, INDEX_TIMESTAMP, REV, EMBEDDED, DOCUMENT_ID, ORIGINAL_DOCUMENT);

    private final NormalizedJsonLd document;

    protected IndexedJsonLdDoc(NormalizedJsonLd document) {
        this.document = document;
    }

    public static IndexedJsonLdDoc from(NormalizedJsonLd document){
        return document!=null ? new IndexedJsonLdDoc(document) : null;
    }

    public boolean hasRevision(String revision){
        return Objects.equals(document.getAs(REV, String.class), revision);
    }

    public String getRevision(){
        return document.getAs(REV, String.class);
    }

    public static IndexedJsonLdDoc create(){
        return new IndexedJsonLdDoc(new NormalizedJsonLd());
    }

    public NormalizedJsonLd getDoc() {
        return document;
    }

    public void setUserId(String userId){
        document.addProperty(USER_ID, userId);
    }

    public String getUserId(){
        return document.getAs(USER_ID, String.class);
    }

    public void setTimestamp(Long timestamp){
        document.addProperty(TIMESTAMP, timestamp);
    }

    public Long getTimestamp(){
        return document.getAs(TIMESTAMP, Long.class);
    }

    public void setIndexTimestamp(Long timestamp){
        document.addProperty(INDEX_TIMESTAMP, timestamp);
    }

    public Long getIndexTimestamp(){
        return document.getAs(INDEX_TIMESTAMP, Long.class);
    }

    public void setCollection(String collection){
        document.addProperty(EBRAINSVocabulary.META_SPACE, collection);
        document.addProperty(COLLECTION, collection);
    }

    public String getCollection(){
        return document.getAs(COLLECTION, String.class);
    }

    public void setEmbedded(boolean embedded){
        getDoc().put(EMBEDDED, embedded);
    }

    public boolean isEmbedded(){
        Boolean embedded = getDoc().getAs(EMBEDDED, Boolean.class);
        return embedded != null && embedded.booleanValue();
    }

    public void setAlternative(boolean alternative){
        getDoc().put(ALTERNATIVE, alternative);
    }

    public boolean isAlternative(){
        Boolean alternative = getDoc().getAs(ALTERNATIVE, Boolean.class);
        return alternative != null && alternative.booleanValue();
    }


    public void setDocumentId(UUID documentId){
        if(documentId!=null) {
            getDoc().put(DOCUMENT_ID, documentId);
        }
        else{
            getDoc().remove(DOCUMENT_ID);
        }
    }

    public UUID getDocumentId(){
        String documentId = getDoc().getAs(DOCUMENT_ID, String.class);
        return documentId!=null ? UUID.fromString(documentId) : null;
    }

    public void updateIdentifiers(){
        getDoc().put(IDENTIFIERS, getDoc().allIdentifiersIncludingId());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexedJsonLdDoc that = (IndexedJsonLdDoc) o;
        return Objects.equals(document, that.document);
    }

    @Override
    public int hashCode() {
        return Objects.hash(document);
    }
}

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

package eu.ebrains.kg.commons.query;

import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.EntityId;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;

import java.util.List;

public class KgQuery {

    private NormalizedJsonLd payload;
    private DataStage stage;
    private List<EntityId> idRestrictions;

    public KgQuery() {
    }

    public KgQuery(NormalizedJsonLd payload, DataStage stage) {
        this.payload = payload;
        this.stage = stage;
    }

    public NormalizedJsonLd getPayload() {
        return payload;
    }

    public KgQuery setPayload(NormalizedJsonLd payload) {
        this.payload = payload;
        return this;
    }

    public DataStage getStage() {
        return stage;
    }

    public KgQuery setStage(DataStage stage) {
        this.stage = stage;
        return this;
    }

    public List<EntityId> getIdRestrictions() {
        return idRestrictions;
    }

    public KgQuery setIdRestrictions(List<EntityId> idRestrictions) {
        this.idRestrictions = idRestrictions;
        return this;
    }

    public static String getKgQueryType() {
        return EBRAINSVocabulary.META_TYPE + "/Query";
    }
}

/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

package eu.ebrains.kg.commons.query;

import eu.ebrains.kg.commons.exception.MissingQueryFieldsException;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;

import java.util.List;

public class KgQuery {

    private NormalizedJsonLd payload;
    private DataStage stage;
    private InstanceId idRestriction;
    private List<SpaceName> restrictToSpaces;

    public KgQuery() {
    }

    public KgQuery(NormalizedJsonLd payload, DataStage stage) {
        this.payload = payload;
        this.stage = stage;
        this.validateQuery();
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

    public InstanceId getIdRestriction() {
        return idRestriction;
    }

    public KgQuery setIdRestriction(InstanceId idRestriction) {
        this.idRestriction = idRestriction;
        return this;
    }

    public List<SpaceName> getRestrictToSpaces() {
        return restrictToSpaces;
    }

    public KgQuery setRestrictToSpaces(List<SpaceName> restrictToSpaces) {
        this.restrictToSpaces = restrictToSpaces;
        return this;
    }

    private void validateQuery(){
        if (this.payload == null || this.payload.size() == 0) {
            throw new MissingQueryFieldsException("The provided query is empty");
        }
        NormalizedJsonLd meta = this.payload.getAs(EBRAINSVocabulary.QUERY_META, NormalizedJsonLd.class);
        if (meta == null) {
            throw new MissingQueryFieldsException(String.format("The query provided is missing a value for %s", EBRAINSVocabulary.QUERY_META));
        } else {
            String type = meta.getAs(EBRAINSVocabulary.QUERY_TYPE, String.class);
            if (type == null || type.isEmpty()) {
                throw new MissingQueryFieldsException(String.format("The query provided is missing a value for %s in %s",EBRAINSVocabulary.QUERY_TYPE, EBRAINSVocabulary.QUERY_META));
            }
        }
        List<NormalizedJsonLd> structure = this.payload.getAsListOf(EBRAINSVocabulary.QUERY_STRUCTURE, NormalizedJsonLd.class);
        if (structure == null || structure.isEmpty()) {
            throw new MissingQueryFieldsException(String.format("The query provided is missing a value for %s", EBRAINSVocabulary.QUERY_STRUCTURE));
        }
    }
}

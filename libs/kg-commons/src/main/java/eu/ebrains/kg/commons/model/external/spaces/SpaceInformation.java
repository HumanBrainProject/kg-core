/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.commons.model.external.spaces;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.ebrains.kg.commons.jsonld.DynamicJson;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;

import java.util.List;

public class SpaceInformation extends DynamicJson {

    @JsonGetter(SchemaOrgVocabulary.IDENTIFIER)
    public String getIdentifier() {
        return getAs(SchemaOrgVocabulary.IDENTIFIER, String.class);
    }

    @JsonSetter(SchemaOrgVocabulary.IDENTIFIER)
    public void setIdentifier(String identifier) {
        this.put(SchemaOrgVocabulary.IDENTIFIER, identifier);
    }

    @JsonGetter(SchemaOrgVocabulary.NAME)
    public String getName() {
        return getAs(SchemaOrgVocabulary.NAME, String.class);
    }

    @JsonSetter(SchemaOrgVocabulary.NAME)
    public void setName(String name) {
        this.put(SchemaOrgVocabulary.NAME, name);
    }

    @JsonGetter(EBRAINSVocabulary.META_PERMISSIONS)
    public List<Functionality> getPermissions() {
        return getAsListOf(EBRAINSVocabulary.META_PERMISSIONS, Functionality.class);
    }

    @JsonSetter(EBRAINSVocabulary.META_PERMISSIONS)
    public void setPermissions(List<Functionality> permissions) {
        this.put(EBRAINSVocabulary.META_PERMISSIONS, permissions);
    }
}
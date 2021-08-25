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

package eu.ebrains.kg.commons.model.external.types;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.ebrains.kg.commons.jsonld.DynamicJson;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;

import java.util.List;
@JsonPropertyOrder(alphabetic=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Property extends DynamicJson {

    @JsonGetter(SchemaOrgVocabulary.IDENTIFIER)
    public String getIdentifier() {
        return getAs(SchemaOrgVocabulary.IDENTIFIER, String.class);
    }

    @JsonSetter(SchemaOrgVocabulary.IDENTIFIER)
    public void setIdentifier(String identifier) {
        put(SchemaOrgVocabulary.IDENTIFIER, identifier);
    }

    @JsonGetter(SchemaOrgVocabulary.DESCRIPTION)
    public String getDescription() {
        return getAs(SchemaOrgVocabulary.DESCRIPTION, String.class);
    }

    @JsonSetter(SchemaOrgVocabulary.DESCRIPTION)
    public void setDescription(String description) {
        put(SchemaOrgVocabulary.DESCRIPTION, description);
    }

    @JsonGetter(SchemaOrgVocabulary.NAME)
    public String getName() {
        return getAs(SchemaOrgVocabulary.NAME, String.class);
    }

    @JsonSetter(SchemaOrgVocabulary.NAME)
    public void setName(String name) {
        put(SchemaOrgVocabulary.NAME, name);
    }

    @JsonGetter(EBRAINSVocabulary.META_OCCURRENCES)
    public Integer getOccurrences() {
        return getAs(EBRAINSVocabulary.META_OCCURRENCES, Integer.class);
    }

    @JsonSetter(EBRAINSVocabulary.META_OCCURRENCES)
    public void setOccurrences(Integer occurrences) {
        put(EBRAINSVocabulary.META_OCCURRENCES, occurrences);
    }

    @JsonGetter(EBRAINSVocabulary.META_NAME_REVERSE_LINK)
    public String getNameForReverseLink() {
        return getAs(EBRAINSVocabulary.META_NAME_REVERSE_LINK, String.class);
    }

    @JsonSetter(EBRAINSVocabulary.META_NAME_REVERSE_LINK)
    public void setNameForReverseLink(String nameForReverseLink) {
        put(EBRAINSVocabulary.META_NAME_REVERSE_LINK, nameForReverseLink);
    }

    @JsonGetter(EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES)
    public List<TargetType> getTargetTypes() {
        return getAsListOf(EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES, TargetType.class);
    }

    @JsonSetter(EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES)
    public void setTargetTypes(List<TargetType> targetTypes) {
        put(EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES, targetTypes);
    }

}

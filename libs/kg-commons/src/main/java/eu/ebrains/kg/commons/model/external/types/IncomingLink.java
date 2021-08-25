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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;

import java.util.List;
import java.util.Objects;
@JsonPropertyOrder(alphabetic=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncomingLink {

    @JsonProperty(SchemaOrgVocabulary.IDENTIFIER)
    private String identifier;

    @JsonProperty(EBRAINSVocabulary.META_PROPERTY_SOURCE_TYPES)
    private List<SourceType> sourceTypes;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<SourceType> getSourceTypes() {
        return sourceTypes;
    }

    public void setSourceTypes(List<SourceType> sourceTypes) {
        this.sourceTypes = sourceTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IncomingLink that = (IncomingLink) o;
        return Objects.equals(identifier, that.identifier) && Objects.equals(sourceTypes, that.sourceTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, sourceTypes);
    }
}

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

import java.util.Objects;
@JsonPropertyOrder(alphabetic=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpaceReference {

    @JsonProperty(EBRAINSVocabulary.META_SPACE)
    private String space;

    @JsonProperty(EBRAINSVocabulary.META_OCCURRENCES)
    private Integer occurrences;

    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public Integer getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(Integer occurrences) {
        this.occurrences = occurrences;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpaceReference that = (SpaceReference) o;
        return Objects.equals(space, that.space) && Objects.equals(occurrences, that.occurrences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(space, occurrences);
    }
}

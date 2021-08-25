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

import java.util.List;
@JsonPropertyOrder(alphabetic=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TargetType {

    @JsonProperty(EBRAINSVocabulary.META_OCCURRENCES)
    private Integer occurrences;

    @JsonProperty(EBRAINSVocabulary.META_SPACES)
    private List<SpaceReference> spaces;

    @JsonProperty(EBRAINSVocabulary.META_TYPE)
    private String type;


    public Integer getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(Integer occurrences) {
        this.occurrences = occurrences;
    }

    public List<SpaceReference> getSpaces() {
        return spaces;
    }

    public void setSpaces(List<SpaceReference> spaces) {
        this.spaces = spaces;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

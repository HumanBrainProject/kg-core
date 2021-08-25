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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.ebrains.kg.commons.model.internal.spaces.Space;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;


public class SpaceSpecification {

    @JsonProperty(SchemaOrgVocabulary.NAME)
    private String name;

    @JsonProperty(SchemaOrgVocabulary.IDENTIFIER)
    private String identifier;

    @JsonProperty(EBRAINSVocabulary.META_AUTORELEASE_SPACE)
    private Boolean autoRelease;

    @JsonProperty(EBRAINSVocabulary.META_CLIENT_SPACE)
    private Boolean clientSpace;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Boolean getAutoRelease() {
        return autoRelease;
    }

    public void setAutoRelease(Boolean autoRelease) {
        this.autoRelease = autoRelease;
    }

    public Boolean getClientSpace() {
        return clientSpace;
    }

    public void setClientSpace(Boolean clientSpace) {
        this.clientSpace = clientSpace;
    }

    @JsonIgnore
    public Space toSpace(){
       return new Space(new SpaceName(name), autoRelease != null && autoRelease, false);
    }

}

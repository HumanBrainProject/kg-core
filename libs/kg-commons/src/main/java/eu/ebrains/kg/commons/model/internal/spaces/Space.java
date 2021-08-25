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

package eu.ebrains.kg.commons.model.internal.spaces;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import eu.ebrains.kg.commons.jsonld.DynamicJson;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.external.spaces.SpaceInformation;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.Permission;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;

import java.beans.Transient;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Space extends DynamicJson {

    public Space(){}

    public Space(SpaceName name, boolean autoRelease, boolean clientSpace) {
        setName(name);
        setIdentifier(name.getName());
        setAutoRelease(autoRelease);
        setClientSpace(clientSpace);
    }
    @JsonIgnore
    private boolean existsInDB;

    @JsonIgnore
    public boolean isExistsInDB() {
        return existsInDB;
    }

    @JsonIgnore
    public void setExistsInDB(boolean existsInDB) {
        this.existsInDB = existsInDB;
    }

    @JsonIgnore
    private boolean reflected;

    @JsonIgnore
    public boolean isReflected() {
        return reflected;
    }


    @JsonIgnore
    public Space setReflected(boolean reflected) {
        this.reflected = reflected;
        return this;
    }

    @JsonSetter(SchemaOrgVocabulary.IDENTIFIER)
    public void setIdentifier(String name) {
        this.put(SchemaOrgVocabulary.IDENTIFIER, name);
    }

    @JsonGetter(SchemaOrgVocabulary.IDENTIFIER)
    public String getIdentifier() {
        return this.getAs(SchemaOrgVocabulary.IDENTIFIER, String.class);
    }

    @JsonSetter(SchemaOrgVocabulary.NAME)
    public void setName(SpaceName name) {
        this.put(SchemaOrgVocabulary.NAME, name!=null ? name.getName() : null);
    }

    @JsonGetter(SchemaOrgVocabulary.NAME)
    public SpaceName getName() {
        return this.getAs(SchemaOrgVocabulary.NAME, SpaceName.class);
    }


    @Override
    @JsonIgnore
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Space)) return false;
        Space space = (Space) o;
        return Objects.equals(getName(), space.getName());
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        return Objects.hash(getName());
    }

    @JsonGetter(EBRAINSVocabulary.META_CLIENT_SPACE)
    public boolean isClientSpace() {
        return getAs(EBRAINSVocabulary.META_CLIENT_SPACE, Boolean.class, false);
    }

    @JsonSetter(EBRAINSVocabulary.META_CLIENT_SPACE)
    public void setClientSpace(Boolean clientSpace) {
        this.put(EBRAINSVocabulary.META_CLIENT_SPACE, clientSpace);
    }

    @JsonGetter(EBRAINSVocabulary.META_INTERNAL_SPACE)
    public boolean isInternalSpace() {
        return getAs(EBRAINSVocabulary.META_INTERNAL_SPACE, Boolean.class, false);
    }

    @JsonSetter(EBRAINSVocabulary.META_INTERNAL_SPACE)
    public void setInternalSpace(Boolean internalSpace) {
        this.put(EBRAINSVocabulary.META_CLIENT_SPACE, internalSpace);
    }

    @JsonGetter(EBRAINSVocabulary.META_AUTORELEASE_SPACE)
    public boolean isAutoRelease() {
        return getAs(EBRAINSVocabulary.META_AUTORELEASE_SPACE, Boolean.class, false);
    }

    @JsonSetter(EBRAINSVocabulary.META_AUTORELEASE_SPACE)
    public void setAutoRelease(Boolean autoRelease) {
        this.put(EBRAINSVocabulary.META_AUTORELEASE_SPACE, autoRelease);
    }

    @JsonIgnore
    public static JsonLdId createId(SpaceName name){
        return EBRAINSVocabulary.createIdForStructureDefinition("spaces", name.getName());
    }

    @JsonIgnore
    public SpaceInformation toSpaceInformation(){
        SpaceInformation information = new SpaceInformation();
        information.setName(getName() != null ? getName().getName() : null);
        information.setIdentifier(getIdentifier());
        keySet().stream().filter(k -> k.startsWith(String.format("%s/", EBRAINSVocabulary.META_SPACE))).forEach(k -> {
            information.put(k, get(k));
        });
        return information;

    }


}


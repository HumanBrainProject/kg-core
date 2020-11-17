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

package eu.ebrains.kg.commons.model;

import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;

import java.util.Objects;
import java.util.Set;

public class Space {

    private SpaceName name;
    private boolean autoRelease;
    private Set<Functionality> permissions;

    private Space(){}

    public Space(SpaceName name, boolean autoRelease) {
        setName(name);
        setAutoRelease(autoRelease);
    }

    public void setName(SpaceName name) {
        this.name = name;
    }

    public SpaceName getName() {
        return name;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Space)) return false;
        Space space = (Space) o;
        return Objects.equals(name, space.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public Set<Functionality> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Functionality> permissions) {
        this.permissions = permissions;
    }


    public NormalizedJsonLd toJsonLd() {
        NormalizedJsonLd payload = new NormalizedJsonLd();
        payload.put(JsonLdConsts.TYPE, EBRAINSVocabulary.META_SPACEDEFINITION_TYPE);
        payload.setId(createId(name));
        payload.put(SchemaOrgVocabulary.NAME, getName());
        payload.put(SchemaOrgVocabulary.IDENTIFIER, getName());
        payload.put(EBRAINSVocabulary.META_SPACE, getName());
        payload.put(EBRAINSVocabulary.META_AUTORELEASE_SPACE, isAutoRelease());
        return payload;
    }

    public static JsonLdId createId(SpaceName name){
        return EBRAINSVocabulary.createIdForStructureDefinition("spaces", name.getName());
    }


    public boolean isAutoRelease() {
        return autoRelease;
    }

    public void setAutoRelease(boolean autoRelease) {
        this.autoRelease = autoRelease;
    }

    public static Space fromJsonLd(NormalizedJsonLd payload) {
        if (payload != null) {
            Space space = new Space();
            space.setName(payload.getAs(SchemaOrgVocabulary.NAME, SpaceName.class));
            space.setAutoRelease(payload.getAs(EBRAINSVocabulary.META_AUTORELEASE_SPACE, Boolean.class, false));
            return space;
        }
        return null;
    }

}


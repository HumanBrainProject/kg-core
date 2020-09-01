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
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;

import java.util.Objects;
import java.util.Set;

public class Space {

    private String name;
    private boolean clientSpace;
    private boolean autoRelease;
    private Set<Functionality> permissions;

    public Space() {
        this(null);
    }

    public Space(String name){
        this(name, false, false);
    }

    public Space(String name, boolean clientSpace, boolean autoRelease) {
        setName(name);
        setClientSpace(clientSpace);
        setAutoRelease(autoRelease);
    }

    protected String normalizeName(String name) {
        return name!=null ? name.replaceAll("_", "-") : null;
    }

    public void setName(String name) {
        this.name = normalizeName(name);
    }

    public String getName() {
        return name;
    }

    public boolean isClientSpace() {
        return clientSpace;
    }

    public void setClientSpace(boolean clientSpace) {
        this.clientSpace = clientSpace;
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
        payload.setId(EBRAINSVocabulary.createIdForStructureDefinition("spaces", getName()));
        payload.put(SchemaOrgVocabulary.NAME, getName());
        payload.put(SchemaOrgVocabulary.IDENTIFIER, getName());
        payload.put(EBRAINSVocabulary.META_SPACE, getName());
        payload.put(EBRAINSVocabulary.META_CLIENT_SPACE, isClientSpace());
        payload.put(EBRAINSVocabulary.META_AUTORELEASE_SPACE, isAutoRelease());
        return payload;
    }

    public boolean isAutoRelease() {
        return autoRelease;
    }

    public void setAutoRelease(boolean autoRelease) {
        this.autoRelease = autoRelease;
    }

    public static Space fromJsonLd(NormalizedJsonLd payload) {
        if (payload != null && payload.getTypes().contains(EBRAINSVocabulary.META_SPACEDEFINITION_TYPE)) {
            Space space = new Space();
            space.setName(payload.getAs(SchemaOrgVocabulary.NAME, String.class));
            space.setClientSpace(payload.getAs(EBRAINSVocabulary.META_CLIENT_SPACE, Boolean.class, false));
            space.setAutoRelease(payload.getAs(EBRAINSVocabulary.META_AUTORELEASE_SPACE, Boolean.class, false));
            return space;
        }
        return null;
    }

}


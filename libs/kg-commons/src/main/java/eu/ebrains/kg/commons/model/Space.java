/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
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
    private boolean clientSpace;
    private boolean internalSpace;
    private Set<Functionality> permissions;

    private Space(){}


    public Space(SpaceName name, boolean autoRelease, boolean clientSpace) {
        setName(name);
        setAutoRelease(autoRelease);
        setClientSpace(clientSpace);
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

    public boolean isClientSpace() {
        return clientSpace;
    }

    public void setClientSpace(boolean clientSpace) {
        this.clientSpace = clientSpace;
    }

    public boolean isInternalSpace() {
        return internalSpace;
    }

    public void setInternalSpace(boolean internalSpace) {
        this.internalSpace = internalSpace;
    }

    public NormalizedJsonLd toJsonLd() {
        NormalizedJsonLd payload = new NormalizedJsonLd();
        String spaceName = getName() != null ? getName().getName() : null;
        payload.put(JsonLdConsts.TYPE, EBRAINSVocabulary.META_SPACEDEFINITION_TYPE);
        payload.setId(createId(name));
        payload.put(SchemaOrgVocabulary.NAME, spaceName);
        payload.put(SchemaOrgVocabulary.IDENTIFIER, spaceName);
        payload.put(EBRAINSVocabulary.META_SPACE, spaceName);
        if(isAutoRelease()) {
            payload.put(EBRAINSVocabulary.META_AUTORELEASE_SPACE, isAutoRelease());
        }
        else{
            payload.remove(EBRAINSVocabulary.META_AUTORELEASE_SPACE);
        }
        if(isClientSpace()) {
            payload.put(EBRAINSVocabulary.META_CLIENT_SPACE, isClientSpace());
        }
        else{
            payload.remove(EBRAINSVocabulary.META_CLIENT_SPACE);
        }
        if(isInternalSpace()) {
            payload.put(EBRAINSVocabulary.META_INTERNAL_SPACE, isInternalSpace());
        }
        else {
            payload.remove(EBRAINSVocabulary.META_INTERNAL_SPACE);
        }
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
            space.setClientSpace(payload.getAs(EBRAINSVocabulary.META_CLIENT_SPACE, Boolean.class, false));
            space.setInternalSpace(payload.getAs(EBRAINSVocabulary.META_INTERNAL_SPACE, Boolean.class, false));
            return space;
        }
        return null;
    }

}


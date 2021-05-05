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

import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Type {

    private String name;
    private String labelProperty;
    private transient Boolean ignoreIncomingLinks;

    public String getLabelProperty() {
        return labelProperty;
    }

    public void setLabelProperty(String labelProperty) {
        this.labelProperty = labelProperty;
    }

    public Type() {
    }

    public Type(String name) {
        this.name = URLDecoder.decode(name, StandardCharsets.UTF_8);
    }

    public String getName() {
        return name;
    }

    public String getEncodedName() {
        return URLEncoder.encode(getName(), StandardCharsets.UTF_8);
    }

    public Boolean getIgnoreIncomingLinks() {
        return ignoreIncomingLinks;
    }

    public void setIgnoreIncomingLinks(Boolean ignoreIncomingLinks) {
        this.ignoreIncomingLinks = ignoreIncomingLinks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Type type = (Type) o;
        return Objects.equals(name, type.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public static Type fromPayload(NormalizedJsonLd payload) {
        Type targetType = new Type(payload.primaryIdentifier());
        targetType.setLabelProperty(payload.getAs(EBRAINSVocabulary.META_TYPE_LABEL_PROPERTY, String.class));
        targetType.setIgnoreIncomingLinks(payload.getAs(EBRAINSVocabulary.META_IGNORE_INCOMING_LINKS, Boolean.class));
        return targetType;
    }

    public static String labelFromName(String fullyQualifiedName) {
        if (fullyQualifiedName != null) {
            if (fullyQualifiedName.startsWith("@")) {
                return fullyQualifiedName.replace("@", "");
            }
            if (fullyQualifiedName.lastIndexOf("#") > -1) {
                return fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf("#") + 1);
            }
            try {
                URI uri = new URI(fullyQualifiedName);
                return uri.getPath() != null ? uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1) : null;
            } catch (URISyntaxException e) {
                return fullyQualifiedName;
            }
        }
        return null;
    }


}

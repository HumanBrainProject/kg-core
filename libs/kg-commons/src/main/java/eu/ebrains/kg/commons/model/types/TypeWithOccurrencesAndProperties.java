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

package eu.ebrains.kg.commons.model.types;

import java.util.List;
import java.util.Objects;

public class TypeWithOccurrencesAndProperties {

    private String type;
    private Long occurrences;
    private List<Property> properties;
    private List<String> spaces;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(Long occurrences) {
        this.occurrences = occurrences;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public List<String> getSpaces() {
        return spaces;
    }

    public void setSpaces(List<String> spaces) {
        this.spaces = spaces;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeWithOccurrencesAndProperties type1 = (TypeWithOccurrencesAndProperties) o;
        return Objects.equals(type, type1.type) && Objects.equals(occurrences, type1.occurrences) && Objects.equals(properties, type1.properties) && Objects.equals(spaces, type1.spaces);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, occurrences, properties, spaces);
    }
}

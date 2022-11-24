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

public class PropertyReflection {

    private String property;

    private Long occurrences;

    private List<TargetTypeWithOccurrence> targetTypes;

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public Long getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(Long occurrences) {
        this.occurrences = occurrences;
    }

    public List<TargetTypeWithOccurrence> getTargetTypes() {
        return targetTypes;
    }

    public void setTargetTypes(List<TargetTypeWithOccurrence> targetTypes) {
        this.targetTypes = targetTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyReflection property1 = (PropertyReflection) o;
        return Objects.equals(property, property1.property) && Objects.equals(occurrences, property1.occurrences) && Objects.equals(targetTypes, property1.targetTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(property, occurrences, targetTypes);
    }
}

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

import eu.ebrains.kg.commons.model.SpaceName;

import java.util.List;
import java.util.Objects;

public class SpaceDefinition {

    private String collectionName;
    private SpaceName name;

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public SpaceName getName() {
        return name;
    }

    public void setName(SpaceName name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpaceDefinition that = (SpaceDefinition) o;
        return Objects.equals(collectionName, that.collectionName) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collectionName, name);
    }
}
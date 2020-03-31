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

import eu.ebrains.kg.commons.permission.Functionality;

import java.util.Objects;
import java.util.Set;

public class Space {

    private String name;
    private Set<Functionality> permissions;

    public Space() {
    }

    public Space(String name) {
        setName(name);
    }

    protected String normalizeName(String name) {
        return name.replaceAll("_", "-");
    }

    public void setName(String name) {
        this.name = normalizeName(name);
    }

    public String getName() {
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
}


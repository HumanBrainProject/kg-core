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

package eu.ebrains.kg.commons.permission;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A permission group consolidates functionality elements and can be hierarchically stacked. This is a formalized convention which will be materialized in the authentication system.
 * <p>
 * Example: Space-administrator inherits the permissions of Space-Curator etc...
 */
public enum GlobalPermissionGroup {

    //A power client can do almost everything except for manipulating clients.
    POWER_CLIENT(null, Arrays.stream(Functionality.values()).filter(f -> f.getFunctionalityGroup()!= Functionality.FunctionalityGroup.CLIENT).collect(Collectors.toList()).toArray(new Functionality[0])),
    ADMIN(null, Functionality.values());

    private final GlobalPermissionGroup childPermissionGroup;
    private final String name;
    private final Set<Functionality> functionality;

    GlobalPermissionGroup(GlobalPermissionGroup childPermissionGroup, Functionality... functionality) {
        this.name = name().toLowerCase();
        this.functionality = new HashSet<>(Arrays.asList(functionality));
        this.childPermissionGroup = childPermissionGroup;
    }

    public GlobalPermissionGroup getChildPermissionGroup() {
        return childPermissionGroup;
    }

    public String getName() {
        return name;
    }

    public Set<Functionality> getFunctionality() {
        return functionality;
    }

    private Set<Functionality> getAllFunctionality() {
        Set<Functionality> functionalitySet = new HashSet<>(getFunctionality());
        if (getChildPermissionGroup() != null) {
            functionalitySet.addAll(getChildPermissionGroup().getAllFunctionality());
        }
        return functionalitySet;
    }

    public Role toRole() {
        return new Role(getName(), getSubroles());
    }

    private Set<Role> getSubroles() {
        return getAllFunctionality().stream().map(f -> new FunctionalityInstance(f, null, null).toRole()).collect(Collectors.toSet());
    }
}

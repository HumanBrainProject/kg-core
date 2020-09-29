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

package eu.ebrains.kg.commons.permission.roles;

import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A role consolidates functionality elements and can be hierarchically stacked for inheritance.
 * <p>
 * Example: Space-administrator inherits the permissions of Space-Curator etc...
 */
public enum UserRole {
    CONSUMER(null, Functionality.READ_RELEASED, Functionality.EXECUTE_QUERY, Functionality.CREATE_QUERY, Functionality.READ_QUERY, Functionality.DELETE_QUERY, Functionality.READ_SPACE),
    REVIEWER(CONSUMER, Functionality.READ, Functionality.SUGGEST, Functionality.INVITE_FOR_REVIEW),
    EDITOR(REVIEWER, Functionality.WRITE, Functionality.CREATE, Functionality.INVITE_FOR_SUGGESTION),
    OWNER(EDITOR, Functionality.RELEASE, Functionality.DELETE, Functionality.UNRELEASE),
    ADMIN(null, Functionality.values());

    private final UserRole childPermissionGroup;
    private final String name;
    private final Set<Functionality> functionality;

    UserRole(UserRole childPermissionGroup, Functionality... functionality) {
        this.name = name().toLowerCase();
        this.functionality = new HashSet<>(Arrays.asList(functionality));
        this.childPermissionGroup = childPermissionGroup;
    }

    public UserRole getChildPermissionGroup() {
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

    public Role toRole(SpaceName space) {
        return new Role(String.format("%s:%s", space != null ? space.getName() : "", getName()));
    }

    public static Set<FunctionalityInstance> fromRole(String role) {
        String[] roleSplit = role.trim().split("\\:");
        if (roleSplit.length == 2) {
           UserRole userRole = Arrays.stream(UserRole.values()).filter(r -> r.getName().equals(roleSplit[1])).findFirst().orElse(null);
           if(userRole!=null) {
               return userRole.getFunctionalityInstances(!roleSplit[0].equals("") ? new SpaceName(roleSplit[0]) : null);
           }
        }
        return Collections.emptySet();
    }

    private Set<FunctionalityInstance> getFunctionalityInstances(SpaceName space) {
        return getAllFunctionality().stream().map(f -> new FunctionalityInstance(f, space, null)).collect(Collectors.toSet());
    }

    public static List<UserRole> getAllSpacePermissionGroups() {
        return Arrays.asList(values());
    }

}

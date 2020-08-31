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

import eu.ebrains.kg.commons.model.Space;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A permission group consolidates functionality elements and can be hierarchically stacked.
 * <p>
 * Example: Space-administrator inherits the permissions of Space-Curator etc...
 */
public enum SpacePermissionGroup {
    CONSUMER(null, Functionality.READ_RELEASED, Functionality.EXECUTE_QUERY, Functionality.CREATE_QUERY, Functionality.READ_QUERY, Functionality.DELETE_QUERY),
    REVIEWER(CONSUMER, Functionality.READ, Functionality.SUGGEST, Functionality.INVITE_FOR_REVIEW),
    EDITOR(REVIEWER, Functionality.WRITE, Functionality.CREATE, Functionality.INVITE_FOR_SUGGESTION),
    OWNER(EDITOR, Functionality.RELEASE, Functionality.DELETE, Functionality.UNRELEASE),
    ADMIN(OWNER, Functionality.CREATE_PERMISSION);

    private final SpacePermissionGroup childPermissionGroup;
    private final String name;
    private final Set<Functionality> functionality;

    SpacePermissionGroup(SpacePermissionGroup childPermissionGroup, Functionality... functionality) {
        this.name = name().toLowerCase();
        this.functionality = new HashSet<>(Arrays.asList(functionality));
        this.childPermissionGroup = childPermissionGroup;
    }

    public SpacePermissionGroup getChildPermissionGroup() {
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

    public Role toRole(Space space) {
        return new Role(String.format("%s:%s", space != null ? space.getName() : "", getName()), getSubroles(space));
    }

    private Set<Role> getSubroles(Space space) {
        return getAllFunctionality().stream().map(f -> new FunctionalityInstance(f, space, null).toRole()).collect(Collectors.toSet());
    }

    public static List<SpacePermissionGroup> getAllSpacePermissionGroups() {
        return Arrays.asList(values());
    }

}

/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
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
public enum RoleMapping {
    CONSUMER(false, true,false, null, Functionality.READ_RELEASED),
    REVIEWER(true, false, true, CONSUMER, Functionality.READ, Functionality.SUGGEST, Functionality.INVITE_FOR_REVIEW, Functionality.MINIMAL_READ, Functionality.RELEASE_STATUS),
    EDITOR(true, false, true, REVIEWER, Functionality.WRITE, Functionality.CREATE, Functionality.INVITE_FOR_SUGGESTION, Functionality.DELETE),
    OWNER(true, false, true, EDITOR, Functionality.RELEASE, Functionality.UNRELEASE, Functionality.MANAGE_SPACE),
    ADMIN(true, false, true, null, Functionality.values());

    private final RoleMapping childRole;
    private final String name;
    private final Set<Functionality> functionality;
    private final boolean globalMinimalRead;
    private final boolean globalMinimalReadReleased;
    private final boolean globalRestrictedUserListing;

    RoleMapping(boolean globalMinimalRead, boolean globalMinimalReadReleased, boolean globalRestrictedUserListing, RoleMapping childRole, Functionality... functionality) {
        this.globalRestrictedUserListing = globalRestrictedUserListing;
        this.globalMinimalRead = globalMinimalRead;
        this.globalMinimalReadReleased = globalMinimalReadReleased;
        this.name = name().toLowerCase().replace("_", "-");
        this.functionality = new HashSet<>(Arrays.asList(functionality));
        this.childRole = childRole;
    }

    public RoleMapping getChildRole() {
        return childRole;
    }

    public String getName() {
        return name;
    }

    public Set<Functionality> getFunctionality() {
        return functionality;
    }

    private Set<Functionality> getAllFunctionality() {
        Set<Functionality> functionalitySet = new HashSet<>(getFunctionality());
        if (getChildRole() != null) {
            functionalitySet.addAll(getChildRole().getAllFunctionality());
        }
        return functionalitySet;
    }

    public Role toRole(SpaceName space) {
        return new Role(String.format("%s:%s", space != null ? space.getName() : "", getName()));
    }

    public static Set<FunctionalityInstance> fromRole(String role) {
        String[] roleSplit = role.trim().split(":");
        String space = null;
        String kgRole = null;
        if (roleSplit.length == 2) {
            //This is the default mapping for kg roles (internal roles)
            space = roleSplit[0];
            kgRole = roleSplit[1];
        }
        if(kgRole!=null && space!=null) {
            String fixedKgRole = kgRole;
            RoleMapping userRole = Arrays.stream(RoleMapping.values()).filter(r -> r.getName().equals(fixedKgRole)).findFirst().orElse(null);
            if (userRole != null) {
                return userRole.getFunctionalityInstances(!space.equals("") ? new SpaceName(space) : null);
            }
        }
        return Collections.emptySet();
    }

    private Set<FunctionalityInstance> getFunctionalityInstances(SpaceName space) {
        Set<FunctionalityInstance> functionalityInstances = getAllFunctionality().stream().map(f -> new FunctionalityInstance(f, space, null)).collect(Collectors.toSet());
        if(globalRestrictedUserListing) {
            //Every authenticated user with at least reviewer rights in one of the spaces has the right to list users in a limited way
            functionalityInstances.add(new FunctionalityInstance(Functionality.LIST_USERS_LIMITED, null, null));
        }
        if(globalMinimalReadReleased){
            functionalityInstances.add(new FunctionalityInstance(Functionality.MINIMAL_READ_RELEASED, null, null));
        }
        if(globalMinimalRead){
            functionalityInstances.add(new FunctionalityInstance(Functionality.MINIMAL_READ, null, null));
        }
        return functionalityInstances;
    }

    public static RoleMapping[] getRemainingUserRoles(RoleMapping... excludedRoles){
        List<RoleMapping> roleMappings = Arrays.stream(RoleMapping.values()).filter(r -> Arrays.stream(excludedRoles).noneMatch(e -> e == r)).collect(Collectors.toList());
        if(Arrays.stream(excludedRoles).noneMatch(Objects::isNull)){
            roleMappings.add(null);
        }
        return roleMappings.toArray(RoleMapping[]::new);
    }
}

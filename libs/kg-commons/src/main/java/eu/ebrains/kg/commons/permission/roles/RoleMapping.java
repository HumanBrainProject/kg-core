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

import eu.ebrains.kg.commons.exception.ForbiddenException;
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
    CONSUMER(false, true,null, Functionality.READ_RELEASED, Functionality.EXECUTE_QUERY, Functionality.CREATE_QUERY, Functionality.READ_QUERY, Functionality.DELETE_QUERY, Functionality.READ_SPACE),
    REVIEWER(true, false, CONSUMER, Functionality.READ, Functionality.SUGGEST, Functionality.INVITE_FOR_REVIEW, Functionality.MINIMAL_READ, Functionality.RELEASE_STATUS),
    EDITOR(true, false, REVIEWER, Functionality.WRITE, Functionality.CREATE, Functionality.INVITE_FOR_SUGGESTION),
    OWNER(true, false, EDITOR, Functionality.RELEASE, Functionality.DELETE, Functionality.UNRELEASE),
    ADMIN(true, false, null, Functionality.values()),

    //This is a marker role -> it is to be able to flag a user as a technical (client) user.
    IS_CLIENT(false, false, null);

    private final RoleMapping childRole;
    private final String name;
    private final Set<Functionality> functionality;
    private final boolean globalMinimalRead;
    private final boolean globalMinimalReadReleased;

    RoleMapping(boolean globalMinimalRead, boolean globalMinimalReadReleased, RoleMapping childRole, Functionality... functionality) {
        this.globalMinimalRead = globalMinimalRead;
        this.globalMinimalReadReleased = globalMinimalReadReleased;
        this.name = name().toLowerCase().replaceAll("_", "-");
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
        String[] roleSplit = role.trim().split("\\:");
        String space = null;
        String kgrole = null;
        if (roleSplit.length == 2) {
            //This is the default mapping for kg roles (internal roles)
            space = roleSplit[0];
            kgrole = roleSplit[1];
        }
        else{
            Map<String, String> externalRoleMapping = new HashMap<>();
            //TODO make this configurable
            externalRoleMapping.put("-administrator", RoleMapping.OWNER.getName());
            externalRoleMapping.put("-viewer", RoleMapping.REVIEWER.getName());
            externalRoleMapping.put("-editor", RoleMapping.EDITOR.getName());
            String roleMapping = externalRoleMapping.keySet().stream().filter(role::endsWith).findFirst().orElse(null);
            if(roleMapping!=null && !role.equalsIgnoreCase(roleMapping)){
                //Alternatively, we also allow roles to be specified from external sources (e.g. the EBRAINS Collaboratory)
                space = role.substring(0, role.length()-roleMapping.length());
                if(space.trim().equals("")){
                    throw new ForbiddenException("Tried to create global access rights with an external role mapping");
                }
                kgrole = externalRoleMapping.get(roleMapping);
            }
        }
        if(kgrole!=null && space!=null) {
            String fixedKgRole = kgrole;
            RoleMapping userRole = Arrays.stream(RoleMapping.values()).filter(r -> r.getName().equals(fixedKgRole)).findFirst().orElse(null);
            if (userRole != null) {
                return userRole.getFunctionalityInstances(!space.equals("") ? new SpaceName(space) : null);
            }
        }
        return Collections.emptySet();
    }

    private Set<FunctionalityInstance> getFunctionalityInstances(SpaceName space) {
        Set<FunctionalityInstance> functionalityInstances = getAllFunctionality().stream().map(f -> new FunctionalityInstance(f, space, null)).collect(Collectors.toSet());
        if(globalMinimalReadReleased){
            functionalityInstances.add(new FunctionalityInstance(Functionality.MINIMAL_READ_RELEASED, null, null));
        }
        if(globalMinimalRead){
            functionalityInstances.add(new FunctionalityInstance(Functionality.MINIMAL_READ, null, null));
        }
        return functionalityInstances;
    }

    public static List<RoleMapping> getAllSpacePermissionGroups() {
        return Arrays.asList(values());
    }

    public static RoleMapping[] getRemainingUserRoles(RoleMapping[] excludedRoles){
        List<RoleMapping> roleMappings = Arrays.stream(RoleMapping.values()).filter(r -> r != RoleMapping.IS_CLIENT && Arrays.stream(excludedRoles).noneMatch(e -> e == r)).collect(Collectors.toList());
        if(Arrays.stream(excludedRoles).noneMatch(Objects::isNull)){
            roleMappings.add(null);
        }
        return roleMappings.toArray(RoleMapping[]::new);
    }
}

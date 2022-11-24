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

package eu.ebrains.kg.commons.permissions.controller;

import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.permission.Permission;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The permission service allows to evaluate if a user has the permissions to execute a specific action or not.
 */
@Component
public class Permissions {

    private Set<FunctionalityInstance> getExpectedFunctionalityList(Functionality functionality, SpaceName space, UUID id) {
        Set<FunctionalityInstance> instances = new HashSet<>();
        if (functionality.getAllowedPermissionLevels().contains(Permission.Level.GLOBAL)) {
            instances.add(new FunctionalityInstance(functionality, null, null));
        }
        if (space != null && functionality.getAllowedPermissionLevels().contains(Permission.Level.SPACE)) {
            instances.add(new FunctionalityInstance(functionality, space, null));
        }
        if (id != null && functionality.getAllowedPermissionLevels().contains(Permission.Level.INSTANCE)) {
            instances.add(new FunctionalityInstance(functionality, null, id));
        }
        return instances;
    }

    public boolean hasPermission(UserWithRoles userWithRoles, Functionality functionality, SpaceName space) {
        return hasPermission(userWithRoles, functionality, space, null);
    }

    private boolean checkFunctionalities(Functionality functionality, SpaceName space, UUID id, List<FunctionalityInstance> permissions) {
        Set<FunctionalityInstance> expectedRoles = getExpectedFunctionalityList(functionality, space, id);
        boolean fullMatch = expectedRoles.stream().anyMatch(permissions::contains);
        if(fullMatch){
            return true;
        }
        final Set<FunctionalityInstance> applicableWildcardRoles = permissions.stream().filter(i -> i.getId() == null && i.getSpace() != null && i.getSpace().isWildcard() && i.getFunctionality() == functionality).collect(Collectors.toSet());
        return applicableWildcardRoles.stream().anyMatch(wildcardRole -> expectedRoles.stream().anyMatch(wildcardRole::matchesWildcard));
    }

    public boolean hasPermission(UserWithRoles userWithRoles, Functionality functionality, SpaceName space, UUID id) {
        if (userWithRoles == null || functionality == null) {
            return false;
        }
        return checkFunctionalities(functionality, space, id, userWithRoles.getPermissions());
    }

    public boolean hasGlobalPermission(UserWithRoles userWithRoles, Functionality functionality) {
        return hasPermission(userWithRoles, functionality, null, null);
    }

    @NotNull
    public Set<SpaceName> getSpacesForPermission(Set<SpaceName> spaces, UserWithRoles userWithRoles, Functionality functionality) {
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();
        if (functionality == null || hasGlobalPermission(userWithRoles, functionality)) {
            return Collections.emptySet();
        }
        final Set<FunctionalityInstance> applicableWildcardRoles = permissions.stream().filter(i -> i.getId() == null && i.getSpace() != null && i.getSpace().isWildcard() && i.getFunctionality() == functionality).collect(Collectors.toSet());
        final Stream<SpaceName> directHits = permissions.stream().filter(f -> f.getId() == null && f.getFunctionality().equals(functionality) && spaces.contains(f.getSpace())).map(FunctionalityInstance::getSpace).filter(Objects::nonNull);
        if(!applicableWildcardRoles.isEmpty()) {
            final Stream<SpaceName> spacesFromWildcards = spaces.stream().filter(space -> applicableWildcardRoles.stream().anyMatch(w -> w.getSpace().matchesWildcard(space)));
            return Stream.concat(directHits, spacesFromWildcards).collect(Collectors.toSet());
        }
        else {
            return directHits.collect(Collectors.toSet());
        }
    }

    public Set<UUID> getInstancesWithExplicitPermission(List<FunctionalityInstance> permissions, Functionality functionality) {
        return permissions.stream().filter(f -> f.getFunctionality().equals(functionality)).map(FunctionalityInstance::getId).filter(Objects::nonNull).collect(Collectors.toSet());
    }


}

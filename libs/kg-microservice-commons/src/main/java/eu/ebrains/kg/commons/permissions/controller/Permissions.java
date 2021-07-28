/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
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

import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.permission.Permission;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

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
        if (space != null && id != null && functionality.getAllowedPermissionLevels().contains(Permission.Level.INSTANCE)) {
            instances.add(new FunctionalityInstance(functionality, space, id));
        }
        return instances;
    }

    private boolean isServiceAccountForClientSpace(UserWithRoles userWithRoles, SpaceName space) {
        return userWithRoles != null && space != null && userWithRoles.getClientId() != null && userWithRoles.getClientId().equals(space.getName()) && userWithRoles.getUser() != null && userWithRoles.getUser().isServiceAccountForClient(userWithRoles.getClientId());
    }

    public boolean hasPermission(UserWithRoles userWithRoles, Functionality functionality, SpaceName space) {
        return hasPermission(userWithRoles, functionality, space, null);
    }

    private boolean checkFunctionalities(Functionality functionality, SpaceName space, UUID id, List<FunctionalityInstance> permissions) {
        Set<FunctionalityInstance> expectedRoles = getExpectedFunctionalityList(functionality, space, id);
        return expectedRoles.stream().anyMatch(permissions::contains);
    }


    public Functionality getMinimalReadFunctionality(DataStage stage) {
        return Functionality.MINIMAL_READ;
    }

    public Functionality getReadFunctionality(DataStage stage) {
        switch (stage) {
            case IN_PROGRESS:
                return Functionality.READ;
            case RELEASED:
                return Functionality.READ_RELEASED;
        }
        return null;
    }

    public boolean hasEitherUserOrClientPermissionFor(UserWithRoles userWithRoles, Functionality functionality, SpaceName space, UUID id){
        return userWithRoles != null && hasPermission(userWithRoles, functionality, space, id, userWithRoles.getPermissionsOfEitherUserOrClient());
    }

    public boolean hasPermission(UserWithRoles userWithRoles, Functionality functionality, SpaceName space, UUID id) {
        return userWithRoles != null && hasPermission(userWithRoles, functionality, space, id, userWithRoles.getPermissions());
    }

    private boolean hasPermission(UserWithRoles userWithRoles, Functionality functionality, SpaceName space, UUID id, List<FunctionalityInstance> functionalityInstances) {
        if (functionality == null) {
            return false;
        }
        switch (functionality) {
            case CREATE_SPACE:
                if (space != null && userWithRoles != null) {
                    boolean clientOwnedSpace = isServiceAccountForClientSpace(userWithRoles, space);
                    boolean privateUserSpace = space.equals(userWithRoles.getPrivateSpace());
                    //A special case is, that a client service account can create its own space even if there are no explicitly declared rights. The same is true for a user and the private space
                    if (clientOwnedSpace || privateUserSpace) {
                        return true;
                    }
                    break;
                }
        }
        return checkFunctionalities(functionality, space, id, functionalityInstances);
    }

    public boolean hasGlobalPermission(UserWithRoles userWithRoles, Functionality functionality) {
        return hasPermission(userWithRoles, functionality, null, null);
    }

    public Set<SpaceName> getSpacesForPermission(UserWithRoles userWithRoles, Functionality functionality) {
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();
        if (functionality == null) {
            return Collections.emptySet();
        }
        if (hasGlobalPermission(userWithRoles, functionality)) {
            return null;
        }
        return permissions.stream().filter(f -> f.getFunctionality().equals(functionality)).map(FunctionalityInstance::getSpace).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public Set<InstanceId> getInstancesWithExplicitPermission(List<FunctionalityInstance> permissions, Functionality functionality) {
        return permissions.stream().filter(f -> f.getFunctionality().equals(functionality)).map(FunctionalityInstance::getInstanceId).filter(Objects::nonNull).collect(Collectors.toSet());
    }


}

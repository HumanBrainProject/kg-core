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

package eu.ebrains.kg.commons.permissions.controller;

import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.Space;
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
public class PermissionSvc {

    private Set<FunctionalityInstance> getExpectedFunctionalityList(Functionality functionality, Space space, UUID id) {
        Set<FunctionalityInstance> instances = new HashSet<>();
        if(functionality.getAllowedPermissionLevels().contains(Permission.Level.GLOBAL)){
            instances.add(new FunctionalityInstance(functionality, null, null));
        }
        if(space != null && functionality.getAllowedPermissionLevels().contains(Permission.Level.SPACE)){
            instances.add(new FunctionalityInstance(functionality, space, null));
        }
        if(space != null && id!=null && functionality.getAllowedPermissionLevels().contains(Permission.Level.INSTANCE)){
            instances.add(new FunctionalityInstance(functionality, space, id));
        }
        return instances;
    }

    private boolean isServiceAccountForClientSpace(UserWithRoles userWithRoles, Space space){
        return userWithRoles != null && space != null && userWithRoles.getClientId() != null && userWithRoles.getClientId().equals(space.getName()) && userWithRoles.getUser()!=null && userWithRoles.getUser().isServiceAccountForClient(userWithRoles.getClientId()) && checkFunctionalities(Functionality.IS_CLIENT, space, null, userWithRoles.getPermissions());

    }

    public boolean hasPermission(UserWithRoles userWithRoles, Functionality functionality, Space space) {
        return hasPermission(userWithRoles, functionality, space, null);
    }

    private boolean checkFunctionalities(Functionality functionality, Space space, UUID id, List<FunctionalityInstance> permissions){
        Set<FunctionalityInstance> expectedRoles = getExpectedFunctionalityList(functionality, space, id);
        return expectedRoles.stream().anyMatch(permissions::contains);
    }

    public boolean hasPermission(UserWithRoles userWithRoles, Functionality functionality, Space space, UUID id) {
        boolean clientOwnedSpace = isServiceAccountForClientSpace(userWithRoles, space);
        switch (functionality){
            case CREATE_SPACE:
                //One special case is, that a client service account can create its own space even if there are no explicitly declared rights.
                if(clientOwnedSpace){
                    return true;
                }
                break;
            case READ_QUERY:
            case CREATE_QUERY:
            case EXECUTE_QUERY:
            case EXECUTE_SYNC_QUERY:
            case DELETE_QUERY:
                //For queries in client owned spaces, only the client service account and users with global rights for this functionality (e.g. admins) are allowed to do so
                if(space != null && space.isClientSpace()){
                    if(clientOwnedSpace || hasGlobalPermission(userWithRoles, functionality)){
                        return true;
                    }
                    else{
                        return false;
                    }
                }
        }
        return checkFunctionalities(functionality, space, id, userWithRoles.getPermissions());
    }

    public boolean hasGlobalPermission(UserWithRoles userWithRoles, Functionality functionality){
        return hasPermission(userWithRoles, functionality, null, null);
    }

    public Set<Space> getSpacesForPermission(UserWithRoles userWithRoles, Functionality functionality) {
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();
        if(functionality==null){
            return Collections.emptySet();
        }
        if(hasGlobalPermission(userWithRoles, functionality)){
            return null;
        }
        return permissions.stream().filter(f -> f.getFunctionality().equals(functionality)).map(FunctionalityInstance::getSpace).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public Set<InstanceId> getInstancesWithExplicitPermission(List<FunctionalityInstance> permissions,Functionality functionality) {
        return permissions.stream().filter(f -> f.getFunctionality().equals(functionality)).map(FunctionalityInstance::getInstanceId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

}

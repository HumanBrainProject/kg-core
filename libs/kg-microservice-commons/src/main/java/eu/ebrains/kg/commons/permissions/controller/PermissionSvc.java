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
        if(functionality.getAllowedPermissionLevels().contains(Permission.Level.SPACE)){
            instances.add(new FunctionalityInstance(functionality, space, null));
        }
        if(functionality.getAllowedPermissionLevels().contains(Permission.Level.INSTANCE)){
            instances.add(new FunctionalityInstance(functionality, space, id));
        }
        return instances;
    }


    public boolean hasPermission(Functionality functionality, Space space, List<FunctionalityInstance> permissions) {
        Set<FunctionalityInstance> expectedRoles = getExpectedFunctionalityList(functionality, space, null);
        return expectedRoles.stream().anyMatch(permissions::contains);
    }

    public boolean hasPermission(Functionality functionality, Space space, UUID id, List<FunctionalityInstance> permissions) {
        Set<FunctionalityInstance> expectedRoles = getExpectedFunctionalityList(functionality, space, id);
        return expectedRoles.stream().anyMatch(permissions::contains);
    }

    public boolean hasGlobalPermission(Functionality functionality, List<FunctionalityInstance> functionalityInstances){
        return hasPermission(functionality, null, null, functionalityInstances);
    }

    public Set<Space> getSpacesForPermission(List<FunctionalityInstance> permissions, Functionality functionality) {
        if(functionality==null){
            return Collections.emptySet();
        }
        if(hasGlobalPermission(functionality, permissions)){
            return null;
        }
        return permissions.stream().filter(f -> f.getFunctionality().equals(functionality)).map(FunctionalityInstance::getSpace).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public Set<InstanceId> getInstancesWithExplicitPermission(List<FunctionalityInstance> permissions,Functionality functionality) {
        return permissions.stream().filter(f -> f.getFunctionality().equals(functionality)).map(FunctionalityInstance::getInstanceId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

}

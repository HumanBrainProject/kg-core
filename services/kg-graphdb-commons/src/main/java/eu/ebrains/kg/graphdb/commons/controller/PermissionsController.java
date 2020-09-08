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

package eu.ebrains.kg.graphdb.commons.controller;

import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.permissions.controller.PermissionSvc;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PermissionsController {

    private final PermissionSvc permissions;
    private final IdUtils idUtils;

    public PermissionsController(PermissionSvc permissions, IdUtils idUtils) {
        this.permissions = permissions;
        this.idUtils = idUtils;
    }

    public Set<Space> whitelistedSpaceReads(UserWithRoles userWithRoles){
        Functionality functionality = Functionality.READ_SPACE;
        if(!permissions.hasGlobalPermission(userWithRoles, functionality)){
            //We only need to filter if there is no "global" read available...
            return userWithRoles.getPermissions().stream().filter(p -> p.getFunctionality() == functionality).map(FunctionalityInstance::getSpace).filter(Objects::nonNull).collect(Collectors.toSet());
        }
        return null;
    }
    
    public Map<String, Object> whitelistFilterForReadInstances(UserWithRoles userWithRoles, DataStage stage) {
        Functionality readFunctionality = getReadFunctionality(stage);
        if (!permissions.hasGlobalPermission(userWithRoles, readFunctionality)){
            //We only need to filter if there is no "global" read available...
            Map<String, Object> bindVars = new HashMap<>();
            Set<Space> spacesWithReadPermission = permissions.getSpacesForPermission(userWithRoles, readFunctionality);
            Set<InstanceId> instancesWithReadPermissions = permissions.getInstancesWithExplicitPermission(userWithRoles.getPermissions(), readFunctionality);
            bindVars.put(AQL.READ_ACCESS_BY_SPACE, spacesWithReadPermission != null ? spacesWithReadPermission.stream().map(s -> ArangoCollectionReference.fromSpace(s).getCollectionName()).collect(Collectors.toList()) : Collections.emptyList());
            bindVars.put(AQL.READ_ACCESS_BY_INVITATION, instancesWithReadPermissions != null ? instancesWithReadPermissions.stream().map(i -> idUtils.buildAbsoluteUrl(i.getUuid()).getId()).collect(Collectors.toList()): Collections.emptyList());
            return bindVars;
        }
        return null;
    }


    private Functionality getReadFunctionality(DataStage stage) {
        switch (stage) {
            case IN_PROGRESS:
                return Functionality.READ;
            case RELEASED:
                return Functionality.READ_RELEASED;
        }
        return null;
    }


}

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

package eu.ebrains.kg.graphdb.commons.controller;

import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PermissionsController {

    private final Permissions permissions;
    private final IdUtils idUtils;

    public PermissionsController(Permissions permissions, IdUtils idUtils) {
        this.permissions = permissions;
        this.idUtils = idUtils;
    }

    public Set<SpaceName> whitelistedSpaceReads(UserWithRoles userWithRoles){
        Functionality functionality = Functionality.READ_SPACE;
        if(!permissions.hasGlobalPermission(userWithRoles, functionality)){
            //We only need to filter if there is no "global" read available...
            return userWithRoles.getPermissions().stream().filter(p -> p.getFunctionality() == functionality).map(FunctionalityInstance::getSpace).filter(Objects::nonNull).collect(Collectors.toSet());
        }
        return null;
    }

    public boolean canManageTypesAndProperties(UserWithRoles userWithRoles){
        return permissions.hasGlobalPermission(userWithRoles, Functionality.DEFINE_TYPES_AND_PROPERTIES);
    }

    public boolean canManageSpaces(UserWithRoles userWithRoles){
        return permissions.hasGlobalPermission(userWithRoles, Functionality.MANAGE_SPACE);
    }

    public boolean hasGlobalReadPermissions(UserWithRoles userWithRoles, DataStage stage){
        Functionality readFunctionality = getReadFunctionality(stage);
        return permissions.hasGlobalPermission(userWithRoles, readFunctionality);
    }

    public Set<SpaceName> removeSpacesWithoutReadAccess(Set<SpaceName> spaces, UserWithRoles userWithRoles, DataStage stage){
        Functionality readFunctionality = getReadFunctionality(stage);
        Set<SpaceName> spacesWithReadPermission = permissions.getSpacesForPermission(userWithRoles, readFunctionality);
        spaces.retainAll(spacesWithReadPermission);
        return spaces;
    }

    public Set<UUID> getInstancesWithExplicitPermission(UserWithRoles userWithRoles, DataStage stage){
        Functionality readFunctionality = getReadFunctionality(stage);
        return permissions.getInstancesWithExplicitPermission(userWithRoles.getPermissions(), readFunctionality);
    }


    public Map<String, Object> whitelistFilterForReadInstances(UserWithRoles userWithRoles, DataStage stage) {
        Functionality readFunctionality = getReadFunctionality(stage);
        if (!permissions.hasGlobalPermission(userWithRoles, readFunctionality)){
            //We only need to filter if there is no "global" read available...
            Map<String, Object> bindVars = new HashMap<>();
            Set<SpaceName> spacesWithReadPermission = permissions.getSpacesForPermission(userWithRoles, readFunctionality);
            Set<UUID> instancesWithReadPermissions = getInstancesWithExplicitPermission(userWithRoles, stage);
            bindVars.put(AQL.READ_ACCESS_BY_SPACE, spacesWithReadPermission != null ? spacesWithReadPermission.stream().map(s -> ArangoCollectionReference.fromSpace(s).getCollectionName()).collect(Collectors.toList()) : Collections.emptyList());
            bindVars.put(AQL.READ_ACCESS_BY_INVITATION, instancesWithReadPermissions != null ? instancesWithReadPermissions.stream().map(i -> idUtils.buildAbsoluteUrl(i).getId()).collect(Collectors.toList()): Collections.emptyList());
            return bindVars;
        }
        return null;
    }


    public Functionality getMinimalReadFunctionality(DataStage stage) {
        switch (stage) {
            case IN_PROGRESS:
                return Functionality.MINIMAL_READ;
            case RELEASED:
                //In the released section, we don't allow the minimal read
                return null;
        }
        return null;
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


}

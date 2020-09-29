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

package eu.ebrains.kg.admin.controller;

import eu.ebrains.kg.admin.serviceCall.AdminToAuthentication;
import eu.ebrains.kg.admin.serviceCall.AdminToPrimaryStore;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.permission.roles.Role;
import eu.ebrains.kg.commons.permission.roles.UserRole;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class AdminSpaceController {
    private final AdminToPrimaryStore adminToPrimaryStore;
    private final AdminToAuthentication authenticationSvc;

    public AdminSpaceController(AdminToPrimaryStore adminToPrimaryStore, AdminToAuthentication authenticationSvc) {
        this.adminToPrimaryStore = adminToPrimaryStore;
        this.authenticationSvc = authenticationSvc;
    }

    private List<Role> findRoles(SpaceName space, UserRole group, List<Role> collector){
        if(group.getChildPermissionGroup()!=null){
            findRoles(space, group.getChildPermissionGroup(), collector);
        }
        collector.add(group.toRole(space));
        return collector;
    }

    public InstanceId createSpace(Space space, boolean global) {
        List<Role> roles = findRoles(space.getName(), UserRole.ADMIN, new ArrayList<>());
        List<InstanceId> instanceIds = defineSpace(space, global);
        authenticationSvc.createRoles(roles);
        return instanceIds.size()==1 ? instanceIds.get(0) : null;
    }

    public void removeSpace(SpaceName space) {
        authenticationSvc.removeRoles(FunctionalityInstance.getRolePatternForSpace(space));
    }

    public List<User> getUsersByPermissionGroup(SpaceName space, UserRole group) {
        return authenticationSvc.getUsersInRole(group.toRole(space).getName());
    }

    public void addUserToSpace(String nativeUserId, SpaceName space, UserRole permissionGroup) {
        authenticationSvc.addUserToRole(permissionGroup.toRole(space).getName(), nativeUserId);
    }

    public  List<InstanceId> defineSpace(Space space, boolean global) {
        NormalizedJsonLd payload = space.toJsonLd();
        return adminToPrimaryStore.postEvent(Event.createUpsertEvent(global ? InternalSpace.GLOBAL_SPEC : space.getName(), UUID.nameUUIDFromBytes(payload.id().getId().getBytes(StandardCharsets.UTF_8)), Event.Type.INSERT, payload), false);
    }

}

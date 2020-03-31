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

import eu.ebrains.kg.admin.serviceCall.AuthenticationSvcForAdmin;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.permission.Role;
import eu.ebrains.kg.commons.permission.SpacePermissionGroup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SpaceController {
    private final ArangoRepository repository;
    private final AuthenticationSvcForAdmin authenticationSvc;

    public SpaceController(ArangoRepository repository, AuthenticationSvcForAdmin authenticationSvc) {
        this.repository = repository;
        this.authenticationSvc = authenticationSvc;
    }

    private List<Role> findRoleInGroup(Space space, SpacePermissionGroup group, List<Role> collector){
        if(group.getChildPermissionGroup()!=null){
            findRoleInGroup(space, group.getChildPermissionGroup(), collector);
        }
        collector.addAll(group.getFunctionality().stream().map(f ->  new FunctionalityInstance(f, space, null).toRole()).collect(Collectors.toList()));
        collector.add(group.toRole(space));
        return collector;
    }

    public void createSpace(Space space) {
        List<Role> roles = findRoleInGroup(space, SpacePermissionGroup.ADMIN, new ArrayList<>());
        authenticationSvc.createRoles(roles);
    }

    public void removeSpace(Space space) {
        authenticationSvc.removeRoles(FunctionalityInstance.getRolePatternForSpace(space));
    }

    public List<User> getUsersByPermissionGroup(Space space, SpacePermissionGroup group) {
        return authenticationSvc.getUsersInRole(group.toRole(space).getName());
    }

    public void addUserToSpace(String nativeUserId, Space space, SpacePermissionGroup permissionGroup) {
        authenticationSvc.addUserToRole(permissionGroup.toRole(space).getName(), nativeUserId);
    }

}

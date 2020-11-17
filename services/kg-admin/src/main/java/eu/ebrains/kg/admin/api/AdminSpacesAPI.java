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

package eu.ebrains.kg.admin.api;

import eu.ebrains.kg.admin.controller.AdminSpaceController;
import eu.ebrains.kg.admin.controller.AdminUserController;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The spaces api allows to manage spaces in the EBRAINS KG
 */

@RestController
@RequestMapping("/internal/admin/spaces")
public class AdminSpacesAPI {

    private final AdminSpaceController spaceController;
    private final AdminUserController userController;

    public AdminSpacesAPI(AdminSpaceController spaceController, AdminUserController userController) {
        this.spaceController = spaceController;
        this.userController = userController;
    }

    @Operation(summary = "Get the available permission groups for a space")
    @GetMapping("/{id}/permissions")
    public List<RoleMapping> getPermissions(@PathVariable("id") String id) {
        return RoleMapping.getAllSpacePermissionGroups();
    }

    @Operation(summary = "Get the users which have a specific permission for this space")
    @GetMapping("/{id}/permissions/{permission}/users")
    public List<User> getUsersForPermissionsInSpace(@PathVariable("id") String id, @PathVariable("permission") RoleMapping permission) {
        return spaceController.getUsersByPermissionGroup(new SpaceName(id), permission);
    }

    @Operation(summary = "Register a user in the given space with the according permission group")
    @PutMapping("/{id}/permissions/{permission}/users/{userId}")
    public void registerUserInSpace(@PathVariable("id") String id, @PathVariable("permission") RoleMapping permissionGroup, @PathVariable("userId") String userId) {
        spaceController.addUserToSpace(userController.getNativeId(userId), new SpaceName(id), permissionGroup);
    }

}

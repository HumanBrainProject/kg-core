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

import com.arangodb.ArangoDBException;
import eu.ebrains.kg.admin.controller.AdminSpaceController;
import eu.ebrains.kg.admin.controller.AdminUserController;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSpace(@PathVariable("id") String id) {
        try {
            spaceController.removeSpace(new SpaceName(id));
            return ResponseEntity.ok(String.format("Successfully removed space %s", id));
        } catch (ArangoDBException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> addSpace(@PathVariable("id") String id, @RequestParam("autorelease") boolean autoRelease) {
        try {
            Space space = new Space(new SpaceName(id), autoRelease);
            spaceController.createSpace(space, true);
            return ResponseEntity.ok(String.format("Successfully created space %s", space));
        } catch (ArangoDBException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
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

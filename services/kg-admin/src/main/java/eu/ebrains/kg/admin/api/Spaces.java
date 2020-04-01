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
import eu.ebrains.kg.admin.controller.ArangoRepository;
import eu.ebrains.kg.admin.controller.SpaceController;
import eu.ebrains.kg.admin.controller.UserController;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.permission.SpacePermissionGroup;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The spaces api allows to manage spaces in the EBRAINS KG
 */

@RestController
@RequestMapping("/spaces")
public class Spaces {

    private final ArangoRepository repository;
    private final SpaceController spaceController;
    private final UserController userController;

    public Spaces(ArangoRepository repository, SpaceController spaceController, UserController userController) {
        this.repository = repository;
        this.spaceController = spaceController;
        this.userController = userController;
    }

    @GetMapping("")
    public ResponseEntity<List<Space>> getSpaces() {
        try {
            ArangoCollectionReference arangoCollectionReference = new ArangoCollectionReference("spaces", true);
             return ResponseEntity.ok(repository.getEntities(arangoCollectionReference, Space.class));
        } catch (ArangoDBException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Space> getSpace(@PathVariable("id") String id) {
        try {
            ArangoCollectionReference arangoCollectionReference = new ArangoCollectionReference("spaces", true);
            return ResponseEntity.ok(repository.getEntity(arangoCollectionReference, id, Space.class));
        } catch (ArangoDBException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSpace(@PathVariable("id") String id) {
        try {
            spaceController.removeSpace(new Space(id));
            return ResponseEntity.ok(String.format("Successfully inserted the space with id %s", id));
        } catch (ArangoDBException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<String> addSpace(@PathVariable("id") String id) {
        try {
            spaceController.createSpace(new Space(id));
            return ResponseEntity.ok(String.format("Successfully inserted the space with id %s", id));
        } catch (ArangoDBException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @ApiOperation("Get the available permission groups for a space")
    @GetMapping("/{id}/permissions")
    public List<SpacePermissionGroup> getPermissions(@PathVariable("id") String id) {
        return SpacePermissionGroup.getAllSpacePermissionGroups();
    }

    @ApiOperation("Get the users which have a specific permission for this space")
    @GetMapping("/{id}/permissions/{permission}/users")
    public List<User> getUsersForPermissionsInSpace(@PathVariable("id") String id, @PathVariable("permission") SpacePermissionGroup permission) {
        return spaceController.getUsersByPermissionGroup(new Space(id), permission);
    }

    @ApiOperation("Register a user in the given space with the according permission group")
    @PutMapping("/{id}/permissions/{permission}/users/{userId}")
    public void registerUserInSpace(@PathVariable("id") String id, @PathVariable("permission") SpacePermissionGroup permissionGroup, @PathVariable("userId") String userId) {
        spaceController.addUserToSpace(userController.getNativeId(userId), new Space(id), permissionGroup);
    }

}

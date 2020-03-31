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

package eu.ebrains.kg.authentication.api;

import eu.ebrains.kg.authentication.keycloak.KeycloakController;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.permission.Role;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequestMapping("/roles")
@RestController
public class Roles {

    private final KeycloakController keycloakController;

    public Roles(KeycloakController keycloakController) {
        this.keycloakController = keycloakController;
    }

    @PostMapping
    public void createRoles(@RequestBody List<Role> roles) {
        keycloakController.getNonExistingRoles(roles).forEach(keycloakController::createRoleForClient);
    }

    @DeleteMapping("/{rolePattern}")
    public void removeRoles(@PathVariable("rolePattern") String rolePattern) {
        keycloakController.removeRolesFromClient(URLDecoder.decode(rolePattern, StandardCharsets.UTF_8));
    }

    @GetMapping("/{role}/users")
    public List<User> getUsersInRole(@PathVariable("role") String role) {
        return keycloakController.getUsersInRole(URLDecoder.decode(role, StandardCharsets.UTF_8));
    }

    @PutMapping("/{role}/users/{nativeUserId}")
    public void addUserToRole(@PathVariable("role") String role, @PathVariable("nativeUserId") String nativeUserId) {
        keycloakController.addUserToRole(nativeUserId, URLDecoder.decode(role, StandardCharsets.UTF_8));
    }
}

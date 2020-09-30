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
import eu.ebrains.kg.commons.permission.roles.Role;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequestMapping("/internal/authentication/roles")
@RestController
@ConditionalOnProperty(value = "eu.ebrains.kg.test", havingValue = "false", matchIfMissing = true)
public class AuthenticationRolesAPI {

    private final KeycloakController keycloakController;

    public AuthenticationRolesAPI(KeycloakController keycloakController) {
        this.keycloakController = keycloakController;
    }

    @GetMapping("/{role}/users")
    public List<User> getUsersInRole(@PathVariable("role") String role) {
        return keycloakController.getUsersInRole(URLDecoder.decode(role, StandardCharsets.UTF_8));
    }

    @PutMapping("/{role}/users/{nativeUserId}")
    public void addUserToRole(@PathVariable("role") String role, @PathVariable("nativeUserId") String nativeUserId) {
        keycloakController.addUserToRole(nativeUserId, URLDecoder.decode(role, StandardCharsets.UTF_8));
    }

    @PostMapping
    public void createRoles(@RequestBody List<Role> roles) {
        keycloakController.createRoles(roles);
    }
}

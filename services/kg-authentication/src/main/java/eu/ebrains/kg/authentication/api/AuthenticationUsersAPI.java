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

import com.auth0.jwt.interfaces.Claim;
import eu.ebrains.kg.authentication.keycloak.KeycloakController;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RequestMapping("/internal/authentication/users")
@RestController
@ConditionalOnProperty(value = "eu.ebrains.kg.test", havingValue = "false", matchIfMissing = true)
public class AuthenticationUsersAPI {

    private final KeycloakController keycloakController;

    public AuthenticationUsersAPI(KeycloakController keycloakController) {
        this.keycloakController = keycloakController;
    }

    @GetMapping(value = "/authorization/endpoint", produces = MediaType.TEXT_PLAIN_VALUE)
    public String authEndpoint() {
        return keycloakController.getServerUrl();
    }

    @GetMapping(value = "/authorization/tokenEndpoint", produces = MediaType.TEXT_PLAIN_VALUE)
    public String tokenEndpoint() {
        return keycloakController.getTokenEndpoint();
    }


    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> getMyUserInfo() {
        Map<String, Claim> userProfile = keycloakController.getUserProfile();
        return userProfile != null ? ResponseEntity.ok(keycloakController.buildUserInfoFromKeycloak(userProfile)) : ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/meWithRoles", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserWithRoles> getRoles() {
        Map<String, Claim> userProfile = keycloakController.getUserProfile();
        Map<String, Claim> clientProfile = keycloakController.getClientProfile();
        UserWithRoles userWithRoles = userProfile != null && clientProfile != null ? new UserWithRoles(keycloakController.buildUserInfoFromKeycloak(userProfile), keycloakController.buildRoleListFromKeycloak(userProfile), keycloakController.buildRoleListFromKeycloak(clientProfile), keycloakController.getClientInfoFromKeycloak(clientProfile)) : null;
        return userWithRoles != null ? ResponseEntity.ok(userWithRoles) : ResponseEntity.notFound().build();
    }

    @GetMapping("/profiles")
    public ResponseEntity<User> getOtherUserInfoAsList(@PathVariable("nativeId") String nativeId) {
        User userById = keycloakController.getOtherUserInfo(nativeId);
        return userById != null ? ResponseEntity.ok(userById) : ResponseEntity.notFound().build();
    }

    @GetMapping("/profiles/{nativeId}")
    public ResponseEntity<User> getOtherUserInfo(@PathVariable("nativeId") String nativeId) {
        User userById = keycloakController.getOtherUserInfo(nativeId);
        return userById != null ? ResponseEntity.ok(userById) : ResponseEntity.notFound().build();
    }

    @GetMapping("/profiles/byAttribute/{attribute}/{value}")
    public ResponseEntity<List<User>> getUsersByAttribute(@PathVariable("attribute") String attribute, @PathVariable("value") String value) {
        List<User> users = keycloakController.getUsersByAttribute(attribute, value);
        return users != null ? ResponseEntity.ok(users) : ResponseEntity.notFound().build();
    }
}

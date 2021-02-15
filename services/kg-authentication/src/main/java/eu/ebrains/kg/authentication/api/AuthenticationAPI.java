/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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
import eu.ebrains.kg.authentication.keycloak.KeycloakInitialSetup;
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.model.Credential;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.roles.Role;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AuthenticationAPI implements Authentication.Client {

    private final KeycloakController keycloakController;

    private final KeycloakInitialSetup initialSetup;

    public AuthenticationAPI(KeycloakInitialSetup initialSetup, KeycloakController keycloakController) {
        this.initialSetup = initialSetup;
        this.keycloakController = keycloakController;
    }

    /** CLIENTS **/
    @Override
    public eu.ebrains.kg.commons.model.Client registerClient(eu.ebrains.kg.commons.model.Client client) {
        return keycloakController.registerClient(client);
    }

    @Override
    public void unregisterClient(String clientName) {
        keycloakController.unregisterClient(new eu.ebrains.kg.commons.model.Client(clientName).getIdentifier());
    }

    @Override
    public ClientAuthToken fetchToken(String clientId, String clientSecret) {
        return new ClientAuthToken(keycloakController.authenticate(clientId, clientSecret));
    }

    /** ROLES **/
    @Override
    public List<User> getUsersInRole(String role) {
        return keycloakController.getUsersInRole(role);
    }

    @Override
    public void addUserToRole(String role, String nativeUserId) {
        keycloakController.addUserToRole(nativeUserId, role);
    }

    @Override
    public void createRoles(List<Role> roles) {
        keycloakController.createRoles(roles);
    }

    @Override
    public void removeRoles(String rolePattern){
        keycloakController.removeRolesFromClient(rolePattern);
    }


    /** USERS **/
    @Override
    public String authEndpoint() {
        return keycloakController.getServerUrl();
    }

    @Override
    public String tokenEndpoint() {
        return keycloakController.getTokenEndpoint();
    }

    @Override
    public User getMyUserInfo() {
        Map<String, Claim> userProfile = keycloakController.getUserProfile();
        return userProfile != null ? keycloakController.buildUserInfoFromKeycloak(userProfile) : null;
    }

    @Override
    public UserWithRoles getRoles() {
        //TODO cache by tokens
        Map<String, Claim> userProfile = keycloakController.getUserProfile();
        Map<String, Claim> clientProfile = keycloakController.getClientProfile();
        return userProfile != null ? new UserWithRoles(keycloakController.buildUserInfoFromKeycloak(userProfile), keycloakController.buildRoleListFromKeycloak(userProfile), keycloakController.buildRoleListFromKeycloak(clientProfile), keycloakController.getClientInfoFromKeycloak(clientProfile)) : null;
    }

    @Override
    public User getOtherUserInfo(String nativeId) {
        return keycloakController.getOtherUserInfo(nativeId);
    }

    @Override
    public List<User> getUsersByAttribute(String attribute, String value) {
        return keycloakController.getUsersByAttribute(attribute, value);
    }


    /** SETUP **/

    @Override
    public String setup(Credential credential){
        return initialSetup.initialSetup(credential.getUser(), credential.getPassword());
    }

}

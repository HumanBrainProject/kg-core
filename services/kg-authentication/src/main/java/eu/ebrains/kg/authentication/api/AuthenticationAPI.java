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

package eu.ebrains.kg.authentication.api;

import eu.ebrains.kg.authentication.controller.AuthenticationRepository;
import eu.ebrains.kg.authentication.keycloak.KeycloakController;
import eu.ebrains.kg.authentication.keycloak.KeycloakInitialSetup;
import eu.ebrains.kg.authentication.model.UserOrClientProfile;
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.exception.NotAcceptedTermsOfUseException;
import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.model.Credential;
import eu.ebrains.kg.commons.model.TermsOfUse;
import eu.ebrains.kg.commons.model.TermsOfUseResult;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.roles.Role;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthenticationAPI implements Authentication.Client {

    private final KeycloakController keycloakController;

    private final KeycloakInitialSetup initialSetup;

    private final AuthenticationRepository authenticationRepository;

    private final Permissions permissions;


    public AuthenticationAPI(KeycloakController keycloakController, KeycloakInitialSetup initialSetup, AuthenticationRepository authenticationRepository, Permissions permissions) {
        this.keycloakController = keycloakController;
        this.initialSetup = initialSetup;
        this.authenticationRepository = authenticationRepository;
        this.permissions = permissions;
    }

    /**
     * CLIENTS
     **/
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

    /**
     * ROLES
     **/
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
    public void removeRoles(String rolePattern) {
        keycloakController.removeRolesFromClient(rolePattern);
    }


    /**
     * USERS
     **/
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
        UserOrClientProfile userProfile = keycloakController.getUserProfile(false);
        return userProfile != null ? keycloakController.buildUserInfoFromKeycloak(userProfile.getClaims()) : null;
    }


    @Override
    public UserWithRoles getRoles(boolean checkForTermsOfUse) {
        UserOrClientProfile userProfile = keycloakController.getUserProfile(true);
        if (userProfile != null) {
            User user = keycloakController.buildUserInfoFromKeycloak(userProfile.getClaims());
            UserOrClientProfile clientProfile = keycloakController.getClientProfile(true);
            // We only do the terms of use check for direct access calls (the clients are required to ensure that the user
            // agrees to the terms of use.
            UserWithRoles userWithRoles = new UserWithRoles(user, userProfile.getRoleNames(), clientProfile != null ? clientProfile.getRoleNames() : null,
                    keycloakController.getClientInfoFromKeycloak(clientProfile != null ? clientProfile.getClaims() : null));
            if(checkForTermsOfUse && clientProfile==null) {
                TermsOfUse termsOfUseToAccept = authenticationRepository.findTermsOfUseToAccept(user.getNativeId());
                if (termsOfUseToAccept != null) {
                    throw new NotAcceptedTermsOfUseException(termsOfUseToAccept);
                }
            }
            //fetch the public spaces and assign the consumer role for them
            return userWithRoles;
        } else {
            return null;
        }
    }

    @Override
    public User getOtherUserInfo(String nativeId) {
        return keycloakController.getOtherUserInfo(nativeId);
    }

    @Override
    public List<User> getUsersByAttribute(String attribute, String value) {
        return keycloakController.getUsersByAttribute(attribute, value);
    }

    @Override
    public TermsOfUseResult getTermsOfUse() {
        TermsOfUse termsOfUse;
        UserOrClientProfile userProfile = keycloakController.getUserProfile(false);
        if (userProfile != null) {
            User user = keycloakController.buildUserInfoFromKeycloak(userProfile.getClaims());
            if (user != null) {
                termsOfUse = authenticationRepository.findTermsOfUseToAccept(user.getNativeId());
                if (termsOfUse != null) {
                    return new TermsOfUseResult(termsOfUse, false);
                }
            }
        }
        termsOfUse = authenticationRepository.getCurrentTermsOfUse();
        return termsOfUse != null ? new TermsOfUseResult(termsOfUse, true) : null;
    }

    @Override
    public void acceptTermsOfUse(String version) {
        User user = getMyUserInfo();
        if (user != null) {
            authenticationRepository.acceptTermsOfUse(version, user.getNativeId());
        } else {
            throw new IllegalArgumentException("Was not able to resolve the user information");
        }
    }

    @Override
    public void registerTermsOfUse(TermsOfUse termsOfUse) {
        //This is special -> the user doesn't need to accept the terms of use to register them (otherwise we would have a dead-lock)
        if (!permissions.hasGlobalPermission(this.getRoles(false), Functionality.DEFINE_TERMS_OF_USE)){
            throw new UnauthorizedException("You don't have the rights to define terms of use");
        }
        authenticationRepository.setCurrentTermsOfUse(termsOfUse);
    }

    @Override
    public boolean isSpacePublic(String space) {
        return authenticationRepository.isSpacePublic(space);
    }

    @Override
    public void setSpacePublic(String space){
        setSpacePublic(space, true);
    }

    @Override
    public void setSpaceProtected(String space){
        setSpacePublic(space, false);
    }

    private void setSpacePublic(String space, boolean publicSpace) {
        if(!permissions.hasGlobalPermission(this.getRoles(true), Functionality.DEFINE_PUBLIC_SPACE)){
            throw new UnauthorizedException("You don't have the rights do define a space to be public or not.");
        }
        authenticationRepository.setSpacePublic(space, publicSpace);
    }

    /**
     * SETUP
     **/
    @Override
    public String setup(Credential credential) {
        return initialSetup.initialSetup(credential.getUser(), credential.getPassword());
    }

}

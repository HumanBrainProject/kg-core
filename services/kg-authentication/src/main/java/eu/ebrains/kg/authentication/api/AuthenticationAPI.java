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

import eu.ebrains.kg.authentication.config.AuthorizationConfiguration;
import eu.ebrains.kg.authentication.controller.AuthenticationRepository;
import eu.ebrains.kg.authentication.controller.TermsOfUseRepository;
import eu.ebrains.kg.authentication.keycloak.KeycloakClient;
import eu.ebrains.kg.authentication.keycloak.KeycloakController;
import eu.ebrains.kg.authentication.model.UserOrClientProfile;
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.exception.NotAcceptedTermsOfUseException;
import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AuthenticationAPI implements Authentication.Client {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final KeycloakClient keycloakClient;

    private final KeycloakController keycloakController;

    private final AuthenticationRepository authenticationRepository;

    private final TermsOfUseRepository termsOfUseRepository;

    private final Permissions permissions;

    private final AuthorizationConfiguration authorizationConfiguration;

    public AuthenticationAPI(KeycloakClient keycloakClient, KeycloakController keycloakController, AuthenticationRepository authenticationRepository, TermsOfUseRepository termsOfUseRepository, Permissions permissions, AuthorizationConfiguration authorizationConfiguration) {
        this.keycloakController = keycloakController;
        this.keycloakClient = keycloakClient;
        this.authenticationRepository = authenticationRepository;
        this.termsOfUseRepository = termsOfUseRepository;
        this.permissions = permissions;
        this.authorizationConfiguration = authorizationConfiguration;
        if(authorizationConfiguration.isDisablePermissionAuthorization()){
            logger.warn("ATTENTION: You have disabled the authorization requirement for defining permissions! This is meant to be active only for the first execution! Please define a mapping for your administrator and set this property to false!");
        }
    }


    /**
     * CLIENTS
     **/
    @Override
    public ClientAuthToken fetchToken(String clientId, String clientSecret) {
        return new ClientAuthToken(keycloakController.authenticate(clientId, clientSecret));
    }

    /**
     * USERS
     **/
    @Override
    public String authEndpoint() {
        return keycloakClient.getServerUrl();
    }

    @Override
    public String openIdConfigUrl() {
        return keycloakClient.getOpenIdConfigUrl();
    }

    @Override
    public String tokenEndpoint() {
        return keycloakClient.getTokenEndpoint();
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
            if(clientProfile!=null && !keycloakController.isServiceAccount(clientProfile.getClaims())){
                throw new UnauthorizedException("The client authorization credentials you've passed doesn't belong to a service account. This is not allowed!");
            }
            List<UUID> invitationRoles = authenticationRepository.getInvitationRoles(user.getNativeId());
            UserWithRoles userWithRoles = new UserWithRoles(user, userProfile.getRoleNames(), clientProfile != null ? clientProfile.getRoleNames() : null, invitationRoles,
                    keycloakController.getClientInfoFromKeycloak(clientProfile != null ? clientProfile.getClaims() : null));
            // We only do the terms of use check for direct access calls (the clients are required to ensure that the user
            // agrees to the terms of use.)
            if(checkForTermsOfUse && clientProfile==null) {
                TermsOfUse termsOfUseToAccept = authenticationRepository.findTermsOfUseToAccept(user.getNativeId());
                if (termsOfUseToAccept != null) {
                    throw new NotAcceptedTermsOfUseException(termsOfUseToAccept);
                }
            }
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
    public List<ReducedUserInformation> findUsers(String search) {
        if(!permissions.hasGlobalPermission(this.getRoles(false), Functionality.LIST_USERS_LIMITED)){
            throw new UnauthorizedException("You don't have the rights to list users");
        }
        return keycloakController.findUsers(search);
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
        termsOfUse = termsOfUseRepository.getCurrentTermsOfUse();
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
        termsOfUseRepository.setCurrentTermsOfUse(termsOfUse);
    }

    /**
     * PERMISSIONS
     **/
    @Override
    public JsonLdDoc updateClaimForRole(RoleMapping role, String space, Map<?, ?> claimPattern, boolean removeClaim) {
        if(removeClaim){
            if(authorizationConfiguration.isDisablePermissionAuthorization() || permissions.hasGlobalPermission(this.getRoles(false), Functionality.DELETE_PERMISSION)) {
                return authenticationRepository.removeClaimFromRole(role.toRole(SpaceName.fromString(space)), claimPattern);
            }
            else{
                throw new UnauthorizedException("You don't have the rights to remove permissions");
            }
        }
        else{
            if(authorizationConfiguration.isDisablePermissionAuthorization() || permissions.hasGlobalPermission(this.getRoles(false), Functionality.CREATE_PERMISSION)) {
                return authenticationRepository.addClaimToRole(role.toRole(SpaceName.fromString(space)), claimPattern);
            }
            else{
                throw new UnauthorizedException("You don't have the rights to add permissions");
            }
        }
    }

    @Override
    public JsonLdDoc getClaimForRole(RoleMapping role, String space) {
        if(canShowPermissions()) {
            return authenticationRepository.getClaimForRole(role.toRole(SpaceName.fromString(space)));
        }
        else{
            throw new UnauthorizedException("You don't have the rights to show permissions");
        }
    }

    private boolean canShowPermissions(){
        final UserWithRoles roles = this.getRoles(false);
        return authorizationConfiguration.isDisablePermissionAuthorization() || permissions.hasGlobalPermission(roles, Functionality.DELETE_PERMISSION) || permissions.hasGlobalPermission(roles, Functionality.CREATE_PERMISSION);
    }

    @Override
    public List<JsonLdDoc> getAllRoleDefinitions() {
        final UserWithRoles roles = this.getRoles(false);
        if(canShowPermissions()) {
            return authenticationRepository.getAllRoleDefinitions();
        }
        else{
            throw new UnauthorizedException("You don't have the rights to show permissions");
        }
    }
}

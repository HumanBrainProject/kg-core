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

package eu.ebrains.kg.authentication.keycloak;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import eu.ebrains.kg.authentication.AuthenticationConfig;
import eu.ebrains.kg.authentication.model.OpenIdConfig;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.model.Client;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.permission.Role;
import eu.ebrains.kg.commons.permission.SpacePermissionGroup;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import javax.ws.rs.ProcessingException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeycloakController {

    private final KeycloakConfig config;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final JsonAdapter jsonAdapter;

    private final KeycloakClient keycloakClient;

    private final AuthContext authContext;

    private ClientScopeRepresentation profileScope;

    private final AuthenticationConfig authenticationConfig;

    private final KeycloakUsers keycloakUsers;

    private final JWTVerifier jwtVerifier;


    public KeycloakController(KeycloakConfig config, KeycloakClient keycloakClient, JsonAdapter jsonAdapter, AuthContext authContext, AuthenticationConfig authenticationConfig, KeycloakUsers keycloakUsers) {
        this.config = config;
        this.jsonAdapter = jsonAdapter;
        this.keycloakClient = keycloakClient;
        this.authContext = authContext;
        this.authenticationConfig = authenticationConfig;
        this.keycloakUsers = keycloakUsers;
        this.jwtVerifier = authenticationConfig.isDevelopMode() ? null : JWT.require(getAlgorithmFromKeycloakConfig(keycloakClient.getPublicKey())).withIssuer(config.getIssuer()).build(); //Reusable verifier instance;
    }

    private OpenIdConfig openIdConfig;

    public String getServerUrl() {
        return config.getServerUrl();
    }

    public String getTokenEndpoint() {
        return openIdConfig.getTokenEndpoint();
    }

    public String authenticate(String clientId, String clientSecret) {
        Map<?, ?> result = WebClient.builder().build().post().uri(openIdConfig.getTokenEndpoint()).body(BodyInserters.fromFormData("grant_type", "client_credentials").with("client_id", clientId).with("client_secret", clientSecret)).retrieve().bodyToMono(Map.class).block();
        if (result != null) {
            Object access_token = result.get("access_token");
            if (access_token != null) {
                return access_token.toString();
            }
        }
        return null;
    }


    private ClientRepresentation findClient(String clientName) {
        List<ClientRepresentation> clients = keycloakClient.getRealmResource().clients().findByClientId(clientName);
        if (clients != null && !clients.isEmpty()) {
            if (clients.size() == 1) {
                return clients.get(0);
            } else {
                throw new RuntimeException(String.format("Found ambiguous client with the client name %s", clientName));
            }
        }
        return null;
    }

    public void unregisterClient(String clientName) {
        ClientRepresentation c = findClient(clientName);
        if (c != null) {
            ClientResource clientResource = keycloakClient.getRealmResource().clients().get(c.getId());
            String serviceAccountId = clientResource.getServiceAccountUser().getId();
            UserResource userResource = keycloakClient.getRealmResource().users().get(serviceAccountId);
            userResource.roles().clientLevel(keycloakClient.getTechnicalClientId()).remove(userResource.roles().clientLevel(keycloakClient.getTechnicalClientId()).listAll());
        }
    }

    public Client registerClient(Client client) {
        ClientRepresentation c = findClient(client.getIdentifier());
        if (c != null) {
            client.setServiceAccountId(keycloakClient.getRealmResource().clients().get(c.getId()).getServiceAccountUser().getId());
        } else {
            //Create if it doesn't exist
            c = new ClientRepresentation();
            c.setClientId(client.getIdentifier());
            c.setDirectAccessGrantsEnabled(true);
            c.setImplicitFlowEnabled(false);
            c.setStandardFlowEnabled(false);
            c.setServiceAccountsEnabled(true);
            keycloakClient.getRealmResource().clients().create(c);
            c = findClient(client.getIdentifier());
        }
        if (c != null) {
            ClientResource clientResource = keycloakClient.getRealmResource().clients().get(c.getId());
            String serviceAccountId = clientResource.getServiceAccountUser().getId();
            String profileClientScopeId = getProfileClientScopeId(clientResource);
            if (profileClientScopeId != null) {
                clientResource.addDefaultClientScope(profileClientScopeId);
            }
            UserResource userResource = keycloakClient.getRealmResource().users().get(serviceAccountId);
            userResource.roles().clientLevel(keycloakClient.getTechnicalClientId()).add(getServiceAccountRoles(client.getSpace()));
            client.setServiceAccountId(serviceAccountId);
        }
        return client;
    }

    private String getProfileClientScopeId(ClientResource clientResource) {
        if (profileScope == null) {
            profileScope = clientResource.getDefaultClientScopes().stream().filter(scope -> scope.getName().equals("profile")).findFirst().orElse(null);
        }
        return profileScope != null ? profileScope.getId() : null;
    }

    private List<RoleRepresentation> getServiceAccountRoles(Space space) {
        String ownerRole = SpacePermissionGroup.OWNER.toRole(space).getName();
        RoleResource ownerRoleResource = keycloakClient.getClientResource().roles().get(ownerRole);
        String isClientRole = new FunctionalityInstance(Functionality.IS_CLIENT, null, null).toRole().getName();
        RoleResource isClientRoleResource = keycloakClient.getClientResource().roles().get(isClientRole);
        return Arrays.asList(ownerRoleResource.toRepresentation(), isClientRoleResource.toRepresentation());
    }

    public void createRoleForClient(Role role) {
        keycloakClient.createRoleForClient(role);
    }

    public List<Role> getNonExistingRoles(List<Role> roles) {
        List<String> existingRoles = keycloakClient.getClientResource().roles().list().stream().map(RoleRepresentation::getName).collect(Collectors.toList());
        return roles.stream().filter(r -> !existingRoles.contains(r.getName())).collect(Collectors.toList());
    }

    public List<User> getUsersInRole(String role) {
        RoleResource roleResource = keycloakClient.getClientResource().roles().get(role);
        Set<UserRepresentation> roleUserMembers = roleResource.getRoleUserMembers();
        return roleUserMembers.stream().map(u -> new User(u.getUsername(), u.getFirstName() + " " + u.getLastName(), null, u.getFirstName(), u.getLastName(), u.getId())).collect(Collectors.toList());
    }

    public void addUserToRole(String userId, String role) {
        RoleResource roleResource = keycloakClient.getClientResource().roles().get(role);
        UserResource userResource = keycloakClient.getRealmResource().users().get(userId);
        userResource.roles().clientLevel(keycloakClient.getTechnicalClientId()).add(Collections.singletonList(roleResource.toRepresentation()));
    }

    public void removeRolesFromClient(String rolePattern) {
        List<RoleRepresentation> roles = keycloakClient.getClientResource().roles().list();
        roles.stream().filter(r -> r.getName().matches(rolePattern)).forEach(r -> {
            logger.info(String.format("Removing role %s from client %s", r.getName(), keycloakClient.getClientId()));
            keycloakClient.getClientResource().roles().deleteRole(r.getName());
        });
    }

    @PostConstruct
    public void setup() {
        if (!authenticationConfig.isDevelopMode()) {
            if (config.getPwd() != null && !config.getPwd().isEmpty()) {
                try {
                    keycloakClient.ensureRealmClientAndGlobalRolesInKeycloak();
                } catch (ProcessingException ex) {
                    try {
                        logger.warn("Was not able to connect to keycloak - trying again in 5 secs...");
                        Thread.sleep(5000);
                        setup();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            loadOpenIdConfig();
        }
    }

    private void loadOpenIdConfig() {
        String result = WebClient.builder().build().get().uri(config.getConfigUrl()).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(String.class).block();
        openIdConfig = jsonAdapter.fromJson(result, OpenIdConfig.class);
    }


    @Cacheable(value = "usersByAttribute")
    public List<User> getUsersByAttribute(String attribute, String value) {
        return keycloakUsers.getAllUsers().stream().filter(user -> {
            if (user.getAttributes() != null) {
                List<String> values = user.getAttributes().get(attribute);
                return values != null && values.contains(value);
            }
            return false;
        }).map(userRepresentation -> new User(userRepresentation.getUsername(), userRepresentation.getFirstName() + " " + userRepresentation.getLastName(),
                //We explicitly do not share the email address if requested. The name should be sufficient to identify the person.
                null
                , userRepresentation.getFirstName(), userRepresentation.getLastName(), userRepresentation.getId())).collect(Collectors.toList());
    }

    @Cacheable(value = "userInfo")
    public User getOtherUserInfo(String id) {
        UserRepresentation userRepresentation;
        try {
            UUID uuid = UUID.fromString(id);
            logger.trace(String.format("Found uuid: %s", uuid.toString()));
            userRepresentation = keycloakClient.getRealmResource().users().get(id).toRepresentation();
        } catch (IllegalArgumentException e) {
            logger.trace("Resolving by user name");
            List<UserRepresentation> users = keycloakClient.getRealmResource().users().search(id);
            //Exact fit of username...
            userRepresentation = users.stream().filter(u -> u.getUsername().equals(id)).findFirst().orElse(null);
        }
        if (userRepresentation != null) {
            return new User(userRepresentation.getUsername(), userRepresentation.getFirstName() + " " + userRepresentation.getLastName(),
                    //We explicitly do not share the email address if requested. The name should be sufficient to identify the person.
                    null
                    , userRepresentation.getFirstName(), userRepresentation.getLastName(), userRepresentation.getId());
        }
        return null;
    }


    public Map<String, Claim> getClientProfile() {
        if (authContext.getAuthTokens() == null || authContext.getAuthTokens().getClientAuthToken() == null) {
            throw new UnauthorizedException("You haven't provided the required credentials! Please define a Client-Authorization header with your bearer token!");
        }
        return getInfo(authContext.getAuthTokens().getClientAuthToken().getBearerToken());
    }

    public Map<String, Claim> getUserProfile() {
        if (authContext.getAuthTokens() == null || authContext.getAuthTokens().getUserAuthToken() == null) {
            throw new UnauthorizedException("You haven't provided the required credentials! Please define an Authorization header with your bearer token!");
        }
        return getInfo(authContext.getAuthTokens().getUserAuthToken().getBearerToken());
    }

    Map<String, Claim> getInfo(String token) {
        String bareToken = token.substring("Bearer ".length());
        try {
            return jwtVerifier.verify(bareToken).getClaims();
        } catch (JWTVerificationException ex) {
            throw new UnauthorizedException(ex);
        }
    }

    private Algorithm getAlgorithmFromKeycloakConfig(String publicKey) {
        try {
            logger.debug(String.format("Validation by public RSA key (%s) of keycloak host %s", publicKey, config.getIssuer()));
            byte[] buffer = Base64.getDecoder().decode(publicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
            RSAPublicKey key = (RSAPublicKey) keyFactory.generatePublic(keySpec);
            Algorithm algorithm = Algorithm.RSA256(key, null);
            logger.info(String.format("Initialized validation by public RSA key of keycloak host %s", config.getIssuer()));
            return algorithm;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public String getClientInfoFromKeycloak(Map<String, Claim> authClientInfo) {
        Claim preferred_username = authClientInfo.get("preferred_username");
        return preferred_username != null ? preferred_username.asString().substring("service-account-".length()) : null;
    }

    public User buildUserInfoFromKeycloak(Map<String, Claim> authUserInfo) {
        Claim userName = authUserInfo.get("preferred_username");
        Claim nativeId = authUserInfo.get("sub");
        Claim name = authUserInfo.get("name");
        Claim givenName = authUserInfo.get("given_name");
        Claim familyName = authUserInfo.get("family_name");
        Claim email = authUserInfo.get("email");
        return new User(userName != null ? userName.asString() : null, name != null ? name.asString() : null, email != null ? email.asString() : null, givenName != null ? givenName.asString() : null, familyName != null ? familyName.asString() : null, nativeId != null ? nativeId.asString() : null);
    }

    public List<String> buildRoleListFromKeycloak(Map<String, Claim> authInfo) {
        Claim resource_access = authInfo.get("resource_access");
        if (resource_access != null) {
            Object kgResources = resource_access.asMap().get(config.getClientId());
            if (kgResources instanceof Map) {
                Map<String, Object> kg = (Map<String, Object>) kgResources;
                Object roles = kg.get("roles");
                if (roles instanceof List) {
                    //We're only interested in roles of our client
                    return (List<String>) roles;
                }
            }
        }
        return Collections.emptyList();
    }

}

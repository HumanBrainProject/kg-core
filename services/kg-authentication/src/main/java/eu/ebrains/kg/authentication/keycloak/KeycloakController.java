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

package eu.ebrains.kg.authentication.keycloak;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.google.common.collect.Streams;
import eu.ebrains.kg.authentication.controller.AuthenticationRepository;
import eu.ebrains.kg.authentication.model.OpenIdConfig;
import eu.ebrains.kg.authentication.model.UserOrClientProfile;
import eu.ebrains.kg.commons.AuthTokenContext;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.model.Client;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.permission.roles.Role;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.ProcessingException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;

@ConditionalOnProperty(value = "eu.ebrains.kg.test", havingValue = "false", matchIfMissing = true)
@Component
public class KeycloakController {

    private final KeycloakConfig config;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final JsonAdapter jsonAdapter;

    private final KeycloakClient keycloakClient;

    private final AuthTokenContext authTokenContext;

    private final KeycloakUsers keycloakUsers;

    private final JWTVerifier jwtVerifier;

    private final KeycloakUserInfoMapping userInfoMapping;

    private final AuthenticationRepository authenticationRepository;


    public KeycloakController(KeycloakConfig config, KeycloakClient keycloakClient, JsonAdapter jsonAdapter, AuthTokenContext authTokenContext, KeycloakUsers keycloakUsers, KeycloakUserInfoMapping userInfoMapping, AuthenticationRepository authenticationRepository) {
        this.config = config;
        this.jsonAdapter = jsonAdapter;
        this.keycloakClient = keycloakClient;
        this.authTokenContext = authTokenContext;
        this.keycloakUsers = keycloakUsers;
        this.jwtVerifier = JWT.require(getAlgorithmFromKeycloakConfig(keycloakClient.getPublicKey())).withIssuer(config.getIssuer()).build(); //Reusable verifier instance;
        this.userInfoMapping = userInfoMapping;
        this.authenticationRepository = authenticationRepository;
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
        if (c == null) {
            //Create if it doesn't exist
            c = new ClientRepresentation();
            c.setClientId(client.getIdentifier());
            keycloakClient.getRealmResource().clients().create(c);
            c = findClient(client.getIdentifier());
        }
        if (c != null) {
            c.setDirectAccessGrantsEnabled(false);
            c.setImplicitFlowEnabled(false);
            c.setStandardFlowEnabled(false);
            c.setServiceAccountsEnabled(true);
            c.setFullScopeAllowed(false);
            keycloakClient.getRealmResource().clients().get(c.getId()).update(c);
            List<Role> roles = Arrays.stream(RoleMapping.values()).filter(r -> r != RoleMapping.IS_CLIENT).map(p -> p.toRole(client.getSpace().getName())).collect(Collectors.toList());
            createRoles(roles);
            ClientResource clientResource = keycloakClient.getRealmResource().clients().get(c.getId());
            String serviceAccountId = clientResource.getServiceAccountUser().getId();
            ClientScopeRepresentation kgClientScope = keycloakClient.getKgClientScope();
            if (kgClientScope != null) {
                clientResource.addDefaultClientScope(kgClientScope.getId());
            }
            UserResource userResource = keycloakClient.getRealmResource().users().get(serviceAccountId);
            userResource.roles().clientLevel(keycloakClient.getTechnicalClientId()).add(getServiceAccountRoles(client.getSpace().getName()));
            client.setServiceAccountId(serviceAccountId);
        }
        return client;
    }

    private List<RoleRepresentation> getServiceAccountRoles(SpaceName space) {
        String ownerRole = RoleMapping.OWNER.toRole(space).getName();
        String clientRole = RoleMapping.IS_CLIENT.toRole(null).getName();

        RoleResource ownerRoleResource = keycloakClient.getClientResource().roles().get(ownerRole);
        RoleResource isClientRoleResource = keycloakClient.getClientResource().roles().get(clientRole);
        return Arrays.asList(ownerRoleResource.toRepresentation(), isClientRoleResource.toRepresentation());
    }

    public void createRoles(List<Role> roles) {
        roles.forEach(keycloakClient::createRoleForClient);
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

    private final int maxTries = 10;

    private void retryConnectingToKeycloak(int connectionTry) {
        if (config.getKgCoreClientSecret() == null || config.getKgCoreClientSecret().isBlank()) {
            logger.error("You haven't provided the kg-core client secret. If this is a new installation, you should consider executing the setup api");
        } else {
            if (maxTries > connectionTry) {
                try {
                    keycloakClient.ensureDefaultClientAndGlobalRolesInKeycloak();
                } catch (ProcessingException ex) {
                    if (ex.getCause() instanceof NotAuthorizedException) {
                        logger.error("The registered kg-core keycloak account is not authorized. Please make sure, you have the right credential (if this is a new installation, you should consider executing the setup api) ");
                    } else {
                        try {
                            logger.warn("Was not able to connect to keycloak - trying again in 5 secs...");
                            Thread.sleep(5000);
                            retryConnectingToKeycloak(connectionTry + 1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                logger.error(String.format("Gave up to connect to Keycloak after %d attempts - maybe the authentication system is not properly set up", maxTries));
            }
        }
    }

    @PostConstruct
    public void setup() {
        retryConnectingToKeycloak(0);
        loadOpenIdConfig();
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


    public UserOrClientProfile getClientProfile(boolean fetchRoles) {
        if (authTokenContext.getAuthTokens() == null || authTokenContext.getAuthTokens().getClientAuthToken() == null) {
            return null;
        }
        return getInfo(authTokenContext.getAuthTokens().getClientAuthToken().getBearerToken(), fetchRoles);
    }

    public UserOrClientProfile getUserProfile(boolean fetchRoles) {
        if (authTokenContext.getAuthTokens() == null || authTokenContext.getAuthTokens().getUserAuthToken() == null) {
            throw new UnauthorizedException("You haven't provided the required credentials! Please define an Authorization header with your bearer token!");
        }
        return getInfo(authTokenContext.getAuthTokens().getUserAuthToken().getBearerToken(), fetchRoles);
    }

    UserOrClientProfile getInfo(String token, boolean fetchRoles) {
        String bareToken = token.substring("Bearer ".length());
        try {
            Map<String, Claim> claims = jwtVerifier.verify(bareToken).getClaims();
            if(fetchRoles){
                return new UserOrClientProfile(claims, userInfoMapping.getUserOrClientProfile(token, openIdConfig.getUserInfoEndpoint()));
            }
            return new UserOrClientProfile(claims, null);
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
        if (authClientInfo != null) {
            Claim preferred_username = authClientInfo.get("preferred_username");
            return preferred_username != null ? preferred_username.asString().substring("service-account-".length()) : null;
        } else {
            return "direct access";
        }
    }

    public User buildUserInfoFromKeycloak(Map<String, Claim> claims) {
        Claim userName = claims.get("preferred_username");
        Claim nativeId = claims.get("sub");
        Claim name = claims.get("name");
        Claim givenName = claims.get("given_name");
        Claim familyName = claims.get("family_name");
        Claim email = claims.get("email");
        return new User(userName != null ? userName.asString() : null, name != null ? name.asString() : null, email != null ? email.asString() : null, givenName != null ? givenName.asString() : null, familyName != null ? familyName.asString() : null, nativeId != null ? nativeId.asString() : null);
    }

}

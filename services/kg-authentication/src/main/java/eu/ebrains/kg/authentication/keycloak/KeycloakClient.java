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

import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.permission.GlobalPermissionGroup;
import eu.ebrains.kg.commons.permission.Role;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class KeycloakClient {

    private final KeycloakConfig config;

    private final Keycloak adminKeycloak;

    private final WebClient.Builder webclient;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ClientResource clientResource;

    private String technicalClientId;

    public KeycloakClient(KeycloakConfig config, Keycloak adminKeycloak, @Qualifier("internal") WebClient.Builder internalWebClient) {
        this.config = config;
        this.adminKeycloak = adminKeycloak;
        this.webclient = internalWebClient;
    }

    String getCurrentAccessToken() {
        return this.adminKeycloak.tokenManager().getAccessTokenString();
    }

    String getClientId() {
        return config.getClientId();
    }

    String getPublicKey(){
        Map issuerInfo = this.webclient.build().get().uri(config.getIssuer()).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(Map.class).block();
        Object public_key = issuerInfo.get("public_key");
        if(public_key instanceof String){
            return (String)public_key;
        }
        return null;
    }

    private synchronized ClientRepresentation getClient() {
        if (clientResource == null) {
            List<ClientRepresentation> byClientId = getRealmResource().clients().findByClientId(config.getClientId());
            if (byClientId != null && !byClientId.isEmpty()) {
                ClientRepresentation clientRepresentation = byClientId.get(0);
                technicalClientId = clientRepresentation.getId();
                clientResource = getRealmResource().clients().get(technicalClientId);
                return clientRepresentation;
            }
            return null;
        }
        return clientResource.toRepresentation();
    }

    ClientResource getClientResource() {
        if (clientResource == null) {
            getClient();
        }
        return clientResource;
    }

    public String getTechnicalClientId() {
        return technicalClientId;
    }

    RealmResource getRealmResource() {
        return this.adminKeycloak.realm(config.getRealm());
    }

    void ensureRealmClientAndGlobalRolesInKeycloak() {
        RealmRepresentation realmRepresentation = getRealmResource().toRepresentation();
        if (realmRepresentation == null) {
            try {
                this.adminKeycloak.realms().create(createRealm());
            } catch (ForbiddenException e) {
                throw new UnauthorizedException(String.format("The keycloak user you have specified does not have the permissions to create a realm and the realm %s doesn't exist yet", config.getRealm()));
            }
        }
        try {
            try {
                if (getClient() == null) {
                    createDefaultClient();
                }
                configureDefaultClient();
            } catch (ForbiddenException e) {
                throw new UnauthorizedException(String.format("Your keycloak user is not allowed to read clients of realm %s", config.getRealm()));
            }
        } catch (ForbiddenException e) {
            throw new UnauthorizedException("The keycloak user you've specified is not allowed to read the realms");
        }
    }

    private void configureDefaultClient() {
        try {
            ClientRepresentation clientRepresentation = new ClientRepresentation();
            clientRepresentation.setClientId(config.getClientId());
            clientRepresentation.setEnabled(true);
            clientRepresentation.setConsentRequired(true);
            clientRepresentation.setImplicitFlowEnabled(true);
            clientRepresentation.setStandardFlowEnabled(false);
            Map<String, String> attributes = new HashMap<>();
            attributes.put("access.token.lifespan", "1800");
            clientRepresentation.setAttributes(attributes);
            clientRepresentation.setRedirectUris(Arrays.asList(getRedirectUri(), "http://localhost*"));
            getClientResource().update(clientRepresentation);
            Functionality.getGlobalFunctionality().forEach(f -> createRoleForClient(new FunctionalityInstance(f, null, null).toRole()));
            Arrays.stream(GlobalPermissionGroup.values()).forEach(p -> createRoleForClient(p.toRole()));
        } catch (ForbiddenException e) {
            throw new UnauthorizedException(String.format("Your keycloak account does not allow to configure clients in realm %s", config.getRealm()));
        }
    }

    private void createDefaultClient() {
        try {
            ClientRepresentation clientRepresentation = new ClientRepresentation();
            clientRepresentation.setClientId(config.getClientId());
            getRealmResource().clients().create(clientRepresentation);
        } catch (ForbiddenException e) {
            throw new UnauthorizedException(String.format("Your keycloak client does not allow to create clients in realm %s", config.getRealm()));
        }
    }


    private RealmRepresentation createRealm() {
        RealmRepresentation kgRealm = new RealmRepresentation();
        kgRealm.setRealm(config.getRealm());
        kgRealm.setEnabled(true);
        kgRealm.setUserManagedAccessAllowed(true);
        kgRealm.setRegistrationAllowed(true);
        kgRealm.setVerifyEmail(true);
        kgRealm.setLoginWithEmailAllowed(true);
        kgRealm.setResetPasswordAllowed(true);
        //TODO Check numbers / make them configurable.
        kgRealm.setSsoSessionIdleTimeout(4 * 60);
        kgRealm.setSsoSessionMaxLifespan(7 * 24 * 60);
        kgRealm.setAccessTokenLifespan(60 * 60);
        kgRealm.setAccessTokenLifespanForImplicitFlow(60 * 60);
        kgRealm.setMaxDeltaTimeSeconds(30 * 60);

        return kgRealm;
    }

    private String getRedirectUri() {
        return String.format("%s%s*", config.isHttps() ? "https://" : "http://", config.getIp());
    }


    public void createRoleForClient(Role role) {
        try {
            getClientResource().roles().get(role.getName()).toRepresentation();
        } catch (NotFoundException e) {
            RoleRepresentation roleRepresentation = new RoleRepresentation();
            roleRepresentation.setName(role.getName());
            logger.info(String.format("Create role %s in client %s", role.getName(), getClientId()));

            getClientResource().roles().create(roleRepresentation);
            if (role.getIncludedRoles() != null && !role.getIncludedRoles().isEmpty()) {
                RoleResource roleResource = getClientResource().roles().get(role.getName());
                roleResource.addComposites(role.getIncludedRoles().stream().map(r -> {
                    try {
                        return getClientResource().roles().get(r.getName()).toRepresentation();
                    } catch (NotFoundException notFound) {
                        logger.error(String.format("Was not able to find role %s", r.getName()));
                        throw notFound;
                    }
                }).collect(Collectors.toList()));
            }
        }
    }

}

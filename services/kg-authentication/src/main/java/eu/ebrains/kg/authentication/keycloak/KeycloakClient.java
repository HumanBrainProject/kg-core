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

import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.permission.roles.Role;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class KeycloakClient {

    private final KeycloakConfig config;

    private final Keycloak kgKeycloak;

    private final WebClient.Builder webclient;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ClientResource clientResource;

    private String technicalClientId;

    public KeycloakClient(KeycloakConfig config, Keycloak kgKeycloak, @Qualifier("direct") WebClient.Builder internalWebClient) {
        this.config = config;
        this.webclient = internalWebClient;
        this.kgKeycloak = kgKeycloak;
    }

    String getClientId() {
        return config.getKgClientId();
    }

    String getPublicKey() {
        Map issuerInfo = this.webclient.build().get().uri(config.getIssuer()).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(Map.class).block();
        Object public_key = issuerInfo.get("public_key");
        if (public_key instanceof String) {
            return (String) public_key;
        }
        return null;
    }

    public ClientScopeRepresentation getKgClientScope(){
        return getRealmResource().clientScopes().findAll().stream().filter(sc -> sc.getName().equals(config.getKgClientId())).findFirst().orElse(null);
    }


    private synchronized ClientRepresentation getClient() {
        if (clientResource == null) {
            List<ClientRepresentation> byClientId = getRealmResource().clients().findByClientId(config.getKgClientId());
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
        return this.kgKeycloak.realm(config.getRealm());
    }

    public void ensureDefaultClientAndGlobalRolesInKeycloak() {
        try {
        getRealmResource().toRepresentation();
        try {
                boolean initialCreation = false;
                if (getClient() == null) {
                    createDefaultClient();
                    initialCreation = true;
                }
                configureDefaultClient(initialCreation);
            } catch (ForbiddenException e) {
                throw new UnauthorizedException(String.format("Your keycloak user is not allowed to read clients of realm %s", config.getRealm()));
            }
        } catch (ForbiddenException e) {
            throw new UnauthorizedException("The keycloak user you've specified is not allowed to read the realms");
        }
    }

    private void configureDefaultClient(boolean initialConfig) {
        try {
            ClientRepresentation clientRepresentation = new ClientRepresentation();
            clientRepresentation.setClientId(config.getKgClientId());
            clientRepresentation.setEnabled(true);
            clientRepresentation.setConsentRequired(true);
            clientRepresentation.setImplicitFlowEnabled(true);
            clientRepresentation.setStandardFlowEnabled(true);
            clientRepresentation.setFullScopeAllowed(false);
            clientRepresentation.setPublicClient(true);
            Map<String, String> attributes = new HashMap<>();
            attributes.put("display.on.consent.screen", "true");
            attributes.put("access.token.lifespan", "1800");
            attributes.put("pkce.code.challenge.method", "S256");
            attributes.put("consent.screen.text", "By using the EBRAINS Knowledge Graph, you agree to the according terms of use available at https://kg.ebrains.eu/search-terms-of-use.html");
            clientRepresentation.setAttributes(attributes);
            if (initialConfig) {
                clientRepresentation.setRedirectUris(Arrays.asList(getRedirectUri(), "http://localhost*"));
            }
            getClientResource().update(clientRepresentation);
            Arrays.stream(RoleMapping.values()).forEach(p -> createRoleForClient(p.toRole(null)));
        } catch (ForbiddenException e) {
            throw new UnauthorizedException(String.format("Your keycloak account does not allow to configure clients in realm %s", config.getRealm()));
        }
    }


    private void createDefaultClient() {
        try {
            ClientRepresentation clientRepresentation = new ClientRepresentation();
            clientRepresentation.setClientId(config.getKgClientId());
            getRealmResource().clients().create(clientRepresentation);
            getClientResource().getDefaultClientScopes().forEach(c -> getClientResource().removeDefaultClientScope(c.getId()));
        } catch (ForbiddenException e) {
            throw new UnauthorizedException(String.format("Your keycloak client does not allow to create clients in realm %s", config.getRealm()));
        }
    }


    private String getRedirectUri() {
        return String.format("%s%s*", config.isHttps() ? "https://" : "http://", config.getIp());
    }


    public void createRoleForClient(Role role) {
        try {
            getClientResource().roles().get(role.getName()).toRepresentation();
        } catch (NotFoundException e) {
            try {
                RoleRepresentation roleRepresentation = new RoleRepresentation();
                roleRepresentation.setName(role.getName());
                logger.info(String.format("Create role %s in client %s", role.getName(), getClientId()));
                getClientResource().roles().create(roleRepresentation);
            } catch (ForbiddenException ex) {
                throw new UnauthorizedException(String.format("Your keycloak account does not allow to configure roles in realm %s and client %s", config.getRealm(), config.getKgClientId()));
            }
        }
    }

    public Map<String, Object> getRolesFromUserInfoFromCache(String token, String userInfoEndpoint){
        return this.webclient.build().get().uri(userInfoEndpoint).accept(MediaType.APPLICATION_JSON).header("Authorization", token).retrieve().bodyToMono(Map.class).block();
    }

}

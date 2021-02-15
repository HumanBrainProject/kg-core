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

package eu.ebrains.kg.authentication.keycloak;

import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.permission.roles.Role;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class KeycloakClient {

    private final KeycloakConfig config;

    private final Keycloak kgKeycloak;

    private final WebClient.Builder webclient;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ClientResource clientResource;

    private String technicalClientId;

    private final String kgClientScopeName;

    public String getKgClientScopeName() {
        return kgClientScopeName;
    }

    public KeycloakClient(KeycloakConfig config, Keycloak kgKeycloak, @Qualifier("direct") WebClient.Builder internalWebClient) {
        this.config = config;
        this.webclient = internalWebClient;
        this.kgKeycloak = kgKeycloak;
        this.kgClientScopeName = config.getKgClientId() + "-scope";
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
            ClientScopeRepresentation clientScope = createClientScope();
            ClientRepresentation clientRepresentation = new ClientRepresentation();
            clientRepresentation.setClientId(config.getKgClientId());
            clientRepresentation.setEnabled(true);
            clientRepresentation.setConsentRequired(true);
            clientRepresentation.setImplicitFlowEnabled(true);
            clientRepresentation.setStandardFlowEnabled(true);
            clientRepresentation.setFullScopeAllowed(false);
            clientRepresentation.setPublicClient(true);
            getClientResource().addDefaultClientScope(clientScope.getId());
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

    public ClientScopeRepresentation getKgClientScope(){
        return getRealmResource().clientScopes().findAll().stream().filter(sc -> sc.getName().equals(kgClientScopeName)).findFirst().orElse(null);
    }

    private ClientScopeRepresentation createClientScope() {
        ClientScopeRepresentation clientScope = getKgClientScope();
        if (clientScope == null) {
            clientScope = new ClientScopeRepresentation();
        }
        clientScope.setName(kgClientScopeName);
        clientScope.setProtocol("openid-connect");
        Map<String, String> attrs = new HashMap<>();
        attrs.put("display.on.consent.screen", "true");
        attrs.put("include.in.token.scope", "true");
        attrs.put("consent.screen.text", "Knowledge Graph: Your roles in the Knowledge Graph. \n " +
                "By making use of the EBRAINS Knowledge Graph, you agree to the terms of use available at https://kg.ebrains.eu/search-terms-of-use.html");
        clientScope.setAttributes(attrs);
        String roleMapperName = "client roles";
        List<ProtocolMapperRepresentation> protocolMappers = clientScope.getProtocolMappers();
        if(protocolMappers==null){
            protocolMappers = new ArrayList<>();
            clientScope.setProtocolMappers(protocolMappers);
        }
        ProtocolMapperRepresentation mapper = protocolMappers.stream().filter(p -> p.getName().equals(roleMapperName)).findFirst().orElse(null);
        if(mapper == null){
            mapper = new ProtocolMapperRepresentation();
        }
        mapper.setProtocolMapper("oidc-usermodel-client-role-mapper");
        mapper.setProtocol("openid-connect");
        mapper.setName("client roles");
        Map<String, String> c = new HashMap<>();
        c.put("access.token.claim", "true");
        c.put("claim.name", "resource_access.kg.roles");
        c.put("id.token.claim", "false");
        c.put("jsonType.label", "String");
        c.put("multivalued", "true");
        c.put("userinfo.token.claim", "true");
        c.put("usermodel.clientRoleMapping.clientId", config.getKgClientId());
        c.put("usermodel.clientRoleMapping.rolePrefix", "");
        mapper.setConfig(c);
        if (clientScope.getId()!=null) {
            getRealmResource().clientScopes().get(clientScope.getId()).update(clientScope);
        } else {
            Response response = getRealmResource().clientScopes().create(clientScope);
            Response.Status status = response.getStatusInfo().toEnum();
            if (Response.Status.Family.SUCCESSFUL != status.getFamily()) {
                throw new RuntimeException(status.getReasonPhrase());
            }
        }
        clientScope = getKgClientScope();
        if ( clientScope != null) {
            List<ProtocolMapperRepresentation> reloadedProtocolMappers = clientScope.getProtocolMappers();
            List<String> existingMappers = reloadedProtocolMappers==null ? new ArrayList<>() : reloadedProtocolMappers.stream().map(ProtocolMapperRepresentation::getName).collect(Collectors.toList());
            List<String> builtinProtocolMappers = Stream.of("given name", "full name", "family name", "email", "username").filter(l -> !existingMappers.contains(l)).collect(Collectors.toList());
            List<ProtocolMapperRepresentation> mappers = kgKeycloak.serverInfo().getInfo().getBuiltinProtocolMappers().get("openid-connect").stream().filter(p -> builtinProtocolMappers.contains(p.getName())).collect(Collectors.toList());
            getRealmResource().clientScopes().get(clientScope.getId()).getProtocolMappers().createMapper(mappers);
            if(mapper.getId()!=null){
                getRealmResource().clientScopes().get(clientScope.getId()).getProtocolMappers().update(mapper.getId(), mapper);
            }
            else{
                getRealmResource().clientScopes().get(clientScope.getId()).getProtocolMappers().createMapper(mapper);
            }
            syncKgScopeWithKgRoles(clientScope.getId());
        }
        return clientScope;
    }

    public void syncKgScopeWithKgRoles(){
        ClientScopeRepresentation kgClientScope = getKgClientScope();
        if(kgClientScope!=null) {
            syncKgScopeWithKgRoles(kgClientScope.getId());
        }
    }

    private void syncKgScopeWithKgRoles(String clientScopeId){
        List<RoleRepresentation> existingRoles = getRealmResource().clientScopes().get(clientScopeId).getScopeMappings().clientLevel(getClient().getId()).listAll();
        Set<String> existingRoleNames = existingRoles.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
        List<RoleRepresentation> clientRoles = getRealmResource().clients().get(getClient().getId()).roles().list();
        final AtomicInteger counter = new AtomicInteger();
        //We need to make sure that there are not too many roles reported at once. This is why we split the number of roles into chunks.
        Collection<List<RoleRepresentation>> newRolesChunks = clientRoles.stream().filter(r -> !existingRoleNames.contains(r.getName())).collect(Collectors.groupingBy(it -> counter.getAndIncrement() / 100)).values();
        newRolesChunks.forEach(newRoles -> {
            getRealmResource().clientScopes().get(clientScopeId).getScopeMappings().clientLevel(getClient().getId()).add(newRoles);
        });
        Set<String> clientRoleNames = clientRoles.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
        Collection<List<RoleRepresentation>> outdatedRolesChunks = existingRoles.stream().filter(r -> !clientRoleNames.contains(r.getName())).collect(Collectors.groupingBy(it -> counter.getAndIncrement() / 100)).values();
        outdatedRolesChunks.forEach(outdatedRoles -> {
            getRealmResource().clientScopes().get(clientScopeId).getScopeMappings().clientLevel(getClient().getId()).remove(outdatedRoles);
        });

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

}

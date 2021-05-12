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
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class KeycloakInitialSetup {

    private final KeycloakConfig config;

    private final Logger logger = LoggerFactory.getLogger(getClass());


    public KeycloakInitialSetup(KeycloakConfig keycloakConfig) {
        this.config = keycloakConfig;
    }

    private RealmRepresentation getOrCreateRealm(Keycloak keycloak) {
        RealmRepresentation realmRepresentation;
        try {
            realmRepresentation = keycloak.realm(config.getRealm()).toRepresentation();
        } catch (NotFoundException ex) {
            realmRepresentation = null;
        }
        if (realmRepresentation == null) {
            try {
                logger.info(String.format("Now creating the realm %s because it doesn't exist", config.getRealm()));
                keycloak.realms().create(createRealm());
                realmRepresentation = keycloak.realm(config.getRealm()).toRepresentation();
            } catch (ForbiddenException e) {
                throw new UnauthorizedException(String.format("The keycloak user you have specified does not have the permissions to create a realm and the realm %s doesn't exist yet", config.getRealm()));
            }
        }
        return realmRepresentation;
    }

    private ClientRepresentation getClientRepresentation(Keycloak keycloak, String clientId) {
        try {
            List<ClientRepresentation> byClientId = keycloak.realm(config.getRealm()).clients().findByClientId(clientId);
            if (byClientId != null && !byClientId.isEmpty()) {
                return byClientId.get(0);
            }
            return null;
        }
        catch (NotFoundException e){
            return null;
        }
    }

    private ClientRepresentation getOrCreateKgCoreClient(Keycloak keycloak) {
        ClientRepresentation clientRepresentation = getClientRepresentation(keycloak, config.getKgCoreClientId());
        if (clientRepresentation == null) {
            try {
                keycloak.realm(config.getRealm()).clients().create(createKgCoreClient());
                return getClientRepresentation(keycloak, config.getKgCoreClientId());
            } catch (ForbiddenException e) {
                throw new UnauthorizedException(String.format("The provided keycloak credentials do not allow to create the client %s in realm %s", config.getKgCoreClientId(), config.getRealm()));
            }
        }
        return clientRepresentation;
    }

    private String configureKgCoreClient(Keycloak keycloak, ClientRepresentation c){
        c.setDirectAccessGrantsEnabled(false);
        c.setImplicitFlowEnabled(false);
        c.setStandardFlowEnabled(false);
        c.setServiceAccountsEnabled(true);
        c.setPublicClient(false);
        c.setBearerOnly(false);
        c.setFullScopeAllowed(false);
        ClientResource clientResource = keycloak.realm(config.getRealm()).clients().get(c.getId());
        clientResource.update(c);
        List<ClientScopeRepresentation> clientScopes = keycloak.realm(config.getRealm()).clientScopes().findAll();
        String realmManagementScope = clientScopes.stream().filter(scope -> scope.getName().equals("realm-management")).map(scope -> scope.getId()).findFirst().orElse(null);
        clientResource.addDefaultClientScope(realmManagementScope);
        UserRepresentation serviceAccountUser = clientResource.getServiceAccountUser();
        ClientRepresentation realmClient = getClientRepresentation(keycloak, "realm-management");
        if(realmClient!=null) {
            RoleScopeResource roleScopeResource = keycloak.realm(config.getRealm()).users().get(serviceAccountUser.getId()).roles().clientLevel(realmClient.getId());
            List<RoleRepresentation> roleRepresentations = roleScopeResource.listAvailable();
            List<String> roles = Arrays.asList("create-client", "manage-clients", "manage-users", "query-clients", "query-groups", "query-users", "view-clients", "view-realm", "view-users");
            roleScopeResource.add(roleRepresentations.stream().filter(r -> roles.contains(r.getName())).collect(Collectors.toList()));
        }
        else{
            throw new IllegalStateException(String.format("Was not able to find the client \"realm-management\" and therefore was not able to properly configure the client %s", config.getKgCoreClientId()));
        }
        return clientResource.getSecret().getValue();
    }

    private ClientRepresentation createKgCoreClient() {
        ClientRepresentation c = new ClientRepresentation();
        c.setClientId(config.getKgCoreClientId());
        return c;
    }

    public String initialSetup(String user, String password) {
        Keycloak keycloak = KeycloakBuilder.builder().username(user).password(password).serverUrl(config.getServerUrl()).realm("master").clientId("admin-cli").build();
        getOrCreateRealm(keycloak);
        ClientRepresentation kgCoreClient = getOrCreateKgCoreClient(keycloak);
        return configureKgCoreClient(keycloak, kgCoreClient);
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

}

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
import eu.ebrains.kg.authentication.controller.InvitationController;
import eu.ebrains.kg.authentication.controller.TermsOfUseRepository;
import eu.ebrains.kg.authentication.keycloak.KeycloakClient;
import eu.ebrains.kg.authentication.keycloak.KeycloakController;
import eu.ebrains.kg.authentication.model.InstanceScope;
import eu.ebrains.kg.authentication.model.Invitation;
import eu.ebrains.kg.commons.api.GraphDBScopes;
import eu.ebrains.kg.commons.api.Ids;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InvitationAPI implements eu.ebrains.kg.commons.api.Invitation.Client {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AuthenticationRepository authenticationRepository;
    private final InvitationController invitationController;
    private final KeycloakController keycloakController;

    public InvitationAPI(AuthenticationRepository authenticationRepository, InvitationController invitationController, KeycloakController keycloakController) {
        this.authenticationRepository = authenticationRepository;
        this.invitationController = invitationController;
        this.keycloakController = keycloakController;
    }

    @Override
    public void inviteUserForInstance(UUID id, UUID userId) {
        authenticationRepository.createInvitation(new Invitation(id.toString(), userId.toString()));
        this.invitationController.calculateInstanceScope(id);
    }

    @Override
    public void revokeUserInvitation(UUID id, UUID userId) {
        authenticationRepository.deleteInvitation(new Invitation(id.toString(), userId.toString()));
    }

    @Override
    public List<ReducedUserInformation> listInvitations(UUID id) {
        if(id!=null){
            final List<Invitation> allInvitationsByInstanceId = authenticationRepository.getAllInvitationsByInstanceId(id.toString());
            return allInvitationsByInstanceId.stream().map(i -> keycloakController.getUserById(i.getUserId())).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void calculateInstanceScope(UUID id) {
        this.invitationController.calculateInstanceScope(id);
    }
}

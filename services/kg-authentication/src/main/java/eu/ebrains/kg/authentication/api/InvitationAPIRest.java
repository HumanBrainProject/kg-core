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

import eu.ebrains.kg.commons.api.Invitation;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.model.ReducedUserInformation;
import eu.ebrains.kg.commons.model.TermsOfUse;
import eu.ebrains.kg.commons.model.TermsOfUseResult;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequestMapping("/internal/invitations")
@RestController
@ConditionalOnProperty(value = "eu.ebrains.kg.test", havingValue = "false", matchIfMissing = true)
public class InvitationAPIRest implements Invitation {

    private final InvitationAPI invitationAPI;

    public InvitationAPIRest(InvitationAPI invitationAPI) {
        this.invitationAPI = invitationAPI;
    }


    @Override
    @PutMapping("{instanceId}/{userId}")
    public void inviteUserForInstance(@PathVariable("instanceId") UUID id, @PathVariable("userId") UUID userId) {
        invitationAPI.inviteUserForInstance(id, userId);
    }

    @Override
    @DeleteMapping("{instanceId}/{userId}")
    public void revokeUserInvitation(@PathVariable("instanceId")UUID id,  @PathVariable("userId") UUID userId) {
        invitationAPI.revokeUserInvitation(id, userId);
    }

    @Override
    @GetMapping("{instanceId}")
    public List<ReducedUserInformation> listInvitations(@PathVariable("instanceId")UUID id) {
        return invitationAPI.listInvitations(id);
    }

    @Override
    @PutMapping("{instanceId}")
    public void calculateInstanceScope(@PathVariable("instanceId")UUID id) {
        invitationAPI.calculateInstanceScope(id);
    }
}

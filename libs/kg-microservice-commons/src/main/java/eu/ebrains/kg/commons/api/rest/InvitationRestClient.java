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

package eu.ebrains.kg.commons.api.rest;

import eu.ebrains.kg.commons.AuthTokenContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.api.Invitation;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.model.ReducedUserInformation;
import eu.ebrains.kg.commons.model.TermsOfUse;
import eu.ebrains.kg.commons.model.TermsOfUseResult;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestClient
public class InvitationRestClient implements Invitation.Client {

    private final static String SERVICE_URL = "http://kg-authentication/internal/invitations";
    private final ServiceCall serviceCall;
    private final AuthTokenContext authTokenContext;

    public InvitationRestClient(ServiceCall serviceCall, AuthTokenContext authTokenContext) {
        this.serviceCall = serviceCall;
        this.authTokenContext = authTokenContext;
    }

    @Override
    public void inviteUserForInstance(UUID id, UUID userId) {
        serviceCall.put(String.format("%s/%s/%s", SERVICE_URL, id, userId), null, authTokenContext.getAuthTokens(),  Void.class);
    }

    @Override
    public void revokeUserInvitation(UUID id, UUID userId) {
        serviceCall.delete(String.format("%s/%s/%s", SERVICE_URL, id, userId), authTokenContext.getAuthTokens(), Void.class);
    }

    @Override
    public List<ReducedUserInformation> listInvitations(UUID id) {
        return Arrays.asList(serviceCall.get(String.format("%s/%s", SERVICE_URL, id), authTokenContext.getAuthTokens(), ReducedUserInformation[].class));
    }

    @Override
    public void calculateInstanceScope(UUID id) {
        Arrays.asList(serviceCall.put(String.format("%s/%s", SERVICE_URL, id), null, authTokenContext.getAuthTokens(), Void.class));
    }
}

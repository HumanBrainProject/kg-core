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
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.model.ReducedUserInformation;
import eu.ebrains.kg.commons.model.TermsOfUse;
import eu.ebrains.kg.commons.model.TermsOfUseResult;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import org.springframework.http.MediaType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestClient
public class AuthenticationRestClient implements Authentication.Client {

    private final static String SERVICE_URL = "http://kg-authentication/internal/authentication";
    private final ServiceCall serviceCall;
    private final AuthTokenContext authTokenContext;

    public AuthenticationRestClient(ServiceCall serviceCall, AuthTokenContext authTokenContext) {
        this.serviceCall = serviceCall;
        this.authTokenContext = authTokenContext;
    }

    @Override
    public ClientAuthToken fetchToken(String clientId, String clientSecret) {
        return serviceCall.post(String.format("%s/clients/%s/token", SERVICE_URL, clientId), clientSecret, authTokenContext.getAuthTokens(), ClientAuthToken.class);
    }

    @Override
    public String authEndpoint() {
        return serviceCall.get(String.format("%s/users/authorization/endpoint", SERVICE_URL), MediaType.TEXT_PLAIN, authTokenContext.getAuthTokens(), String.class);
    }

    @Override
    public String tokenEndpoint() {
        return serviceCall.get(String.format("%s/users/authorization/tokenEndpoint", SERVICE_URL), MediaType.TEXT_PLAIN, authTokenContext.getAuthTokens(), String.class);
    }

    @Override
    public User getMyUserInfo() {
        return serviceCall.get(String.format("%s/users/me", SERVICE_URL), authTokenContext.getAuthTokens(), User.class);
    }

    @Override
    public UserWithRoles getRoles(boolean checkForTermsOfUse) {
        return serviceCall.get(String.format("%s/users/meWithRoles?checkForTermsOfUse=%b", SERVICE_URL, checkForTermsOfUse), authTokenContext.getAuthTokens(), UserWithRoles.class);
    }

    @Override
    public User getOtherUserInfo(String nativeId) {
        return serviceCall.get( String.format("%s/users/profiles/%s", SERVICE_URL, nativeId), authTokenContext.getAuthTokens(), User.class);
    }

    @Override
    public List<User> getUsersByAttribute(String attribute, String value) {
        return Arrays.asList(serviceCall.get(String.format("%s/users/profiles/byAttribute/%s/%s", SERVICE_URL, attribute, value), authTokenContext.getAuthTokens(), User[].class));
    }

    @Override
    public TermsOfUseResult getTermsOfUse() {
        return serviceCall.get(String.format("%s/termsOfUse", SERVICE_URL),  authTokenContext.getAuthTokens(), TermsOfUseResult.class);
    }

    @Override
    public void acceptTermsOfUse(String version) {
        serviceCall.post(String.format("%s/termsOfUse/%s/accept", SERVICE_URL, version), null, authTokenContext.getAuthTokens(), Void.class);
    }

    @Override
    public void registerTermsOfUse(TermsOfUse termsOfUse) {
        serviceCall.post(String.format("%s/termsOfUse", SERVICE_URL), termsOfUse, authTokenContext.getAuthTokens(), Void.class);
    }

    @Override
    public JsonLdDoc updateClaimForRole(RoleMapping role, String space, Map<?, ?> claimPattern, boolean removeClaim) {
        return serviceCall.patch(String.format("%s/permissions/%s?remove=%b%s", SERVICE_URL, role, removeClaim, space!=null ? "&space="+space : ""), claimPattern, authTokenContext.getAuthTokens(), JsonLdDoc.class);
    }

    @Override
    public JsonLdDoc getClaimForRole(RoleMapping role, String space) {
        return serviceCall.get(String.format("%s/permissions/%s%s", SERVICE_URL, role, space!=null ? "&space="+space : ""), authTokenContext.getAuthTokens(), JsonLdDoc.class);
    }

    @Override
    public List<JsonLdDoc> getAllRoleDefinitions() {
        return Arrays.asList(serviceCall.get(String.format("%s/permissions", SERVICE_URL), authTokenContext.getAuthTokens(), JsonLdDoc[].class));
    }


    @Override
    public List<ReducedUserInformation> findUsers(String name) {
        return Arrays.asList(serviceCall.get(String.format("%s/usersFromIAM", SERVICE_URL), authTokenContext.getAuthTokens(), ReducedUserInformation[].class));
    }

}

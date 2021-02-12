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

package eu.ebrains.kg.commons.api.rest;

import eu.ebrains.kg.commons.AuthTokenContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.model.Credential;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.roles.Role;
import org.springframework.http.MediaType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

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
    public eu.ebrains.kg.commons.model.Client registerClient(eu.ebrains.kg.commons.model.Client client) {
        return serviceCall.put(String.format("%s/clients", SERVICE_URL), client, authTokenContext.getAuthTokens(), eu.ebrains.kg.commons.model.Client.class);
    }

    @Override
    public void unregisterClient(String clientName) {
        serviceCall.delete(String.format("%s/clients/%s", SERVICE_URL, clientName), authTokenContext.getAuthTokens(), eu.ebrains.kg.commons.model.Client.class);
    }

    @Override
    public ClientAuthToken fetchToken(String clientId, String clientSecret) {
        return serviceCall.post(String.format("%s/clients/%s/token", SERVICE_URL, clientId), clientSecret, authTokenContext.getAuthTokens(), ClientAuthToken.class);
    }

    @Override
    public List<User> getUsersInRole(String role) {
        return Arrays.asList(serviceCall.get(String.format("%s/roles/%s/users", SERVICE_URL, URLEncoder.encode(role, StandardCharsets.UTF_8)), authTokenContext.getAuthTokens(), User[].class));
    }

    @Override
    public void addUserToRole(String role, String nativeUserId) {
        serviceCall.put(String.format("%s/roles/%s/users/%s", SERVICE_URL, URLEncoder.encode(role, StandardCharsets.UTF_8), nativeUserId), null, authTokenContext.getAuthTokens(), Void.class);
    }

    @Override
    public void createRoles(List<Role> roles) {
        serviceCall.post(String.format("%s/roles", SERVICE_URL), roles, authTokenContext.getAuthTokens(), Void.class);
    }

    @Override
    public void removeRoles(String rolePattern) {
        serviceCall.delete(String.format("%s/roles/%s", SERVICE_URL, URLEncoder.encode(rolePattern, StandardCharsets.UTF_8)), authTokenContext.getAuthTokens(), Void.class);
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
    public UserWithRoles getRoles() {
        return null;
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
    public String setup(Credential credential) {
        return serviceCall.put(String.format("%s/setup", SERVICE_URL), credential, null, String.class);
    }
}

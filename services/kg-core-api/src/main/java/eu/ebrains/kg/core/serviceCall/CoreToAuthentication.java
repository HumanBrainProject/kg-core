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

package eu.ebrains.kg.core.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.model.Client;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.permission.roles.Role;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class CoreToAuthentication {
    private final ServiceCall serviceCall;
    private final AuthContext authContext;

    public static final String SPACE = "http://kg-authentication/internal/authentication";

    public CoreToAuthentication(ServiceCall serviceCall, AuthContext authContext) {
        this.serviceCall = serviceCall;
        this.authContext = authContext;
    }

    public User getMyUserProfile(){
        return serviceCall.get(String.format("%s/users/me", SPACE), authContext.getAuthTokens(), User.class);
    }

    public String endpoint() {
        return serviceCall.get(String.format("%s/users/authorization/endpoint", SPACE), MediaType.TEXT_PLAIN,  authContext.getAuthTokens(), String.class);
    }

    public String tokenEndpoint() {
        return serviceCall.get(String.format("%s/users/authorization/tokenEndpoint", SPACE), MediaType.TEXT_PLAIN,  authContext.getAuthTokens(), String.class);
    }

    public List<User> getUsersByAttribute(String attribute, String value){
        return Arrays.asList(serviceCall.get(String.format("%s/users/profiles/byAttribute/%s/%s", SPACE, attribute, value), authContext.getAuthTokens(), User[].class));
    }

    public void createRoles(List<Role> roles) {
        serviceCall.post(String.format("%s/roles", SPACE), roles, authContext.getAuthTokens(), Void.class);
    }

    public void removeRoles(String pattern) {
        serviceCall.delete(String.format("%s/roles/%s", SPACE, URLEncoder.encode(pattern, StandardCharsets.UTF_8)), authContext.getAuthTokens(), Void.class);
    }

    public Client registerClient(Client client) {
        return serviceCall.put(String.format("%s/clients", SPACE), client, authContext.getAuthTokens(), Client.class);
    }

    public Client unregisterClient(String clientName) {
        return serviceCall.delete(String.format("%s/clients/%s", SPACE, clientName), authContext.getAuthTokens(), Client.class);
    }

}

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

package eu.ebrains.kg.admin.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.model.Client;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.permission.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class AuthenticationSvcForAdmin {

    public static final String SPACE = "http://kg-authentication/";
    @Autowired
    ServiceCall serviceCall;

    @Autowired
    AuthContext authContext;

    public User getUser() {
        return serviceCall.get(SPACE + "users/me", authContext.getAuthTokens(), User.class);
    }

    public User getOtherUser(String nativeId) {
        return serviceCall.get(SPACE + String.format("users/profiles/%s", nativeId), authContext.getAuthTokens(), User.class);
    }

    public Client registerClient(Client client) {
        return serviceCall.put(String.format("%sclients", SPACE), client, authContext.getAuthTokens(), Client.class);
    }

    public Client unregisterClient(String clientName) {
        return serviceCall.delete(String.format("%sclients/%s", SPACE, clientName), authContext.getAuthTokens(), Client.class);
    }

    public void createRoles(List<Role> roles) {
        serviceCall.post(SPACE + "roles", roles, authContext.getAuthTokens(), Void.class);
    }

    public void removeRoles(String pattern) {
        serviceCall.delete(String.format("%sroles/%s", SPACE, URLEncoder.encode(pattern, StandardCharsets.UTF_8)), authContext.getAuthTokens(), Void.class);
    }

    public void addUserToRole(String role, String nativeUserId) {
        serviceCall.put(String.format("%sroles/%s/users/%s", SPACE, URLEncoder.encode(role, StandardCharsets.UTF_8), nativeUserId), null, authContext.getAuthTokens(), Void.class);
    }

    public List<User> getUsersInRole(String role) {
        return Arrays.asList(serviceCall.get(String.format("%sroles/%s/users", SPACE, URLEncoder.encode(role, StandardCharsets.UTF_8)), authContext.getAuthTokens(), User[].class));
    }



}

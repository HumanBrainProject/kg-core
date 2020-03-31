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
import eu.ebrains.kg.commons.model.User;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class Authentication4CoreSvc {
    private final ServiceCall serviceCall;
    private final AuthContext authContext;

    public Authentication4CoreSvc(ServiceCall serviceCall, AuthContext authContext) {
        this.serviceCall = serviceCall;
        this.authContext = authContext;
    }
//
//    public String login(String redirectUri) {
//        return serviceCall.get(String.format("http://kg-authentication/users/login?redirect_uri=%s", redirectUri), MediaType.TEXT_PLAIN, authContext.getAuthTokens(), String.class);
//    }

    public User getMyUserProfile(){
        return serviceCall.get("http://kg-authentication/users/me", authContext.getAuthTokens(), User.class);
    }

    public String endpoint() {
        return serviceCall.get("http://kg-authentication/users/authorization/endpoint", MediaType.TEXT_PLAIN,  authContext.getAuthTokens(), String.class);
    }

    public String tokenEndpoint() {
        return serviceCall.get("http://kg-authentication/users/authorization/tokenEndpoint", MediaType.TEXT_PLAIN,  authContext.getAuthTokens(), String.class);
    }

    public List<User> getUsersByAttribute(String attribute, String value){
        return Arrays.asList(serviceCall.get(String.format("http://kg-authentication/users/profiles/byAttribute/%s/%s", attribute, value), authContext.getAuthTokens(), User[].class));
    }
}

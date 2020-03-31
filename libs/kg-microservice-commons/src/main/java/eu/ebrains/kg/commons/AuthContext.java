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

package eu.ebrains.kg.commons;

import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.serviceCall.AuthenticationSvc;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AuthContext {

    private final AuthenticationSvc authenticationSvc;

    private UserWithRoles userWithRoles;

    public AuthContext(AuthenticationSvc authenticationSvc) {
        this.authenticationSvc = authenticationSvc;
    }

    private AuthTokens authTokens;

    public AuthTokens getAuthTokens() {
        return authTokens;
    }

    public void setAuthTokens(AuthTokens authTokens) {
        this.authTokens = authTokens;
    }

    public UserWithRoles getUserWithRoles() {
        if(userWithRoles==null){
            userWithRoles = authenticationSvc.getUserWithRoles();
        }
        return userWithRoles;
    }

    public Space getClientSpace(){
        return getUserWithRoles()!=null && getUserWithRoles().getClientId()!=null ? new Space(getUserWithRoles().getClientId()) : null;
    }

    public String getUserId(){
        UserWithRoles userWithRoles = getUserWithRoles();
        return userWithRoles == null || userWithRoles.getUser() ==null ? null : userWithRoles.getUser().getNativeId();
    }
}

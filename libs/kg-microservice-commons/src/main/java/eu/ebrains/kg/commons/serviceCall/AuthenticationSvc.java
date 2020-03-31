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

package eu.ebrains.kg.commons.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationSvc {

    public static final String SPACE = "http://kg-authentication/";
    @Autowired
    ServiceCall serviceCall;

    @Autowired
    AuthContext authContext;

    @Autowired
    IdUtils idUtils;

    public UserWithRoles getUserWithRoles() {
        return serviceCall.get(SPACE + "users/meWithRoles", authContext.getAuthTokens(), UserWithRoles.class);
    }

    public User getUserById(String userId) {
        return serviceCall.get(SPACE+ String.format("users/profiles/%s", userId), authContext.getAuthTokens(), User.class);
    }

}

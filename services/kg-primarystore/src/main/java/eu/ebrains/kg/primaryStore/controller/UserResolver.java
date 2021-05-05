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

package eu.ebrains.kg.primaryStore.controller;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import org.springframework.stereotype.Component;

@Component
public class UserResolver {

    private final Authentication.Client authentication;
    private final AuthContext authContext;

    public UserResolver(Authentication.Client authentication, AuthContext authContext) {
        this.authentication = authentication;
        this.authContext = authContext;
    }

    public User resolveUser(Event event){
         //The user information is only relevant for the native space -> for any later stage, it will be represented as part of the alternatives.
        if (event.getType().getStage() == DataStage.NATIVE) {
            if (event.getUserId() != null) {
                //The caller defines a user id - we try to resolve it and take the user information from the submitted one (happens e.g. if a technical client acts "in-behalf-of".
                User usr = authentication.getOtherUserInfo(event.getUserId());
                if (usr == null) {
                    usr = new User(String.format("unresolved-%s", event.getUserId()), String.format("Unresolved %s", event.getUserId()), null, null, null, event.getUserId());
                }
                return usr;
            } else {
                UserWithRoles userWithRoles = authContext.getUserWithRoles();
                if(userWithRoles!=null) {
                    return authContext.getUserWithRoles().getUser();
                }
            }
        }
        return null;
    }

}

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

package eu.ebrains.kg.primaryStore.controller;

import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
import org.springframework.stereotype.Component;

@Component
public class UserResolver {

    public final ToAuthentication authenticationSvc;

    public UserResolver(ToAuthentication authenticationSvc) {
        this.authenticationSvc = authenticationSvc;
    }

    public User resolveUser(Event event, UserWithRoles userWithRoles){
         //The user information is only relevant for the native space -> for any later stage, it will be represented as part of the alternatives.
        if (event.getType().getStage() == DataStage.NATIVE) {
            if (event.getUserId() != null) {
                //The caller defines a user id - we try to resolve it and take the user information from the submitted one (happens e.g. if a technical client acts "in-behalf-of".
                User usr = authenticationSvc.getUserById(event.getUserId());
                if (usr == null) {
                    usr = new User(String.format("unresolved-%s", event.getUserId()), String.format("Unresolved %s", event.getUserId()), null, null, null, event.getUserId());
                }
                return usr;
            } else if(userWithRoles!=null){
                return userWithRoles.getUser();
            }
        }
        return null;
    }

}

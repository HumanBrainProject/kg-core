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

package eu.ebrains.kg.commons.models;

import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A composite containing user information together with the assigned roles in the context of a client (originating from the authentication system)
 */
public class UserWithRoles {
    User user;
    List<String> permissions;
    String clientId;

    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    public UserWithRoles() {
    }

    public UserWithRoles(User user, List<String> userRoles, List<String> clientRoles, String clientId) {
        this.user = user;
        this.permissions = evaluatePermissions(userRoles, clientRoles);
        this.clientId = clientId;
    }

    /**
     * @return the id of the client, the reported roles apply to.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @return the user information
     */
    public User getUser() {
        return user;
    }

    /**
     * @return the list of functionalities, the user is allowed to execute
     */
    public List<FunctionalityInstance> getPermissions() {
        return permissions.stream().map(FunctionalityInstance::fromRoleName).collect(Collectors.toList());
    }

    /**
     * Evaluates the roles of a user in the context of a client by combining the roles of both
     *
     * @return the list of permissions applicable for the user using this client.
     */
    private List<String> evaluatePermissions(List<String> userRoleNames, List<String> clientRoleNames){
        if(userRoleNames==null || clientRoleNames == null){
            //We're lacking of authentication information -> we default to "no permissions"
            return Collections.emptyList();
        }
        List<FunctionalityInstance> userRoles = userRoleNames.stream().map(FunctionalityInstance::fromRoleName).filter(i -> i.getFunctionality() != null).collect(Collectors.toList());
        Map<Functionality, List<FunctionalityInstance>> clientRoles = clientRoleNames.stream().map(FunctionalityInstance::fromRoleName).filter(i -> i.getFunctionality() != null).collect(Collectors.groupingBy(FunctionalityInstance::getFunctionality));
        if(!clientRoles.containsKey(Functionality.IS_CLIENT)){
            throw new IllegalArgumentException("The Client-Authorization token you've passed doesn't belong to a client. This is not allowed!");
        }
        List<String> functionalities = new ArrayList<>();
        //Filter the user roles by the client permissions
        for (FunctionalityInstance userRole : userRoles) {
            Functionality functionality = userRole.getFunctionality();
            if(clientRoles.containsKey(functionality)){
                List<FunctionalityInstance> clientFunctionalities = clientRoles.get(functionality);
                FunctionalityInstance global = null;
                FunctionalityInstance space = null;
                FunctionalityInstance instance = null;
                for (FunctionalityInstance clientFunctionality : clientFunctionalities) {
                    if(clientFunctionality.getSpace()==null && clientFunctionality.getInstanceId()==null){
                        //This is the global functionality of the client - in any case, this is allowed
                        global = userRole;
                    }
                    if(clientFunctionality.getSpace()!=null && clientFunctionality.getInstanceId()==null){
                        //This is a space-limited functionality for this client...
                        if(userRole.getSpace() == null && userRole.getInstanceId()==null){
                            // ... the user has a global permission, so we restrict it to the client space
                            space = clientFunctionality;
                        }
                        if(userRole.getSpace()!=null && userRole.getSpace().equals(clientFunctionality.getSpace())){
                            //... the user has a permission for the space so we can provide access to it.
                            space = clientFunctionality;
                        }
                    }
                    if(clientFunctionality.getSpace()!=null && clientFunctionality.getInstanceId()!=null){
                        if(userRole.getSpace() == null && userRole.getInstanceId()==null){
                            // ... the user has a global permission, so we restrict it to the instance of the client
                            instance = clientFunctionality;
                        }
                        if(userRole.getSpace()!=null && userRole.getInstanceId()==null && userRole.getSpace().equals(clientFunctionality.getSpace())){
                            //... the user has a permission for the space so we can provide access to the instance
                            instance = clientFunctionality;
                        }
                        if(userRole.getSpace()!=null && userRole.getInstanceId()!=null && userRole.getSpace().equals(clientFunctionality.getSpace()) && userRole.getInstanceId().equals(clientFunctionality.getInstanceId())){
                            //... the user has a permission for the same instance
                            instance = clientFunctionality;
                        }
                    }
                }
                if(global!=null){
                    functionalities.add(global.getRoleName());
                }
                else if(space!=null){
                    functionalities.add(space.getRoleName());
                }
                else if(instance!=null){
                    functionalities.add(instance.getRoleName());
                }
            }
        }
        logger.trace(String.format("Available roles for user %s in client %s: %s", user!=null ? user.getUserName() : "anonymous", clientId, String.join(", ", functionalities)));
        return functionalities;
    }
}

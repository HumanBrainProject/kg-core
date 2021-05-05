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

package eu.ebrains.kg.commons.models;

import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A composite containing user information together with the assigned roles in the context of a client (originating from the authentication system)
 */
public class UserWithRoles {
    private User user;
    private List<String> clientRoles;
    private List<String> userRoles;
    private String clientId;
    private boolean serviceAccount;
    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    private final static String CLIENT_ROLE_NAME = RoleMapping.IS_CLIENT.toRole(null).getName();

    // For serialization
    @SuppressWarnings("unused")
    private UserWithRoles() {
    }

    public UserWithRoles(User user, List<String> userRoles, List<String> clientRoles, String clientId) {
        this.user = user;
        this.userRoles = userRoles;
        this.clientRoles = clientRoles;
        this.clientId = clientId;
        this.serviceAccount = userRoles.contains(CLIENT_ROLE_NAME);
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
        return evaluatePermissions(userRoles, clientRoles);
    }

    public boolean isServiceAccount() {
        return serviceAccount;
    }

    public SpaceName getPrivateSpace(){
        return new SpaceName("private-"+user.getNativeId());
    }

    /**
     * Evaluates the roles of a user in the context of a client by combining the roles of both
     *
     * @return the list of permissions applicable for the user using this client.
     */
    List<FunctionalityInstance> evaluatePermissions(List<String> userRoleNames, List<String> clientRoleNames) {
        if (userRoleNames == null) {
            //We're lacking of authentication information -> we default to "no permissions"
            return Collections.emptyList();
        }
        //Add an implicit role for the user private space.
        String privateSpaceRole = RoleMapping.OWNER.toRole(getPrivateSpace()).getName();
        List<FunctionalityInstance> userFunctionalities = Stream.concat(Stream.of(privateSpaceRole), userRoleNames.stream()).map(RoleMapping::fromRole).flatMap(Collection::stream).distinct().collect(Collectors.toList());
        List<FunctionalityInstance> result = new ArrayList<>();
        if(clientRoleNames != null) {
            Map<Functionality, List<FunctionalityInstance>> clientFunctionalities = clientRoleNames.stream().map(RoleMapping::fromRole).flatMap(Collection::stream).distinct().collect(Collectors.groupingBy(FunctionalityInstance::getFunctionality));
            if (!clientRoleNames.contains(CLIENT_ROLE_NAME)) {
                throw new IllegalArgumentException("The client authorization credentials you've passed doesn't belong to a service account. This is not allowed!");
            }
            //Filter the user roles by the client permissions (only those user permissions are guaranteed which are also allowed by the client)
            for (FunctionalityInstance userRole : userFunctionalities) {
                Functionality functionality = userRole.getFunctionality();
                if (clientFunctionalities.containsKey(functionality)) {
                    FunctionalityInstance global = null;
                    FunctionalityInstance space = null;
                    FunctionalityInstance instance = null;
                    for (FunctionalityInstance clientFunctionality : clientFunctionalities.get(functionality)) {
                        if (clientFunctionality.getSpace() == null && clientFunctionality.getInstanceId() == null) {
                            //The client provides this functionality on a global permission layer, so this is ok.
                            global = userRole;
                        }
                        if (clientFunctionality.getSpace() != null && clientFunctionality.getInstanceId() == null) {
                            //This is a space-limited functionality for this client...
                            if (userRole.getSpace() == null && userRole.getInstanceId() == null) {
                                // ... the user has a global permission, so we restrict it to the client space
                                space = clientFunctionality;
                            }
                            if (userRole.getSpace() != null && userRole.getSpace().equals(clientFunctionality.getSpace())) {
                                //... the user has a permission for the space so we can provide access to it.
                                space = clientFunctionality;
                            }
                        }
                        if (clientFunctionality.getSpace() != null && clientFunctionality.getInstanceId() != null) {
                            //This is an instance-limited functionality for this client...
                            if (userRole.getSpace() == null && userRole.getInstanceId() == null) {
                                // ... the user has a global permission, so we restrict it to the instance of the client
                                instance = clientFunctionality;
                            }
                            if (userRole.getSpace() != null && userRole.getInstanceId() == null && userRole.getSpace().equals(clientFunctionality.getSpace())) {
                                //... the user has a permission for the space so we restrict it to the instance of the client
                                instance = clientFunctionality;
                            }
                            if (userRole.getSpace() != null && userRole.getInstanceId() != null && userRole.getSpace().equals(clientFunctionality.getSpace()) && userRole.getInstanceId().equals(clientFunctionality.getInstanceId())) {
                                //... the user has a permission for the same instance
                                instance = clientFunctionality;
                            }
                        }
                    }
                    if (global != null) {
                        result.add(global);
                    } else if (space != null) {
                        result.add(space);
                    } else if (instance != null) {
                        result.add(instance);
                    }
                }
            }
        }
        else{
           return userFunctionalities;
        }
        //TODO reduce the result list by removing subpermissions if there are existing in upper hierarchies.
        logger.trace(String.format("Available roles for user %s in client %s: %s", user != null ? user.getUserName() : "anonymous", clientId, String.join(", ", result.stream().map(Object::toString).collect(Collectors.toSet()))));
        return result;
    }
}

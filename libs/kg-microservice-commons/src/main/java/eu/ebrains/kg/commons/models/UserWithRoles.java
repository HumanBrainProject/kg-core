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
import org.springframework.util.CollectionUtils;

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
    private List<UUID> invitations;
    private String clientId;
    private List<FunctionalityInstance> permissions;
    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    // For serialization
    @SuppressWarnings("unused")
    private UserWithRoles() {
    }

    public UserWithRoles(User user, List<String> userRoles, List<String> clientRoles, String clientId){
        this(user, userRoles, clientRoles, Collections.emptyList(), clientId);
    }

    public UserWithRoles(User user, List<String> userRoles, List<String> clientRoles, List<UUID> invitations, String clientId) {
        this.user = user;
        this.userRoles = userRoles == null ? null : Collections.unmodifiableList(userRoles);
        this.clientRoles = clientRoles == null ? null : Collections.unmodifiableList(clientRoles);
        this.clientId = clientId;
        this.invitations = invitations == null ? null : Collections.unmodifiableList(invitations);
        this.permissions = calculatePermissions();
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
        return permissions;
    }

    private List<FunctionalityInstance> calculatePermissions(){
        //Invitation permissions are added after permission evaluation (of global and space)
        final List<FunctionalityInstance> functionalityInstances = evaluatePermissions(userRoles, clientRoles);
        if(!CollectionUtils.isEmpty(invitations)){
            return Stream.concat(functionalityInstances.stream(),
                    invitations.stream().map(i -> new FunctionalityInstance(Functionality.READ, null, i)))
                    .distinct().collect(Collectors.toList());
        }
        else {
            return functionalityInstances;
        }
    }

    public SpaceName getPrivateSpace(){
        return new SpaceName("private-"+user.getNativeId());
    }

    private List<FunctionalityInstance> reduceFunctionalities(Map<Functionality, List<FunctionalityInstance>> functionalityMap){
        List<FunctionalityInstance> reducedList = new ArrayList<>();
        for (Functionality functionality : functionalityMap.keySet()) {
            Optional<FunctionalityInstance> globalFunctionality = functionalityMap.get(functionality).stream().filter(i -> i.getId() == null && i.getSpace() == null).findAny();
            if(globalFunctionality.isPresent()){
                //We have a global permission -> we don't need any other
                reducedList.add(globalFunctionality.get());
            }
            else{
                Map<SpaceName, List<FunctionalityInstance>> functionalityBySpace = functionalityMap.get(functionality).stream().collect(Collectors.groupingBy(FunctionalityInstance::getSpace));
                functionalityBySpace.forEach((s, spaceInstances) -> {
                    Optional<FunctionalityInstance> spacePermission = spaceInstances.stream().filter(i -> i.getId() == null).findAny();
                    if(spacePermission.isPresent()){
                        //We have a space permission for this functionality -> instance level permissions are not needed
                        reducedList.add(spacePermission.get());
                    }
                    else{
                        //We neither have a global nor a space permission -> we need all instance permissions in the list
                        reducedList.addAll(spaceInstances);
                    }
                });
            }
        }
        return reducedList;
    }

    /**
     * For a few cases (e.g. reading a query), it is sufficient if the user OR the client has the permissions.
     *
     * @return the list of permissions applicable for the user using this client.
     */
    List<FunctionalityInstance> evaluatePermissionCombinations(List<String> userRoleNames, List<String> clientRoleNames) {
        Stream<String> rolesStream;
        if(userRoleNames!=null && clientRoleNames!=null){
            rolesStream = Stream.concat(userRoleNames.stream(), clientRoleNames.stream());
        }
        else if(userRoleNames!=null){
            rolesStream = userRoleNames.stream();
        }
        else if(clientRoleNames!=null){
            rolesStream = clientRoleNames.stream();
        }
        else{
            return Collections.emptyList();
        }
        return reduceFunctionalities(rolesStream.map(RoleMapping::fromRole).flatMap(Collection::stream).distinct().collect(Collectors.groupingBy(FunctionalityInstance::getFunctionality)));
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
        List<FunctionalityInstance> userFunctionalities = reduceFunctionalities(userRoleNames.stream().map(RoleMapping::fromRole).flatMap(Collection::stream).distinct().collect(Collectors.groupingBy(FunctionalityInstance::getFunctionality)));
        Set<FunctionalityInstance> result = new HashSet<>();
        if(clientRoleNames != null) {
            Map<Functionality, List<FunctionalityInstance>> clientFunctionalities = clientRoleNames.stream().map(RoleMapping::fromRole).flatMap(Collection::stream).distinct().collect(Collectors.groupingBy(FunctionalityInstance::getFunctionality));
            //Filter the user roles by the client permissions (only those user permissions are guaranteed which are also allowed by the client)
            for (FunctionalityInstance userRole : userFunctionalities) {
                Functionality functionality = userRole.getFunctionality();
                if (clientFunctionalities.containsKey(functionality)) {
                    for (FunctionalityInstance clientFunctionality : clientFunctionalities.get(functionality)) {
                        FunctionalityInstance global = null;
                        FunctionalityInstance space = null;
                        FunctionalityInstance instance = null;
                        if (clientFunctionality.getSpace() == null && clientFunctionality.getId() == null) {
                            //The client provides this functionality on a global permission layer, so the user role can be accepted
                            global = userRole;
                        }
                        if (clientFunctionality.getSpace() != null && clientFunctionality.getId() == null) {
                            //This is a space-limited functionality for this client...
                            if (userRole.getSpace() == null && userRole.getInstanceId() == null) {
                                // ... the user has a global permission, so we restrict it to the client space
                                space = clientFunctionality;
                            }
                            if (userRole.getSpace() != null  && userRole.getSpace().equals(clientFunctionality.getSpace())) {
                                //... the client has permission for the space so we can provide access to the user role
                                // (regardless if it is a space or instance permission since both are valid)
                                space = userRole;
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
                                instance = userRole;
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
        }
        else{
           return userFunctionalities;
        }
        logger.trace(String.format("Available roles for user %s in client %s: %s", user != null ? user.getUserName() : "anonymous", clientId, String.join(", ", result.stream().map(Object::toString).collect(Collectors.toSet()))));
        return new ArrayList<>(result);
    }

    public List<UUID> getInvitations(){
        return invitations;
    }

    public boolean hasInvitations(){
        return this.invitations!=null && !this.invitations.isEmpty();
    }

    public final static UserWithRoles INTERNAL_ADMIN = createInternalAdminUser();

    private static UserWithRoles createInternalAdminUser(){
        User user = new User("kgInternalAdmin", "KG Internal admin user", "kg@ebrains.eu", "KG Internal", "Admin", "kgInternalAdmin");
        return new UserWithRoles(user, Collections.singletonList(RoleMapping.ADMIN.toRole(null).getName()), null, null);
    }


}

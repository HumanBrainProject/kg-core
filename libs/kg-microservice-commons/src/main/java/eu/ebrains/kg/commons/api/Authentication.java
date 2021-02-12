/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.commons.api;

import eu.ebrains.kg.commons.model.Credential;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.roles.Role;

import java.util.List;

public interface Authentication {

    interface Client extends Authentication {}

    eu.ebrains.kg.commons.model.Client registerClient(eu.ebrains.kg.commons.model.Client client);

    void unregisterClient(String clientName);

    ClientAuthToken fetchToken(String clientId, String clientSecret);

    List<User> getUsersInRole(String role);

    void addUserToRole(String role, String nativeUserId);

    void createRoles(List<Role> roles);

    void removeRoles(String rolePattern);

    String authEndpoint();

    String tokenEndpoint();

    User getMyUserInfo();

    UserWithRoles getRoles();

    User getOtherUserInfo(String nativeId);

    List<User> getUsersByAttribute(String attribute, String value);

    String setup(Credential credential);
}

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

package eu.ebrains.kg.authentication.keycloak;

import eu.ebrains.kg.commons.model.ReducedUserInformation;
import eu.ebrains.kg.commons.model.User;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class KeycloakUsers {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final KeycloakClient keycloakClient;
    private final Keycloak keycloakAdmin;

    public KeycloakUsers(KeycloakClient keycloakClient, Keycloak keycloakAdmin) {
        this.keycloakClient = keycloakClient;
        this.keycloakAdmin = keycloakAdmin;
    }


    private boolean matchesFirstOrLastName(String firstName, String lastName, String[] words){
        for(String w : words){
            if(!firstName.toLowerCase().contains(w.toLowerCase()) && !lastName.toLowerCase().contains(w.toLowerCase())){
                return false;
            }
        }
        return true;
    }

    public List<ReducedUserInformation> findUser(String search){
        final List<UserRepresentation> result = getUsers().search(search, 0, 20);
        final String[] words = search.split(" ");
        // We remove all elements where there are no matches in first or last name to prevent brute-force extraction of e-mail addresses.
        // In theory, we could end up with a problem of pagination (due to the post-removal) but this should not occur for real values - we therefore take the risk
        return result.stream().filter(r -> matchesFirstOrLastName(r.getFirstName(), r.getLastName(), words)).
                map(r -> new ReducedUserInformation(String.format("%s %s", r.getFirstName(), r.getLastName()), r.getUsername(), null, r.getId())).
                collect(Collectors.toList());
    }



    @Cacheable(value = "userInfo")
    public User getOtherUserInfo(String id) {
        UserRepresentation userRepresentation;
        try {
            UUID uuid = UUID.fromString(id);
            logger.trace(String.format("Found uuid: %s", uuid.toString()));
            userRepresentation = getUsers().get(id).toRepresentation();
        } catch (IllegalArgumentException e) {
            logger.trace("Resolving by user name");
            List<UserRepresentation> users =  getUsers().search(id);
            //Exact fit of username...
            userRepresentation = users.stream().filter(u -> u.getUsername().equals(id)).findFirst().orElse(null);
        }
        if (userRepresentation != null) {
            return new User(userRepresentation.getUsername(), userRepresentation.getFirstName() + " " + userRepresentation.getLastName(),
                    //We explicitly do not share the email address if requested. The name should be sufficient to identify the person.
                    null
                    , userRepresentation.getFirstName(), userRepresentation.getLastName(), userRepresentation.getId());
        }
        return null;
    }

    private UsersResource getUsers(){
        return keycloakAdmin.realm(keycloakClient.getRealm()).users();
    }

    @Cacheable(value = "allUsers")
    public List<UserRepresentation>  getAllUsers(){
        UsersResource users = getUsers();
        Integer numberOfUsers = users.count();
        int pages = numberOfUsers / 100 + 1;
        int currentPage = 0;
        List<UserRepresentation> allUserRepresentations = new ArrayList<>();
        while (currentPage < pages) {
            allUserRepresentations.addAll(users.list(currentPage * 100, 100));
            currentPage++;
        }
        return allUserRepresentations;
    }

    @CacheEvict(allEntries = true, cacheNames = "allUsers")
    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000)
    public void clearUsersCache(){

    }
}

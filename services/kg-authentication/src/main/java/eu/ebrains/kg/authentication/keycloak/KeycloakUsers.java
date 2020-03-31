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

package eu.ebrains.kg.authentication.keycloak;

import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeycloakUsers {

    private final KeycloakClient keycloakClient;

    public KeycloakUsers(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    @Cacheable(value = "allUsers")
    public List<UserRepresentation>  getAllUsers(){
        UsersResource users = keycloakClient.getRealmResource().users();
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

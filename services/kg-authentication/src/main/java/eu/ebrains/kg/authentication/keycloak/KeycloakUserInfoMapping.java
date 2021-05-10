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

import eu.ebrains.kg.authentication.model.ClientRoleMapping;
import eu.ebrains.kg.commons.JsonAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeycloakUserInfoMapping {

    private final JsonAdapter jsonAdapter;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public KeycloakUserInfoMapping(JsonAdapter jsonAdapter) {
        this.jsonAdapter = jsonAdapter;
    }

    public final static class UserInfoMapping extends LinkedHashMap<String, LinkedHashMap<String, List<ClientRoleMapping>>> {
    }

    @Cacheable("userInfoMappings")
    public UserInfoMapping getMappings() {
        Resource resource = new ClassPathResource("userInfoMapping.json");
        try{
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String content = reader.lines().collect(Collectors.joining("\n"));
                return jsonAdapter.fromJson(content, UserInfoMapping.class);
            }
        }
        catch (IOException e){
            throw new RuntimeException("Was not able to read the userInfoMapping");
        }
    }


    public List<String> mapRoleNames(Map<String, Object> userInfo, UserInfoMapping mappings) {
        Set<String> roleNames = new HashSet<>();
        mappings.forEach((rolesKey, rolesValue) -> {
            if (userInfo.containsKey(rolesKey)) {
                Object roles = userInfo.get(rolesKey);
                if (roles instanceof Map) {
                    Map<?, ?> rolesMap = (Map<?, ?>) roles;
                    rolesValue.forEach((clientKey, clientValue) -> {
                        if (rolesMap.containsKey(clientKey)) {
                            Object userRolesByKey = rolesMap.get(clientKey);
                            if (userRolesByKey instanceof List) {
                                Set<String> processedRoleNames = ((List<?>) userRolesByKey).stream().filter(r -> r instanceof String).map(r -> (String) r).map(userRole -> {
                                    for (ClientRoleMapping clientRoleMapping : clientValue) {
                                        if (userRole.matches(clientRoleMapping.getFrom())) {
                                            return userRole.replaceAll(clientRoleMapping.getFrom(), clientRoleMapping.getTo());
                                        }
                                    }
                                    return userRole;
                                }).collect(Collectors.toSet());
                                roleNames.addAll(processedRoleNames);
                            }
                        }
                    });
                }
            }
        });
        return new ArrayList<>(roleNames);
    }


}

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

package eu.ebrains.kg;

import eu.ebrains.kg.authentication.keycloak.KeycloakConfig;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableEurekaClient
@SpringBootApplication
@Configuration
@EnableCaching
public class KgAuthenticationApplication {

    public static void main(String[] args) {
        SpringApplication.run(KgAuthenticationApplication.class, args);
    }

    @Bean
    public Keycloak createKeycloakAdmin(KeycloakConfig config) {
        return KeycloakBuilder.builder().username(config.getUser()).password(config.getPwd()).serverUrl(config.getServerUrl()).realm("master").clientId("admin-cli").build();
    }
}

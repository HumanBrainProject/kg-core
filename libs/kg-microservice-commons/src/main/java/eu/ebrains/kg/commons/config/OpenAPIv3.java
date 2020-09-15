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

package eu.ebrains.kg.commons.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenAPIv3 {

    @Bean
    public OpenAPI customOpenAPI(@Value("${spring.application.name}") String applicationName, @Value("${eu.ebrains.kg.login.endpoint}") String loginEndpoint, @Value("${eu.ebrains.kg.api.basePath}") String basePath, @Value("${eu.ebrains.kg.api.versioned}") boolean versioned) {
        SecurityScheme clientId = new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-Id");
        SecurityScheme clientSecret = new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-Secret");
        SecurityScheme clientServiceAccountSecret = new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-ServiceAccount-Secret");
        SecurityRequirement clientSecretReq = new SecurityRequirement().addList("clientId").addList("clientSecret").addList("userToken").addList("clientServiceAccountSecret");

        SecurityScheme clientToken = new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-Authorization");
        SecurityRequirement clientTokenReq = new SecurityRequirement().addList("clientToken").addList("userToken");
        SecurityScheme userToken = new SecurityScheme().type(SecurityScheme.Type.OAUTH2).flows(new OAuthFlows().implicit(new OAuthFlow().authorizationUrl(loginEndpoint)));

        return new OpenAPI().components(new Components())
                .schemaRequirement("clientId", clientId).schemaRequirement("clientSecret", clientSecret).schemaRequirement("clientServiceAccountSecret", clientServiceAccountSecret).schemaRequirement("clientToken", clientToken).schemaRequirement("userToken", userToken)
                .security(Arrays.asList(clientSecretReq, clientTokenReq));
    }
}

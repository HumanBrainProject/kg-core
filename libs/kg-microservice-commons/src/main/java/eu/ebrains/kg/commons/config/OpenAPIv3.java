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

import eu.ebrains.kg.commons.Version;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenAPIv3 {

    @Bean
    public GroupedOpenApi coreApi() {
        return GroupedOpenApi.builder()
                .group("core")
                .packagesToScan("eu.ebrains.kg.core")
                .pathsToExclude("/*/extra/**").addOpenApiCustomiser(new OpenApiCustomiser() {
                    @Override
                    public void customise(OpenAPI openApi) {
                        System.out.println(openApi);
                    }
                })
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = "eu.ebrains.kg.api.doc.hideInternal", havingValue = "false", matchIfMissing = true)
    public GroupedOpenApi internalApi() {
        return GroupedOpenApi.builder()
                .group("internal")
                .packagesToScan("eu.ebrains.kg")
                .pathsToExclude("eu.ebrains.kg.core")
                .pathsToExclude("/*/extra/**")
                .build();
    }

    @Bean
    public GroupedOpenApi extraApi() {
        return GroupedOpenApi.builder()
                .group("xtra")
                .packagesToScan("eu.ebrains.kg")
                .pathsToMatch("/*/extra/**")
                .build();
    }


    @Bean
    public OpenAPI customOpenAPI(@Value("${spring.application.name}") String applicationName, @Value("${eu.ebrains.kg.login.endpoint}") String loginEndpoint, @Value("${eu.ebrains.kg.api.basePath}") String basePath, @Value("${eu.ebrains.kg.api.versioned}") boolean versioned, @Value("${eu.ebrains.kg.server}") String server) {
        SecurityScheme clientId = new SecurityScheme().name("Client ID").type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-Id").description("The client-id for the proxied client-authentication. To be provided with \"client-secret\" and either \"client-serviceAccount-secret\" or the \"user-token\"");
        SecurityScheme clientSecret = new SecurityScheme().name("clientSecret").type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-Secret").description("The client-secret for the proxied client-authentication. To be provided with \"client-id\" and either \"client-serviceAccount-secret\" or the \"user-token\"");
        SecurityScheme clientServiceAccountSecret = new SecurityScheme().name("clientServiceAccountSecret").type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-ServiceAccount-Secret").description("Provide the client-secret a second time to authenticate as the service account with the full authentication mechanisms. To be provided with \"client-id\" and \"client-secret\"");
        SecurityRequirement clientSecretSaReq = new SecurityRequirement().addList("clientId").addList("clientSecret").addList("clientServiceAccountSecret");
        SecurityRequirement clientSecretUserReq = new SecurityRequirement().addList("clientId").addList("clientSecret").addList("userToken");
        SecurityScheme clientToken = new SecurityScheme().name("clientToken").type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-Authorization").description("The already resolved token for the client account. This is the recommended way of authenticating clients since you don't expose your static credentials to the KG core but handle it on the client side.");
        SecurityRequirement clientTokenReq = new SecurityRequirement().addList("clientToken").addList("userToken");
        OAuthFlow oAuthFlow = new OAuthFlow();
        oAuthFlow.authorizationUrl(loginEndpoint);
        SecurityScheme userToken = new SecurityScheme().name("userToken").type(SecurityScheme.Type.OAUTH2).flows(new OAuthFlows().implicit(oAuthFlow)).description("The browser-based user authentication.");
        OpenAPI openapi = new OpenAPI().openapi("3.0.3");
        return openapi.info(new Info().version(Version.API).title(String.format("This is the %s API", applicationName)).license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")).termsOfService("https://kg.ebrains.eu/search-terms-of-use.html")).components(new Components()).schemaRequirement("clientId", clientId).schemaRequirement("clientSecret", clientSecret).schemaRequirement("clientServiceAccountSecret", clientServiceAccountSecret).schemaRequirement("clientToken", clientToken).schemaRequirement("userToken", userToken)
                .security(Arrays.asList(clientTokenReq, clientSecretUserReq, clientSecretSaReq));
    }
}

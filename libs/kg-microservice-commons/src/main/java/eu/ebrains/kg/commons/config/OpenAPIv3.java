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

package eu.ebrains.kg.commons.config;

import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.config.openApiGroups.Advanced;
import eu.ebrains.kg.commons.config.openApiGroups.Simple;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class OpenAPIv3 {


    private static String[] getPathsByAnnotation(RequestMappingHandlerMapping requestHandlerMapping, String restrictToPackage, Class<? extends Annotation>... groupAnnotations){
        List<String> paths = new ArrayList<>();
        String[] pathsArr = {};
        requestHandlerMapping.getHandlerMethods()
                .forEach((key, value) -> {
                    if(restrictToPackage == null || value.getMethod().getDeclaringClass().getPackageName().startsWith(restrictToPackage)) {
                        boolean containsAnnotation = Arrays.stream(groupAnnotations).anyMatch(g -> AnnotationUtils.findAnnotation(value.getMethod(), g) != null || AnnotationUtils.findAnnotation(value.getMethod().getDeclaringClass(), g) != null);
                        if (containsAnnotation) {
                            paths.add(key.getPatternsCondition().getPatterns().iterator().next());
                        }
                    }
                });
       return paths.toArray(pathsArr);
    }

    @Bean
    public GroupedOpenApi simpleApi(RequestMappingHandlerMapping requestHandlerMapping) {
        return GroupedOpenApi.builder()
                .group("0 simple")
                .pathsToMatch(getPathsByAnnotation(requestHandlerMapping, "eu.ebrains.kg.core", Simple.class))
                .build();
    }

    @Bean
    public GroupedOpenApi advancedApi(RequestMappingHandlerMapping requestHandlerMapping) {
        return GroupedOpenApi.builder()
                .group("1 advanced")
                .pathsToMatch(getPathsByAnnotation(requestHandlerMapping, "eu.ebrains.kg.core", Simple.class, Advanced.class))
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi(RequestMappingHandlerMapping requestHandlerMapping) {
        return GroupedOpenApi.builder()
                .group("2 admin")
                .pathsToMatch(getPathsByAnnotation(requestHandlerMapping, "eu.ebrains.kg.core", Admin.class))
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("3 all")
                .packagesToScan("eu.ebrains.kg.core")
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = "eu.ebrains.kg.api.doc.hideInternal", havingValue = "false", matchIfMissing = true)
    public GroupedOpenApi internalApi() {
        return GroupedOpenApi.builder()
                .group("4 internal")
                .packagesToScan("eu.ebrains.kg")
                .packagesToExclude("eu.ebrains.kg.core")
                .build();
    }


    @Bean
    public OpenAPI customOpenAPI(@Value("${spring.application.name}") String applicationName, @Value("${eu.ebrains.kg.login.endpoint}") String loginEndpoint, @Value("${eu.ebrains.kg.api.basePath}") String basePath, @Value("${eu.ebrains.kg.api.versioned}") boolean versioned, @Value("${eu.ebrains.kg.server}") String server) {
        SecurityScheme clientId = new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-Id").description("The client-id for the proxied client-authentication. To be provided with \"Client-SA-Secret\" or the combination of \"Client-Secret\" and \"Authorization\"");
        SecurityScheme clientSecret = new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-Secret").description("The client-secret for the proxied client-authentication. To be provided with \"Client-Id\" and \"Authorization\"");
        SecurityScheme serviceAccountSecret = new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-SA-Secret").description("Provide the client-secret in this header to authenticate as the service account. To be provided with \"Client-Id\"");

        SecurityScheme clientToken = new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-Authorization").description("The bearer token for the service account to contextualize the authentication to this client. Although convenience mechanisms for authenticating clients directly with client-id and client-secret exist (see technical documentation), this is the recommended way of integration since you don't expose any credentials but the short-lived access-tokens to the KG core.");

        OAuthFlow oAuthFlow = new OAuthFlow();
        oAuthFlow.authorizationUrl(loginEndpoint);
        SecurityScheme userToken = new SecurityScheme().name("Authorization").type(SecurityScheme.Type.OAUTH2).flows(new OAuthFlows().implicit(oAuthFlow)).description("The user authentication");

        SecurityRequirement userWithoutClientReq = new SecurityRequirement().addList("Authorization");
        SecurityRequirement userWithClientByToken = new SecurityRequirement().addList("Client-Authorization").addList("Authorization");
        SecurityRequirement userWithClientByClientSecret = new SecurityRequirement().addList("Client-Id").addList("Client-Secret").addList("Authorization");
        SecurityRequirement serviceAccountByClientSecret = new SecurityRequirement().addList("Client-Id").addList("Client-SA-Secret");

        OpenAPI openapi = new OpenAPI().openapi("3.0.3");
        return openapi.info(new Info().version(Version.API).title(String.format("This is the %s API", applicationName)).license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")).termsOfService("https://kg.ebrains.eu/search-terms-of-use.html"))
                .components(new Components()).schemaRequirement("Authorization", userToken).schemaRequirement("Client-Authorization", clientToken)
                .security(Arrays.asList(userWithoutClientReq, userWithClientByToken,  userWithClientByClientSecret, serviceAccountByClientSecret));
    }
}

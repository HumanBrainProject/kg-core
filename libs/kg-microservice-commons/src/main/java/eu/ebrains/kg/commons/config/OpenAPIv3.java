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

package eu.ebrains.kg.commons.config;

import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.config.openApiGroups.AnonymousAccess;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class OpenAPIv3 {

    private static final String PREVIOUS_VERSION = "previous";
    private static final String CURRENT_VERSION = "current";
    private static final String UPCOMING_VERSION = "upcoming";


    private static final List<SecurityRequirement> SECURITY_REQUIREMENTS = createAuthenticationRequirements();

    private static List<SecurityRequirement> createAuthenticationRequirements(){
        SecurityRequirement userWithoutClientReq = new SecurityRequirement().addList("Authorization");
        SecurityRequirement userWithClientByToken = new SecurityRequirement().addList("Client-Authorization").addList("Authorization");
        SecurityRequirement userWithClientByClientSecret = new SecurityRequirement().addList("Client-Id").addList("Client-Secret").addList("Authorization");
        SecurityRequirement serviceAccountByClientSecret = new SecurityRequirement().addList("Client-Id").addList("Client-SA-Secret");
        return Arrays.asList(userWithoutClientReq, userWithClientByToken, userWithClientByClientSecret, serviceAccountByClientSecret);
    }


    private static String[] getPathsByAnnotation(RequestMappingHandlerMapping requestHandlerMapping, Class<? extends Annotation> includeAnnotation, Class<? extends Annotation> excludeAnnotation) {
        List<String> paths = new ArrayList<>();
        String[] pathsArr = {};
        requestHandlerMapping.getHandlerMethods()
                .forEach((key, value) -> {
                    boolean containsAnnotation = includeAnnotation == null || AnnotationUtils.findAnnotation(value.getMethod(), includeAnnotation) != null || AnnotationUtils.findAnnotation(value.getMethod().getDeclaringClass(), includeAnnotation) != null;
                    boolean excluded = excludeAnnotation != null && (AnnotationUtils.findAnnotation(value.getMethod(), excludeAnnotation) != null || AnnotationUtils.findAnnotation(value.getMethod().getDeclaringClass(), excludeAnnotation) != null);
                    if (containsAnnotation && !excluded) {
                        paths.add(key.getPatternsCondition().getPatterns().iterator().next());
                    }
                });
        return paths.toArray(pathsArr);
    }

    private final static String CURRENT_PACKAGE = "eu.ebrains.kg.core.api.current";

    private final static String TITLE_TEMPLATE = "This is the %s EBRAINS KG API (%s)%s";



    private final static class Customizer implements OperationCustomizer {
        @Override
        public Operation customize(Operation operation, HandlerMethod handlerMethod) {
            if (AnnotationUtils.findAnnotation(handlerMethod.getMethod(), AnonymousAccess.class) == null) {
                operation.setSecurity(SECURITY_REQUIREMENTS);
            }
            return operation;
        }
    }

    @Bean
    public GroupedOpenApi current(RequestMappingHandlerMapping requestHandlerMapping) {
        return GroupedOpenApi.builder()
                .addOperationCustomizer(new Customizer())
                .group(CURRENT_VERSION)
                .addOpenApiCustomiser(openApi -> openApi.getInfo().title(String.format(TITLE_TEMPLATE, CURRENT_VERSION, Version.CURRENT, StringUtils.EMPTY)).version(Version.CURRENT))
                .packagesToScan(CURRENT_PACKAGE)
                .pathsToMatch(getPathsByAnnotation(requestHandlerMapping, null, Admin.class))
                .build();
    }


    @Bean
    public GroupedOpenApi currentAdmin(RequestMappingHandlerMapping requestHandlerMapping) {
        return GroupedOpenApi.builder()
                .addOperationCustomizer(new Customizer())
                .group(String.format("· %s (admin)", CURRENT_VERSION))
                .addOpenApiCustomiser(openApi -> openApi.getInfo().title(String.format(TITLE_TEMPLATE, CURRENT_VERSION, Version.CURRENT, " for administrators")).version(Version.CURRENT)
                        .description("This is the admin API containing endpoints to manage the KG. Note that the endpoints shown require you to authenticate with a user having administrator rights."))
                .packagesToScan(CURRENT_PACKAGE)
                .pathsToMatch(getPathsByAnnotation(requestHandlerMapping, Admin.class, null))
                .build();
    }

    private final static String UPCOMING_PACKAGE = "eu.ebrains.kg.core.api.upcoming";

    @Bean
    public GroupedOpenApi upcoming(RequestMappingHandlerMapping requestHandlerMapping) {
        return GroupedOpenApi.builder()
                .addOperationCustomizer(new Customizer())
                .group(UPCOMING_VERSION)
                .addOpenApiCustomiser(openApi -> openApi.getInfo().title(String.format(TITLE_TEMPLATE, UPCOMING_VERSION, Version.UPCOMING, StringUtils.EMPTY)).version(Version.UPCOMING))
                .packagesToScan(UPCOMING_PACKAGE)
                .pathsToMatch(getPathsByAnnotation(requestHandlerMapping, null, Admin.class))
                .build();
    }

    @Bean
    public GroupedOpenApi separator(RequestMappingHandlerMapping requestHandlerMapping) {
        return GroupedOpenApi.builder().group("¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯").packagesToScan("eu.ebrains.kg.core.api.nonexistent").build();
    }


    @Bean
    public GroupedOpenApi upcomingAdmin(RequestMappingHandlerMapping requestHandlerMapping) {
        return GroupedOpenApi.builder()
                .addOperationCustomizer(new Customizer())
                .group(String.format("· %s (admin)", UPCOMING_VERSION))
                .addOpenApiCustomiser(openApi -> openApi.getInfo().title(String.format(TITLE_TEMPLATE, UPCOMING_VERSION, Version.UPCOMING,  " for administrators")).version(Version.UPCOMING)
                        .description("This is the admin API containing endpoints to manage the KG. Note that the endpoints shown require you to authenticate with a user having administrator rights."))
                .packagesToScan(UPCOMING_PACKAGE)
                .pathsToMatch(getPathsByAnnotation(requestHandlerMapping, Admin.class, null))
                .build();
    }


    @Bean
    public OpenAPI genericOpenAPI(@Value("${eu.ebrains.kg.login.endpoint}") String loginEndpoint, @Value("${eu.ebrains.kg.login.tokenEndpoint}") String tokenEndpoint, @Value("${eu.ebrains.kg.commit}") String commit) {
        SecurityScheme clientToken = new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-Authorization").description("The bearer token for the service account to contextualize the authentication to this client.");
        OAuthFlow oAuthFlow = new OAuthFlow();
        oAuthFlow.authorizationUrl(loginEndpoint);
        oAuthFlow.refreshUrl(tokenEndpoint);
        oAuthFlow.tokenUrl(tokenEndpoint);
        oAuthFlow.addExtension("client_id", "kg");
        SecurityScheme userToken = new SecurityScheme().name("Authorization").type(SecurityScheme.Type.OAUTH2).flows(new OAuthFlows().authorizationCode(oAuthFlow)).description("The user authentication");

        OpenAPI openapi = new OpenAPI().openapi("3.0.3");

        String description = String.format("*Commit:  %s.* \n\n" +
                "You can find and try out all endpoints ready to be consumed as an end-user. \n\n" +
                "For some sections, the endpoints are grouped to separate **\"advanced\"** functionalities pointing out that these are suited for more advanced use-cases.\n\n" +
                "At the bottom, you can also find the **\"xtra\"** section which contains endpoints built for rather specific use-cases which you can use but most probably are not what you're looking for.", commit);

        return openapi.info(new Info().description(description).license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")).termsOfService("https://kg.ebrains.eu/search-terms-of-use.html"))
                .components(new Components()).schemaRequirement("Authorization", userToken).schemaRequirement("Client-Authorization", clientToken);
    }

}

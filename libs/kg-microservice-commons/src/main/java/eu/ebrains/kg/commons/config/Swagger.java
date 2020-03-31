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

import com.google.common.base.Predicates;
import eu.ebrains.kg.commons.Version;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.*;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.ApiKeyVehicle;
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The swagger configuration containing elements for authentication and removal of internal endpoints
 */
@Configuration
@EnableSwagger2
public class Swagger {

    private final String applicationName;
    private final String loginEndpoint;

    public Swagger(@Value("${spring.application.name}") String applicationName, @Value("${eu.ebrains.kg.login.endpoint}") String loginEndpoint) {
        this.applicationName = applicationName;
        this.loginEndpoint = loginEndpoint;
    }


    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(applicationName)
                .description(String.format("This is the %s API", applicationName))
                .license("Apache 2.0")
                .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0.html")
                .termsOfServiceUrl("https://kg.ebrains.eu/search-terms-of-use.html")
                .version(Version.API)
                .contact(new Contact("", "", "kg@ebrains.eu"))
                .build();
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.basePackage("eu.ebrains.kg")).paths(Predicates.and(Predicates.not(PathSelectors.regex("^/error.*")), Predicates.not(PathSelectors.regex("^/"))))
                .build().apiInfo(apiInfo()).securitySchemes(getSecuritySchemes()).securityContexts(Collections.singletonList(securityContext()));
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(Arrays.asList(new SecurityReference("Client token", new AuthorizationScope[0]), new SecurityReference("HBP token", new AuthorizationScope[0])))
                .forPaths(PathSelectors.regex("(?!/nexus).*"))
                .build();
    }

    List<GrantType> grantTypes() {
        GrantType grantType = new ImplicitGrantBuilder()
                .loginEndpoint(new LoginEndpoint(this.loginEndpoint))
                .build();
        return Arrays.asList(grantType);
    }

    @Bean
    public SecurityConfiguration securityInfo() {
        return new SecurityConfiguration("kg", "", "hbp", "EBRAINS Knowledge Graph", "", ApiKeyVehicle.HEADER, "Authorization", ",");
    }

    private List<SecurityScheme> getSecuritySchemes() {
        return Arrays.asList(new ApiKey("Client token", "Client-Authorization", "header"), new OAuthBuilder().name("HBP token").grantTypes(grantTypes()).build());
    }

}

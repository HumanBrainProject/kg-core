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

@Configuration
@EnableSwagger2
public class Swagger {

    private final String applicationName;

    public Swagger(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }


    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(applicationName)
                .description(String.format("This is the %s API", applicationName))
                .license("Apache 2.0")
                .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0.html")
                .termsOfServiceUrl("https://kg.ebrains.eu/search-terms-of-use.html")
                .version("3.0.0")
                .contact(new Contact("", "", "kg@ebrains.eu"))
                .build();
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.basePackage("eu.ebrains.kg")).paths(PathSelectors.regex("^(?!/error).*"))
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
                .loginEndpoint(new LoginEndpoint("https://iam-dev.humanbrainproject.eu/auth/realms/hbp/protocol/openid-connect/auth"))
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

//    securitySchemes:
//    ClientAuth:
//    type: apiKey
//    name: ClientAuthorization
//    in: header
//    description: The authorization bearer token you can get from https://iam-dev.humanbrainproject.eu/auth/realms/kg/protocol/openid-connect/token with your client id and client secret (grant_type="client_credentials").
//    UserAuth:
//    type: oauth2
//    description:
//    flows:
//    implicit:
//    authorizationUrl: https://iam-dev.humanbrainproject.eu/auth/realms/hbp/protocol/openid-connect/auth
//    scopes: {}
//    openIdConnectUrl: https://services.humanbrainproject.eu/oidc/.well-known/openid-configuration

}

/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

import eu.ebrains.kg.commons.api.APINaming;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenAPIv3 {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CLIENT_AUTHORIZATION_HEADER = "Client-Authorization";


    @Bean
    public GroupedOpenApi v3Beta() {
        return GroupedOpenApi.builder()
                .group("v3-beta")
                .packagesToScan("eu.ebrains.kg.core.api.v3beta")
                .build();
    }

    @Bean
    public GroupedOpenApi v3() {
        return GroupedOpenApi.builder()
                .group("v3")
                .packagesToScan("eu.ebrains.kg.core.api.v3")
                .build();
    }

    @Bean
    public OpenAPI genericOpenAPI(@Value("${eu.ebrains.kg.login.endpoint}") String loginEndpoint, @Value("${eu.ebrains.kg.login.tokenEndpoint}") String tokenEndpoint, @Value("${eu.ebrains.kg.commit}") String commit) {
        SecurityScheme clientToken = new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name(CLIENT_AUTHORIZATION_HEADER).description("The bearer token for the service account to contextualize the authentication to this client. If you don't know what this is about and just want to test the API, just leave it blank. :)");

        OAuthFlow oAuthFlow = new OAuthFlow();
        oAuthFlow.refreshUrl(tokenEndpoint);
        oAuthFlow.tokenUrl(tokenEndpoint);
        oAuthFlow.authorizationUrl(loginEndpoint);
        oAuthFlow.addExtension("client_id", "kg");
        SecurityScheme userToken = new SecurityScheme().name("Authorization").type(SecurityScheme.Type.OAUTH2).flows(new OAuthFlows().authorizationCode(oAuthFlow)).description("The user authentication");

        SecurityRequirement userWithoutClientReq = new SecurityRequirement().addList(AUTHORIZATION_HEADER);
        SecurityRequirement userWithClientByToken = new SecurityRequirement().addList(CLIENT_AUTHORIZATION_HEADER).addList(AUTHORIZATION_HEADER);
        SecurityRequirement userWithClientByClientSecret = new SecurityRequirement().addList("Client-Id").addList("Client-Secret").addList(AUTHORIZATION_HEADER);
        SecurityRequirement serviceAccountByClientSecret = new SecurityRequirement().addList("Client-Id").addList("Client-SA-Secret");


        OpenAPI openapi = new OpenAPI().openapi("3.0.3");
        String description = String.format("This is the API of the EBRAINS Knowledge Graph (commit %s).", commit);
        return openapi.tags(APINaming.orderedTags()).info(new Info().title("This is the EBRAINS KG API").description(description)
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                        .termsOfService("https://kg.ebrains.eu/search-terms-of-use.html")).components(new Components())
                .schemaRequirement(AUTHORIZATION_HEADER, userToken).schemaRequirement(CLIENT_AUTHORIZATION_HEADER, clientToken)
                .security(Arrays.asList(userWithoutClientReq, userWithClientByToken,  userWithClientByClientSecret, serviceAccountByClientSecret));
    }
}

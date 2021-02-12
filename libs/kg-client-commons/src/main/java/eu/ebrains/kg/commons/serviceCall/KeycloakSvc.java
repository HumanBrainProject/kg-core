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

package eu.ebrains.kg.commons.serviceCall;

import eu.ebrains.kg.commons.api.Authentication;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class KeycloakSvc {

    private final Authentication.Client authentication;
    private String endpoint;
    private final WebClient.Builder internalWebClient;

    public KeycloakSvc(Authentication.Client authentication, @Qualifier("direct") WebClient.Builder internalWebClient) {
        this.authentication = authentication;
        this.internalWebClient = internalWebClient;
    }

    private String getEndpoint() {
        if (endpoint == null) {
            endpoint = authentication.tokenEndpoint();
        }
        return endpoint;
    }

    public String getToken(String clientId, String clientSecret) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        Map<?,?> result = internalWebClient.build()
                .post()
                .uri(getEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return result.get("access_token") instanceof String ? (String)result.get("access_token") : null;
    }
}

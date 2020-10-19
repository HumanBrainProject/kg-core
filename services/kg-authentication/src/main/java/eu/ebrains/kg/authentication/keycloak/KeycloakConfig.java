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

package eu.ebrains.kg.authentication.keycloak;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {

    @Bean
    public Keycloak createKeycloak(KeycloakConfig config) {
        return KeycloakBuilder.builder().grantType(OAuth2Constants.CLIENT_CREDENTIALS).clientSecret(config.getKgCoreClientSecret()).clientId(config.getKgCoreClientId()).serverUrl(config.getServerUrl()).realm(config.getRealm()).build();
    }

    @Value("${eu.ebrains.kg.https}")
    boolean https;

    @Value("${eu.ebrains.kg.ip}")
    String ip;

    @Value("${eu.ebrains.kg.authentication.keycloak.kg.clientId}")
    String kgClientId;

    @Value("${eu.ebrains.kg.authentication.keycloak.kgCore.clientSecret}")
    String kgCoreClientSecret;

    @Value("${eu.ebrains.kg.authentication.keycloak.kgCore.clientId}")
    String kgCoreClientId;

    @Value("${eu.ebrains.kg.authentication.keycloak.serverUrl}")
    String serverUrl;

    @Value("${eu.ebrains.kg.authentication.keycloak.realm}")
    String realm;

    @Value("${eu.ebrains.kg.authentication.keycloak.redirectUri}")
    String redirectUri;

    @Value("${eu.ebrains.kg.authentication.keycloak.issuer}")
    String issuer;

    @Value("${eu.ebrains.kg.authentication.keycloak.configUrl}")
    String configUrl;

    public String getIp() {
        return ip;
    }

    public String getKgClientId() {
        return kgClientId;
    }

    public String getKgCoreClientSecret() {
        return kgCoreClientSecret;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getKgCoreClientId() {
        return kgCoreClientId;
    }

    public String getRealm() {
        return realm;
    }

    public String getConfigUrl() {
        return configUrl;
    }

    public boolean isHttps() {
        return https;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}

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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {

    @Value("${eu.ebrains.kg.https}")
    boolean https;

    @Value("${eu.ebrains.kg.ip}")
    String ip;

    @Value("${eu.ebrains.kg.authentication.keycloak.clientId}")
    String clientId;

    @Value("${eu.ebrains.kg.authentication.keycloak.clientSecret}")
    String clientSecret;

    @Value("${eu.ebrains.kg.authentication.keycloak.serverUrl}")
    String serverUrl;

    @Value("${eu.ebrains.kg.authentication.keycloak.user}")
    String user;

    @Value("${eu.ebrains.kg.authentication.keycloak.pwd}")
    String pwd;

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

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getUser() {
        return user;
    }

    public String getPwd() {
        return pwd;
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

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

package eu.ebrains.kg.authentication.keycloak;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import eu.ebrains.kg.authentication.model.UserOrClientProfile;
import eu.ebrains.kg.commons.AuthTokenContext;
import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.model.User;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@ConditionalOnProperty(value = "eu.ebrains.kg.test", havingValue = "false", matchIfMissing = true)
@Component
public class KeycloakController {

    private static final String PREFERRED_USERNAME = "preferred_username";

    private final KeycloakClient keycloakClient;

    private final AuthTokenContext authTokenContext;

    private final JWTVerifier jwtVerifier;

    private final UserInfoMapping userInfoMapping;


    public KeycloakController(KeycloakClient keycloakClient, AuthTokenContext authTokenContext, UserInfoMapping userInfoMapping) {
        this.keycloakClient = keycloakClient;
        this.authTokenContext = authTokenContext;
        this.jwtVerifier = keycloakClient.getJWTVerifier(); // Reusable verifier instance
        this.userInfoMapping = userInfoMapping;
    }

    public String authenticate(String clientId, String clientSecret) {
        Map<?, ?> result = WebClient.builder().build().post().uri(keycloakClient.getTokenEndpoint()).body(BodyInserters.fromFormData("grant_type", "client_credentials").with("client_id", clientId).with("client_secret", clientSecret)).retrieve().bodyToMono(Map.class).block();
        if (result != null) {
            Object access_token = result.get("access_token");
            if (access_token != null) {
                return access_token.toString();
            }
        }
        return null;
    }

    public UserOrClientProfile getClientProfile(boolean fetchRoles) {
        AuthTokens authTokens = authTokenContext.getAuthTokens();
        if (authTokens == null || authTokens.getClientAuthToken() == null) {
            return null;
        }
        return getInfo(authTokens.getClientAuthToken().getBearerToken(), fetchRoles);
    }

    public UserOrClientProfile getUserProfile(boolean fetchRoles) {
        AuthTokens authTokens = authTokenContext.getAuthTokens();
        if (authTokens == null || authTokens.getUserAuthToken() == null) {
            throw new UnauthorizedException("You haven't provided the required credentials! Please define an Authorization header with your bearer token!");
        }
        return getInfo(authTokens.getUserAuthToken().getBearerToken(), fetchRoles);
    }

    UserOrClientProfile getInfo(String token, boolean fetchRoles) {
        String bareToken = token.substring("Bearer ".length());
        try {
            Map<String, Claim> claims = jwtVerifier.verify(bareToken).getClaims();
            if (fetchRoles) {
                return new UserOrClientProfile(claims, userInfoMapping.getUserOrClientProfile(token));
            }
            return new UserOrClientProfile(claims, null);
        } catch (JWTVerificationException ex) {
            throw new UnauthorizedException(ex);
        }
    }

    public String getClientInfoFromKeycloak(Map<String, Claim> authClientInfo) {
        if (authClientInfo != null) {
            Claim preferred_username = authClientInfo.get(PREFERRED_USERNAME);
            return preferred_username != null ? preferred_username.asString().substring("service-account-".length()) : null;
        } else {
            return null;
        }
    }

    public boolean isServiceAccount(Map<String, Claim> claims) {
        Claim userName = claims.get(PREFERRED_USERNAME);
        return userName!=null && userName.asString().startsWith("service-account-");
    }

    public User buildUserInfoFromKeycloak(Map<String, Claim> claims) {
        Claim userName = claims.get(PREFERRED_USERNAME);
        Claim nativeId = claims.get("sub");
        Claim name = claims.get("name");
        Claim givenName = claims.get("given_name");
        Claim familyName = claims.get("family_name");
        Claim email = claims.get("email");
        return new User(userName != null ? userName.asString() : null, name != null ? name.asString() : null, email != null ? email.asString() : null, givenName != null ? givenName.asString() : null, familyName != null ? familyName.asString() : null, nativeId != null ? nativeId.asString() : null);
    }

}

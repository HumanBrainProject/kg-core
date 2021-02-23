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

package eu.ebrains.kg.commons;

import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.UserAuthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;


@Component
/**
 * This is the access point for getting the token of the current user and the client.
 * Usually, this bean is used indirectly from ({@link AuthContext}) but in some cases, this can cause a circular bean
 * reference. In these cases, this bean can be used directly.
 */
public class AuthTokenContext {
    private final HttpServletRequest request;
    private final KeycloakSvc keycloakSvc;
    private final static Logger logger = LoggerFactory.getLogger(AuthTokenContext.class);

    // IntelliJ reports the injection of HttpServletRequest to be an error -> but it actually is not...
    public AuthTokenContext(KeycloakSvc keycloakSvc, HttpServletRequest request) {
        this.request = request;
        this.keycloakSvc = keycloakSvc;
    }

    public AuthTokens getAuthTokens() {
        String clientAuthorization = request.getHeader("Client-Authorization");
        String userAuthorization = request.getHeader("Authorization");
        if(clientAuthorization==null) {
            String clientId = request.getHeader("Client-Id");
            String clientSecret = request.getHeader("Client-Secret");
            String clientSaSecret = request.getHeader("Client-SA-Secret");
            if (clientId != null) {
                //TODO token caching!?
                if (clientSecret != null) {
                    clientAuthorization = keycloakSvc.getToken(clientId, clientSecret);
                } else if (clientSaSecret != null) {
                    clientAuthorization = keycloakSvc.getToken(clientId, clientSaSecret);
                    if(userAuthorization==null) {
                        userAuthorization = clientAuthorization;
                    }
                }
            }
        }
        String transactionId = request.getHeader("Transaction-Id");
        AuthTokens authTokens = new AuthTokens(
                userAuthorization != null ? new UserAuthToken(userAuthorization) : null,
                clientAuthorization != null ? new ClientAuthToken(clientAuthorization) : null
        );
        if (transactionId != null) {
            try {
                authTokens.setTransactionId(UUID.fromString(transactionId));
            } catch (IllegalArgumentException e) {
                logger.warn(String.format("Was receiving an invalid (non-UUID) transaction-id: %s", transactionId));
            }
        }
        return authTokens;
    }
}

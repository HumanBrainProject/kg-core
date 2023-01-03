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

package eu.ebrains.kg.commons;

import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.UserAuthToken;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * This is the access point for getting the token of the current user and the client.
 * Usually, this bean is used indirectly from ({@link AuthContext}) but in some cases, this can cause a circular bean
 * reference. In these cases, this bean can be used directly.
 */
@Component
public class AuthTokenContext {
    private final HttpServletRequest request;
    private final static Logger logger = LoggerFactory.getLogger(AuthTokenContext.class);

    public static final String CLIENT_AUTHORIZATION_HEADER = "Client-Authorization";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    // IntelliJ reports the injection of HttpServletRequest to be an error -> but it actually is not...
    public AuthTokenContext(HttpServletRequest request) {
        this.request = request;
    }

    public AuthTokens getAuthTokens() {
        Map<String, String> headers = RequestHeadersHolder.get();
        if(headers==null){
            headers = RequestHeadersHolder.createHeadersMap(request);
        }
        String clientAuthorization = headers.get(CLIENT_AUTHORIZATION_HEADER.toLowerCase());
        String userAuthorization = headers.get(AUTHORIZATION_HEADER.toLowerCase());
        String transactionId = headers.get("Transaction-Id".toLowerCase());
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

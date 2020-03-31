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

package eu.ebrains.kg.commons;

import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.UserAuthToken;

/**
 * This is a DTO carrying the token information
 */
public class AuthTokens {

    protected UserAuthToken userAuthToken;
    protected ClientAuthToken clientAuthToken;

    public AuthTokens() {
    }

    public AuthTokens(UserAuthToken userAuthToken, ClientAuthToken clientAuthToken) {
        this.userAuthToken = userAuthToken;
        this.clientAuthToken = clientAuthToken;
    }

    public UserAuthToken getUserAuthToken() {
        return userAuthToken;
    }

    public void setUserAuthToken(UserAuthToken userAuthToken) {
        this.userAuthToken = userAuthToken;
    }

    public ClientAuthToken getClientAuthToken() {
        return clientAuthToken;
    }

    public void setClientAuthToken(ClientAuthToken clientAuthToken) {
        this.clientAuthToken = clientAuthToken;
    }
}

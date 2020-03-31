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
import eu.ebrains.kg.commons.serviceCall.KeycloakSvc;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class ServiceCallWithClientSecret extends AbstractServiceCall {

    //It's ok that this is shared in a singleton -> we only have one client token application-wide
    private ClientAuthToken token;

    private final KeycloakSvc keycloakSvc;
    private final String clientId;
    private final String clientSecret;

    public ServiceCallWithClientSecret(@Qualifier("loadbalanced") WebClient.Builder loadBalancedWebClient, KeycloakSvc keycloakSvc, @Value("${eu.ebrains.kg.clientId}") String clientId, @Value("${eu.ebrains.kg.clientSecret}") String clientSecret) {
        super(loadBalancedWebClient);
        this.keycloakSvc = keycloakSvc;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private synchronized ClientAuthToken refreshToken(){
        String token = keycloakSvc.getToken(this.clientId, this.clientSecret);
        return token == null ? null : new ClientAuthToken(token);
    }


    private ClientAuthToken getToken(boolean forceRefresh){
        if(token == null || forceRefresh){
            token = refreshToken();
        }
        return token;
    }

    @Override
    protected <T> T sendRequest(WebClient.RequestHeadersSpec<?> spec, MediaType mediaType, AuthTokens authTokens, Class<T> returnType) {
        try {
            ClientAuthToken token = getToken(false);
            authTokens.setClientAuthToken(token);
            //Nexus adapters are a little special: They use their service account for ingestion since there is no direct user involvement.
            authTokens.setUserAuthToken(new UserAuthToken(token.getRawToken()));
            return super.sendRequest(spec, mediaType, authTokens, returnType);
        }
        catch(WebClientResponseException e){
            if(e.getStatusCode() == HttpStatus.UNAUTHORIZED){
                //If we receive an unauthorized error, we refresh the token and retry once.
                ClientAuthToken token = getToken(true);
                authTokens.setClientAuthToken(token);
                //Nexus adapters are a little special: They use their service account for ingestion since there is no direct user involvement.
                authTokens.setUserAuthToken(new UserAuthToken(token.getRawToken()));
                return super.sendRequest(spec, mediaType, authTokens, returnType);
            }
            else {
                throw e;
            }
        }

    }
}

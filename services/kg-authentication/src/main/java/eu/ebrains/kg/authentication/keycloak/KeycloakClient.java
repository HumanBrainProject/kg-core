/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import eu.ebrains.kg.authentication.model.IssuerInfo;
import eu.ebrains.kg.authentication.model.OpenIdConfig;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.function.Function;

@Component
public class KeycloakClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final KeycloakConfig config;

    private final WebClient.Builder webclient;

    private final JsonAdapter jsonAdapter;

    private OpenIdConfig openIdConfig;

    private IssuerInfo issuerInfo;

    public KeycloakClient(JsonAdapter jsonAdapter, KeycloakConfig config, WebClient.Builder internalWebClient) {
        this.config = config;
        this.webclient = internalWebClient;
        this.jsonAdapter = jsonAdapter;
    }


    @PostConstruct
    public void initialize() {
        openIdConfig = loadWithRetry(0, i-> {
            String result = WebClient.builder().build().get().uri(config.getConfigUrl()).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(String.class).block();
            return jsonAdapter.fromJson(result, OpenIdConfig.class);
        });
        issuerInfo = loadWithRetry(0, i -> {
            String result = WebClient.builder().build().get().uri(openIdConfig.getIssuer()).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(String.class).block();
            return jsonAdapter.fromJson(result, IssuerInfo.class);
        });
        logger.info("Successfully loaded keycloak configuration");
    }

    public String getServerUrl(){
        return openIdConfig.getIssuer().split("/realms")[0];
    }

    public String getRealm(){
        return issuerInfo.getRealm();
    }

    private final int maxTries = 10;

    private <X> X loadWithRetry(int currentTry, Function<Integer, X> function) {
        if (maxTries > currentTry) {
            try {
                return function.apply(currentTry);
            } catch (Exception ex) {
                try {
                    logger.warn("Was not able to connect to keycloak - trying again in 5 secs...");
                    Thread.sleep(5000);
                    return loadWithRetry(currentTry + 1, function);
                } catch (InterruptedException e) {
                    logger.error("Interrupted keycloak connection", e);
                    throw new RuntimeException(e);
                }
            }
        } else {
            final String error = String.format("Gave up to connect to Keycloak after %d attempts - maybe the authentication system is not properly set up or it is down", maxTries);
            logger.error(error);
            throw new RuntimeException(error);
        }
    }

    JWTVerifier getJWTVerifier() {
        return JWT.require(getAlgorithmFromKeycloakConfig(issuerInfo.getPublicKey())).withIssuer(openIdConfig.getIssuer()).build();
    }

    public String getTokenEndpoint() {
        return openIdConfig.getTokenEndpoint();
    }

    private Algorithm getAlgorithmFromKeycloakConfig(String publicKey) {
        try {
            logger.debug(String.format("Validation by public RSA key (%s) of keycloak host %s", publicKey, openIdConfig.getIssuer()));
            byte[] buffer = Base64.getDecoder().decode(publicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
            RSAPublicKey key = (RSAPublicKey) keyFactory.generatePublic(keySpec);
            Algorithm algorithm = Algorithm.RSA256(key, null);
            logger.info(String.format("Initialized validation by public RSA key of keycloak host %s", openIdConfig.getIssuer()));
            return algorithm;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getUserInfo(String token){
        try {
            return this.webclient.build().get().uri(this.openIdConfig.getUserInfoEndpoint()).accept(MediaType.APPLICATION_JSON).header("Authorization", token).retrieve().bodyToMono(Map.class).block();
        }
        catch(WebClientResponseException.Unauthorized e){
            throw new UnauthorizedException();
        }
    }

}

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

import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.exception.ServiceException;
import eu.ebrains.kg.commons.exception.ServiceNotAvailableException;
import eu.ebrains.kg.commons.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

/**
 * This is the abstract implementation of a service call - a REST call invoked against another microservice of KG core
 */
public abstract class AbstractServiceCall {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final WebClient.Builder loadBalancedWebClient;

    public AbstractServiceCall(WebClient.Builder loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    public <T> T get(String uri, AuthTokens authContext, Class<T> returnType) {
        logger.trace("Sending a get");
        return executeRequest(loadBalancedWebClient.build().get().uri(uri),  MediaType.APPLICATION_JSON, authContext, returnType);
    }

    public <T> T get(String uri, MediaType mediaType, AuthTokens authContext, Class<T> returnType) {
        logger.trace("Sending a get");
        return executeRequest(loadBalancedWebClient.build().get().uri(uri), mediaType, authContext, returnType);
    }

    public <T> T post(String uri, Object payload, AuthTokens authContext, Class<T> returnType) {
        logger.trace("Sending a post");
        return executeRequest(loadBalancedWebClient.build().post().uri(uri).contentType(MediaType.APPLICATION_JSON).body(payload == null ? BodyInserters.empty() : BodyInserters.fromObject(payload)), MediaType.APPLICATION_JSON, authContext, returnType);
    }

    public <T> T put(String uri, Object payload, AuthTokens authContext, Class<T> returnType) {
        logger.trace("Sending a put");
        return executeRequest(loadBalancedWebClient.build().put().uri(uri).contentType(MediaType.APPLICATION_JSON).body(payload == null ? BodyInserters.empty() : BodyInserters.fromObject(payload)), MediaType.APPLICATION_JSON, authContext, returnType);
    }


    public <T> T patch(String uri, Object payload, AuthTokens authContext, Class<T> returnType) {
        logger.trace("Sending a put");
        return executeRequest(loadBalancedWebClient.build().patch().uri(uri).contentType(MediaType.APPLICATION_JSON).body(payload == null ? BodyInserters.empty() : BodyInserters.fromObject(payload)), MediaType.APPLICATION_JSON, authContext, returnType);
    }

    public <T> T delete(String uri, AuthTokens authContext, Class<T> returnType) {
        logger.trace("Sending a post");
        return executeRequest(loadBalancedWebClient.build().delete().uri(uri), MediaType.APPLICATION_JSON, authContext, returnType);
    }

    private <T> T executeRequest(WebClient.RequestHeadersSpec<?> spec, MediaType mediaType, AuthTokens authContext, Class<T> returnType) {
        try {
            return sendRequest(spec, mediaType, authContext, returnType);
        } catch (WebClientResponseException e) {
            return handleWebClientResponseException(e, logger);
        }
    }

    protected <T> T sendRequest(WebClient.RequestHeadersSpec<?> spec, MediaType mediaType, AuthTokens authContext, Class<T> returnType) {
        WebClient.RequestHeadersSpec<?> request = spec.accept(mediaType);
        UUID requestUUID = UUID.randomUUID();
        long launch = new Date().getTime();
        logger.debug(String.format("Sending service request %s", requestUUID));
        if (authContext != null && authContext.getUserAuthToken() != null) {
            request = request.headers(h -> h.put(HttpHeaders.AUTHORIZATION, Collections.singletonList(authContext.getUserAuthToken().getBearerToken())));
        }
        if(authContext != null && authContext.getClientAuthToken() != null){
            request = request.headers(h -> h.put("Client-Authorization", Collections.singletonList(authContext.getClientAuthToken().getBearerToken())));
        }
        WebClient.ResponseSpec retrieve = request.retrieve();
        T result = retrieve.bodyToMono(returnType).block();
        logger.debug(String.format("Received result of request %s in %dms", requestUUID.toString(), new Date().getTime()-launch));
        return result;
    }

    public static <T> T handleWebClientResponseException(WebClientResponseException e, Logger logger) {
        switch (e.getStatusCode()) {
            case SERVICE_UNAVAILABLE:
                throw new ServiceNotAvailableException(String.format("Service %s not available", e.getRequest().getURI().getHost()));
            case FORBIDDEN:
                logger.error("Was not allowed to execute service request");
                throw new ForbiddenException(e.getResponseBodyAsString());
            case UNAUTHORIZED:
                logger.error("Was not authorized to execute service request");
                throw new UnauthorizedException(e.getResponseBodyAsString());
            case NOT_FOUND:
                return null;
            case CONFLICT:
            case PAYLOAD_TOO_LARGE:
                logger.error(String.format("Service exception: %d %s", e.getRawStatusCode(), e.getResponseBodyAsString()));
                throw new ServiceException(e.getRawStatusCode(), e.getResponseBodyAsString());
            default:
                logger.error(String.format("Was trying to execute a %s query against %s but was receiving an error: %s (%d) - %s", e.getRequest().getMethodValue(), e.getRequest().getURI().toString(), e.getStatusText(), e.getStatusCode().value(), e.getResponseBodyAsString()));
                throw e;
        }
    }

}

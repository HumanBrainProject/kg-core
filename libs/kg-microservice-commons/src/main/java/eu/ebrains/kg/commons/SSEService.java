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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

@Component
public class SSEService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final WebClient.Builder sseWebClient;
    private final WebClient.Builder externalSSEWebClient;

    public SSEService(@Qualifier("sse") WebClient.Builder sseWebClient, @Qualifier("sse-external") WebClient.Builder externalSSEWebClient) {
        this.externalSSEWebClient = externalSSEWebClient;
        this.sseWebClient = sseWebClient;
    }
    public Flux<ServerSentEvent> sse(String uri, String lastEventId, ClientAuthToken authToken) {
        return sse(uri, lastEventId, true, authToken);
    }

    public Flux<ServerSentEvent> sse(String uri, String lastEventId, boolean useLoadbalancer, ClientAuthToken authToken) {
        return sse(uri, lastEventId, useLoadbalancer ? this.sseWebClient : this.externalSSEWebClient, authToken);
    }

    private Flux<ServerSentEvent> sse(String uri, String lastEventId, WebClient.Builder webClient, ClientAuthToken authToken) {
        try {
            WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec = webClient.build().get();
            if (lastEventId != null) {
                requestHeadersUriSpec.header("Last-Event-ID", lastEventId);
            }
            if(authToken != null){
                requestHeadersUriSpec.header("Authorization", authToken.getBearerToken());
            }
            return requestHeadersUriSpec.uri(uri).accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_STREAM_JSON).retrieve().bodyToFlux(ServerSentEvent.class);
        } catch (WebClientResponseException e) {
            logger.error(String.format("Was trying to connect to the SSE stream at %s but was receiving an error: %s (%d) - %s", e.getRequest().getMethodValue(), e.getRequest().getURI().toString(), e.getStatusText(), e.getStatusCode().value(), e.getResponseBodyAsString()));
            throw e;
        }
    }

}

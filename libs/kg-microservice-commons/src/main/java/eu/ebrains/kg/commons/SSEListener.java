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

import com.google.gson.Gson;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.models.EventProcessor;
import eu.ebrains.kg.commons.models.EventStoreSvc;
import eu.ebrains.kg.commons.models.EventTracker;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.annotation.Async;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeoutException;

public abstract class SSEListener<T extends Event> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EventStoreSvc primaryStoreSvc;

    private final EventProcessor eventProcessor;

    private final EventTracker eventTracker;

    private final Gson gson;


    public SSEListener(EventStoreSvc primaryStoreSvc, EventProcessor eventProcessor, EventTracker eventTracker, Gson gson) {
        this.primaryStoreSvc = primaryStoreSvc;
        this.eventProcessor = eventProcessor;
        this.eventTracker = eventTracker;
        this.gson = gson;
    }

    @Async
    public void connectToSSEStream(final DataStage stage, ClientAuthToken authToken) {
        doConnectToSSEStream(stage, authToken);
    }

    private void doConnectToSSEStream(final DataStage stage,  ClientAuthToken authToken) {
        try {
            Flux<ServerSentEvent> serverSentEventFlux = primaryStoreSvc.connectToSSE(eventTracker.getLastSeenEventId(stage), authToken);
            serverSentEventFlux.log().timeout(Duration.of(5, ChronoUnit.MINUTES)).subscribe(this::consumeSSE, throwable -> SSEListener.this.waitAndReconnectAfterException(stage, authToken, throwable), () -> SSEListener.this.waitAndReconnect(stage, authToken));
        } catch (Exception ex) {
            waitAndReconnectAfterException(stage, authToken, ex);
        }
    }

    protected abstract T fromServerSentEvent(ServerSentEvent serverSentEvent);

    private void consumeSSE(ServerSentEvent serverSentEvent) {
        logger.info(String.format("Received SSE: %s", serverSentEvent.id()));
        logger.debug(String.format("Received SSE: %s", serverSentEvent.data()));
        T event = fromServerSentEvent(serverSentEvent);
        if (event != null) {
            eventProcessor.addEventToQueue(event);
            eventProcessor.processQueue();
        }
    }

    private void waitAndReconnectAfterException(DataStage stage,ClientAuthToken authToken, Throwable e) {
        if (e instanceof TimeoutException) {
            logger.info("There was no activity on the event stream for a while - I'm reconnecting");
            logger.debug("Details of timeout", e);
        } else {
            logger.error("SSE stream interrupted", e);
        }
        waitAndReconnect(stage, authToken);
    }

    private void waitAndReconnect(DataStage stage, ClientAuthToken authToken) {
        try {
            logger.info("Waiting for 10secs until I reconnect");
            Thread.sleep(10000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        doConnectToSSEStream(stage, authToken);
    }

}

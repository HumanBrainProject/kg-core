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

package eu.ebrains.kg.commons.models;

import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.EventId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public abstract class EventProcessor<T extends Event & EventId> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public EventProcessor(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 10000)
    public void scheduledEventProcessing() {
        logger.trace("Checking event processing regularly...");
        processQueue();
    }

    protected final static int QUEUE_SIZE=1000000;
    protected final static int NUMBER_OF_MAX_RETRIES = 10;

    protected final Queue<T> eventQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);

    protected final Object queueLock = new Object();

    public void addEventToQueue(T event){
        eventQueue.add(event);
    }

    public boolean isSettled(){
        return eventQueue.isEmpty();
    }

    public void processQueue() {
        threadPoolTaskExecutor.execute(() -> {
            logger.debug("Checking for new events to process");
            synchronized (queueLock) {
                int numberOfTries = 0;
                while(!eventQueue.isEmpty()) {
                    T element = eventQueue.peek();
                    String eventId = element.getEventId();
                    boolean processed = true;
                    if (element != null) {
                        logger.debug(String.format("Processing event %s in the eventQueue - %d left", eventId , eventQueue.size()));
                        try{
                            handleEvent(element);
                            logger.debug(String.format("Handled event %s a %d try/tries!", eventId, numberOfTries+1));
                            numberOfTries = 0;
                        }
                        catch (Exception e){
                            if(numberOfTries>NUMBER_OF_MAX_RETRIES){
                                logger.error(String.format("Was not able to handle event %s - skipping it!", eventId), e);
                                eventQueue.remove();
                            }
                            else{
                                logger.debug(String.format("Was not able to handle event %s - trying again (the %d time)",eventId, numberOfTries+1));
                                numberOfTries++;
                                processed = false;
                                try{Thread.sleep(5000);}
                                catch(InterruptedException ie){

                                }
                            }
                        }
                    }
                    if(processed) {
                        //Wait with removal of the queue in case the element handling was not successful...
                        eventQueue.remove();
                    }
                }
            }
        });
    }

    protected abstract void handleEvent(T persistedEvent);
}

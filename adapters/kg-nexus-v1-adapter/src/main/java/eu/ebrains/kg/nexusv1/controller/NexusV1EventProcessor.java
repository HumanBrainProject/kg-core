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

package eu.ebrains.kg.nexusv1.controller;

import eu.ebrains.kg.commons.models.EventProcessor;
import eu.ebrains.kg.nexusv1.models.NexusV1Event;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component("nexus-v1-event-processor")
public class NexusV1EventProcessor extends EventProcessor<NexusV1Event> {

    private final EventDispatcher eventDispatcher;

    public NexusV1EventProcessor(ThreadPoolTaskExecutor threadPoolTaskExecutor, EventDispatcher eventDispatcher) {
        super(threadPoolTaskExecutor);
        this.eventDispatcher = eventDispatcher;
        super.threadPoolTaskExecutor.setCorePoolSize(1);
        super.threadPoolTaskExecutor.setMaxPoolSize(1);
    }


    @Override
    protected void handleEvent(NexusV1Event event) {
        eventDispatcher.dispatchEvent(event);
    }

}

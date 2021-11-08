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

package eu.ebrains.kg.primaryStore.api;

import eu.ebrains.kg.commons.api.PrimaryStoreEvents;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.primaryStore.controller.EventProcessor;
import eu.ebrains.kg.primaryStore.controller.InferenceProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class PrimaryStoreEventsAPI implements PrimaryStoreEvents.Client {

    private final EventProcessor eventProcessor;

    private final InferenceProcessor inferenceProcessor;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PrimaryStoreEventsAPI(EventProcessor eventProcessor, InferenceProcessor inferenceProcessor) {
        this.eventProcessor = eventProcessor;
        this.inferenceProcessor = inferenceProcessor;
    }

    @Override
    public Set<InstanceId> postEvent(Event event) {
        return eventProcessor.postEvent(event);
    }


    @Override
    public void infer(String space, UUID id) {
        eventProcessor.autoRelease(inferenceProcessor.triggerInference(new SpaceName(space), id));
    }

}

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

package eu.ebrains.kg.commons.models;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.ZonedDateTime;

/**
 * A data structure to pass (and override) event information from an external source (e.g. from adapters)
 */
public class ExternalEventInformation {
    @Parameter(description = "The original user of this event. This is only considered if the submitting client has the according permissions.")
    private String externalUserDefinition;
    @Parameter(description = "The original time of this event. This is only considered if the submitting client has the according permissions.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime externalEventTime;

    public String getExternalUserDefinition() {
        return externalUserDefinition;
    }

    public void setExternalUserDefinition(String externalUserDefinition) {
        this.externalUserDefinition = externalUserDefinition;
    }

    public ZonedDateTime getExternalEventTime() {
        return externalEventTime;
    }

    public void setExternalEventTime(ZonedDateTime externalEventTime) {
        this.externalEventTime = externalEventTime;
    }
}

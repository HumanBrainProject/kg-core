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

package eu.ebrains.kg.primaryStore.model;

import eu.ebrains.kg.commons.model.PersistedEvent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;

public class FailedEvent {
    private PersistedEvent persistedEvent;
    private String reason;
    private ZonedDateTime failureTime;

    public FailedEvent() {
    }

    public FailedEvent(PersistedEvent persistedEvent, Exception e, ZonedDateTime failureTime) {
        this.persistedEvent = persistedEvent;
        StringWriter sw  = new StringWriter();
        e.printStackTrace(new PrintWriter((sw)));
        this.reason = sw.toString();
        this.failureTime = failureTime;
    }

    public PersistedEvent getPersistedEvent() {
        return persistedEvent;
    }


    public String getReason() {
        return reason;
    }


    public ZonedDateTime getFailureTime() {
        return failureTime;
    }

}

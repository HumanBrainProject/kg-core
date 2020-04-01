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

import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.models.EventTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Component("nexus-v1-event-tracker")
public class NexusV1EventTracker implements EventTracker {

    private String lastSeenEventId;
    private Long numberOfEventsProcessed;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Path nexusV1EventTrackingFilePath = Paths.get("eventTracking.json");

    public void updateLastSeenEventId(String eventId) {
        if (eventId != null && !eventId.equals(lastSeenEventId)) {
            this.lastSeenEventId = eventId;
            this.numberOfEventsProcessed = this.numberOfEventsProcessed == null ? 1 : this.numberOfEventsProcessed + 1;
            try {
                saveLastSeenEventIdToDB(eventId);
            }catch (IOException e){
                logger.error(e.getMessage());
            }
        }
    }

    public String getLastSeenEventId(DataStage stage) {
        if (this.lastSeenEventId == null) {
            try{
                loadLastSeenEventIdFromFile();
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        return this.lastSeenEventId;
    }

    private Path getOrCreateEventTrackingFilePath() throws IOException {
        if (!this.nexusV1EventTrackingFilePath.toFile().exists()) {
            return Files.createFile(this.nexusV1EventTrackingFilePath);
        }
        return this.nexusV1EventTrackingFilePath;
    }

    private void saveLastSeenEventIdToDB(String lastSeenEventId) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(getOrCreateEventTrackingFilePath().toAbsolutePath().toString()));
        writer.write(lastSeenEventId);
        writer.close();
    }

    private String loadLastSeenEventIdFromFile() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(getOrCreateEventTrackingFilePath().toAbsolutePath().toString()));
        return br.readLine();
    }

}

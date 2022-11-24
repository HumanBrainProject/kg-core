/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

package eu.ebrains.kg.release.controller;

import eu.ebrains.kg.commons.api.GraphDBInstances;
import eu.ebrains.kg.commons.api.PrimaryStoreEvents;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.ReleaseStatus;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class Release {

    private final GraphDBInstances.Client graphDBInstances;
    private final PrimaryStoreEvents.Client primaryStoreEvents;

    public Release(GraphDBInstances.Client graphDBInstances, PrimaryStoreEvents.Client primaryStoreEvents) {
        this.graphDBInstances = graphDBInstances;
        this.primaryStoreEvents = primaryStoreEvents;
    }

    public void release(SpaceName space, UUID id, String revision) {
        IndexedJsonLdDoc jsonLdDoc = getInstance(DataStage.IN_PROGRESS, space, id);
        if (jsonLdDoc == null) {
            throw new IllegalArgumentException(String.format("Instance %s/%s not found", space.getName(), id));
        } else if (revision!=null && jsonLdDoc.hasRevision(revision)) {
            //Revision is optional -> if not provided, we just continue with the releasing process.
            throw new IllegalArgumentException("Incorrect revision provided");
        }
        primaryStoreEvents.postEvent(new Event(space, id, jsonLdDoc.getDoc(), Event.Type.RELEASE, new Date()));
    }

    public void unrelease(SpaceName space, UUID id) {
        IndexedJsonLdDoc jsonLdDoc = getInstance(DataStage.RELEASED, space, id);
        if (jsonLdDoc == null) {
            throw new IllegalArgumentException(String.format("Instance %s/%s not found", space.getName(), id));
        }
        primaryStoreEvents.postEvent(new Event(space, id, jsonLdDoc.getDoc(), Event.Type.UNRELEASE, new Date()));
    }

    public ReleaseStatus getStatus(SpaceName space, UUID id, ReleaseTreeScope treeScope) {
        return graphDBInstances.getReleaseStatus(space!=null ? space.getName() : null, id, treeScope);
    }

    public Map<UUID, ReleaseStatus> getIndividualInstanceStatuses(List<InstanceId> instanceIds, ReleaseTreeScope releaseTreeScope) {
        return graphDBInstances.getIndividualReleaseStatus(instanceIds, releaseTreeScope);
    }

    private IndexedJsonLdDoc getInstance(DataStage stage, SpaceName space, UUID id) {
        return IndexedJsonLdDoc.from(graphDBInstances.getInstanceById(space.getName(), id, stage, true, false, false, null, true));
    }
}

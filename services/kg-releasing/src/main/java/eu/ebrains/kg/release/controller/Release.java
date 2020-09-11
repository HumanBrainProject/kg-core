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

package eu.ebrains.kg.release.controller;

import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.ReleaseStatus;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.release.serviceCall.ReleaseToGraphDB;
import eu.ebrains.kg.release.serviceCall.ReleaseToPrimaryStore;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
public class Release {

    private final ReleaseToGraphDB graphDBSvc;

    private final ReleaseToPrimaryStore releasePrimaryStoreSvc;


    public Release(ReleaseToGraphDB graphDBSvc, ReleaseToPrimaryStore releasePrimaryStoreSvc) {
        this.graphDBSvc = graphDBSvc;
        this.releasePrimaryStoreSvc = releasePrimaryStoreSvc;
    }

    public void release(Space space, UUID id, String revision) {
        IndexedJsonLdDoc jsonLdDoc = getInstance(DataStage.IN_PROGRESS, space, id);
        if (jsonLdDoc == null) {
            throw new IllegalArgumentException(String.format("Instance %s/%s not found", space.getName(), id));
        } else if (revision!=null && jsonLdDoc.hasRevision(revision)) {
            //Revision is optional -> if not provided, we just continue with the releasing process.
            throw new IllegalArgumentException("Incorrect revision provided");
        }
        releasePrimaryStoreSvc.pushToStore(new Event(space, id, jsonLdDoc.getDoc(), Event.Type.RELEASE, new Date()));
    }

    public void unrelease(Space space, UUID id) {
        IndexedJsonLdDoc jsonLdDoc = getInstance(DataStage.RELEASED, space, id);
        if (jsonLdDoc == null) {
            throw new IllegalArgumentException(String.format("Instance %s/%s not found", space.getName(), id));
        }
        releasePrimaryStoreSvc.pushToStore(new Event(space, id, jsonLdDoc.getDoc(), Event.Type.UNRELEASE, new Date()));
    }

    public ReleaseStatus getStatus(Space space, UUID id, ReleaseTreeScope treeScope) {
        return graphDBSvc.getReleaseStatus(space, id, treeScope);
    }

    private IndexedJsonLdDoc getInstance(DataStage stage, Space space, UUID id) {
        return graphDBSvc.getWithEmbedded(stage, space, id);
    }
}

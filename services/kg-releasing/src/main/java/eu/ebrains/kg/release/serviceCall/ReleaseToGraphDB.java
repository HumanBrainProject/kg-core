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

package eu.ebrains.kg.release.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.ReleaseStatus;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReleaseToGraphDB {

    private final ServiceCall serviceCall;

    private final AuthContext authContext;

    private static final String SERVICE_ENDPOINT = "http://kg-graphdb-sync/internal/graphdb";

    public ReleaseToGraphDB(ServiceCall serviceCall, AuthContext authContext) {
        this.serviceCall = serviceCall;
        this.authContext = authContext;
    }

    public IndexedJsonLdDoc getWithEmbedded(DataStage stage, Space space, UUID id){
        return IndexedJsonLdDoc.from(serviceCall.get(String.format("%s/%s/instances/%s/%s?returnEmbedded=true", SERVICE_ENDPOINT, stage.name(), space.getName(), id), authContext.getAuthTokens(), NormalizedJsonLd.class));
    }

    public ReleaseStatus getReleaseStatus(Space space, UUID id, ReleaseTreeScope treeScope){
        return serviceCall.get(String.format("%s/%s/instances/%s/%s/releaseStatus?releaseTreeScope=%s", SERVICE_ENDPOINT, DataStage.LIVE, space.getName(), id, treeScope.toString()), authContext.getAuthTokens(), ReleaseStatus.class);
    }

}

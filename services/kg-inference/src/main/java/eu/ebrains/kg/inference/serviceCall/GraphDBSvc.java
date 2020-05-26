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

package eu.ebrains.kg.inference.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdPayloadMapping;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.Space;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GraphDBSvc {

    private final ServiceCall serviceCall;
    private final AuthContext authContext;

    public GraphDBSvc(ServiceCall serviceCall, AuthContext authContext) {
        this.serviceCall = serviceCall;
        this.authContext = authContext;
    }

    private final static String SERVICE_URL = "http://kg-graphdb-sync/internal/graphdb";

    public List<IndexedJsonLdDoc> getRelatedInstancesByIdentifiers(Space space, UUID id, DataStage stage, boolean embedded) {
        return Arrays.stream(serviceCall.get(String.format("%s/%s/instances/%s/%s/relatedByIdentifier?returnEmbedded=%b", SERVICE_URL, stage.name(), space.getName(), id, embedded), authContext.getAuthTokens(), NormalizedJsonLd[].class)).map(IndexedJsonLdDoc::from).collect(Collectors.toList());
    }

    public List<IndexedJsonLdDoc> getRelatedInstancesByIncomingRelation(Space space, UUID id, DataStage stage, String relation, boolean useOriginalTo) {
        return Arrays.stream(serviceCall.get(String.format("%s/%s/instances/%s/%s/relatedByIncomingRelation?useOriginalTo=%s&relation=%s", SERVICE_URL,  stage.name(), space.getName(), id, useOriginalTo, URLEncoder.encode(relation, StandardCharsets.UTF_8)), authContext.getAuthTokens(), NormalizedJsonLd[].class)).map(IndexedJsonLdDoc::from).collect(Collectors.toList());
    }

    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(DataStage stage, List<UUID> ids, Space space, boolean returnEmbedded, boolean returnAlternatives){
        return serviceCall.post(SERVICE_URL + String.format("/%s/instancesByIds?returnEmbedded=%b&returnAlternatives=%b", stage.name(), returnEmbedded, returnAlternatives), ids.stream().map(id -> new InstanceId(id, space)).map(InstanceId::serialize).collect(Collectors.toList()), authContext.getAuthTokens(), IdPayloadMapping.class);
    }

    public IndexedJsonLdDoc getInstanceById(Space space, UUID id, DataStage stage, boolean removeInternalProperties, boolean embedded){
        return IndexedJsonLdDoc.from(serviceCall.get(String.format("%s/%s/instances/%s/%s?removeInternalProperties=%b&returnEmbedded=%b", SERVICE_URL, stage.name(), space.getName(), id, removeInternalProperties, embedded), authContext.getAuthTokens(), NormalizedJsonLd.class));
    }

}

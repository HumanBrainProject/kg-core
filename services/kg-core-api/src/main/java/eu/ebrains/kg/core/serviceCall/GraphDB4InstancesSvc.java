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

package eu.ebrains.kg.core.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.query.KgQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GraphDB4InstancesSvc {

    private final ServiceCall serviceCall;

    private final AuthContext authContext;

    public GraphDB4InstancesSvc(ServiceCall serviceCall, AuthContext authContext) {
        this.serviceCall = serviceCall;
        this.authContext = authContext;
    }

    private final static String BASE_URL = "http://kg-graphdb-sync";

    public NormalizedJsonLd getInstance(DataStage stage, InstanceId instanceId, boolean returnEmbedded, boolean returnAlternatives) {
        return serviceCall.get(BASE_URL + String.format("/%s/instances/%s?returnEmbedded=%b&returnAlternatives=%b", stage.name(), instanceId.serialize(), returnEmbedded, returnAlternatives), authContext.getAuthTokens(), NormalizedJsonLd.class);
    }

    public Paginated<NormalizedJsonLd> getInstancesByType(DataStage stage, Type type, PaginationParam paginationParam, String searchByLabel, boolean returnEmbedded, boolean returnAlternatives) {
        return serviceCall.get(BASE_URL + String.format("/%s/instancesByType?type=%s&from=%d&size=%s&returnEmbedded=%b&searchByLabel=%s&returnAlternatives=%b", stage.name(), type.getEncodedName(), paginationParam.getFrom(), paginationParam.getSize() != null ? String.valueOf(paginationParam.getSize()) : "", returnEmbedded, searchByLabel != null ? searchByLabel : "", returnAlternatives), authContext.getAuthTokens(), PaginatedDocuments.class);
    }

    public Paginated<NormalizedJsonLd> getQueriesByType(DataStage stage, PaginationParam paginationParam, String searchByLabel, boolean returnAlternatives, Type rootType) {
        return serviceCall.get(BASE_URL + String.format("/%s/queriesByType?from=%d&size=%s&searchByLabel=%s&returnAlternatives=%b&rootType=%s", stage.name(), paginationParam.getFrom(), paginationParam.getSize() != null ? String.valueOf(paginationParam.getSize()) : "", searchByLabel != null ? searchByLabel : "", returnAlternatives, rootType != null ? rootType.getEncodedName() : ""), authContext.getAuthTokens(), PaginatedDocuments.class);
    }

    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(DataStage stage, List<InstanceId> ids, boolean returnEmbedded, boolean returnAlternatives) {
        return serviceCall.post(BASE_URL + String.format("/%s/instancesByIds?returnEmbedded=%b&returnAlternatives=%b", stage.name(), returnEmbedded, returnAlternatives), ids.stream().map(InstanceId::serialize).collect(Collectors.toList()), authContext.getAuthTokens(), IdPayloadMapping.class);
    }

    public SuggestionResult getSuggestedLinksForProperty(NormalizedJsonLd payload, DataStage stage, InstanceId instanceId, UUID originalId, String propertyName, Type type, String search, PaginationParam paginationParam) {
        return serviceCall.post(BASE_URL + String.format("/%s/instances/%s/suggestedLinksForProperty?property=%s&type=%s&search=%s&from=%d&size=%s", stage.name(), instanceId != null ? instanceId.serialize() : String.format("un    known/%s", originalId), propertyName, type != null ? type.getEncodedName() : "", search != null ? search : "", paginationParam.getFrom(), paginationParam.getSize() != null ? String.valueOf(paginationParam.getSize()) : ""), payload, authContext.getAuthTokens(), SuggestionResult.class);

    }

    public JsonLdDoc getSpace(DataStage stage, InstanceId instanceId) {
        return serviceCall.get(String.format(BASE_URL + "/%s/instances/%s", stage.name(), instanceId.serialize()), authContext.getAuthTokens(), JsonLdDoc.class);
    }

    public Paginated<NormalizedJsonLd> executeQuery(KgQuery query, PaginationParam paginationParam) {
        return serviceCall.post(BASE_URL + String.format("/queries?from=%d&size=%s", paginationParam.getFrom(), paginationParam.getSize() != null ? String.valueOf(paginationParam.getSize()) : ""), query, authContext.getAuthTokens(), PaginatedDocuments.class);
    }
}

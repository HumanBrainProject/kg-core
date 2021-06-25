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

package eu.ebrains.kg.commons.api.rest;

import eu.ebrains.kg.commons.AuthTokenContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.api.GraphDBInstances;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestClient
public class GraphDBInstancesRestClient implements GraphDBInstances.Client {

    private final static String SERVICE_URL = "http://kg-graphdb-sync/internal/graphdb";
    private final ServiceCall serviceCall;
    private final AuthTokenContext authTokenContext;

    public GraphDBInstancesRestClient(ServiceCall serviceCall, AuthTokenContext authTokenContext) {
        this.serviceCall = serviceCall;
        this.authTokenContext = authTokenContext;
    }

    @Override
    public Paginated<NormalizedJsonLd> getIncomingLinks(String space, UUID id, DataStage stage, String property, String type, PaginationParam paginationParam) {
        return serviceCall.get(String.format("%s/%s/instances/%s/%s?property=%s&type=%s&from=%d&size=%d",
                SERVICE_URL, stage.name(), space, id, URLEncoder.encode(property, StandardCharsets.UTF_8), URLEncoder.encode(type, StandardCharsets.UTF_8), paginationParam.getFrom(), paginationParam.getSize()),
                authTokenContext.getAuthTokens(),
                PaginatedDocuments.class);
    }

    @Override
    public NormalizedJsonLd getQueryById(String space, UUID id, DataStage stage) {
        return serviceCall.get(String.format("%s/%s/queries/%s/%s", SERVICE_URL, stage.name(), space, id), authTokenContext.getAuthTokens(), NormalizedJsonLd.class);
    }

    @Override
    public NormalizedJsonLd getInstanceById(String space, UUID id, DataStage stage, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize, boolean removeInternalProperties) {
        return serviceCall.get(String.format("%s/%s/instances/%s/%s?returnEmbedded=%b&returnAlternatives=%b&returnIncomingLinks=%b&incomingLinksPageSize=%d",
                SERVICE_URL, stage.name(), space, id, returnEmbedded, returnAlternatives, returnIncomingLinks, incomingLinksPageSize),
                authTokenContext.getAuthTokens(),
                NormalizedJsonLd.class);
    }

    @Override
    public Paginated<NormalizedJsonLd> getInstancesByType(DataStage stage, String type, String space, String searchByLabel, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam) {
        return serviceCall.get(String.format("%s/%s/instancesByType?type=%s&from=%d&size=%s&returnEmbedded=%b&searchByLabel=%s&space=%s&returnAlternatives=%b",
                SERVICE_URL, stage.name(), new Type(type).getEncodedName(), paginationParam.getFrom(), paginationParam.getSize() != null ? String.valueOf(paginationParam.getSize()) : "", returnEmbedded, searchByLabel != null ? searchByLabel : "", space != null ? space : "", returnAlternatives),
                authTokenContext.getAuthTokens(),
                PaginatedDocuments.class);
    }

    @Override
    public Paginated<NormalizedJsonLd> getQueriesByType(DataStage stage, String searchByLabel, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam, String rootType) {
        return serviceCall.get(String.format("%s/%s/queriesByType?from=%d&size=%s&searchByLabel=%s&returnAlternatives=%b&rootType=%s",
                SERVICE_URL, stage.name(), paginationParam.getFrom(), paginationParam.getSize() != null ? String.valueOf(paginationParam.getSize()) : "", searchByLabel != null ? searchByLabel : "", returnAlternatives, rootType != null ? new Type(rootType).getEncodedName() : ""),
                authTokenContext.getAuthTokens(),
                PaginatedDocuments.class);
    }

    @Override
    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(List<String> instanceIds, DataStage stage, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize) {
        return serviceCall.post(String.format("%s/%s/instancesByIds?returnEmbedded=%b&returnAlternatives=%b&returnIncomingLinks=%b&incomingLinksPageSize%d",
                SERVICE_URL, stage.name(), returnEmbedded, returnAlternatives, returnIncomingLinks, incomingLinksPageSize),
                instanceIds,
                authTokenContext.getAuthTokens(),
                IdPayloadMapping.class);
    }

    @Override
    public Map<UUID, String> getLabels(List<String> instanceIds, DataStage stage) {
        return serviceCall.post(String.format("%s/%s/instancesByIds/labels",
                SERVICE_URL, stage.name()),
                instanceIds,
                authTokenContext.getAuthTokens(),
                UUIDtoString.class);
    }

    @Override
    public List<NormalizedJsonLd> getInstancesByIdentifier(String identifier, String space, DataStage stage) {
        return Arrays.asList(
                serviceCall.get(String.format("%s/%s/instancesByIdentifier/%s?identifier=%s",
                        SERVICE_URL, stage.name(), space, identifier),
                        authTokenContext.getAuthTokens(), NormalizedJsonLd[].class));
    }

    @Override
    public List<NormalizedJsonLd> getDocumentWithRelatedInstancesByIdentifiers(String space, UUID id, DataStage stage, boolean returnEmbedded, boolean returnAlternatives) {
        return Arrays.asList(serviceCall.get(String.format("%s/%s/instances/%s/%s/relatedByIdentifier?returnEmbedded=%b",
                SERVICE_URL, stage.name(), space, id, returnEmbedded),
                authTokenContext.getAuthTokens(), NormalizedJsonLd[].class));
    }

    @Override
    public List<NormalizedJsonLd> getDocumentWithIncomingRelatedInstances(String space, UUID id, DataStage stage, String relation, boolean useOriginalTo, boolean returnEmbedded, boolean returnAlternatives) {
        return Arrays.asList(serviceCall.get(String.format("%s/%s/instances/%s/%s/relatedByIncomingRelation?useOriginalTo=%s&relation=%s",
                SERVICE_URL, stage.name(), space, id, useOriginalTo, URLEncoder.encode(relation, StandardCharsets.UTF_8)),
                authTokenContext.getAuthTokens(), NormalizedJsonLd[].class));
    }
//
//    @Override
//    public List<NormalizedJsonLd> getDocumentWithOutgoingRelatedInstances(String space, UUID id, DataStage stage, String relation, boolean returnEmbedded, boolean returnAlternatives) {
//        return null;
//    }

    @Override
    public GraphEntity getNeighbors(String space, UUID id, DataStage stage) {
        return serviceCall.get(String.format("%s/%s/instances/%s/%s/neighbors",
                SERVICE_URL, stage.name(), space, id),
                authTokenContext.getAuthTokens(),
                GraphEntity.class);

    }

    @Override
    public ReleaseStatus getReleaseStatus(String space, UUID id, ReleaseTreeScope treeScope) {
        return serviceCall.get(String.format("%s/%s/instances/%s/%s/releaseStatus?releaseTreeScope=%s",
                SERVICE_URL, DataStage.IN_PROGRESS, space, id, treeScope.toString()),
                authTokenContext.getAuthTokens(),
                ReleaseStatus.class);
    }

    @Override
    public SuggestionResult getSuggestedLinksForProperty(NormalizedJsonLd payload, DataStage stage, String space, UUID id, String propertyName, String sourceType, String targetType, String search, PaginationParam paginationParam) {
        return serviceCall.post(String.format("%s/%s/instances/%s/%s/suggestedLinksForProperty?property=%s&sourceType=%s&targetType=%s&search=%s&from=%d&size=%s",
                SERVICE_URL, stage.name(), space != null ? space : "unknown", id, propertyName, sourceType != null ? URLEncoder.encode(sourceType, StandardCharsets.UTF_8) : "", targetType != null ? URLEncoder.encode(targetType, StandardCharsets.UTF_8) : "", search != null ? search : "", paginationParam.getFrom(), paginationParam.getSize() != null ? String.valueOf(paginationParam.getSize()) : ""),
                payload,
                authTokenContext.getAuthTokens(),
                SuggestionResult.class);
    }
}

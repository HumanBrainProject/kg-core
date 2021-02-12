/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.commons.api.rest;

import eu.ebrains.kg.commons.AuthTokenContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.api.GraphDBTypes;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;

import java.util.List;
import java.util.Map;

@RestClient
public class GraphDBTypesRestClient implements GraphDBTypes.Client {

    private final static String SERVICE_URL = "http://kg-graphdb-sync/internal/graphdb";
    private final ServiceCall serviceCall;
    private final AuthTokenContext authTokenContext;

    public GraphDBTypesRestClient(ServiceCall serviceCall, AuthTokenContext authTokenContext) {
        this.serviceCall = serviceCall;
        this.authTokenContext = authTokenContext;
    }

    @Override
    public Paginated<NormalizedJsonLd> getTypes(DataStage stage, String space, boolean withIncomingLinks, PaginationParam paginationParam) {
        return serviceCall.get(String.format("%s/%s/types?space=%s&from=%d&size=%s&withIncomingLinks=%b",
                SERVICE_URL, stage.name(), space != null ? space : "", paginationParam.getFrom(), paginationParam.getSize() != null ? String.valueOf(paginationParam.getSize()) : "", withIncomingLinks),
                authTokenContext.getAuthTokens(),
                PaginatedDocuments.class);
    }

    @Override
    public Paginated<NormalizedJsonLd> getTypesWithProperties(DataStage stage, String space, boolean withCounts, boolean withIncomingLinks, PaginationParam paginationParam) {
        return serviceCall.get(String.format("%s/%s/typesWithProperties?space=%s&from=%d&size=%s&withCounts=%b&withIncomingLinks=%b",
                SERVICE_URL, stage.name(), space != null ? space : "", paginationParam.getFrom(), paginationParam.getSize() != null ? String.valueOf(paginationParam.getSize()) : "", withCounts, withIncomingLinks),
                authTokenContext.getAuthTokens(),
                PaginatedDocuments.class);
    }

    @Override
    public Map<String, Result<NormalizedJsonLd>> getTypesByName(List<String> types, DataStage stage, String space) {
        return serviceCall.post(String.format("%s/%s/typesByName?space=%s",
                SERVICE_URL, stage.name(), space != null ? space : ""),
                types,
                authTokenContext.getAuthTokens(),
                StringPayloadMapping.class);
    }

    @Override
    public Map<String, Result<NormalizedJsonLd>> getTypesWithPropertiesByName(List<String> types, DataStage stage, boolean withCounts, boolean withIncomingLinks, String space) {
        return serviceCall.post(String.format("%s/%s/typesWithPropertiesByName?space=%s",
                SERVICE_URL, stage.name(), space != null ? space : ""),
                types,
                authTokenContext.getAuthTokens(),
                StringPayloadMapping.class);
    }
}

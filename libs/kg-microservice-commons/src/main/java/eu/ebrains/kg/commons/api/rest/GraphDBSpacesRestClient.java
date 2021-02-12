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
import eu.ebrains.kg.commons.api.GraphDBSpaces;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginatedDocuments;
import eu.ebrains.kg.commons.model.PaginationParam;

@RestClient
public class GraphDBSpacesRestClient implements GraphDBSpaces.Client {

    private final static String SERVICE_URL = "http://kg-graphdb-sync/internal/graphdb";
    private final ServiceCall serviceCall;
    private final AuthTokenContext authTokenContext;

    public GraphDBSpacesRestClient(ServiceCall serviceCall, AuthTokenContext authTokenContext) {
        this.serviceCall = serviceCall;
        this.authTokenContext = authTokenContext;
    }

    @Override
    public NormalizedJsonLd getSpace(DataStage stage, String space) {
        return serviceCall.get(String.format("%s/%s/spaces/%s",
                SERVICE_URL, stage.name(), space),
                authTokenContext.getAuthTokens(),
                NormalizedJsonLd.class);
    }

    @Override
    public Paginated<NormalizedJsonLd> getSpaces(DataStage stage, PaginationParam paginationParam) {
        return serviceCall.get(String.format("%s/%s/spaces?from=%d&size=%s",
                SERVICE_URL, stage.name(), paginationParam.getFrom(), paginationParam.getSize() != null ? String.valueOf(paginationParam.getSize()) : ""),
                authTokenContext.getAuthTokens(),
                PaginatedDocuments.class);
    }
}

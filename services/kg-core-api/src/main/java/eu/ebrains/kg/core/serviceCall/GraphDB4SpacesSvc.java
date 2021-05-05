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

package eu.ebrains.kg.core.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import org.springframework.stereotype.Component;

@Component
public class GraphDB4SpacesSvc {
    private final ServiceCall serviceCall;

    private final AuthContext authContext;

    private final String BASE_URL = "http://kg-graphdb-sync";

    public GraphDB4SpacesSvc(ServiceCall serviceCall, AuthContext authContext) {
        this.serviceCall = serviceCall;
        this.authContext = authContext;
    }


    public NormalizedJsonLd getSpace(Space space, DataStage stage) {
        String relativeUrl = String.format("/%s/spaces/%s", stage.name(), space.getName());
        return serviceCall.get(BASE_URL + relativeUrl, authContext.getAuthTokens(), NormalizedJsonLd.class);
    }

    public Paginated<NormalizedJsonLd> getSpaces(DataStage stage, PaginationParam paginationParam) {
        String relativeUrl = String.format("/%s/spaces", stage.name());
        if (paginationParam != null && paginationParam.getSize() != null) {
            relativeUrl = String.format("%s?from=%d&size=%d", relativeUrl, paginationParam.getFrom(), paginationParam.getSize());
        }
        return serviceCall.get(BASE_URL + relativeUrl, authContext.getAuthTokens(), PaginatedDocuments.class);
    }


}

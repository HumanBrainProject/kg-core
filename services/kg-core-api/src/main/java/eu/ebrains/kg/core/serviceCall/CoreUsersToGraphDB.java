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
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginatedDocuments;
import eu.ebrains.kg.commons.model.PaginationParam;
import org.springframework.stereotype.Component;

@Component
public class CoreUsersToGraphDB {
    private final ServiceCall serviceCall;

    private final AuthContext authContext;

    private final String BASE_URL = "http://kg-graphdb-sync/internal/graphdb";

    public CoreUsersToGraphDB(ServiceCall serviceCall, AuthContext authContext) {
        this.serviceCall = serviceCall;
        this.authContext = authContext;
    }

    public Paginated<NormalizedJsonLd> getUsers(PaginationParam paginationParam) {
        String relativeUrl = String.format("/users?from=%d&size=%s", paginationParam.getFrom(), paginationParam.getSize() != null ? String.valueOf(paginationParam.getSize()) : "");
        return serviceCall.get(BASE_URL + relativeUrl, authContext.getAuthTokens(), PaginatedDocuments.class);
    }

}

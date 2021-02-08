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

package eu.ebrains.kg.commons.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.exception.AmbiguousIdException;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IdsAPIRestClient implements IdsAPI.Client {

    private final static String SERVICE_URL = "http://kg-ids/internal";

    private final ServiceCall serviceCall;
    private final AuthContext authContext;
    private final IdUtils idUtils;

    private static class ListOfJsonLdIds extends ArrayList<JsonLdId> {}
    private static class ListOfJsonLdIdMappings extends ArrayList<JsonLdIdMapping> {}

    public IdsAPIRestClient(ServiceCall serviceCall, AuthContext authContext, IdUtils idUtils) {
        this.serviceCall = serviceCall;
        this.authContext = authContext;
        this.idUtils = idUtils;
    }

    @Override
    public List<JsonLdId> createOrUpdateId(IdWithAlternatives idWithAlternatives, DataStage stage) {
        return null;
    }

    @Override
    public List<JsonLdId> deprecateId(DataStage stage, UUID id, boolean revert) {
        return serviceCall.delete(String.format("%s/ids/%s/%s?revert=%b", SERVICE_URL, DataStage.IN_PROGRESS.name(), id, revert), authContext.getAuthTokens(), ListOfJsonLdIds.class);
    }

    @Override
    public List<JsonLdIdMapping> resolveId(List<IdWithAlternatives> idWithAlternatives, DataStage stage) throws AmbiguousIdException {
        return serviceCall.post(String.format("%s/ids/%s/resolved", SERVICE_URL, stage.name()), idWithAlternatives, authContext.getAuthTokens(), ListOfJsonLdIdMappings.class);
    }
}

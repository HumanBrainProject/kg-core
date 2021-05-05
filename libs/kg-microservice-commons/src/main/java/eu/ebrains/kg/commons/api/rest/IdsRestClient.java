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
import eu.ebrains.kg.commons.api.Ids;
import eu.ebrains.kg.commons.exception.AmbiguousIdException;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;

import java.util.*;

@RestClient
public class IdsRestClient implements Ids.Client {

    private final static String SERVICE_URL = "http://kg-ids/internal/ids";
    private final ServiceCall serviceCall;
    private final AuthTokenContext authTokenContext;

    private static class ListOfJsonLdIds extends ArrayList<JsonLdId> {}
    private static class ListOfJsonLdIdMappings extends ArrayList<JsonLdIdMapping> {}

    public IdsRestClient(ServiceCall serviceCall, AuthTokenContext authTokenContext) {
        this.serviceCall = serviceCall;
        this.authTokenContext = authTokenContext;
    }

    @Override
    public List<JsonLdId> createOrUpdateId(IdWithAlternatives idWithAlternatives, DataStage stage) {
        JsonLdId[] result = serviceCall.post(String.format("%s/%s", SERVICE_URL, stage.name()), idWithAlternatives, authTokenContext.getAuthTokens(), JsonLdId[].class);
        if (result != null) {
            return Arrays.asList(result);
        }
        return Collections.emptyList();
    }

    @Override
    public List<JsonLdId> deprecateId(DataStage stage, UUID id, boolean revert) {
        return serviceCall.delete(String.format("%s/%s/%s?revert=%b", SERVICE_URL, DataStage.IN_PROGRESS.name(), id, revert), authTokenContext.getAuthTokens(), ListOfJsonLdIds.class);
    }

    @Override
    public List<JsonLdIdMapping> resolveId(List<IdWithAlternatives> idWithAlternatives, DataStage stage) throws AmbiguousIdException {
        return serviceCall.post(String.format("%s/%s/resolved", SERVICE_URL, stage.name()), idWithAlternatives, authTokenContext.getAuthTokens(), ListOfJsonLdIdMappings.class);
    }
}

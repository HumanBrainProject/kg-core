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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.commons.api.rest;

import eu.ebrains.kg.commons.AuthTokenContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.api.Release;
import eu.ebrains.kg.commons.model.ReleaseStatus;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@RestClient
public class ReleaseRestClient implements Release.Client {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static String SERVICE_URL = "http://kg-releasing/internal/releases";
    private final ServiceCall serviceCall;
    private final AuthTokenContext authTokenContext;

    public ReleaseRestClient(ServiceCall serviceCall, AuthTokenContext authTokenContext) {
        this.serviceCall = serviceCall;
        this.authTokenContext = authTokenContext;
    }

    @Override
    public void releaseInstance(String space, UUID id, String revision) {
        serviceCall.put(String.format("%s/%s/%s%s",
                SERVICE_URL, space, id, revision != null ? String.format("?rev=%s", revision) : ""),
                null,
                authTokenContext.getAuthTokens(),
                Void.class);

    }

    @Override
    public void unreleaseInstance(String space, UUID id) {
        serviceCall.delete(String.format("%s/%s/%s",
                SERVICE_URL, space, id), authTokenContext.getAuthTokens(), Void.class);

    }

    @Override
    public ReleaseStatus getReleaseStatus(String space, UUID id, ReleaseTreeScope releaseTreeScope) {
        return serviceCall.get(String.format("%s/%s/%s?releaseTreeScope=%s",
                SERVICE_URL, space, id, releaseTreeScope),
                authTokenContext.getAuthTokens(),
                ReleaseStatus.class);
    }
}

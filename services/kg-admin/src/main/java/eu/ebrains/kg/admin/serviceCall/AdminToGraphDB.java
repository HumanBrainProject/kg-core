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

package eu.ebrains.kg.admin.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Client;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Space;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdminToGraphDB {

    @Autowired
    ServiceCall serviceCall;

    @Autowired
    AuthContext authContext;

    private final static String BASE_URL = "http://kg-graphdb-sync/internal/graphdb";

    public String addClientMeta(Client client) {
        return serviceCall.put(BASE_URL+"/clients/" ,client, new AuthTokens(), String.class);
    }


    public NormalizedJsonLd getInstance(DataStage stage, Space space, UUID id, boolean embedded) {
        return serviceCall.get(BASE_URL+String.format("/%s/instances/%s/%s?returnEmbedded=%b", stage.name(), space.getName(), id, embedded), authContext.getAuthTokens(), NormalizedJsonLd.class);
    }

}

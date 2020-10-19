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

package eu.ebrains.kg.primaryStore.serviceCall;

import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.model.SpaceName;
import org.springframework.stereotype.Component;

@Component
public class PrimaryStoreToGraphDB {
    private final ServiceCall serviceCall;

    private final String BASE_URL = "http://kg-graphdb-sync/internal/graphdb";

    public PrimaryStoreToGraphDB(ServiceCall serviceCall) {
        this.serviceCall = serviceCall;
    }

    public Space getSpace(SpaceName space, DataStage stage, AuthTokens authTokens) {
        String relativeUrl = String.format("/%s/spaces/%s", stage.name(), space.getName());
        return Space.fromJsonLd(serviceCall.get(BASE_URL + relativeUrl, authTokens, NormalizedJsonLd.class));
    }

}

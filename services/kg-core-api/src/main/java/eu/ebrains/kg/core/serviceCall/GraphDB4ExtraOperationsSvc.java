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

import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Space;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GraphDB4ExtraOperationsSvc {

    @Autowired
    ServiceCall serviceCall;

    private final static String BASE_URL = "http://kg-graphdb-sync";

    public List<IndexedJsonLdDoc> getRelatedInstancesByIdentifiers(Space space, String identifier, DataStage stage, AuthTokens authTokens) {
        return Arrays.stream(serviceCall.get(String.format("%s/%s/instancesByIdentifier/%s?identifier=%s", BASE_URL, stage.name(), space.getName(), identifier), authTokens, NormalizedJsonLd[].class)).map(IndexedJsonLdDoc::from).collect(Collectors.toList());
    }

    public List<String> getDocumentIdsBySpace(Space space, DataStage stage, AuthTokens authTokens) {
        return Arrays.asList(serviceCall.get(String.format("%s/documentIds/%s", BASE_URL, space.getName()), authTokens, String[].class));
    }

    public NormalizedJsonLd getInstance(DataStage stage, InstanceId instanceId, boolean returnEmbedded, boolean returnAlternatives, boolean removeInternalProperties, AuthTokens authTokens) {
        return serviceCall.get(BASE_URL+String.format("/%s/instances/%s?returnEmbedded=%b&returnAlternatives=%b&removeInternalProperties=%b", stage.name(), instanceId.serialize(), returnEmbedded, returnAlternatives, removeInternalProperties), authTokens, NormalizedJsonLd.class);
    }

}

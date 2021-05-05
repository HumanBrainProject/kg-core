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

package eu.ebrains.kg.coreToQueryComparison.serviceCall;

import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.coreToQueryComparison.model.NexusSchema;
import eu.ebrains.kg.coreToQueryComparison.model.QueryAPIResult;
import eu.ebrains.kg.coreToQueryComparison.model.StructureResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Component
public class OldQuerySvc {

    private final WebClient.Builder webclient;
    private final String queryEndpoint;

    public OldQuerySvc(@Qualifier("external") WebClient.Builder webclient, @Value("${eu.ebrains.kg.test.queryEndpoint}") String queryEndpoint) {
        this.webclient = webclient;
        this.queryEndpoint = queryEndpoint;
    }

    public List<NormalizedJsonLd> getInstances(DataStage stage, NexusSchema nexusSchema, Integer size, String oidcToken){
        QueryAPIResult instances = this.webclient.build().get().uri(String.format("%s/internal/api/instances/%s/%s/%s/%s?databaseScope=%s%s", this.queryEndpoint, nexusSchema.getOrg(), nexusSchema.getDomain(), nexusSchema.getSchema(), nexusSchema.getVersion(), stage == DataStage.LIVE ? "INFERRED" : "RELEASED", size!=null ? String.format("&start=0&size=%d", size) : "")).header("Authorization", oidcToken).retrieve().bodyToMono(QueryAPIResult.class).block();
        return instances!=null ? instances.getResults() : Collections.emptyList();
    }

    public List<NormalizedJsonLd> getSchemas(){
        StructureResult schemas = this.webclient.build().get().uri(String.format("%s/api/structure?withLinks=false", this.queryEndpoint)).retrieve().bodyToMono(StructureResult.class).block();
        return schemas!=null ? schemas.getSchemas() : Collections.emptyList();
    }

}

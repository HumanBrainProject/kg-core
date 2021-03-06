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

package eu.ebrains.kg.core.controller;

import eu.ebrains.kg.commons.api.GraphDBInstances;
import eu.ebrains.kg.commons.api.GraphDBQueries;
import eu.ebrains.kg.commons.api.JsonLd;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.query.KgQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
/**
 * The query controller contains the orchestration logic for the query operations
 */
@Component
public class CoreQueryController {

    private final GraphDBInstances.Client graphDBInstances;
    private final GraphDBQueries.Client graphDBQueries;
    private final CoreInstanceController instanceController;
    private final JsonLd.Client jsonLd;

    public CoreQueryController(GraphDBInstances.Client graphDBInstances, GraphDBQueries.Client graphDBQueries, CoreInstanceController instanceController, JsonLd.Client jsonLd) {
        this.graphDBInstances = graphDBInstances;
        this.graphDBQueries = graphDBQueries;
        this.instanceController = instanceController;
        this.jsonLd = jsonLd;
    }

    public ResponseEntity<Result<NormalizedJsonLd>> createNewQuery(NormalizedJsonLd query, UUID queryId, SpaceName space){
        return instanceController.createNewInstance(query, queryId, space, new ResponseConfiguration(), new IngestConfiguration().setNormalizePayload(false), null);
    }

    public ResponseEntity<Result<NormalizedJsonLd>> updateQuery(NormalizedJsonLd query, InstanceId instanceId){
        return instanceController.contributeToInstance(query, instanceId, false, new ResponseConfiguration(), new IngestConfiguration().setNormalizePayload(false), null);
    }

    public Paginated<NormalizedJsonLd> listQueries(String search, PaginationParam paginationParam){
        return graphDBInstances.getQueriesByType(DataStage.IN_PROGRESS, search, false, false, paginationParam,null);
    }

    public Paginated<NormalizedJsonLd> listQueriesPerRootType(String search, Type type, PaginationParam paginationParam){
        return graphDBInstances.getQueriesByType(DataStage.IN_PROGRESS, search, false, false, paginationParam, type.getName());
    }

    public KgQuery fetchQueryById(InstanceId instanceId, DataStage stage){
        if(instanceId!=null) {
            final NormalizedJsonLd queryById = graphDBInstances.getQueryById(instanceId.getSpace().getName(), instanceId.getUuid(), stage);
            if(queryById!=null){
                return new KgQuery(queryById, stage);
            }
        }
        return null;
    }

    public Paginated<? extends JsonLdDoc> executeQuery(KgQuery query, Map<String, String> params, PaginationParam paginationParam){
        QueryResult paginatedQueryResult = graphDBQueries.executeQuery(query, params, paginationParam);
        if(paginatedQueryResult!=null){
            if(paginatedQueryResult.getResponseVocab() != null){
                Paginated<NormalizedJsonLd> result = paginatedQueryResult.getResult();
                if(result!=null) {
                    List<NormalizedJsonLd> data = result.getData();
                    List<JsonLdDoc> dataWithAppliedContext = jsonLd.applyVocab(data, paginatedQueryResult.getResponseVocab());
                    return new Paginated<>(dataWithAppliedContext, result.getTotalResults(), result.getSize(), result.getFrom());
                }
            }
            return paginatedQueryResult.getResult();
        }
        return null;
    }

    public Set<InstanceId> deleteQuery(InstanceId instanceId){
        return instanceController.deleteInstance(instanceId, null);
    }
}

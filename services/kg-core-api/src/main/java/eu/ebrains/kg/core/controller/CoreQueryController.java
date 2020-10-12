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

package eu.ebrains.kg.core.controller;

import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.core.serviceCall.CoreInstancesToGraphDB;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
/**
 * The query controller contains the orchestration logic for the query operations
 */
@Component
public class CoreQueryController {

    private final CoreInstancesToGraphDB graphDB4InstancesSvc;

    private final CoreInstanceController instanceController;

    public CoreQueryController(CoreInstancesToGraphDB graphDB4InstancesSvc, CoreInstanceController instanceController) {
        this.graphDB4InstancesSvc = graphDB4InstancesSvc;
        this.instanceController = instanceController;
    }

    public ResponseEntity<Result<NormalizedJsonLd>> createNewQuery(NormalizedJsonLd query, UUID queryId, SpaceName space){
        return instanceController.createNewInstance(query, queryId, space, new ResponseConfiguration(), new IngestConfiguration().setNormalizePayload(false), null);
    }

    public ResponseEntity<Result<NormalizedJsonLd>> updateQuery(NormalizedJsonLd query, InstanceId instanceId){
        return instanceController.contributeToInstance(query, instanceId, false, new ResponseConfiguration(), new IngestConfiguration().setNormalizePayload(false), null);
    }

    public Paginated<NormalizedJsonLd> listQueries(String search, PaginationParam paginationParam){
        return graphDB4InstancesSvc.getQueriesByType(DataStage.IN_PROGRESS, paginationParam, search, false, null);
    }

    public Paginated<NormalizedJsonLd> listQueriesPerRootType(String search, Type type, PaginationParam paginationParam){
        return graphDB4InstancesSvc.getQueriesByType(DataStage.IN_PROGRESS, paginationParam, search, false, type);
    }

    public KgQuery fetchQueryById(UUID queryId, SpaceName space, DataStage stage){
        NormalizedJsonLd instance = graphDB4InstancesSvc.getInstance(DataStage.IN_PROGRESS, new InstanceId(queryId, space), true, false);
        return new KgQuery(instance, stage);
    }

    public Paginated<NormalizedJsonLd> executeQuery(KgQuery query, PaginationParam paginationParam){
        return graphDB4InstancesSvc.executeQuery(query, paginationParam);
    }

    public List<InstanceId> deleteQuery(InstanceId instanceId){
        return instanceController.deleteInstance(instanceId, null);
    }
}

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

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.core.controller.CoreQueryController;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.core.serviceCall.CoreToIds;
import eu.ebrains.kg.core.serviceCall.CoreToJsonLd;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * The query API allows to execute and manage queries on top of the EBRAINS KG. This is the main interface for reading clients.
 */
@RestController
@RequestMapping(Version.API+"/queries")
public class Queries {

    private final IdUtils idUtils;

    private final CoreQueryController queryController;

    private final CoreToJsonLd jsonLdSvc;

    private final CoreToIds idsSvc;

    public Queries(IdUtils idUtils, CoreQueryController queryController, CoreToJsonLd jsonLdSvc, CoreToIds idsSvc) {
        this.idUtils = idUtils;
        this.queryController = queryController;
        this.jsonLdSvc = jsonLdSvc;
        this.idsSvc = idsSvc;
    }

    @ApiOperation(value = "List the queries which have been registered for the given root type")
    @GetMapping
    public PaginatedResult<NormalizedJsonLd> listQueriesPerRootType(PaginationParam paginationParam, @RequestParam(value = "type", required = false) String rootType, @RequestParam(value = "search", required = false) String search) {
        if(rootType != null){
            return PaginatedResult.ok(queryController.listQueriesPerRootType(search, new Type(rootType), paginationParam));
        } else {
            return PaginatedResult.ok(queryController.listQueries(search, paginationParam));
        }
    }

    @ApiOperation(value = "Execute the query in the payload in test mode (e.g. for execution before saving with the KG QueryBuilder)")
    @PostMapping
    public PaginatedResult<NormalizedJsonLd> testQuery(@RequestBody JsonLdDoc query,  PaginationParam paginationParam, @RequestParam("stage") ExposedStage stage) {
        NormalizedJsonLd normalizedJsonLd = jsonLdSvc.toNormalizedJsonLd(query);
        return PaginatedResult.ok(queryController.executeQuery(new KgQuery(normalizedJsonLd, stage.getStage()), paginationParam));
    }

    @ApiOperation(value = "Get the query specification with the given query id in a specific space (note that query ids are unique per space only)")
    @GetMapping("/{queryId}")
    public Result<NormalizedJsonLd> getQuerySpecification(@PathVariable("queryId") UUID queryId, @RequestParam("space") String space) {
        KgQuery kgQuery = queryController.fetchQueryById(queryId, new Space(space), DataStage.IN_PROGRESS);
        return Result.ok(kgQuery.getPayload());
    }

    @ApiOperation(value = "TO BE IMPLEMENTED: Removes a query specification")
    @DeleteMapping("/{queryId}")
    public void removeQuery(@PathVariable("queryId") UUID queryId) {
//    queryController.deleteQuery()
    }

    @ApiOperation(value = "Save a query specification")
    @PutMapping("/{queryId}")
    public ResponseEntity<Result<NormalizedJsonLd>> saveQuery(@RequestBody JsonLdDoc query, @PathVariable(value = "queryId") UUID queryId, @RequestParam("space") String space) {
        NormalizedJsonLd normalizedJsonLd = jsonLdSvc.toNormalizedJsonLd(query);
        normalizedJsonLd.addTypes(KgQuery.getKgQueryType());
        Space querySpace = new Space(space);
        InstanceId resolveId = idsSvc.resolveId(DataStage.IN_PROGRESS, queryId);
        if(resolveId != null){
            return queryController.updateQuery(normalizedJsonLd, resolveId);
        }
        return queryController.createNewQuery(normalizedJsonLd, queryId, querySpace);
    }

    @ApiOperation(value = "Execute a stored query to receive the instances")
    @GetMapping("/{queryId}/instances")
    public PaginatedResult<NormalizedJsonLd> executeQueryById(@PathVariable("queryId") UUID queryId, PaginationParam paginationParam,@RequestParam("space") String space, @RequestParam("stage") ExposedStage stage) {
        KgQuery query = queryController.fetchQueryById(queryId, new Space(space), stage.getStage());
        return PaginatedResult.ok(queryController.executeQuery(query, paginationParam));
    }

    @ApiOperation(value = "TO BE IMPLEMENTED: Returns the meta information of a query specification")
    @GetMapping("/{queryId}/meta")
    public Result<NormalizedJsonLd> getMetaInformation(@PathVariable("queryId") String queryId) {
        return null;
    }

}

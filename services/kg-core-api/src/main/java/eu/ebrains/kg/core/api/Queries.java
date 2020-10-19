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

import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesData;
import eu.ebrains.kg.commons.markers.ExposesInputWithoutEnrichedSensitiveData;
import eu.ebrains.kg.commons.markers.ExposesQuery;
import eu.ebrains.kg.commons.markers.WritesData;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.core.controller.CoreQueryController;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.core.serviceCall.CoreToIds;
import eu.ebrains.kg.core.serviceCall.CoreToJsonLd;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * The query API allows to execute and manage queries on top of the EBRAINS KG. This is the main interface for reading clients.
 */
@RestController
@RequestMapping(Version.API+"/queries")
public class Queries {


    private final CoreQueryController queryController;

    private final CoreToJsonLd jsonLdSvc;

    private final CoreToIds idsSvc;

    public Queries(CoreQueryController queryController, CoreToJsonLd jsonLdSvc, CoreToIds idsSvc) {
        this.queryController = queryController;
        this.jsonLdSvc = jsonLdSvc;
        this.idsSvc = idsSvc;
    }

    @Operation(summary = "List the queries which have been registered for the given root type")
    @GetMapping
    @ExposesQuery
    public PaginatedResult<NormalizedJsonLd> listQueriesPerRootType(@ParameterObject PaginationParam paginationParam, @RequestParam(value = "type", required = false) String rootType, @RequestParam(value = "search", required = false) String search) {
        if(rootType != null){
            return PaginatedResult.ok(queryController.listQueriesPerRootType(search, new Type(rootType), paginationParam));
        } else {
            return PaginatedResult.ok(queryController.listQueries(search, paginationParam));
        }
    }

    @Operation(summary = "Execute the query in the payload in test mode (e.g. for execution before saving with the KG QueryBuilder)")
    @PostMapping
    @ExposesData
    public PaginatedResult<NormalizedJsonLd> testQuery(@RequestBody JsonLdDoc query, @ParameterObject PaginationParam paginationParam, @RequestParam("stage") ExposedStage stage) {
        NormalizedJsonLd normalizedJsonLd = jsonLdSvc.toNormalizedJsonLd(query);
        return PaginatedResult.ok(queryController.executeQuery(new KgQuery(normalizedJsonLd, stage.getStage()), paginationParam));
    }

    @Operation(summary = "Get the query specification with the given query id in a specific space (note that query ids are unique per space only)")
    @GetMapping("/{queryId}")
    @ExposesQuery
    public Result<NormalizedJsonLd> getQuerySpecification(@PathVariable("queryId") UUID queryId, @RequestParam("space") String space) {
        KgQuery kgQuery = queryController.fetchQueryById(queryId, new SpaceName(space), DataStage.IN_PROGRESS);
        return Result.ok(kgQuery.getPayload());
    }

    @Operation(summary = "Removes a query specification")
    @DeleteMapping("/{queryId}")
    @WritesData
    public void removeQuery(@PathVariable("queryId") UUID queryId, @RequestParam("space") String space) {
        queryController.deleteQuery(new InstanceId(queryId, new SpaceName(space)));
    }

    @Operation(summary = "Save a query specification")
    @PutMapping("/{queryId}")
    @WritesData
    @ExposesInputWithoutEnrichedSensitiveData
    public ResponseEntity<Result<NormalizedJsonLd>> saveQuery(@RequestBody JsonLdDoc query, @PathVariable(value = "queryId") UUID queryId, @RequestParam("space") String space) {
        NormalizedJsonLd normalizedJsonLd = jsonLdSvc.toNormalizedJsonLd(query);
        normalizedJsonLd.addTypes(KgQuery.getKgQueryType());
        SpaceName querySpace = new SpaceName(space);
        InstanceId resolveId = idsSvc.resolveId(DataStage.IN_PROGRESS, queryId);
        if(resolveId != null){
            return queryController.updateQuery(normalizedJsonLd, resolveId);
        }
        return queryController.createNewQuery(normalizedJsonLd, queryId, querySpace);
    }

    @Operation(summary = "Execute a stored query to receive the instances")
    @GetMapping("/{queryId}/instances")
    @ExposesData
    public PaginatedResult<NormalizedJsonLd> executeQueryById(@PathVariable("queryId") UUID queryId, @ParameterObject PaginationParam paginationParam,@RequestParam("space") String space, @RequestParam("stage") ExposedStage stage) {
        KgQuery query = queryController.fetchQueryById(queryId, new SpaceName(space), stage.getStage());
        return PaginatedResult.ok(queryController.executeQuery(query, paginationParam));
    }

    @Operation(summary = "TO BE IMPLEMENTED: Returns the meta information of a query specification")
    @GetMapping("/{queryId}/meta")
    @ExposesData
    public Result<NormalizedJsonLd> getMetaInformation(@PathVariable("queryId") String queryId) {
        return null;
    }

}

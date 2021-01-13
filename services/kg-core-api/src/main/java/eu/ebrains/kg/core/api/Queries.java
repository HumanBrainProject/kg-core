/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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
import eu.ebrains.kg.commons.config.openApiGroups.Simple;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesData;
import eu.ebrains.kg.commons.markers.ExposesInputWithoutEnrichedSensitiveData;
import eu.ebrains.kg.commons.markers.ExposesQuery;
import eu.ebrains.kg.commons.markers.WritesData;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.controller.CoreQueryController;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.core.serviceCall.CoreToIds;
import eu.ebrains.kg.core.serviceCall.CoreToJsonLd;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * The query API allows to execute and manage queries on top of the EBRAINS KG. This is the main interface for reading clients.
 */
@RestController
@RequestMapping(Version.API+"/queries")
@Simple
public class Queries {

    private final CoreQueryController queryController;

    private final CoreToJsonLd jsonLdSvc;

    private final CoreToIds idsSvc;

    public Queries(CoreQueryController queryController, CoreToJsonLd jsonLdSvc, CoreToIds idsSvc) {
        this.queryController = queryController;
        this.jsonLdSvc = jsonLdSvc;
        this.idsSvc = idsSvc;
    }

    @Operation(summary = "List the queries and filter them by root type and/or text in the label, name or description")
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

    @Operation(summary = "Get the query specification with the given query id in a specific space")
    @GetMapping("/{queryId}")
    @ExposesQuery
    public Result<NormalizedJsonLd> getQuerySpecification(@PathVariable("queryId") UUID queryId) {
        InstanceId instanceId = idsSvc.resolveId(DataStage.IN_PROGRESS, queryId);
        KgQuery kgQuery = queryController.fetchQueryById(instanceId, DataStage.IN_PROGRESS);
        return Result.ok(kgQuery.getPayload());
    }

    @Operation(summary = "Remove a query specification")
    @DeleteMapping("/{queryId}")
    @WritesData
    public ResponseEntity<Void> removeQuery(@PathVariable("queryId") UUID queryId) {
        InstanceId resolveId = idsSvc.resolveId(DataStage.IN_PROGRESS, queryId);
        if(resolveId!=null) {
            List<InstanceId> instanceIds = queryController.deleteQuery(resolveId);
            if(instanceIds.isEmpty()){
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok().build();
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Create or save a query specification")
    @PutMapping("/{queryId}")
    @WritesData
    @ExposesInputWithoutEnrichedSensitiveData
    public ResponseEntity<Result<NormalizedJsonLd>> saveQuery(@RequestBody JsonLdDoc query, @PathVariable(value = "queryId") UUID queryId, @RequestParam(value = "space", required = false) @Parameter(description = "Required only when the instance is created - but not if it's updated.") String space) {
        NormalizedJsonLd normalizedJsonLd = jsonLdSvc.toNormalizedJsonLd(query);
        normalizedJsonLd.addTypes(EBRAINSVocabulary.META_QUERY_TYPE);
        InstanceId resolveId = idsSvc.resolveId(DataStage.IN_PROGRESS, queryId);
        if(resolveId != null){
            if(space!=null && !resolveId.getSpace().equals(new SpaceName(space))){
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.nok(HttpStatus.CONFLICT.value(), "The query with this UUID already exists in a different space"));
            }
            return queryController.updateQuery(normalizedJsonLd, resolveId);
        }
        if(space==null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.nok(HttpStatus.BAD_REQUEST.value(), "The query with this UUID doesn't exist yet. You therefore need to specify the space where it should be stored."));
        }
        return queryController.createNewQuery(normalizedJsonLd, queryId, new SpaceName(space));
    }

    @Operation(summary = "Execute a stored query to receive the instances")
    @GetMapping("/{queryId}/instances")
    @ExposesData
    public PaginatedResult<NormalizedJsonLd> executeQueryById(@PathVariable("queryId") UUID queryId, @ParameterObject PaginationParam paginationParam, @RequestParam("stage") ExposedStage stage) {
        InstanceId instanceId = idsSvc.resolveId(DataStage.IN_PROGRESS, queryId);
        KgQuery query = queryController.fetchQueryById(instanceId, stage.getStage());
        return PaginatedResult.ok(queryController.executeQuery(query, paginationParam));
    }

}

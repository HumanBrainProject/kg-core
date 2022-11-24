/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

package eu.ebrains.kg.core.api.v3beta;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.JsonLd;
import eu.ebrains.kg.commons.config.openApiGroups.Simple;
import eu.ebrains.kg.commons.exception.InstanceNotFoundException;
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
import eu.ebrains.kg.core.controller.IdsController;
import eu.ebrains.kg.core.model.ExposedStage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The query API allows to execute and manage queries on top of the EBRAINS KG. This is the main interface for reading clients.
 */
@RestController
@RequestMapping(Version.V3_BETA +"/queries")
@Simple
public class QueriesV3Beta {

    private final CoreQueryController queryController;

    private final AuthContext authContext;

    private final JsonLd.Client jsonLd;

    private final IdsController ids;

    public QueriesV3Beta(CoreQueryController queryController, AuthContext authContext, JsonLd.Client jsonLd, IdsController ids) {
        this.queryController = queryController;
        this.authContext = authContext;
        this.jsonLd = jsonLd;
        this.ids = ids;
    }

    @Operation(summary = "List the queries and filter them by root type and/or text in the label, name or description")
    @GetMapping
    @ExposesQuery
    public PaginatedResult<NormalizedJsonLd> listQueriesPerRootType(@ParameterObject PaginationParam paginationParam, @RequestParam(value = "type", required = false) String rootType, @RequestParam(value = "search", required = false) String search) {
        Paginated<NormalizedJsonLd> data;
        if(rootType != null){
            data = queryController.listQueriesPerRootType(search, new Type(rootType), paginationParam);
        } else {
            data =queryController.listQueries(search, paginationParam);
        }
        handleResponse(data);
        return PaginatedResult.ok(data);
    }

    private void handleResponse(Paginated<NormalizedJsonLd> data){
        final SpaceName privateSpace = authContext.getUserWithRoles().getPrivateSpace();
        data.getData().forEach(d -> d.renameSpace(privateSpace, queryController.isInvited(d)));
    }


    @Operation(summary = "Execute the query in the payload (e.g. for execution before saving with the KG QueryBuilder)")
    @PostMapping
    @ExposesData
    public PaginatedStreamResult<? extends JsonLdDoc> runDynamicQuery(@RequestBody JsonLdDoc query, @ParameterObject PaginationParam paginationParam, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "instanceId", required = false) UUID instanceId, @RequestParam(value = "restrictToSpaces", required = false) List<String> restrictToSpaces, @RequestParam(defaultValue = "{}") Map<String, String> allRequestParams) {
        //Remove the non-dynamic parameters from the map
        allRequestParams.remove("stage");
        allRequestParams.remove("instanceId");
        allRequestParams.remove("from");
        allRequestParams.remove("size");
        NormalizedJsonLd normalizedJsonLd = jsonLd.normalize(query, true);
        KgQuery q = new KgQuery(normalizedJsonLd, stage.getStage());
        if(instanceId!=null){
            q.setIdRestrictions(Collections.singletonList(instanceId));
        }
        if(restrictToSpaces!=null){
            q.setRestrictToSpaces(restrictToSpaces.stream().filter(Objects::nonNull).map(r -> SpaceName.getInternalSpaceName(r, authContext.getUserWithRoles().getPrivateSpace())).collect(Collectors.toList()));
        }
        Date startTime = new Date();
        final PaginatedStream<? extends JsonLdDoc> data = queryController.executeQuery(q, allRequestParams, paginationParam);
        PaginatedStreamResult<? extends JsonLdDoc> result = PaginatedStreamResult.ok(data);
        result.setExecutionDetails(startTime, new Date());
        return result;
    }


    @Operation(summary = "Get the query specification with the given query id in a specific space")
    @GetMapping("/{queryId}")
    @ExposesQuery
    public ResponseEntity<Result<NormalizedJsonLd>> getQuerySpecification(@PathVariable("queryId") UUID queryId) {
        InstanceId instanceId = ids.resolveId(DataStage.IN_PROGRESS, queryId);
        if(instanceId != null) {
            NormalizedJsonLd kgQuery = queryController.fetchQueryById(instanceId);
            if(kgQuery == null){
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Result.ok(kgQuery.renameSpace(authContext.getUserWithRoles().getPrivateSpace(), queryController.isInvited(kgQuery))));
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Remove a query specification")
    @DeleteMapping("/{queryId}")
    @WritesData
    public ResponseEntity<Void> removeQuery(@PathVariable("queryId") UUID queryId) {
        InstanceId resolveId = ids.resolveId(DataStage.IN_PROGRESS, queryId);
        if(resolveId!=null) {
            queryController.deleteQuery(resolveId);
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
    public ResponseEntity<Result<NormalizedJsonLd>> saveQuery(@RequestBody JsonLdDoc query, @PathVariable(value = "queryId") UUID queryId, @RequestParam(value = "space", required = false) @Parameter(description = "Required only when the instance is created to specify where it should be stored ("+SpaceName.PRIVATE_SPACE+" for your private space) - but not if it's updated.") String space) {
        NormalizedJsonLd normalizedJsonLd = jsonLd.normalize(query, true);
        normalizedJsonLd.addTypes(EBRAINSVocabulary.META_QUERY_TYPE);
        InstanceId resolveId = ids.resolveId(DataStage.IN_PROGRESS, queryId);
        SpaceName spaceName = authContext.resolveSpaceName(space);
        if(resolveId != null){
            if(spaceName!=null && !resolveId.getSpace().equals(spaceName)){
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.nok(HttpStatus.CONFLICT.value(), "The query with this UUID already exists in a different space", resolveId.getUuid()));
            }
            final ResponseEntity<Result<NormalizedJsonLd>> result = queryController.updateQuery(normalizedJsonLd, resolveId);
            if(result != null) {
                final Result<NormalizedJsonLd> body = result.getBody();
                if(body!=null) {
                    final NormalizedJsonLd data = body.getData();
                    if (data != null) {
                        data.renameSpace(authContext.getUserWithRoles().getPrivateSpace(), queryController.isInvited(data));
                    }
                }
            }
            return result;
        }
        if(spaceName==null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.nok(HttpStatus.BAD_REQUEST.value(), "The query with this UUID doesn't exist yet. You therefore need to specify the space where it should be stored."));
        }
        final ResponseEntity<Result<NormalizedJsonLd>> result = queryController.createNewQuery(normalizedJsonLd, queryId, spaceName);
        if(result != null) {
            final Result<NormalizedJsonLd> body = result.getBody();
            if (body != null) {
                final NormalizedJsonLd data = body.getData();
                if (data != null) {
                    data.renameSpace(authContext.getUserWithRoles().getPrivateSpace(), queryController.isInvited(data));
                }
            }
        }
        return result;
    }

    @Operation(summary = "Execute a stored query to receive the instances")
    @GetMapping("/{queryId}/instances")
    @ExposesData
    public PaginatedStreamResult<? extends JsonLdDoc> executeQueryById(@PathVariable("queryId") UUID queryId, @ParameterObject PaginationParam paginationParam, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "instanceId", required = false) UUID instanceId, @RequestParam(value = "restrictToSpaces", required = false) List<String> restrictToSpaces, @RequestParam(defaultValue = "{}") Map<String, String> allRequestParams) {
        //Remove the non-dynamic parameters from the map
        allRequestParams.remove("stage");
        allRequestParams.remove("instanceId");
        allRequestParams.remove("from");
        allRequestParams.remove("size");
        InstanceId queryInstance = ids.resolveId(DataStage.IN_PROGRESS, queryId);
        final NormalizedJsonLd queryPayload = queryController.fetchQueryById(queryInstance);
        if(queryPayload==null){
            throw new InstanceNotFoundException(String.format("Query with id %s not found", queryId));
        }
        KgQuery query = new KgQuery(queryPayload, stage.getStage());
        if(instanceId!=null){
            query.setIdRestrictions(Collections.singletonList(instanceId));
        }
        if(restrictToSpaces!=null){
            query.setRestrictToSpaces(restrictToSpaces.stream().filter(Objects::nonNull).map(r -> SpaceName.getInternalSpaceName(r, authContext.getUserWithRoles().getPrivateSpace())).collect(Collectors.toList()));
        }
        Date startTime = new Date();
        final PaginatedStream<? extends JsonLdDoc> data = queryController.executeQuery(query, allRequestParams, paginationParam);
        PaginatedStreamResult<? extends JsonLdDoc> result = PaginatedStreamResult.ok(data);
        result.setExecutionDetails(startTime, new Date());
        return result;
    }

}

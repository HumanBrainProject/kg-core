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
import eu.ebrains.kg.commons.models.ExternalEventInformation;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.core.controller.CoreInstanceController;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.core.serviceCall.CoreToIds;
import eu.ebrains.kg.core.serviceCall.CoreToRelease;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The instance API manages the CCRUD (Create, Contribute, Read, Update, Delete) operations for individual entity representations
 */
@RestController
@RequestMapping(Version.API)
public class Instances {

    private final CoreToIds idsSvc;
    private final CoreInstanceController instanceController;
    private final CoreToRelease releaseSvc;
    private final IdUtils idUtils;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Instances(CoreToIds idsSvc, CoreInstanceController instanceController, CoreToRelease releaseSvc, IdUtils idUtils) {
        this.idsSvc = idsSvc;
        this.instanceController = instanceController;
        this.releaseSvc = releaseSvc;
        this.idUtils = idUtils;
    }

    @ApiOperation(value = "Create new instance with a system generated id")
    @PostMapping("/instances")
    public ResponseEntity<Result<NormalizedJsonLd>> createNewInstance(@RequestBody JsonLdDoc jsonLdDoc, @RequestParam(value = "space") String space, ResponseConfiguration responseConfiguration, IngestConfiguration ingestConfiguration, ExternalEventInformation externalEventInformation) {
        Date startTime = new Date();
        UUID id = UUID.randomUUID();
        logger.debug(String.format("Creating new instance with id %s", id));
        ResponseEntity<Result<NormalizedJsonLd>> newInstance = instanceController.createNewInstance(jsonLdDoc, id, space, responseConfiguration, ingestConfiguration, externalEventInformation);
        logger.debug(String.format("Done creating new instance with id %s", id));
        if (ingestConfiguration.isDeferInference()) {
            NormalizedJsonLd idPayload = new NormalizedJsonLd();
            idPayload.setId(idUtils.buildAbsoluteUrl(id));
            newInstance = ResponseEntity.ok(Result.ok(idPayload));
        }
        if (newInstance.getBody() != null) {
            newInstance.getBody().setExecutionDetails(startTime, new Date());
        }
        return newInstance;
    }

    @ApiOperation(value = "Create new instance with a client defined id")
    @PostMapping("/instances/{id}")
    public ResponseEntity<Result<NormalizedJsonLd>> createNewInstance(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @RequestParam(value = "space") String space, ResponseConfiguration responseConfiguration, IngestConfiguration ingestConfiguration, ExternalEventInformation externalEventInformation) {
        Date startTime = new Date();
        //We want to prevent the UUID to be used twice...
        InstanceId instanceId = idsSvc.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.nok(HttpStatus.CONFLICT.value(), String.format("The uuid you're providing (%s) is already in use. Please use a different one or do a PATCH instead", id)));
        }
        logger.debug(String.format("Creating new instance with id %s", id));
        ResponseEntity<Result<NormalizedJsonLd>> newInstance = instanceController.createNewInstance(jsonLdDoc, id, space, responseConfiguration, ingestConfiguration, externalEventInformation);
        logger.debug(String.format("Done creating new instance with id %s", id));
        if(newInstance.getBody()!=null){
            newInstance.getBody().setExecutionDetails(startTime, new Date());
        }
        return newInstance;
    }

    private ResponseEntity<Result<NormalizedJsonLd>> contributeToInstance(JsonLdDoc jsonLdDoc, UUID id, boolean undeprecate, ResponseConfiguration responseConfiguration, IngestConfiguration ingestConfiguration, ExternalEventInformation externalEventInformation, boolean removeNonDeclaredFields) {
        Date startTime = new Date();
        logger.debug(String.format("Contributing to instance with id %s", id));
        InstanceId instanceId = idsSvc.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        } else if (instanceId.isDeprecated()) {
            if (undeprecate) {
                idsSvc.undeprecateInstance(instanceId.getUuid());
            } else {
                return ResponseEntity.status(HttpStatus.GONE).body(Result.nok(HttpStatus.GONE.value(), "The instance you're trying to contribute to has been deprecated."));
            }
        }
        ResponseEntity<Result<NormalizedJsonLd>> resultResponseEntity = instanceController.contributeToInstance(jsonLdDoc, instanceId, removeNonDeclaredFields, responseConfiguration, ingestConfiguration, externalEventInformation);
        logger.debug(String.format("Done contributing to instance with id %s", id));
        resultResponseEntity.getBody().setExecutionDetails(startTime, new Date());
        return resultResponseEntity;
    }

    @ApiOperation(value = "Replace contribution to an existing instance")
    @PutMapping("/instances/{id}")
    public ResponseEntity<Result<NormalizedJsonLd>> contributeToInstanceFullReplacement(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @RequestParam(value = "undeprecate", required = false, defaultValue = "false") boolean undeprecate, ResponseConfiguration responseConfiguration, IngestConfiguration ingestConfiguration, ExternalEventInformation externalEventInformation) {
        return contributeToInstance(jsonLdDoc, id, undeprecate, responseConfiguration, ingestConfiguration, externalEventInformation, true);
    }

    @ApiOperation(value = "Partially update contribution to an existing instance")
    @PatchMapping("/instances/{id}")
    public ResponseEntity<Result<NormalizedJsonLd>> contributeToInstancePartialReplacement(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @RequestParam(value = "undeprecate", required = false, defaultValue = "false") boolean undeprecate, ResponseConfiguration responseConfiguration, IngestConfiguration ingestConfiguration, ExternalEventInformation externalEventInformation) {
        return contributeToInstance(jsonLdDoc, id, undeprecate, responseConfiguration, ingestConfiguration, externalEventInformation, false);
    }

    @ApiOperation(value = "Get the instance by its KG-internal ID")
    @GetMapping("/instances/{id}")
    public ResponseEntity<Result<NormalizedJsonLd>> getInstanceById(@PathVariable("id") UUID id, @RequestParam("stage") ExposedStage stage, ResponseConfiguration responseConfiguration) {
        Date startTime = new Date();
        NormalizedJsonLd instanceById = instanceController.getInstanceById(id, stage.getStage(), responseConfiguration);
        return instanceById != null ? ResponseEntity.ok(Result.ok(instanceById).setExecutionDetails(startTime, new Date())) : ResponseEntity.notFound().build();
    }

    @ApiOperation(value = "Get the scope for the instance by its KG-internal ID")
    @GetMapping("/instances/{id}/scope")
    public ResponseEntity<Result<ScopeElement>> getInstanceScope(@PathVariable("id") UUID id, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "returnPermissions", required = false, defaultValue = "false") boolean returnPermissions) {
        Date startTime = new Date();
        ScopeElement scope = instanceController.getScopeForInstance(id, stage.getStage(), returnPermissions);
        return scope != null ? ResponseEntity.ok(Result.ok(scope).setExecutionDetails(startTime, new Date())) : ResponseEntity.notFound().build();
    }

    @ApiOperation(value = "Get the neighborhood for the instance by its KG-internal ID")
    @GetMapping("/instances/{id}/neighbors")
    public ResponseEntity<Result<GraphEntity>> getNeighbors(@PathVariable("id") UUID id, @RequestParam("stage") ExposedStage stage) {
        Date startTime = new Date();
        GraphEntity scope = instanceController.getNeighbors(id, stage.getStage());
        return scope != null ? ResponseEntity.ok(Result.ok(scope).setExecutionDetails(startTime, new Date())) : ResponseEntity.notFound().build();
    }


    @ApiOperation(value = "Returns a list of instances according to their types")
    @GetMapping("/instances")
    public PaginatedResult<NormalizedJsonLd> getInstances(@RequestParam("stage") ExposedStage stage, @RequestParam("type") String type, @RequestParam(value = "searchByLabel", required = false) String searchByLabel, ResponseConfiguration responseConfiguration, PaginationParam paginationParam) {
        Date startTime = new Date();
        PaginatedResult<NormalizedJsonLd> result = PaginatedResult.ok(instanceController.getInstances(stage.getStage(), new Type(type), searchByLabel, responseConfiguration, paginationParam));
        result.setExecutionDetails(startTime, new Date());
        return result;
    }

    @ApiOperation(value = "Bulk operation of /instances/{id} to read instances by their KG-internal IDs")
    @PostMapping("/instancesByIds")
    public Result<Map<String, Result<NormalizedJsonLd>>> getInstancesByIds(@RequestBody List<String> ids, @RequestParam("stage") ExposedStage stage, ResponseConfiguration responseConfiguration) {
        Date startTime = new Date();
        return Result.ok(instanceController.getInstancesByIds(ids, stage.getStage(), responseConfiguration)).setExecutionDetails(startTime, new Date());
    }


    @ApiOperation(value = "ATTENTION: The result structure will be subject to change! \nRead instances by the given list of (external) identifiers")
    @PostMapping("/instancesByIdentifiers")
    @Deprecated()
    //TODO change return structure to maintain the mapping to the identifier (as with "getInstancesByIds")
    public Result<List<NormalizedJsonLd>> getInstancesByIdentifiers(@RequestBody List<String> identifiers, @RequestParam("stage") ExposedStage stage, ResponseConfiguration responseConfiguration) {
        Date startTime = new Date();
        IdWithAlternatives idWithAlternative = new IdWithAlternatives(UUID.randomUUID(), null, new HashSet<>(identifiers));
        List<InstanceId> instanceIds = idsSvc.resolveIds(stage.getStage(), idWithAlternative, false);
        return Result.ok(instanceController.getInstancesByIds(instanceIds.stream().filter(instanceId -> !instanceId.isDeprecated()).map(i -> i.getUuid().toString()).collect(Collectors.toList()), stage.getStage(), responseConfiguration).values().stream().map(Result::getData).collect(Collectors.toList())).setExecutionDetails(startTime, new Date());
    }

    @ApiOperation(value = "Deprecate an instance")
    @DeleteMapping("/instances/{id}")
    public ResponseEntity<Result<Void>> deleteInstance(@PathVariable("id") UUID id, ExternalEventInformation externalEventInformation) {
        Date startTime = new Date();
        InstanceId instanceId = idsSvc.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        } else {
            ReleaseStatus releaseStatus = releaseSvc.getReleaseStatus(instanceId, ReleaseTreeScope.TOP_INSTANCE_ONLY);
            if (releaseStatus != null && releaseStatus != ReleaseStatus.UNRELEASED) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.nok(HttpStatus.CONFLICT.value(), "Was not able to remove instance because it is released still"));
            }
            instanceController.deleteInstance(instanceId, externalEventInformation);
            return ResponseEntity.ok(Result.<Void>ok().setExecutionDetails(startTime, new Date()));
        }
    }


    //RELEASE instances
    @ApiOperation("Release or re-release an instance")
    @PutMapping("/instances/{id}/release")
    public ResponseEntity<Result<Void>> releaseInstance(@PathVariable("id") UUID id, @RequestParam(value = "revision", required = false) String revision) {
        Date startTime = new Date();
        InstanceId instanceId = idsSvc.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        }
        else if(instanceId.isDeprecated()){
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        releaseSvc.releaseInstance(instanceId, revision);
        return ResponseEntity.ok(Result.<Void>ok().setExecutionDetails(startTime, new Date()));
    }

    @ApiOperation(value = "Unrelease an instance")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The instance that has been unreleased"), @ApiResponse(code = 404, message = "Instance not found")})
    @DeleteMapping("/instances/{id}/release")
    public ResponseEntity<Result<Void>> unreleaseInstance(@PathVariable("id") UUID id) {
        Date startTime = new Date();
        InstanceId instanceId = idsSvc.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        }
        else if(instanceId.isDeprecated()){
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        releaseSvc.unreleaseInstance(instanceId);
        return ResponseEntity.ok(Result.<Void>ok().setExecutionDetails(startTime, new Date()));
    }

    @ApiOperation(value = "Get the release status for an instance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The release status of the instance"),
            @ApiResponse(code = 404, message = "Instance not found")})
    @GetMapping(value = "/instances/{id}/release/status")
    public ResponseEntity<Result<ReleaseStatus>> getReleaseStatus(@PathVariable("id") UUID id, @RequestParam("releaseTreeScope") ReleaseTreeScope releaseTreeScope) {
        InstanceId instanceId = idsSvc.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        }
        else if(instanceId.isDeprecated()){
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        ReleaseStatus releaseStatus = releaseSvc.getReleaseStatus(instanceId, releaseTreeScope);
        return ResponseEntity.ok(Result.ok(releaseStatus));
    }

    @ApiOperation(value = "Get the release status for multiple instances")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The release status of the instance"),
            @ApiResponse(code = 404, message = "Instance not found")})
    @PostMapping(value = "/instancesByIds/release/status")
    public Result<Map<UUID, Result<ReleaseStatus>>> getReleasesStatusByIds(@RequestBody List<UUID> listOfIds, @RequestParam("releaseTreeScope") ReleaseTreeScope releaseTreeScope) {
        List<InstanceId> instanceIds = idsSvc.resolveIdsByUUID(DataStage.IN_PROGRESS, listOfIds, false);
        return Result.ok(instanceIds.stream().filter(instanceId -> !instanceId.isDeprecated()).collect(Collectors.toMap(InstanceId::getUuid, instanceId ->
                Result.ok(releaseSvc.getReleaseStatus(instanceId, releaseTreeScope))
        )));
    }
}

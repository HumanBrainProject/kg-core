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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    public ResponseEntity<Result<NormalizedJsonLd>> createNewInstance(@RequestBody JsonLdDoc jsonLdDoc, @RequestParam(value = "space") String space, @RequestParam(value = "returnPayload", required = false, defaultValue = "true") boolean returnPayload, @RequestParam(value = "returnPermissions", required = false, defaultValue = "false") boolean returnPermissions, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "true") boolean returnEmbedded,  @RequestParam(value = "deferInference", required = false, defaultValue = "false") boolean deferInference, ExternalEventInformation externalEventInformation) {
        UUID id = UUID.randomUUID();
        logger.debug(String.format("Creating new instance with id %s", id));
        ResponseEntity<Result<NormalizedJsonLd>> newInstance = instanceController.createNewInstance(jsonLdDoc, id, space, returnPayload, returnPermissions, returnAlternatives, returnEmbedded, deferInference, externalEventInformation);
        logger.debug(String.format("Done creating new instance with id %s", id));
        if(deferInference){
            NormalizedJsonLd idPayload = new NormalizedJsonLd();
            idPayload.setId(idUtils.buildAbsoluteUrl(id));
            return ResponseEntity.ok(Result.ok(idPayload));
        }
        return newInstance;
    }

    @ApiOperation(value = "Create new instance with a client defined id")
    @PostMapping("/instances/{id}")
    public ResponseEntity<Result<NormalizedJsonLd>> createNewInstance(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @RequestParam(value = "space") String space, @RequestParam(value = "returnPayload", required = false, defaultValue = "true") boolean returnPayload, @RequestParam(value = "returnPermissions", required = false, defaultValue = "false") boolean returnPermissions, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "true") boolean returnEmbedded,  @RequestParam(value = "deferInference", required = false, defaultValue = "false") boolean deferInference, ExternalEventInformation externalEventInformation) {
        //We want to prevent the UUID to be used twice...
        InstanceId instanceId = idsSvc.resolveId(DataStage.IN_PROGRESS, id);
        if(instanceId!=null){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.nok(HttpStatus.CONFLICT.value(), String.format("The uuid you're providing (%s) is already in use. Please use a different one or do a PATCH instead", id)));
        }
        logger.debug(String.format("Creating new instance with id %s", id));
        ResponseEntity<Result<NormalizedJsonLd>> newInstance = instanceController.createNewInstance(jsonLdDoc, id, space, returnPayload, returnPermissions, returnAlternatives, returnEmbedded, deferInference, externalEventInformation);
        logger.debug(String.format("Done creating new instance with id %s", id));
        return newInstance;
    }

    private ResponseEntity<Result<NormalizedJsonLd>> contributeToInstance(JsonLdDoc jsonLdDoc, UUID id, boolean undeprecate,  boolean returnPayload, boolean returnPermissions, boolean returnAlternatives, boolean returnEmbedded, boolean deferInference, ExternalEventInformation externalEventInformation, boolean removeNonDeclaredFields) {
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
        ResponseEntity<Result<NormalizedJsonLd>> resultResponseEntity = instanceController.contributeToInstance(jsonLdDoc, instanceId, removeNonDeclaredFields, returnPayload, returnPermissions, returnAlternatives, returnEmbedded, deferInference, externalEventInformation);
        logger.debug(String.format("Done contributing to instance with id %s", id));
        return resultResponseEntity;
    }

    @ApiOperation(value = "Replace contribution to an existing instance")
    @PutMapping("/instances/{id}")
    public ResponseEntity<Result<NormalizedJsonLd>> contributeToInstanceFullReplacement(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @RequestParam(value = "undeprecate", required = false, defaultValue = "false") boolean undeprecate, @RequestParam(value = "returnPayload", required = false, defaultValue = "true") boolean returnPayload, @RequestParam(value = "returnPermissions", required = false, defaultValue = "false") boolean returnPermissions, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "true") boolean returnEmbedded,  @RequestParam(value = "deferInference", required = false, defaultValue = "false") boolean deferInference, ExternalEventInformation externalEventInformation) {
        return contributeToInstance(jsonLdDoc, id, undeprecate, returnPayload, returnPermissions, returnAlternatives, returnEmbedded, deferInference, externalEventInformation, true);
    }

    @ApiOperation(value = "Partially update contribution to an existing instance")
    @PatchMapping("/instances/{id}")
    public ResponseEntity<Result<NormalizedJsonLd>> contributeToInstancePartialReplacement(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @RequestParam(value = "undeprecate", required = false, defaultValue = "false") boolean undeprecate, @RequestParam(value = "returnPayload", required = false, defaultValue = "true") boolean returnPayload, @RequestParam(value = "returnPermissions", required = false, defaultValue = "false") boolean returnPermissions, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "true") boolean returnEmbedded,  @RequestParam(value = "deferInference", required = false, defaultValue = "false") boolean deferInference, ExternalEventInformation externalEventInformation) {
        return contributeToInstance(jsonLdDoc, id, undeprecate, returnPayload, returnPermissions, returnAlternatives, returnEmbedded, deferInference, externalEventInformation, false);
    }

    @ApiOperation(value = "Get the instance by its KG-internal ID")
    @GetMapping("/instances/{id}")
    public ResponseEntity<Result<NormalizedJsonLd>> getInstanceById(@PathVariable("id") UUID id, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "returnEmbedded", defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnPermissions", defaultValue = "false") boolean returnPermissions) {
        NormalizedJsonLd instanceById = instanceController.getInstanceById(id, stage.getStage(), returnEmbedded, returnAlternatives, returnPermissions);
        return instanceById != null ? ResponseEntity.ok(Result.ok(instanceById)) : ResponseEntity.notFound().build();
    }

    @ApiOperation(value = "Returns a list of instances according to their types")
    @GetMapping("/instances")
    public PaginatedResult<NormalizedJsonLd> getInstances(@RequestParam("stage") ExposedStage stage, @RequestParam("type") String type, @RequestParam(value = "searchByLabel", required = false) String searchByLabel, @RequestParam(value = "returnEmbedded", defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnPermissions", defaultValue = "false") boolean returnPermissions, PaginationParam paginationParam) {
        return PaginatedResult.ok(instanceController.getInstances(stage.getStage(), new Type(type), searchByLabel, returnEmbedded, returnAlternatives, returnPermissions, paginationParam));
    }

    @ApiOperation(value = "Bulk operation of /instances/{id} to read instances by their KG-internal IDs")
    @PostMapping("/instancesByIds")
    public Result<Map<UUID, Result<NormalizedJsonLd>>> getInstancesByIds(@RequestBody List<UUID> ids, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "returnEmbedded", defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnPermissions", defaultValue = "false") boolean returnPermissions) {
        return Result.ok(instanceController.getInstancesByIds(ids, stage.getStage(), returnEmbedded, returnAlternatives, returnPermissions));
    }


    @ApiOperation(value = "Read instances by the given list of (external) identifiers")
    @PostMapping("/instancesByIdentifiers")
    public Result<List<NormalizedJsonLd>> getInstancesByIdentifiers(@RequestBody List<String> identifiers, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "returnEmbedded", defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnPermissions", defaultValue = "false") boolean returnPermissions) {
        IdWithAlternatives idWithAlternative = new IdWithAlternatives(UUID.randomUUID(), null, new HashSet<>(identifiers));
        List<InstanceId> instanceIds = idsSvc.resolveIds(stage.getStage(), idWithAlternative, false);
        return Result.ok(instanceController.getInstancesByIds(instanceIds.stream().filter(instanceId -> !instanceId.isDeprecated()).map(InstanceId::getUuid).collect(Collectors.toList()), stage.getStage(), returnEmbedded, returnAlternatives, returnPermissions).values().stream().map(Result::getData).collect(Collectors.toList()));
    }

    @ApiOperation(value = "Deprecate an instance")
    @DeleteMapping("/instances/{id}")
    public ResponseEntity<Result<Void>> deleteInstance(@PathVariable("id") UUID id, ExternalEventInformation externalEventInformation) {
        InstanceId instanceId = idsSvc.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        } else {
            ReleaseStatus releaseStatus = releaseSvc.getReleaseStatus(instanceId, ReleaseTreeScope.TOP_INSTANCE_ONLY);
            if (releaseStatus != null && releaseStatus != ReleaseStatus.UNRELEASED) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.nok(HttpStatus.CONFLICT.value(), "Was not able to remove instance because it is released still"));
            }
            instanceController.deleteInstance(instanceId, externalEventInformation);
            return ResponseEntity.status(HttpStatus.OK).build();
        }
    }
}

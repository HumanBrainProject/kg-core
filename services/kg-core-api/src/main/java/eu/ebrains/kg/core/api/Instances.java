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

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.GraphDBInstances;
import eu.ebrains.kg.commons.api.Release;
import eu.ebrains.kg.commons.config.openApiGroups.Advanced;
import eu.ebrains.kg.commons.config.openApiGroups.Simple;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.markers.*;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.ExternalEventInformation;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.core.controller.CoreInstanceController;
import eu.ebrains.kg.core.controller.IdsController;
import eu.ebrains.kg.core.model.ExposedStage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The instance API manages the CCRUD (Create, Contribute, Read, Update, Delete) operations for individual entity representations
 */
@RestController
@RequestMapping(Version.API)
public class Instances {
    private final CoreInstanceController instanceController;
    private final Release.Client release;
    private final IdUtils idUtils;
    private final AuthContext authContext;
    private final GraphDBInstances.Client graphDBInstances;
    private final IdsController idsController;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Instances(CoreInstanceController instanceController, Release.Client release, IdUtils idUtils, AuthContext authContext, GraphDBInstances.Client graphDBInstances, IdsController idsController) {
        this.instanceController = instanceController;
        this.release = release;
        this.idUtils = idUtils;
        this.authContext = authContext;
        this.graphDBInstances = graphDBInstances;
        this.idsController = idsController;
    }

    @Operation(summary = "Create new instance with a system generated id")
    @PostMapping("/instances")
    @WritesData
    @ExposesData
    @Simple
    public ResponseEntity<Result<NormalizedJsonLd>> createNewInstance(@RequestBody JsonLdDoc jsonLdDoc, @RequestParam(value = "space") @Parameter(description = "The space name the instance shall be stored in or \""+SpaceName.PRIVATE_SPACE+"\" if you want to store it to your private space") String space, @ParameterObject ResponseConfiguration responseConfiguration, @ParameterObject  IngestConfiguration ingestConfiguration, @ParameterObject ExternalEventInformation externalEventInformation) {
        Date startTime = new Date();
        UUID id = UUID.randomUUID();
        logger.debug(String.format("Creating new instance with id %s", id));
        SpaceName spaceName = authContext.resolveSpaceName(space);
        ResponseEntity<Result<NormalizedJsonLd>> newInstance = instanceController.createNewInstance(jsonLdDoc, id, spaceName, responseConfiguration, ingestConfiguration, externalEventInformation);
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

    @Operation(summary = "Create new instance with a client defined id")
    @PostMapping("/instances/{id}")
    @ExposesData
    @WritesData
    @Simple
    public ResponseEntity<Result<NormalizedJsonLd>> createNewInstance(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @RequestParam(value = "space") @Parameter(description = "The space name the instance shall be stored in or \""+SpaceName.PRIVATE_SPACE+"\" if you want to store it to your private space") String space,  @ParameterObject ResponseConfiguration responseConfiguration, @ParameterObject  IngestConfiguration ingestConfiguration,  @ParameterObject ExternalEventInformation externalEventInformation) {
        Date startTime = new Date();
        //We want to prevent the UUID to be used twice...
        InstanceId instanceId = idsController.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.nok(HttpStatus.CONFLICT.value(), String.format("The uuid you're providing (%s) is already in use. Please use a different one or do a PATCH instead", id)));
        }
        SpaceName spaceName = authContext.resolveSpaceName(space);
        logger.debug(String.format("Creating new instance with id %s", id));
        ResponseEntity<Result<NormalizedJsonLd>> newInstance = instanceController.createNewInstance(jsonLdDoc, id, spaceName, responseConfiguration, ingestConfiguration, externalEventInformation);
        logger.debug(String.format("Done creating new instance with id %s", id));
        if(newInstance.getBody()!=null){
            newInstance.getBody().setExecutionDetails(startTime, new Date());
        }
        return newInstance;
    }

    private ResponseEntity<Result<NormalizedJsonLd>> contributeToInstance(@RequestBody JsonLdDoc jsonLdDoc, UUID id, boolean undeprecate, @ParameterObject ResponseConfiguration responseConfiguration,  @ParameterObject IngestConfiguration ingestConfiguration,  @ParameterObject ExternalEventInformation externalEventInformation, boolean removeNonDeclaredFields) {
        Date startTime = new Date();
        logger.debug(String.format("Contributing to instance with id %s", id));
        InstanceId instanceId = idsController.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        } else if (instanceId.isDeprecated()) {
            if (undeprecate) {
                idsController.undeprecateInstance(instanceId.getUuid());
            } else {
                return ResponseEntity.status(HttpStatus.GONE).body(Result.nok(HttpStatus.GONE.value(), "The instance you're trying to contribute to has been deprecated."));
            }
        }
        ResponseEntity<Result<NormalizedJsonLd>> resultResponseEntity = instanceController.contributeToInstance(jsonLdDoc, instanceId, removeNonDeclaredFields, responseConfiguration, ingestConfiguration, externalEventInformation);
        logger.debug(String.format("Done contributing to instance with id %s", id));
        Result<NormalizedJsonLd> body = resultResponseEntity.getBody();
        if(body!=null){
            body.setExecutionDetails(startTime, new Date());
        }
        return resultResponseEntity;
    }

    @Operation(summary =  "Replace contribution to an existing instance")
    @PutMapping("/instances/{id}")
    @ExposesData
    @WritesData
    @Simple
    public ResponseEntity<Result<NormalizedJsonLd>> contributeToInstanceFullReplacement(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @RequestParam(value = "undeprecate", required = false, defaultValue = "false") boolean undeprecate,  @ParameterObject ResponseConfiguration responseConfiguration,  @ParameterObject IngestConfiguration ingestConfiguration,  @ParameterObject ExternalEventInformation externalEventInformation) {
        return contributeToInstance(jsonLdDoc, id, undeprecate, responseConfiguration, ingestConfiguration, externalEventInformation, true);
    }

    @Operation(summary = "Partially update contribution to an existing instance")
    @PatchMapping("/instances/{id}")
    @ExposesData
    @WritesData
    @Simple
    public ResponseEntity<Result<NormalizedJsonLd>> contributeToInstancePartialReplacement(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @RequestParam(value = "undeprecate", required = false, defaultValue = "false") boolean undeprecate,  @ParameterObject ResponseConfiguration responseConfiguration,  @ParameterObject IngestConfiguration ingestConfiguration,  @ParameterObject ExternalEventInformation externalEventInformation) {
        return contributeToInstance(jsonLdDoc, id, undeprecate, responseConfiguration, ingestConfiguration, externalEventInformation, false);
    }

    @Operation(summary = "Get the instance")
    @GetMapping("/instances/{id}")
    @ExposesData
    @Simple
    public ResponseEntity<Result<NormalizedJsonLd>> getInstanceById(@PathVariable("id") UUID id, @RequestParam("stage") ExposedStage stage, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        Date startTime = new Date();
        NormalizedJsonLd instanceById = instanceController.getInstanceById(id, stage.getStage(), responseConfiguration);
        return instanceById != null ? ResponseEntity.ok(Result.ok(instanceById).setExecutionDetails(startTime, new Date())) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Get incoming links for a specific instance (paginated)")
    @GetMapping("/instances/{id}/incomingLinks")
    @ExposesData
    @Advanced
    public PaginatedResult<NormalizedJsonLd> getIncomingLinks(@PathVariable("id") UUID id, @RequestParam("stage") ExposedStage stage, @RequestParam("property") String property, @RequestParam("type") String type, @ParameterObject PaginationParam paginationParam) {
        return PaginatedResult.ok(instanceController.getIncomingLinks(id, stage.getStage(), URLDecoder.decode(property, StandardCharsets.UTF_8), type!=null ? new Type(type) : null, paginationParam));
    }


    @Operation(summary = "Get the scope for the instance by its KG-internal ID")
    @GetMapping("/instances/{id}/scope")
    @ExposesMinimalData
    @Advanced
    public ResponseEntity<Result<ScopeElement>> getInstanceScope(@PathVariable("id") UUID id, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "returnPermissions", required = false, defaultValue = "false") boolean returnPermissions) {
        Date startTime = new Date();
        ScopeElement scope = instanceController.getScopeForInstance(id, stage.getStage(), returnPermissions);
        return scope != null ? ResponseEntity.ok(Result.ok(scope).setExecutionDetails(startTime, new Date())) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Get the neighborhood for the instance by its KG-internal ID")
    @GetMapping("/instances/{id}/neighbors")
    @ExposesMinimalData
    @Advanced
    public ResponseEntity<Result<GraphEntity>> getNeighbors(@PathVariable("id") UUID id, @RequestParam("stage") ExposedStage stage) {
        Date startTime = new Date();
        GraphEntity scope = instanceController.getNeighbors(id, stage.getStage());
        return scope != null ? ResponseEntity.ok(Result.ok(scope).setExecutionDetails(startTime, new Date())) : ResponseEntity.notFound().build();
    }


    @Operation(summary = "Returns a list of instances according to their types")
    @GetMapping("/instances")
    @ExposesData
    @Simple
    public PaginatedResult<NormalizedJsonLd> getInstances(@RequestParam("stage") ExposedStage stage, @RequestParam("type") String type, @RequestParam(value = "space", required = false) @Parameter(description = "The space of the instances to be listed or \""+SpaceName.PRIVATE_SPACE+"\" for your private space") String space, @RequestParam(value = "searchByLabel", required = false) String searchByLabel, @ParameterObject ResponseConfiguration responseConfiguration, @ParameterObject PaginationParam paginationParam) {
        Date startTime = new Date();
        PaginatedResult<NormalizedJsonLd> result = PaginatedResult.ok(instanceController.getInstances(stage.getStage(), new Type(type), space!=null ? new SpaceName(space) : null, searchByLabel, responseConfiguration, paginationParam));
        result.setExecutionDetails(startTime, new Date());
        return result;
    }

    @Operation(summary = "Bulk operation of /instances/{id} to read instances by their UUIDs")
    @PostMapping("/instancesByIds")
    @ExposesData
    @Advanced
    public Result<Map<String, Result<NormalizedJsonLd>>> getInstancesByIds(@RequestBody List<String> ids, @RequestParam("stage") ExposedStage stage, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        Date startTime = new Date();
        return Result.ok(instanceController.getInstancesByIds(ids, stage.getStage(), responseConfiguration)).setExecutionDetails(startTime, new Date());
    }


    @Operation(summary = "Read instances by the given list of (external) identifiers")
    @PostMapping("/instancesByIdentifiers")
    @ExposesData
    @Advanced
    public Result<Map<String, Result<NormalizedJsonLd>>> getInstancesByIdentifiers(@RequestBody List<String> identifiers, @RequestParam("stage") ExposedStage stage, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        List<IdWithAlternatives> idWithAlternatives = identifiers.stream().filter(Objects::nonNull).map(identifier -> new IdWithAlternatives(UUID.randomUUID(), null, Collections.singleton(identifier))).collect(Collectors.toList());
        Map<UUID, String> uuidToIdentifier = idWithAlternatives.stream().collect(Collectors.toMap(IdWithAlternatives::getId, v -> v.getAlternatives().iterator().next()));
        List<JsonLdIdMapping> jsonLdIdMappings = idsController.resolveIds(stage.getStage(), idWithAlternatives);
        Map<String, InstanceId> identifierToInstanceIdLookup = new HashMap<>();
        jsonLdIdMappings.forEach(jsonLdIdMapping -> {
            if(jsonLdIdMapping.getResolvedIds() != null && jsonLdIdMapping.getResolvedIds().size()==1){
                String identifier = uuidToIdentifier.get(jsonLdIdMapping.getRequestedId());
                JsonLdId resolvedId = jsonLdIdMapping.getResolvedIds().iterator().next();
                identifierToInstanceIdLookup.put(identifier, new InstanceId(idUtils.getUUID(resolvedId), jsonLdIdMapping.getSpace(), jsonLdIdMapping.isDeprecated()));
            }
        });
        Map<String, Result<NormalizedJsonLd>> instancesByIds = instanceController.getInstancesByIds(identifierToInstanceIdLookup.values().stream().filter(id -> !id.isDeprecated()).map(id -> id.getUuid().toString()).collect(Collectors.toList()), stage.getStage(), responseConfiguration);
        Map<String, Result<NormalizedJsonLd>> result = new HashMap<>();
        identifiers.forEach(identifier -> {
            InstanceId instanceId = identifierToInstanceIdLookup.get(identifier);
            if(instanceId!=null){
                result.put(identifier, instancesByIds.get(instanceId.getUuid().toString()));
            }
            else{
                result.put(identifier, Result.nok(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase()));
            }
        });
        return Result.ok(result);
    }

    @Operation(summary = "Deprecate an instance")
    @DeleteMapping("/instances/{id}")
    @WritesData
    //It only indirectly exposes the ids due to its status codes (you can tell if an id exists based on the return code this method provides)
    @ExposesIds
    @Simple
    public ResponseEntity<Result<Void>> deleteInstance(@PathVariable("id") UUID id, @ParameterObject ExternalEventInformation externalEventInformation) {
        Date startTime = new Date();
        InstanceId instanceId = idsController.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        } else {
            ReleaseStatus releaseStatus = release.getReleaseStatus(instanceId.getSpace().getName(), instanceId.getUuid(), ReleaseTreeScope.TOP_INSTANCE_ONLY);
            if (releaseStatus != null && releaseStatus != ReleaseStatus.UNRELEASED) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.nok(HttpStatus.CONFLICT.value(), "Was not able to remove instance because it is released still"));
            }
            instanceController.deleteInstance(instanceId, externalEventInformation);
            return ResponseEntity.ok(Result.<Void>ok().setExecutionDetails(startTime, new Date()));
        }
    }


    //RELEASE instances
    @Operation(summary = "Release or re-release an instance")
    @PutMapping("/instances/{id}/release")
    //It only indirectly exposes the ids due to its status codes (you can tell if an id exists based on the return code this method provides)
    @ExposesIds
    @WritesData
    @Simple
    public ResponseEntity<Result<Void>> releaseInstance(@PathVariable("id") UUID id, @RequestParam(value = "revision", required = false) String revision) {
        Date startTime = new Date();
        InstanceId instanceId = idsController.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        }
        else if(instanceId.isDeprecated()){
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        release.releaseInstance(instanceId.getSpace().getName(), instanceId.getUuid(), revision);
        return ResponseEntity.ok(Result.<Void>ok().setExecutionDetails(startTime, new Date()));
    }

    @Operation(summary =  "Unrelease an instance")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "The instance that has been unreleased"), @ApiResponse(responseCode = "404", description = "Instance not found")})
    @DeleteMapping("/instances/{id}/release")
    //It only indirectly exposes the ids due to its status codes (you can tell if an id exists based on the return code this method provides)
    @ExposesIds
    @WritesData
    @Simple
    public ResponseEntity<Result<Void>> unreleaseInstance(@PathVariable("id") UUID id) {
        Date startTime = new Date();
        InstanceId instanceId = idsController.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        }
        else if(instanceId.isDeprecated()){
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        release.unreleaseInstance(instanceId.getSpace().getName(), instanceId.getUuid());
        return ResponseEntity.ok(Result.<Void>ok().setExecutionDetails(startTime, new Date()));
    }

    @Operation(summary="Get the release status for an instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The release status of the instance"),
            @ApiResponse(responseCode = "404", description = "Instance not found")})
    @GetMapping(value = "/instances/{id}/release/status")
    //It only indirectly exposes the ids due to its status codes (you can tell if an id exists based on the return code this method provides)
    @ExposesIds
    @ExposesReleaseStatus
    @Simple
    public ResponseEntity<Result<ReleaseStatus>> getReleaseStatus(@PathVariable("id") UUID id, @RequestParam("releaseTreeScope") ReleaseTreeScope releaseTreeScope) {
        InstanceId instanceId = idsController.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        }
        else if(instanceId.isDeprecated()){
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        ReleaseStatus releaseStatus = release.getReleaseStatus(instanceId.getSpace().getName(), instanceId.getUuid(), releaseTreeScope);
        return ResponseEntity.ok(Result.ok(releaseStatus));
    }

    @Operation(summary = "Get the release status for multiple instances")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The release status of the instance"),
            @ApiResponse(responseCode = "404", description = "Instance not found")})
    @PostMapping(value = "/instancesByIds/release/status")
    //It only indirectly exposes the ids due to its status codes (you can tell if an id exists based on the return code this method provides)
    @ExposesIds
    @ExposesReleaseStatus
    @Advanced
    public Result<Map<UUID, Result<ReleaseStatus>>> getReleaseStatusByIds(@RequestBody List<UUID> listOfIds, @RequestParam("releaseTreeScope") ReleaseTreeScope releaseTreeScope) {
        List<InstanceId> instanceIds = idsController.resolveIdsByUUID(DataStage.IN_PROGRESS, listOfIds, false);
        return Result.ok(instanceIds.stream().filter(instanceId -> !instanceId.isDeprecated() &&  instanceId.getUuid()!=null).collect(Collectors.toMap(InstanceId::getUuid, instanceId -> {
                    try {
                        return Result.ok(release.getReleaseStatus(instanceId.getSpace().getName(), instanceId.getUuid(), releaseTreeScope));
                    }
                    catch (ForbiddenException ex){
                        return Result.nok(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase());
                    }
                }
        )));
    }



    @Operation(summary = "Returns suggestions for an instance to be linked by the given property (e.g. for the KG Editor)")
    @GetMapping("/instances/{id}/suggestedLinksForProperty")
    @ExposesMinimalData
    @Advanced
    public Result<SuggestionResult> getSuggestedLinksForProperty(@RequestParam("stage") ExposedStage stage, @PathVariable("id") UUID id, @RequestParam(value = "property") String propertyName, @RequestParam(value = "type", required = false) String type, @RequestParam(value = "search", required = false) String search, @ParameterObject PaginationParam paginationParam) {
        return getSuggestedLinksForProperty(null, stage, propertyName, id, type, search, paginationParam);
    }

    @Operation(summary = "Returns suggestions for an instance to be linked by the given property (e.g. for the KG Editor) - and takes into account the passed payload (already chosen values, reflection on dependencies between properties - e.g. providing only parcellations for an already chosen brain atlas)")
    @PostMapping("/instances/{id}/suggestedLinksForProperty")
    @ExposesMinimalData
    @Advanced
    public Result<SuggestionResult> getSuggestedLinksForProperty(@RequestBody NormalizedJsonLd payload, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "property") String propertyName, @PathVariable("id") UUID id, @RequestParam(value = "type", required = false) String type, @RequestParam(value = "search", required = false) String search, @ParameterObject PaginationParam paginationParam) {
        InstanceId instanceId = idsController.resolveId(DataStage.IN_PROGRESS, id);
        if(search!=null) {
            try {
                //The search string is a UUID -> let's try to resolve it - if we're successful, we can shortcut the lookup process.
                UUID uuid = UUID.fromString(search);
                InstanceId resolvedSearchId = idsController.resolveId(DataStage.IN_PROGRESS, uuid);
                if(resolvedSearchId!=null){
                    search = resolvedSearchId.serialize();
                }
            }
            catch(IllegalArgumentException e){
                //The search string is not an id -> we therefore don't treat it.
            }
        }


        return Result.ok(graphDBInstances.getSuggestedLinksForProperty(payload, stage.getStage(), instanceId!=null && instanceId.getSpace()!=null ? instanceId.getSpace().getName() : null, id, propertyName, type != null && !type.isBlank() ? new Type(type).getName() : null, search, paginationParam));
    }


}

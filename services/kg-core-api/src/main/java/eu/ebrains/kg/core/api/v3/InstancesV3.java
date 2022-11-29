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

package eu.ebrains.kg.core.api.v3;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.APINaming;
import eu.ebrains.kg.commons.api.GraphDBInstances;
import eu.ebrains.kg.commons.api.JsonLd;
import eu.ebrains.kg.commons.api.Release;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.config.openApiGroups.Advanced;
import eu.ebrains.kg.commons.config.openApiGroups.Extra;
import eu.ebrains.kg.commons.config.openApiGroups.Simple;
import eu.ebrains.kg.commons.exception.InvalidRequestException;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.*;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.core.api.examples.InstancesExamples;
import eu.ebrains.kg.core.controller.CoreInstanceController;
import eu.ebrains.kg.core.controller.IdsController;
import eu.ebrains.kg.core.controller.VirtualSpaceController;
import eu.ebrains.kg.core.model.ExposedStage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping(Version.V3)
public class InstancesV3 {

    private final CoreInstanceController instanceController;
    private final Release.Client release;
    private final AuthContext authContext;
    private final GraphDBInstances.Client graphDBInstances;
    private final IdsController idsController;
    private final VirtualSpaceController virtualSpaceController;
    private final JsonLd.Client jsonLd;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public InstancesV3(CoreInstanceController instanceController, Release.Client release, AuthContext authContext, GraphDBInstances.Client graphDBInstances, IdsController idsController, VirtualSpaceController virtualSpaceController, JsonLd.Client jsonLd) {
        this.instanceController = instanceController;
        this.release = release;
        this.authContext = authContext;
        this.graphDBInstances = graphDBInstances;
        this.idsController = idsController;
        this.virtualSpaceController = virtualSpaceController;
        this.jsonLd = jsonLd;
    }


    @Operation(
            summary = "Create new instance with a system generated id",
            description = """
            The invocation of this endpoint causes the ingestion of the payload (if valid) in the KG by assigning a new "@id" to it.
                        
            Please note that any "@id" specified in the payload will be interpreted as an additional identifier and therefore added to the "http://schema.org/identifier" array.
            """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
            @ExampleObject(name = InstancesExamples.PAYLOAD_MINIMAL_NAME, description = InstancesExamples.PAYLOAD_MINIMAL_DESC, value = InstancesExamples.PAYLOAD_MINIMAL),
            @ExampleObject(name = InstancesExamples.PAYLOAD_WITH_PROPERTY_NAME, description = InstancesExamples.PAYLOAD_WITH_PROPERTY_DESC, value = InstancesExamples.PAYLOAD_WITH_PROPERTY),
            @ExampleObject(name = InstancesExamples.PAYLOAD_WITH_LINK_NAME, description = InstancesExamples.PAYLOAD_WITH_LINK_DESC, value = InstancesExamples.PAYLOAD_WITH_LINK)
    }))
    @PostMapping("/instances")
    @WritesData
    @ExposesData
    @Simple
    public ResponseEntity<Result<NormalizedJsonLd>> createNewInstance(@RequestBody JsonLdDoc jsonLdDoc, @RequestParam(value = "space") @Parameter(description = "The space name the instance shall be stored in or \"" + SpaceName.PRIVATE_SPACE + "\" if you want to store it to your private space") String space, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        return createNewInstanceWithId(jsonLdDoc, UUID.randomUUID(), space, responseConfiguration);
    }


    @Operation(
            summary = "Create new instance with a client defined id",
            description = """
            The invocation of this endpoint causes the ingestion of the payload (if valid) in the KG by using the specified UUID
            
            Please note that any "@id" specified in the payload will be interpreted as an additional identifier and therefore added to the "http://schema.org/identifier" array.
            """)
    @PostMapping("/instances/{id}")
    @ExposesData
    @WritesData
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
            @ExampleObject(name = InstancesExamples.PAYLOAD_MINIMAL_NAME, description = InstancesExamples.PAYLOAD_MINIMAL_DESC, value = InstancesExamples.PAYLOAD_MINIMAL),
            @ExampleObject(name = InstancesExamples.PAYLOAD_WITH_PROPERTY_NAME, description = InstancesExamples.PAYLOAD_WITH_PROPERTY_DESC, value = InstancesExamples.PAYLOAD_WITH_PROPERTY),
            @ExampleObject(name = InstancesExamples.PAYLOAD_WITH_LINK_NAME, description = InstancesExamples.PAYLOAD_WITH_LINK_DESC, value = InstancesExamples.PAYLOAD_WITH_LINK)
    }))
    @Simple
    public ResponseEntity<Result<NormalizedJsonLd>> createNewInstanceWithId(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @RequestParam(value = "space") @Parameter(description = "The space name the instance shall be stored in or \"" + SpaceName.PRIVATE_SPACE + "\" if you want to store it to your private space") String space, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        Date startTime = new Date();
        SpaceName spaceName = authContext.resolveSpaceName(space);
        logger.debug(String.format("Creating new instance with id %s", id));
        ResponseEntity<Result<NormalizedJsonLd>> newInstance = instanceController.createNewInstance(normalizePayload(jsonLdDoc, true), id, spaceName, responseConfiguration);
        logger.debug(String.format("Done creating new instance with id %s", id));
        final Result<NormalizedJsonLd> body = newInstance.getBody();
        if (body != null) {
            body.setExecutionDetails(startTime, new Date());
        }
        return newInstance;
    }

    private NormalizedJsonLd normalizePayload(JsonLdDoc jsonLdDoc, boolean requiresTypeAtRootLevel) {
        try {
            jsonLdDoc.normalizeTypes();
            jsonLdDoc.validate(requiresTypeAtRootLevel);
        } catch (InvalidRequestException e) {
            //There have been validation errors -> we're going to normalize and validate again...
            final NormalizedJsonLd normalized = jsonLd.normalize(jsonLdDoc, true);
            normalized.validate(requiresTypeAtRootLevel);
            return normalized;
        }
        return new NormalizedJsonLd(jsonLdDoc);
    }


    private ResponseEntity<Result<NormalizedJsonLd>> contributeToInstance(NormalizedJsonLd normalizedJsonLd, UUID id, ExtendedResponseConfiguration responseConfiguration, boolean removeNonDeclaredFields) {
        Date startTime = new Date();
        logger.debug(String.format("Contributing to instance with id %s", id));
        final InstanceId instanceId = idsController.findId(id, normalizedJsonLd.identifiers());
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        }
        ResponseEntity<Result<NormalizedJsonLd>> resultResponseEntity = instanceController.contributeToInstance(normalizedJsonLd, instanceId, removeNonDeclaredFields, responseConfiguration);
        logger.debug(String.format("Done contributing to instance with id %s", id));
        Result<NormalizedJsonLd> body = resultResponseEntity.getBody();
        if (body != null) {
            body.setExecutionDetails(startTime, new Date());
        }
        return resultResponseEntity;
    }

    @Operation(summary = "Replace contribution to an existing instance")
    @PutMapping("/instances/{id}")
    @ExposesData
    @WritesData
    @Simple
    public ResponseEntity<Result<NormalizedJsonLd>> contributeToInstanceFullReplacement(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        return contributeToInstance(normalizePayload(jsonLdDoc, true), id, responseConfiguration, true);
    }

    @Operation(summary = "Partially update contribution to an existing instance")
    @PatchMapping("/instances/{id}")
    @ExposesData
    @WritesData
    @Simple
    public ResponseEntity<Result<NormalizedJsonLd>> contributeToInstancePartialReplacement(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        return contributeToInstance(normalizePayload(jsonLdDoc, false), id, responseConfiguration, false);
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
        return PaginatedResult.ok(instanceController.getIncomingLinks(id, stage.getStage(), URLDecoder.decode(property, StandardCharsets.UTF_8), type != null ? new Type(type) : null, paginationParam));
    }


    @Operation(summary = "Get the scope for the instance by its KG-internal ID")
    @GetMapping("/instances/{id}/scope")
    @ExposesMinimalData
    @Advanced
    public ResponseEntity<Result<ScopeElement>> getInstanceScope(@PathVariable("id") UUID id, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "returnPermissions", required = false, defaultValue = "false") boolean returnPermissions, @RequestParam(value = "applyRestrictions", required = false, defaultValue = "false") boolean applyRestrictions) {
        Date startTime = new Date();
        ScopeElement scope = instanceController.getScopeForInstance(id, stage.getStage(), returnPermissions, applyRestrictions);
        return scope != null ? ResponseEntity.ok(Result.ok(scope).setExecutionDetails(startTime, new Date())) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Get the neighborhood for the instance by its KG-internal ID")
    @GetMapping("/instances/{id}/neighbors")
    @ExposesMinimalData
    @Extra
    public ResponseEntity<Result<GraphEntity>> getNeighbors(@PathVariable("id") UUID id, @RequestParam("stage") ExposedStage stage) {
        Date startTime = new Date();
        GraphEntity scope = instanceController.getNeighbors(id, stage.getStage());
        return scope != null ? ResponseEntity.ok(Result.ok(scope).setExecutionDetails(startTime, new Date())) : ResponseEntity.notFound().build();
    }


    @Operation(summary = "Returns a list of instances according to their types")
    @GetMapping("/instances")
    @ExposesData
    @Simple
    public PaginatedResult<NormalizedJsonLd> listInstances(@RequestParam("stage") ExposedStage stage, @RequestParam("type") @Parameter(examples = {@ExampleObject(name = "person", value = "https://openminds.ebrains.eu/core/Person", description = "An openminds person"), @ExampleObject(name = "datasetVersion", value = "https://openminds.ebrains.eu/core/DatasetVersion", description = "An openminds dataset version")}) String type, @RequestParam(value = "space", required = false) @Parameter(description = "The space of the instances to be listed or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space", examples = {@ExampleObject(name = "myspace", value = "myspace"), @ExampleObject(name = "dataset", value = "dataset")}) String space, @RequestParam(value = "searchByLabel", required = false) String searchByLabel, @RequestParam(value = "filterProperty", required = false) String filterProperty, @RequestParam(value = "filterValue", required = false) String filterValue, @ParameterObject ResponseConfiguration responseConfiguration, @ParameterObject PaginationParam paginationParam) {
        PaginatedResult<NormalizedJsonLd> result;
        Date startTime = new Date();
        if (virtualSpaceController.isVirtualSpace(space)) {
            List<NormalizedJsonLd> instancesByInvitation = virtualSpaceController.getInstancesByInvitation(responseConfiguration, stage.getStage(), type);
            int total = instancesByInvitation.size();
            if (paginationParam.getFrom() != 0 || paginationParam.getSize() != null) {
                int lastIndex = paginationParam.getSize() == null ? instancesByInvitation.size() : Math.min(instancesByInvitation.size(), (int) (paginationParam.getFrom() + paginationParam.getSize()));
                instancesByInvitation = instancesByInvitation.subList((int) paginationParam.getFrom(), lastIndex);
            }
            return PaginatedResult.ok(new Paginated<>(instancesByInvitation, (long) instancesByInvitation.size(), total, paginationParam.getFrom()));
        } else {
            searchByLabel = enrichSearchTermIfItIsAUUID(searchByLabel);
            result = PaginatedResult.ok(instanceController.getInstances(stage.getStage(), new Type(type), SpaceName.fromString(space), searchByLabel, filterProperty, filterValue, responseConfiguration, paginationParam));
        }
        result.setExecutionDetails(startTime, new Date());
        return result;
    }

    @Operation(summary = "Bulk operation of /instances/{id} to read instances by their UUIDs")
    @PostMapping("/instancesByIds")
    @ExposesData
    @Advanced
    public Result<Map<String, Result<NormalizedJsonLd>>> getInstancesByIds(@RequestBody List<String> ids, @RequestParam("stage") ExposedStage stage, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        Date startTime = new Date();
        return Result.ok(instanceController.getInstancesByIds(ids, stage.getStage(), responseConfiguration, null)).setExecutionDetails(startTime, new Date());
    }


    @Operation(summary = "Read instances by the given list of (external) identifiers")
    @PostMapping("/instancesByIdentifiers")
    @ExposesData
    @Advanced
    public Result<Map<String, Result<NormalizedJsonLd>>> getInstancesByIdentifiers(@RequestBody List<String> identifiers, @RequestParam("stage") ExposedStage stage, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        List<IdWithAlternatives> idWithAlternatives = identifiers.stream().filter(Objects::nonNull).map(identifier -> new IdWithAlternatives(UUID.randomUUID(), null, Collections.singleton(identifier))).collect(Collectors.toList());
        Map<UUID, String> uuidToIdentifier = idWithAlternatives.stream().collect(Collectors.toMap(IdWithAlternatives::getId, v -> v.getAlternatives().iterator().next()));
        Map<UUID, InstanceId> resolvedIds = idsController.resolveIds(stage.getStage(), idWithAlternatives);
        Map<String, InstanceId> identifierToInstanceIdLookup = new HashMap<>();
        resolvedIds.keySet().forEach(uuid -> identifierToInstanceIdLookup.put(uuidToIdentifier.get(uuid), resolvedIds.get(uuid)));
        Map<String, Result<NormalizedJsonLd>> instancesByIds = instanceController.getInstancesByIds(identifierToInstanceIdLookup.values().stream().filter(Objects::nonNull).map(id -> id.getUuid().toString()).collect(Collectors.toList()), stage.getStage(), responseConfiguration, null);
        Map<String, Result<NormalizedJsonLd>> result = new HashMap<>();
        identifiers.forEach(identifier -> {
            InstanceId instanceId = identifierToInstanceIdLookup.get(identifier);
            if (instanceId != null) {
                result.put(identifier, instancesByIds.get(instanceId.getUuid().toString()));
            } else {
                result.put(identifier, Result.nok(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase()));
            }
        });
        return Result.ok(result);
    }

    @Operation(summary = "Move an instance to another space")
    @PutMapping("/instances/{id}/spaces/{space}")
    @WritesData
    @ExposesIds
    @Simple
    public ResponseEntity<Result<NormalizedJsonLd>> moveInstance(@PathVariable("id") UUID id, @PathVariable("space") String targetSpace, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        Date startTime = new Date();
        InstanceId instanceId = idsController.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        } else {
            ReleaseStatus releaseStatus = release.getReleaseStatus(instanceId.getSpace().getName(), instanceId.getUuid(), ReleaseTreeScope.TOP_INSTANCE_ONLY);
            if (releaseStatus != null && releaseStatus != ReleaseStatus.UNRELEASED) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.nok(HttpStatus.CONFLICT.value(), "Was not able to move an instance because it is released still", instanceId.getUuid()));
            }
            return instanceController.moveInstance(instanceId, authContext.resolveSpaceName(targetSpace), responseConfiguration);
        }
    }


    @Operation(summary = "Delete an instance")
    @DeleteMapping("/instances/{id}")
    @WritesData
    //It only indirectly exposes the ids due to its status codes (you can tell if an id exists based on the return code this method provides)
    @ExposesIds
    @Simple
    public ResponseEntity<Result<Void>> deleteInstance(@PathVariable("id") UUID id) {
        Date startTime = new Date();
        InstanceId instanceId = idsController.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        } else {
            ReleaseStatus releaseStatus = release.getReleaseStatus(instanceId.getSpace().getName(), instanceId.getUuid(), ReleaseTreeScope.TOP_INSTANCE_ONLY);
            if (releaseStatus != null && releaseStatus != ReleaseStatus.UNRELEASED) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.nok(HttpStatus.CONFLICT.value(), "Was not able to remove instance because it is released still", instanceId.getUuid()));
            }
            instanceController.deleteInstance(instanceId);
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
        release.releaseInstance(instanceId.getSpace().getName(), instanceId.getUuid(), revision);
        return ResponseEntity.ok(Result.<Void>ok().setExecutionDetails(startTime, new Date()));
    }

    @Operation(summary = "Unrelease an instance")
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
        release.unreleaseInstance(instanceId.getSpace().getName(), instanceId.getUuid());
        return ResponseEntity.ok(Result.<Void>ok().setExecutionDetails(startTime, new Date()));
    }

    @Operation(summary = "Get the release status for an instance")
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
        Map<UUID, Result<ReleaseStatus>> result = new HashMap<>();
        final Map<UUID, ReleaseStatus> releaseStatus = release.getIndividualReleaseStatus(instanceIds, releaseTreeScope);
        releaseStatus.keySet().forEach(id -> result.put(id, Result.ok(releaseStatus.get(id))));
        return Result.ok(result);
    }


    @Operation(summary = "Returns suggestions for an instance to be linked by the given property (e.g. for the KG Editor). Please note: This service will return released values for \"additionalValue\" in case a user only has minimal read rights")
    @GetMapping("/instances/{id}/suggestedLinksForProperty")
    @ExposesMinimalData
    @Extra
    public Result<SuggestionResult> getSuggestedLinksForProperty(@RequestParam("stage") ExposedStage stage, @PathVariable("id") UUID id, @RequestParam(value = "property") String propertyName, @RequestParam(value = "sourceType", required = false) @Parameter(description = "The source type for which the given property shall be evaluated. If not provided, the API tries to figure out the type by analyzing the type of the root object of the persisted instance. Please note, that this parameter is mandatory for embedded structures.") String sourceType, @RequestParam(value = "targetType", required = false) @Parameter(description = "The target type of the suggestions. If not provided, suggestions of all possible target types will be returned.") String targetType, @RequestParam(value = "search", required = false) String search, @ParameterObject PaginationParam paginationParam) {
        return getSuggestedLinksForProperty(null, stage, propertyName, id, sourceType, targetType, search, paginationParam);
    }

    @Operation(summary = "Returns suggestions for an instance to be linked by the given property (e.g. for the KG Editor) - and takes into account the passed payload (already chosen values, reflection on dependencies between properties - e.g. providing only parcellations for an already chosen brain atlas). Please note: This service will return released values for \"additionalValue\" in case a user only has minimal read rights")
    @PostMapping("/instances/{id}/suggestedLinksForProperty")
    @ExposesMinimalData
    @Extra
    public Result<SuggestionResult> getSuggestedLinksForProperty(@RequestBody NormalizedJsonLd payload, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "property") String propertyName, @PathVariable("id") UUID id, @Parameter(description = "The source type for which the given property shall be evaluated. If not provided, the API tries to figure out the type by analyzing the type of the root object originating from the payload. Please note, that this parameter is mandatory for embedded structures.") @RequestParam(value = "sourceType", required = false) String sourceType, @Parameter(description = "The target type of the suggestions. If not provided, suggestions of all possible target types will be returned.") @RequestParam(value = "targetType", required = false) String targetType, @RequestParam(value = "search", required = false) String search, @ParameterObject PaginationParam paginationParam) {
        Date start = new Date();
        InstanceId instanceId = idsController.resolveId(DataStage.IN_PROGRESS, id);
        search = enrichSearchTermIfItIsAUUID(search);
        return Result.ok(graphDBInstances.getSuggestedLinksForProperty(payload, stage.getStage(), instanceId != null && instanceId.getSpace() != null ? instanceId.getSpace().getName() : null, id, propertyName, sourceType != null && !sourceType.isBlank() ? new Type(sourceType).getName() : null, targetType != null && !targetType.isBlank() ? new Type(targetType).getName() : null, search, paginationParam)).setExecutionDetails(start, new Date());
    }

    private String enrichSearchTermIfItIsAUUID(String search) {
        if (search != null) {
            try {
                //The search string is a UUID -> let's try to resolve it - if we're successful, we can shortcut the lookup process.
                UUID uuid = UUID.fromString(search);
                InstanceId resolvedSearchId = idsController.resolveId(DataStage.IN_PROGRESS, uuid);
                if (resolvedSearchId != null) {
                    return resolvedSearchId.serialize();
                }
            } catch (IllegalArgumentException e) {
                //The search string is not an id -> we therefore don't treat it.
            }
        }
        return search;
    }

    @Operation(summary = "Create or update an invitation for the given user to review the given instance")
    @PutMapping("/instances/{id}/invitedUsers/{userId}")
    @Advanced
    public void inviteUserForInstance(@PathVariable("id") UUID id, @PathVariable("userId") UUID userId) {
        instanceController.createInvitation(id, userId);
    }

    @Operation(summary = "Revoke an invitation for the given user to review the given instance")
    @DeleteMapping("/instances/{id}/invitedUsers/{userId}")
    @Advanced
    public void revokeUserInvitation(@PathVariable("id") UUID id, @PathVariable("userId") UUID userId) {
        instanceController.revokeInvitation(id, userId);
    }

    @Operation(summary = "List invitations for review for the given instance")
    @GetMapping("/instances/{id}/invitedUsers")
    @Advanced
    public Result<List<String>> listInvitations(@PathVariable("id") UUID id) {
        return Result.ok(instanceController.listInvitedUserIds(id));
    }

    @Operation(summary = "Update invitation scope for this instance")
    @PutMapping("/instances/{id}/invitationScope")
    @Admin
    public void calculateInstanceInvitationScope(@PathVariable("id") UUID id) {
        instanceController.calculateInstanceInvitationScope(id);
    }

    @Operation(summary = "List instances with invitations")
    @GetMapping("/instancesWithInvitations")
    @Advanced
    public Result<List<UUID>> listInstancesWithInvitations() {
        return Result.ok(instanceController.listInstancesWithInvitations());
    }


}

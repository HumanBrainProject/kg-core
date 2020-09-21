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

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.ExternalEventInformation;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.serviceCall.CoreInstancesToGraphDB;
import eu.ebrains.kg.core.serviceCall.CoreToIds;
import eu.ebrains.kg.core.serviceCall.CoreToJsonLd;
import eu.ebrains.kg.core.serviceCall.CoreToPrimaryStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The instance controller contains the orchestration logic for the instance operations
 */
@Component
public class CoreInstanceController {


    private final CoreInstancesToGraphDB graphDbSvc;
    private final CoreToIds idsSvc;
    private final AuthContext authContext;
    private final IdUtils idUtils;
    private final CoreToJsonLd jsonLdSvc;
    private final CoreToPrimaryStore primaryStoreSvc;

    public CoreInstanceController(CoreInstancesToGraphDB graphDbSvc, CoreToIds idsSvc, AuthContext authContext, IdUtils idUtils, CoreToJsonLd jsonLdSvc, CoreToPrimaryStore primaryStoreSvc) {
        this.graphDbSvc = graphDbSvc;
        this.idsSvc = idsSvc;
        this.authContext = authContext;
        this.idUtils = idUtils;
        this.jsonLdSvc = jsonLdSvc;
        this.primaryStoreSvc = primaryStoreSvc;
    }

    public ResponseEntity<Result<NormalizedJsonLd>> createNewInstance(JsonLdDoc jsonLdDoc, UUID id, String space, ResponseConfiguration responseConfiguration, IngestConfiguration ingestConfiguration, ExternalEventInformation externalEventInformation) {
        NormalizedJsonLd normalizedJsonLd;
        if (ingestConfiguration.isNormalizePayload()) {
            normalizedJsonLd = jsonLdSvc.toNormalizedJsonLd(jsonLdDoc);
        } else {
            normalizedJsonLd = new NormalizedJsonLd(jsonLdDoc);
        }
        Space s = new Space(space);
        List<InstanceId> instanceIdsInSameSpace = idsSvc.resolveIds(DataStage.IN_PROGRESS, new IdWithAlternatives(id, s, normalizedJsonLd.allIdentifiersIncludingId()), false).stream().filter(i -> s.equals(i.getSpace())).collect(Collectors.toList());
        //Were only interested in those instance ids in the same space. Since merging is not done cross-space, we want to allow instances being created with the same identifiers across spaces.
        if (!instanceIdsInSameSpace.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.nok(HttpStatus.CONFLICT.value(), String.format("The id and/or the payload you're providing are pointing to the instance(s) %s. Please do a PATCH instead", instanceIdsInSameSpace.stream().map(i -> i.getUuid().toString()).distinct().collect(Collectors.joining(", ")))));
        }
        normalizedJsonLd.defineFieldUpdateTimes(normalizedJsonLd.keySet().stream().collect(Collectors.toMap(k -> k, k -> externalEventInformation != null && externalEventInformation.getExternalEventTime() != null ? externalEventInformation.getExternalEventTime() : ZonedDateTime.now())));
        Event upsertEvent = createUpsertEvent(id, externalEventInformation, normalizedJsonLd, s);
        List<InstanceId> ids = primaryStoreSvc.postEvent(upsertEvent, ingestConfiguration.isDeferInference(), authContext.getAuthTokens());
        return handleIngestionResponse(responseConfiguration, ids);
    }

    public ResponseEntity<Result<NormalizedJsonLd>> contributeToInstance(JsonLdDoc jsonLdDoc, InstanceId instanceId, boolean removeNonDeclaredProperties, ResponseConfiguration responseConfiguration, IngestConfiguration ingestConfiguration, ExternalEventInformation externalEventInformation) {
        NormalizedJsonLd normalizedJsonLd;
        if (ingestConfiguration.isNormalizePayload()) {
            normalizedJsonLd = jsonLdSvc.toNormalizedJsonLd(jsonLdDoc);
        } else {
            normalizedJsonLd = new NormalizedJsonLd(jsonLdDoc);
        }
        normalizedJsonLd = patchInstance(instanceId, normalizedJsonLd, removeNonDeclaredProperties, externalEventInformation != null ? externalEventInformation.getExternalEventTime() : null);
        Event upsertEvent = createUpsertEvent(instanceId.getUuid(), externalEventInformation, normalizedJsonLd, instanceId.getSpace());
        List<InstanceId> ids = primaryStoreSvc.postEvent(upsertEvent, ingestConfiguration.isDeferInference(), authContext.getAuthTokens());
        ResponseEntity<Result<NormalizedJsonLd>> resultResponseEntity = handleIngestionResponse(responseConfiguration, ids);
        return resultResponseEntity;
    }

    public List<InstanceId> deleteInstance(InstanceId instanceId, ExternalEventInformation externalEventInformation) {
        Event deleteEvent = Event.createDeleteEvent(instanceId.getSpace(), instanceId.getUuid(), idUtils.buildAbsoluteUrl(instanceId.getUuid()));
        handleExternalEventInformation(externalEventInformation, deleteEvent);
        return primaryStoreSvc.postEvent(deleteEvent, false, authContext.getAuthTokens());
    }


    private Event createUpsertEvent(UUID id, ExternalEventInformation externalEventInformation, NormalizedJsonLd normalizedJsonLd, Space s) {
        Event upsertEvent = Event.createUpsertEvent(s, id, Event.Type.INSERT, normalizedJsonLd);
        handleExternalEventInformation(externalEventInformation, upsertEvent);
        return upsertEvent;
    }

    private void handleExternalEventInformation(ExternalEventInformation externalEventInformation, Event event) {
        if (externalEventInformation != null) {
            if (externalEventInformation.getExternalUserDefinition() != null) {
                event.setUserId(externalEventInformation.getExternalUserDefinition());
            }
            if (externalEventInformation.getExternalEventTime() != null) {
                event.setReportedTimeStampInMs(externalEventInformation.getExternalEventTime().toInstant().toEpochMilli());
            }
        }
    }

    private NormalizedJsonLd patchInstance(InstanceId instanceId, NormalizedJsonLd normalizedJsonLd, boolean removeNonDefinedKeys, ZonedDateTime eventDateTime) {
        InstanceId nativeId = new InstanceId(idUtils.getDocumentIdForUserAndInstance(authContext.getUserId(), instanceId.getUuid()), instanceId.getSpace(), instanceId.isDeprecated());
        NormalizedJsonLd instance = graphDbSvc.getInstance(DataStage.NATIVE, nativeId, true, false);
        if (instance == null) {
            Map<String, ZonedDateTime> updateTimes = new HashMap<>();
            normalizedJsonLd.keySet().forEach(k -> updateTimes.put(k, eventDateTime != null ? eventDateTime : ZonedDateTime.now()));
            normalizedJsonLd.defineFieldUpdateTimes(updateTimes);
            return normalizedJsonLd;
        } else {
            Map<String, ZonedDateTime> updateTimesFromInstance = instance.fieldUpdateTimes();
            Map<String, ZonedDateTime> updateTimes = updateTimesFromInstance != null ? updateTimesFromInstance : new HashMap<>();
            Set<String> oldKeys = new HashSet<>(instance.keySet());
            normalizedJsonLd.keySet().forEach(k -> {
                Object value = normalizedJsonLd.get(k);
                if (value == null) {
                    updateTimes.remove(k);
                    instance.remove(k);
                } else {
                    updateTimes.put(k, eventDateTime != null ? eventDateTime : ZonedDateTime.now());
                    instance.put(k, value);
                }
            });
            if (removeNonDefinedKeys) {
                oldKeys.removeAll(normalizedJsonLd.keySet());
                oldKeys.forEach(k -> {
                    instance.remove(k);
                    updateTimes.remove(k);
                });
            }
            instance.defineFieldUpdateTimes(updateTimes);
            return instance;
        }
    }

    public Map<String, Result<NormalizedJsonLd>> getInstancesByIds(List<String> ids, DataStage stage, ResponseConfiguration responseConfiguration) {
        Map<String, Result<NormalizedJsonLd>> result = new HashMap<>();
        List<UUID> validUUIDs = ids.stream().map(id -> {
            try {
                return UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());


        List<InstanceId> idsAfterResolution = idsSvc.resolveIdsByUUID(stage, validUUIDs, true);
        idsAfterResolution.stream().filter(instanceId -> !instanceId.isDeprecated()).filter(InstanceId::isUnresolved).forEach(id -> result.put(id.getUuid().toString(), Result.nok(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase())));
        Map<UUID, Result<NormalizedJsonLd>> instancesByIds = graphDbSvc.getInstancesByIds(stage, idsAfterResolution.stream().filter(i -> !i.isUnresolved() && !i.isDeprecated()).collect(Collectors.toList()), responseConfiguration.isReturnEmbedded(), responseConfiguration.isReturnAlternatives());
        instancesByIds.forEach((k,v) -> result.put(k.toString(), v));
        for (String id : ids) {
            if(!result.containsKey(id)){
                result.put(id, Result.nok(HttpStatus.NOT_FOUND.value(), id));
            }
        }
        if (responseConfiguration.isReturnPermissions()) {
            enrichWithPermissionInformation(stage, instancesByIds.values());
        }
        return result;
    }

    public Paginated<NormalizedJsonLd> getInstances(DataStage stage, Type type, String searchByLabel, ResponseConfiguration responseConfiguration, PaginationParam paginationParam) {
        Paginated<NormalizedJsonLd> instancesByType = graphDbSvc.getInstancesByType(stage, type, paginationParam, searchByLabel, responseConfiguration.isReturnEmbedded(), responseConfiguration.isReturnAlternatives());
        if (responseConfiguration.isReturnPermissions()) {
            enrichWithPermissionInformation(stage, instancesByType.getData().stream().map(i -> Result.ok(i)).collect(Collectors.toList()));
        }
        return instancesByType;
    }

    public ResponseEntity<Result<NormalizedJsonLd>> handleIngestionResponse(ResponseConfiguration responseConfiguration, List<InstanceId> instanceIds) {
        Result<NormalizedJsonLd> result;
        if (responseConfiguration.isReturnPayload()) {
            Map<UUID, Result<NormalizedJsonLd>> instancesByIds = graphDbSvc.getInstancesByIds(DataStage.IN_PROGRESS, instanceIds, responseConfiguration.isReturnEmbedded(), responseConfiguration.isReturnAlternatives());
            if (responseConfiguration.isReturnPermissions()) {
                enrichWithPermissionInformation(DataStage.IN_PROGRESS, instancesByIds.values());
            }
            result = AmbiguousResult.ok(instancesByIds.values().stream().map(Result::getData).collect(Collectors.toList()));
        } else {
            result = AmbiguousResult.ok(instanceIds.stream().map(id -> {
                NormalizedJsonLd jsonLd = new NormalizedJsonLd();
                jsonLd.setId(idUtils.buildAbsoluteUrl(id.getUuid()));
                return jsonLd;
            }).collect(Collectors.toList()));
        }
        return result instanceof AmbiguousResult ? ResponseEntity.status(HttpStatus.CONFLICT).body(result) : ResponseEntity.ok(result);
    }

    public NormalizedJsonLd getInstanceById(UUID id, DataStage stage, ResponseConfiguration responseConfiguration) {
        InstanceId instanceId = idsSvc.resolveId(stage, id);
        if (instanceId == null || instanceId.isDeprecated()) {
            return null;
        }
        if (responseConfiguration.isReturnPayload()) {
            NormalizedJsonLd instance = graphDbSvc.getInstance(stage, instanceId, responseConfiguration.isReturnEmbedded(), responseConfiguration.isReturnAlternatives());
            if (responseConfiguration.isReturnPermissions() && instance != null) {
                enrichWithPermissionInformation(stage, Collections.singletonList(Result.ok(instance)));
            }
            return instance;
        } else {
            NormalizedJsonLd idPayload = new NormalizedJsonLd();
            idPayload.setId(idUtils.buildAbsoluteUrl(instanceId.getUuid()));
            return idPayload;
        }
    }

    private void enrichWithPermissionInformation(DataStage stage, Collection<Result<NormalizedJsonLd>> documents) {
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();
        documents.forEach(result -> {
                    NormalizedJsonLd doc = result.getData();
                    String space = doc.getAs(EBRAINSVocabulary.META_SPACE, String.class);
                    Space sp = space != null ? new Space(space) : null;
                    Set<Functionality> functionalities = permissions.stream().filter(p -> Functionality.FunctionalityGroup.INSTANCE == p.getFunctionality().getFunctionalityGroup() && stage != null && stage == p.getFunctionality().getStage()).filter(p -> p.appliesTo(sp, idUtils.getUUID(doc.id()))).map(FunctionalityInstance::getFunctionality).collect(Collectors.toSet());
                    doc.put(EBRAINSVocabulary.META_PERMISSIONS, functionalities);
                }
        );
    }

    private void enrichWithPermissionInformation(DataStage stage, ScopeElement scopeElement, List<FunctionalityInstance> permissions) {
        Space sp = scopeElement.getSpace() != null ? new Space(scopeElement.getSpace()) : null;
        scopeElement.setPermissions(permissions.stream().filter(p -> Functionality.FunctionalityGroup.INSTANCE == p.getFunctionality().getFunctionalityGroup() && stage != null && stage == p.getFunctionality().getStage()).filter(p -> p.appliesTo(sp, scopeElement.getId())).map(FunctionalityInstance::getFunctionality).collect(Collectors.toSet()));
        if(scopeElement.getChildren()!=null){
            scopeElement.getChildren().forEach(c -> enrichWithPermissionInformation(stage, c, permissions));
        }
    }

    public ScopeElement getScopeForInstance(UUID id, DataStage stage, boolean returnPermissions) {
        ScopeElement scope = graphDbSvc.getScope(stage, idsSvc.resolveId(stage, id));
        if(returnPermissions){
            enrichWithPermissionInformation(stage, scope, authContext.getUserWithRoles().getPermissions());
        }
        return scope;
    }

    public GraphEntity getNeighbors(UUID id, DataStage stage) {
        return graphDbSvc.getNeighbors(stage,  idsSvc.resolveId(stage, id));
    }
}

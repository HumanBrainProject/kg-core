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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.core.controller;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.GraphDBInstances;
import eu.ebrains.kg.commons.api.GraphDBScopes;
import eu.ebrains.kg.commons.api.JsonLd;
import eu.ebrains.kg.commons.api.PrimaryStoreEvents;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.ExternalEventInformation;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
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

    private final GraphDBInstances.Client graphDBInstances;
    private final GraphDBScopes.Client graphDBScopes;
    private final IdsController ids;
    private final AuthContext authContext;
    private final IdUtils idUtils;
    private final JsonLd.Client jsonLd;
    private final PrimaryStoreEvents.Client primaryStoreEvents;

    public CoreInstanceController(GraphDBInstances.Client graphDBInstances, GraphDBScopes.Client graphDBScopes, IdsController ids, AuthContext authContext, IdUtils idUtils, JsonLd.Client jsonLd, PrimaryStoreEvents.Client primaryStoreEvents) {
        this.graphDBInstances = graphDBInstances;
        this.graphDBScopes = graphDBScopes;
        this.ids = ids;
        this.authContext = authContext;
        this.idUtils = idUtils;
        this.jsonLd = jsonLd;
        this.primaryStoreEvents = primaryStoreEvents;
    }

    public ResponseEntity<Result<NormalizedJsonLd>> createNewInstance(JsonLdDoc jsonLdDoc, UUID id, SpaceName s, ResponseConfiguration responseConfiguration, IngestConfiguration ingestConfiguration, ExternalEventInformation externalEventInformation) {
        NormalizedJsonLd normalizedJsonLd;
        if (ingestConfiguration.isNormalizePayload()) {
            normalizedJsonLd = jsonLd.normalize(jsonLdDoc, true);
        } else {
            normalizedJsonLd = new NormalizedJsonLd(jsonLdDoc);
        }
        List<InstanceId> instanceIdsInSameSpace = ids.resolveIds(DataStage.IN_PROGRESS, new IdWithAlternatives(id, s, normalizedJsonLd.allIdentifiersIncludingId()), false).stream().filter(i -> s.equals(i.getSpace())).collect(Collectors.toList());
        //Were only interested in those instance ids in the same space. Since merging is not done cross-space, we want to allow instances being created with the same identifiers across spaces.
        if (!instanceIdsInSameSpace.isEmpty()) {
            if (instanceIdsInSameSpace.size() == 1) {
                InstanceId instanceId = instanceIdsInSameSpace.get(0);
                Result<NormalizedJsonLd> conflictResult = Result.nok(HttpStatus.CONFLICT.value(), String.format("The payload you're providing is pointing to the instance %s (either by the " + JsonLdConsts.ID + " or the " + SchemaOrgVocabulary.IDENTIFIER + " field it contains). Please do a PUT or a PATCH to the mentioned id instead.", instanceId.getUuid()));
                conflictResult.getError().setInstanceId(instanceId.getUuid());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictResult);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.nok(HttpStatus.INTERNAL_SERVER_ERROR.value(), String.format("The id and/or the payload you're providing is pointing to multiple instances (%s). This is an invalid state and should be reported to kg@ebrains.eu", instanceIdsInSameSpace.stream().map(i -> i.getUuid().toString()).distinct().collect(Collectors.joining(", ")))));
            }
        }
        normalizedJsonLd.defineFieldUpdateTimes(normalizedJsonLd.keySet().stream().collect(Collectors.toMap(k -> k, k -> externalEventInformation != null && externalEventInformation.getExternalEventTime() != null ? externalEventInformation.getExternalEventTime() : ZonedDateTime.now())));
        Event upsertEvent = createUpsertEvent(id, externalEventInformation, normalizedJsonLd, s);
        Set<InstanceId> ids = primaryStoreEvents.postEvent(upsertEvent, ingestConfiguration.isDeferInference());
        return handleIngestionResponse(responseConfiguration, ids);
    }

    public ResponseEntity<Result<NormalizedJsonLd>> contributeToInstance(JsonLdDoc jsonLdDoc, InstanceId instanceId, boolean removeNonDeclaredProperties, ResponseConfiguration responseConfiguration, IngestConfiguration ingestConfiguration, ExternalEventInformation externalEventInformation) {
        NormalizedJsonLd normalizedJsonLd;
        if (ingestConfiguration.isNormalizePayload()) {
            normalizedJsonLd = jsonLd.normalize(jsonLdDoc, true);
        } else {
            normalizedJsonLd = new NormalizedJsonLd(jsonLdDoc);
        }
        normalizedJsonLd = patchInstance(instanceId, normalizedJsonLd, removeNonDeclaredProperties, externalEventInformation != null ? externalEventInformation.getExternalEventTime() : null);
        Event upsertEvent = createUpsertEvent(instanceId.getUuid(), externalEventInformation, normalizedJsonLd, instanceId.getSpace());
        Set<InstanceId> ids = primaryStoreEvents.postEvent(upsertEvent, ingestConfiguration.isDeferInference());
        return handleIngestionResponse(responseConfiguration, ids);
    }

    public Set<InstanceId> deleteInstance(InstanceId instanceId, ExternalEventInformation externalEventInformation) {
        Event deleteEvent = Event.createDeleteEvent(instanceId.getSpace(), instanceId.getUuid(), idUtils.buildAbsoluteUrl(instanceId.getUuid()));
        handleExternalEventInformation(externalEventInformation, deleteEvent);
        return primaryStoreEvents.postEvent(deleteEvent, false);
    }


    private Event createUpsertEvent(UUID id, ExternalEventInformation externalEventInformation, NormalizedJsonLd normalizedJsonLd, SpaceName s) {
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
        NormalizedJsonLd instance = graphDBInstances.getInstanceById(nativeId.getSpace().getName(), nativeId.getUuid(), DataStage.NATIVE, true, false, false, null, true);
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
                if (value != null && value.equals(EBRAINSVocabulary.RESET_VALUE)) {
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

    public Map<String, Result<NormalizedJsonLd>> getInstancesByIds(List<String> ids, DataStage stage, ExtendedResponseConfiguration responseConfiguration) {
        Map<String, Result<NormalizedJsonLd>> result = new HashMap<>();
        List<UUID> validUUIDs = ids.stream().filter(Objects::nonNull).map(id -> {
            try {
                return UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
        List<InstanceId> idsAfterResolution = this.ids.resolveIdsByUUID(stage, validUUIDs, true);
        idsAfterResolution.stream().filter(instanceId -> !instanceId.isDeprecated()).filter(InstanceId::isUnresolved).forEach(id -> result.put(id.getUuid().toString(), Result.nok(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase())));
        Map<UUID, Result<NormalizedJsonLd>> instancesByIds = graphDBInstances.getInstancesByIds(idsAfterResolution.stream().filter(i -> !i.isUnresolved() && !i.isDeprecated()).map(InstanceId::serialize).collect(Collectors.toList()), stage, responseConfiguration.isReturnEmbedded(), responseConfiguration.isReturnAlternatives(), responseConfiguration.isReturnIncomingLinks(), responseConfiguration.getIncomingLinksPageSize());
        instancesByIds.forEach((k, v) -> result.put(k.toString(), v));
        ids.stream().filter(Objects::nonNull).forEach(
                id -> {
                    if (!result.containsKey(id)) {
                        result.put(id, Result.nok(HttpStatus.NOT_FOUND.value(), id));
                    }
                }
        );
        if (responseConfiguration.isReturnAlternatives()) {
            resolveAlternatives(stage, instancesByIds.values().stream().map(Result::getData).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        if (responseConfiguration.isReturnPermissions()) {
            enrichWithPermissionInformation(stage, instancesByIds.values());
        }
        return result;
    }

    public Paginated<NormalizedJsonLd> getInstances(DataStage stage, Type type, SpaceName space, String searchByLabel, ResponseConfiguration responseConfiguration, PaginationParam paginationParam) {
        Paginated<NormalizedJsonLd> instancesByType = graphDBInstances.getInstancesByType(stage, type != null ? type.getName() : null, space != null ? space.getName() : null, searchByLabel, responseConfiguration.isReturnAlternatives(), responseConfiguration.isReturnEmbedded(), responseConfiguration.isSortByLabel(), paginationParam);
        if (responseConfiguration.isReturnAlternatives()) {
            resolveAlternatives(stage, instancesByType.getData());
        }
        if (responseConfiguration.isReturnPermissions()) {
            enrichWithPermissionInformation(stage, instancesByType.getData().stream().map(Result::ok).collect(Collectors.toList()));
        }
        return instancesByType;
    }

    public ResponseEntity<Result<NormalizedJsonLd>> handleIngestionResponse(ResponseConfiguration responseConfiguration, Set<InstanceId> instanceIds) {
        Result<NormalizedJsonLd> result;
        if (responseConfiguration.isReturnPayload()) {
            Map<UUID, Result<NormalizedJsonLd>> instancesByIds = graphDBInstances.getInstancesByIds(instanceIds.stream().map(InstanceId::serialize).collect(Collectors.toList()),
                    DataStage.IN_PROGRESS,
                    responseConfiguration.isReturnEmbedded(),
                    responseConfiguration.isReturnAlternatives(),
                    responseConfiguration instanceof ExtendedResponseConfiguration && ((ExtendedResponseConfiguration) responseConfiguration).isReturnIncomingLinks(), responseConfiguration instanceof ExtendedResponseConfiguration ? ((ExtendedResponseConfiguration)responseConfiguration).getIncomingLinksPageSize(): null);
            if (responseConfiguration.isReturnAlternatives()) {
                resolveAlternatives(DataStage.IN_PROGRESS, instancesByIds.values().stream().map(Result::getData).collect(Collectors.toList()));
            }
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

    public Paginated<NormalizedJsonLd> getIncomingLinks(UUID id, DataStage stage, String property, Type type, PaginationParam pagination){
        InstanceId instanceId = ids.resolveId(stage, id);
        if(instanceId == null || instanceId.isDeprecated()){
            return null;
        }
        return graphDBInstances.getIncomingLinks(instanceId.getSpace().getName(), instanceId.getUuid(), stage, property, type.getName(), pagination);
    }

    public NormalizedJsonLd getInstanceById(UUID id, DataStage stage, ExtendedResponseConfiguration responseConfiguration) {
        InstanceId instanceId = ids.resolveId(stage, id);
        if (instanceId == null || instanceId.isDeprecated()) {
            return null;
        }
        if (responseConfiguration.isReturnPayload()) {
            NormalizedJsonLd instance = graphDBInstances.getInstanceById(instanceId.getSpace().getName(), instanceId.getUuid(), stage, responseConfiguration.isReturnEmbedded(), responseConfiguration.isReturnAlternatives(), responseConfiguration.isReturnIncomingLinks(), responseConfiguration.getIncomingLinksPageSize(), true);
            if (responseConfiguration.isReturnAlternatives()) {
                resolveAlternatives(stage, Collections.singletonList(instance));
            }
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

    private void resolveAlternatives(DataStage stage, List<NormalizedJsonLd> documents) {
        Map<String, List<Map<String, Object>>> idsForResolution = documents.stream()
                .map(d -> d.get(EBRAINSVocabulary.META_ALTERNATIVE)).filter(Objects::nonNull)
                .filter(a -> a instanceof Map).map(a -> ((Map<?, ?>) a).values()).flatMap(Collection::stream)
                .map(v -> v instanceof Collection ? (Collection<?>) v : Collections.singleton(v))
                .flatMap(Collection::stream).filter(value -> value instanceof Map)
                .map(value -> ((Map<?, ?>) value).get(EBRAINSVocabulary.META_VALUE))
                .filter(Objects::nonNull)
                .map(v -> v instanceof Collection ? (Collection<?>) v : Collections.singleton(v))
                .flatMap(Collection::stream).filter(v -> {
                    if (v instanceof Map) {
                        Object id = ((Map<?, ?>) v).get(JsonLdConsts.ID);
                        return id instanceof String && !idUtils.isInternalId((String) id);
                    }
                    return false;
                }).map(v -> (Map<String, Object>) v)
                .collect(Collectors.groupingBy(k -> (String) k.get(JsonLdConsts.ID)));
        Map<UUID, String> requestToIdentifier = new HashMap<>();
        idsForResolution.keySet().forEach(id -> requestToIdentifier.put(UUID.randomUUID(), id));
        List<IdWithAlternatives> idWithAlternatives = requestToIdentifier.keySet().stream()
                .map(k -> new IdWithAlternatives(k, null, Collections.singleton(requestToIdentifier.get(k))))
                .collect(Collectors.toList());
        List<JsonLdIdMapping> mappings = ids.resolveIds(stage, idWithAlternatives);

        Map<UUID, Set<Map<String, Object>>> updatedObjects = new HashMap<>();
        Set<InstanceId> instanceIds = new HashSet<>();
        mappings.stream().forEach(mapping -> {
            if (mapping.getResolvedIds() != null && mapping.getResolvedIds().size() == 1) {
                JsonLdId resolvedId = mapping.getResolvedIds().iterator().next();
                String requestedIdentifier = requestToIdentifier.get(mapping.getRequestedId());
                List<Map<String, Object>> objectsToBeUpdated = idsForResolution.get(requestedIdentifier);
                objectsToBeUpdated.forEach(o -> {
                    o.put(JsonLdConsts.ID, resolvedId.getId());
                    UUID uuid = idUtils.getUUID(resolvedId);
                    instanceIds.add(new InstanceId(uuid, mapping.getSpace()));
                    updatedObjects.computeIfAbsent(uuid, x -> new HashSet<>()).add(o);
                });
            }
        });
        Map<UUID, String> labels = graphDBInstances.getLabels(instanceIds.stream().map(InstanceId::serialize).collect(Collectors.toList()), stage);
        for (UUID uuid : labels.keySet()) {
            updatedObjects.get(uuid).forEach(o -> o.put(SchemaOrgVocabulary.NAME, labels.get(uuid)));
        }
        //Alternatives are a special case -> we merge the values, so this means we're actually always having a single object at once max. Therefore, let's get rid of the wrapping array
        documents.forEach(d -> {
            List<NormalizedJsonLd> alternatives = d.getAsListOf(EBRAINSVocabulary.META_ALTERNATIVE, NormalizedJsonLd.class);
            if (!alternatives.isEmpty()) {
                d.put(EBRAINSVocabulary.META_ALTERNATIVE, alternatives.get(0));
            } else {
                d.put(EBRAINSVocabulary.META_ALTERNATIVE, null);
            }
        });

    }


    private void enrichWithPermissionInformation(DataStage stage, Collection<Result<NormalizedJsonLd>> documents) {
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();
        documents.forEach(result -> {
                    NormalizedJsonLd doc = result.getData();
                    if(doc!=null) {
                        String space = doc.getAs(EBRAINSVocabulary.META_SPACE, String.class);
                        SpaceName sp = space != null ? new SpaceName(space) : null;
                        Set<Functionality> functionalities = permissions.stream().filter(p -> Functionality.FunctionalityGroup.INSTANCE == p.getFunctionality().getFunctionalityGroup() && stage != null && stage == p.getFunctionality().getStage()).filter(p -> p.appliesTo(sp, idUtils.getUUID(doc.id()))).map(FunctionalityInstance::getFunctionality).collect(Collectors.toSet());
                        doc.put(EBRAINSVocabulary.META_PERMISSIONS, functionalities);
                    }
                }
        );
    }

    private void enrichWithPermissionInformation(DataStage stage, ScopeElement scopeElement, List<FunctionalityInstance> permissions) {
        SpaceName sp = scopeElement.getSpace() != null ? new SpaceName(scopeElement.getSpace()) : null;
        scopeElement.setPermissions(permissions.stream().filter(p -> Functionality.FunctionalityGroup.INSTANCE == p.getFunctionality().getFunctionalityGroup() && stage != null && stage == p.getFunctionality().getStage()).filter(p -> p.appliesTo(sp, scopeElement.getId())).map(FunctionalityInstance::getFunctionality).collect(Collectors.toSet()));
        if (scopeElement.getChildren() != null) {
            scopeElement.getChildren().forEach(c -> enrichWithPermissionInformation(stage, c, permissions));
        }
    }

    public ScopeElement getScopeForInstance(UUID id, DataStage stage, boolean returnPermissions) {
        InstanceId instanceId = ids.resolveId(stage, id);
        if (instanceId != null) {
            ScopeElement scope = graphDBScopes.getScopeForInstance(instanceId.getSpace().getName(), instanceId.getUuid(), stage, true);
            if (returnPermissions) {
                enrichWithPermissionInformation(stage, scope, authContext.getUserWithRoles().getPermissions());
            }
            return scope;
        }
        return null;
    }

    public GraphEntity getNeighbors(UUID id, DataStage stage) {
        InstanceId instanceId = ids.resolveId(stage, id);
        if (instanceId != null) {
            return graphDBInstances.getNeighbors(instanceId.getSpace().getName(), instanceId.getUuid(), stage);
        }
        return null;
    }
}

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

package eu.ebrains.kg.core.controller;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.GraphDBInstances;
import eu.ebrains.kg.commons.api.GraphDBScopes;
import eu.ebrains.kg.commons.api.Invitation;
import eu.ebrains.kg.commons.api.PrimaryStoreEvents;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.exception.InstanceNotFoundException;
import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final PrimaryStoreEvents.Client primaryStoreEvents;
    private final Invitation.Client invitation;
    private final Permissions permissions;


    private final Logger logger = LoggerFactory.getLogger(getClass());

    public CoreInstanceController(GraphDBInstances.Client graphDBInstances, GraphDBScopes.Client graphDBScopes, IdsController ids, AuthContext authContext, IdUtils idUtils, PrimaryStoreEvents.Client primaryStoreEvents, Invitation.Client invitation, Permissions permissions) {
        this.graphDBInstances = graphDBInstances;
        this.graphDBScopes = graphDBScopes;
        this.ids = ids;
        this.authContext = authContext;
        this.idUtils = idUtils;
        this.primaryStoreEvents = primaryStoreEvents;
        this.invitation = invitation;
        this.permissions = permissions;
    }

    public void createInvitation(UUID instanceId, UUID userId) {
        //TODO move permission check to authentication module
        final InstanceId resolvedInstanceId = resolveIdOrThrowException(instanceId);
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.INVITE_FOR_REVIEW, resolvedInstanceId.getSpace(), instanceId)) {
            throw new UnauthorizedException("You don't have the right to invite somebody to this instance.");
        }
        this.invitation.inviteUserForInstance(instanceId, userId);
    }

    private InstanceId resolveIdOrThrowException(UUID instanceId) {
        final InstanceId resolvedInstanceId = ids.resolveId(DataStage.IN_PROGRESS, instanceId);
        if(resolvedInstanceId == null) {
            throw new InstanceNotFoundException(String.format("Instance %s not found", instanceId));
        }
        return resolvedInstanceId;
    }

    public void revokeInvitation(UUID instanceId, UUID userId) {
        //TODO move permission check to authentication module
        final InstanceId resolvedInstanceId = resolveIdOrThrowException(instanceId);
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.INVITE_FOR_REVIEW, resolvedInstanceId.getSpace(), instanceId)) {
            throw new UnauthorizedException("You don't have the right to invite somebody to this instance.");
        }
        this.invitation.revokeUserInvitation(instanceId, userId);
    }

    public List<String> listInvitedUserIds(UUID instanceId) {
        //TODO move permission check to authentication module
        final InstanceId resolvedInstanceId = resolveIdOrThrowException(instanceId);
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.INVITE_FOR_REVIEW, resolvedInstanceId.getSpace(), instanceId)) {
            throw new UnauthorizedException("You don't have the right to list the invitations for this instance");
        }
        return this.invitation.listInvitedUserIds(instanceId);
    }

    public List<UUID> listInstancesWithInvitations() {
        if (!permissions.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.LIST_INVITATIONS)) {
            throw new UnauthorizedException("You don't have the right to list instances with invitations");
        }
        return this.invitation.listInstances();
    }

    public void calculateInstanceInvitationScope(UUID instanceId) {
        final InstanceId resolvedInstanceId = resolveIdOrThrowException(instanceId);
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.UPDATE_INVITATIONS, resolvedInstanceId.getSpace(), instanceId)) {
            throw new UnauthorizedException("You don't have the right to recalculate the invitation scope for this instance");
        }
        this.invitation.calculateInstanceScope(instanceId);
    }

    public ResponseEntity<Result<NormalizedJsonLd>> createNewInstance(NormalizedJsonLd normalizedJsonLd, UUID id, SpaceName s, ExtendedResponseConfiguration responseConfiguration) {
        ids.checkIdForExistence(id, normalizedJsonLd.allIdentifiersIncludingId());
        normalizedJsonLd.defineFieldUpdateTimes(normalizedJsonLd.keySet().stream().collect(Collectors.toMap(k -> k, k -> ZonedDateTime.now())));
        Event upsertEvent = createUpsertEvent(id, normalizedJsonLd, s);
        Set<InstanceId> ids = primaryStoreEvents.postEvent(upsertEvent);
        return handleIngestionResponse(responseConfiguration, ids);
    }


    public ResponseEntity<Result<NormalizedJsonLd>> contributeToInstance(NormalizedJsonLd normalizedJsonLd, InstanceId instanceId, boolean removeNonDeclaredProperties, ResponseConfiguration responseConfiguration) {
        normalizedJsonLd = patchInstance(instanceId, normalizedJsonLd, removeNonDeclaredProperties);
        Event upsertEvent = createUpsertEvent(instanceId.getUuid(), normalizedJsonLd, instanceId.getSpace());
        Set<InstanceId> ids = primaryStoreEvents.postEvent(upsertEvent);
        return handleIngestionResponse(responseConfiguration, ids);
    }


    public Set<InstanceId> deleteInstance(InstanceId instanceId) {
        Event deleteEvent = Event.createDeleteEvent(instanceId.getSpace(), instanceId.getUuid(), idUtils.buildAbsoluteUrl(instanceId.getUuid()));
        return primaryStoreEvents.postEvent(deleteEvent);
    }

    public ResponseEntity<Result<NormalizedJsonLd>> moveInstance(InstanceId instanceId, SpaceName targetSpace, ExtendedResponseConfiguration responseConfiguration) {
        NormalizedJsonLd instance = graphDBInstances.getInstanceById(instanceId.getSpace().getName(), instanceId.getUuid(), DataStage.IN_PROGRESS, true, false, false, null, true);
        if (instance == null) {
            throw new InstanceNotFoundException(String.format("Instance %s not found", instanceId.getUuid()));
        } else {
            if (permissions.hasPermission(authContext.getUserWithRoles(), Functionality.CREATE, targetSpace)) {
                //FIXME make this transactional.
                deleteInstance(instanceId);
                return createNewInstance(instance, instanceId.getUuid(), targetSpace, responseConfiguration);
            } else {
                throw new ForbiddenException(String.format("You are not allowed to move an instance to the space %s", targetSpace));
            }
        }
    }


    private Event createUpsertEvent(UUID id, NormalizedJsonLd normalizedJsonLd, SpaceName s) {
        return Event.createUpsertEvent(s, id, Event.Type.INSERT, normalizedJsonLd);
    }


    private NormalizedJsonLd patchInstance(InstanceId instanceId, NormalizedJsonLd normalizedJsonLd, boolean removeNonDefinedKeys) {
        InstanceId nativeId = new InstanceId(idUtils.getDocumentIdForUserAndInstance(authContext.getUserId(), instanceId.getUuid()), instanceId.getSpace());
        NormalizedJsonLd instance = graphDBInstances.getInstanceById(nativeId.getSpace().getName(), nativeId.getUuid(), DataStage.NATIVE, true, false, false, null, true);
        if (instance == null) {
            Map<String, ZonedDateTime> updateTimes = new HashMap<>();
            normalizedJsonLd.keySet().forEach(k -> updateTimes.put(k, ZonedDateTime.now()));
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
                    updateTimes.put(k, ZonedDateTime.now());
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

    public Map<String, Result<NormalizedJsonLd>> getInstancesByIds(List<String> ids, DataStage stage, ExtendedResponseConfiguration responseConfiguration, String typeRestriction) {
        Map<String, Result<NormalizedJsonLd>> result = new HashMap<>();
        List<UUID> validUUIDs = ids.stream().filter(Objects::nonNull).map(id -> {
            try {
                return UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
        List<InstanceId> idsAfterResolution = this.ids.resolveIdsByUUID(stage, validUUIDs, true);
        idsAfterResolution.stream().filter(InstanceId::isUnresolved).forEach(id -> result.put(id.getUuid().toString(), Result.nok(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase())));
        if (responseConfiguration.isReturnPayload()) {
            Map<UUID, Result<NormalizedJsonLd>> instancesByIds = graphDBInstances.getInstancesByIds(idsAfterResolution.stream().filter(i -> !i.isUnresolved()).map(InstanceId::serialize).collect(Collectors.toList()), stage, typeRestriction, responseConfiguration.isReturnEmbedded(), responseConfiguration.isReturnAlternatives(), responseConfiguration.isReturnIncomingLinks(), responseConfiguration.getIncomingLinksPageSize());
            final SpaceName privateSpaceName = authContext.getUserWithRoles().getPrivateSpace();
            instancesByIds.forEach((k, v) -> {
                if (v.getData() != null) {
                    v.getData().renameSpace(privateSpaceName, isInvited(v.getData()));
                }
                result.put(k.toString(), v);
            });
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
        } else {
            idsAfterResolution.forEach(idAfterResolution -> {
                NormalizedJsonLd idPayload = graphDBInstances.getInstanceByIdAndPayload(idAfterResolution.getSpace().getName(), idAfterResolution.getUuid(), stage, responseConfiguration.isReturnEmbedded(), responseConfiguration.isReturnAlternatives(), responseConfiguration.isReturnIncomingLinks(), responseConfiguration.getIncomingLinksPageSize(), true);
                UUID uuid = idAfterResolution.getUuid();
                idPayload.setId(idUtils.buildAbsoluteUrl(uuid));
                result.put(uuid.toString(), Result.ok(idPayload));
            });
        }
        if (responseConfiguration.isReturnPermissions()) {
            enrichWithPermissionInformation(stage, result.values());
        }
        if (!responseConfiguration.isReturnPayload()) {
            result.values().forEach(r -> {
                    removeSpace(r.getData());
            });
        }
        return result;
    }

    public Paginated<NormalizedJsonLd> getInstances(DataStage stage, Type type, SpaceName space, String searchByLabel, String filterProperty, String filterValue, ResponseConfiguration responseConfiguration, PaginationParam paginationParam) {
        Paginated<NormalizedJsonLd> instancesByType = graphDBInstances.getInstancesByType(stage, type != null ? type.getName() : null, space != null ? space.getName() : null, searchByLabel, filterProperty, filterValue, responseConfiguration.isReturnAlternatives(), responseConfiguration.isReturnEmbedded(), paginationParam);
        Paginated<NormalizedJsonLd> result;

        if (responseConfiguration.isReturnPayload()) {
            if (responseConfiguration.isReturnAlternatives()) {
                resolveAlternatives(stage, instancesByType.getData());
            }
            final SpaceName privateSpaceName = authContext.getUserWithRoles().getPrivateSpace();
            instancesByType.getData().forEach(d -> d.renameSpace(privateSpaceName, isInvited(d)));
            result = instancesByType;
        } else {
            List<NormalizedJsonLd> newResult = new ArrayList<>();
            instancesByType.getData().forEach(r -> {
                final JsonLdId id = r.id();
                if (id != null) {
                    NormalizedJsonLd jsonLd = new NormalizedJsonLd();
                    jsonLd.setId(id);
                    newResult.add(jsonLd);
                }
            });
            result = new Paginated<>(newResult, instancesByType.getTotalResults(), instancesByType.getSize(), instancesByType.getFrom());
        }

        if (responseConfiguration.isReturnPermissions()) {
            enrichWithPermissionInformation(stage, result.getData().stream().map(Result::ok).collect(Collectors.toList()));
        }


        return result;
    }

    public ResponseEntity<Result<NormalizedJsonLd>> handleIngestionResponse(ResponseConfiguration responseConfiguration, Set<InstanceId> instanceIds) {
        Result<NormalizedJsonLd> result;
        if (responseConfiguration.isReturnPayload()) {
            Map<UUID, Result<NormalizedJsonLd>> instancesByIds = graphDBInstances.getInstancesByIds(instanceIds.stream().map(InstanceId::serialize).collect(Collectors.toList()),
                    DataStage.IN_PROGRESS, null,
                    responseConfiguration.isReturnEmbedded(),
                    responseConfiguration.isReturnAlternatives(),
                    responseConfiguration instanceof ExtendedResponseConfiguration && ((ExtendedResponseConfiguration) responseConfiguration).isReturnIncomingLinks(), responseConfiguration instanceof ExtendedResponseConfiguration ? ((ExtendedResponseConfiguration) responseConfiguration).getIncomingLinksPageSize() : null);
            if (responseConfiguration.isReturnAlternatives()) {
                resolveAlternatives(DataStage.IN_PROGRESS, instancesByIds.values().stream().map(Result::getData).collect(Collectors.toList()));
            }
            if (responseConfiguration.isReturnPermissions()) {
                enrichWithPermissionInformation(DataStage.IN_PROGRESS, instancesByIds.values());
            }
            final SpaceName privateSpaceName = authContext.getUserWithRoles().getPrivateSpace();
            result = AmbiguousResult.ok(instancesByIds.values().stream().map(Result::getData).map(r -> r.renameSpace(privateSpaceName, isInvited(r))).collect(Collectors.toList()));
        } else {
            result = AmbiguousResult.ok(instanceIds.stream().map(id -> {
                NormalizedJsonLd jsonLd = new NormalizedJsonLd();
                jsonLd.setId(idUtils.buildAbsoluteUrl(id.getUuid()));
                return jsonLd;
            }).collect(Collectors.toList()));
        }
        return result instanceof AmbiguousResult ? ResponseEntity.status(HttpStatus.CONFLICT).body(result) : ResponseEntity.ok(result);
    }

    public Paginated<NormalizedJsonLd> getIncomingLinks(UUID id, DataStage stage, String property, Type type, PaginationParam pagination) {
        InstanceId instanceId = ids.resolveId(stage, id);
        if (instanceId == null) {
            return null;
        }
        final SpaceName privateSpaceName = authContext.getUserWithRoles().getPrivateSpace();
        final Paginated<NormalizedJsonLd> incomingLinks = graphDBInstances.getIncomingLinks(instanceId.getSpace().getName(), instanceId.getUuid(), stage, property, type.getName(), pagination);
        incomingLinks.getData().forEach(d -> d.renameSpace(privateSpaceName, isInvited(d)));
        return incomingLinks;
    }

    public NormalizedJsonLd getInstanceById(UUID id, DataStage stage, ExtendedResponseConfiguration responseConfiguration) {
        InstanceId instanceId = ids.resolveId(stage, id);
        if (instanceId == null) {
            return null;
        }
        NormalizedJsonLd instance;
        if (responseConfiguration.isReturnPayload()) {
            instance = graphDBInstances.getInstanceById(instanceId.getSpace().getName(), instanceId.getUuid(), stage, responseConfiguration.isReturnEmbedded(), responseConfiguration.isReturnAlternatives(), responseConfiguration.isReturnIncomingLinks(), responseConfiguration.getIncomingLinksPageSize(), true);
            if (responseConfiguration.isReturnAlternatives()) {
                resolveAlternatives(stage, Collections.singletonList(instance));
            }
            if (responseConfiguration.isReturnPermissions() && instance != null) {
                enrichWithPermissionInformation(stage, Collections.singletonList(Result.ok(instance)));
            }
            if (instance != null) {
                instance.renameSpace(authContext.getUserWithRoles().getPrivateSpace(), isInvited(instance));
            }
        } else {
            if (!responseConfiguration.isReturnPermissions() && !responseConfiguration.isReturnIncomingLinks()) {
                NormalizedJsonLd idPayload = new NormalizedJsonLd();
                idPayload.setId(idUtils.buildAbsoluteUrl(instanceId.getUuid()));
                return idPayload;
            } else {
                instance = graphDBInstances.getInstanceByIdAndPayload(instanceId.getSpace().getName(), instanceId.getUuid(), stage, responseConfiguration.isReturnEmbedded(),responseConfiguration.isReturnAlternatives(), responseConfiguration.isReturnIncomingLinks(), responseConfiguration.getIncomingLinksPageSize(), true);
                if (responseConfiguration.isReturnPermissions() && instance != null) {
                    enrichWithPermissionInformation(stage, Collections.singletonList(Result.ok(instance)));
                }
                removeSpace(instance);
            }
        }

        return instance;
    }
    public boolean isInvited(NormalizedJsonLd normalizedJsonLd) {
        final UUID uuid = idUtils.getUUID(normalizedJsonLd.id());
        if (authContext.getUserWithRolesWithoutTermsCheck().getInvitations().contains(uuid)) {
            //The user is invited for this instance
            final String space = normalizedJsonLd.getAs(EBRAINSVocabulary.META_SPACE, String.class, null);
            if(space!=null){
                return !permissions.hasPermission(authContext.getUserWithRolesWithoutTermsCheck(), Functionality.READ, SpaceName.fromString(space));
            }
        }
        return false;
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
        Map<UUID, InstanceId> resolvedIds = ids.resolveIds(stage, idWithAlternatives);
        Map<UUID, Set<Map<String, Object>>> updatedObjects = new HashMap<>();
        Set<InstanceId> instanceIds = new HashSet<>();
        Map<String, InstanceId> instanceIdByIdentifier = new HashMap<>();
        resolvedIds.keySet().forEach(uuid -> instanceIdByIdentifier.put(requestToIdentifier.get(uuid), resolvedIds.get(uuid)));
        instanceIdByIdentifier.keySet().forEach(requestedIdentifier -> {
            List<Map<String, Object>> objectsToBeUpdated = idsForResolution.get(requestedIdentifier);
            objectsToBeUpdated.forEach(o -> {
                final InstanceId instanceId = instanceIdByIdentifier.get(requestedIdentifier);
                if (instanceId != null) {
                    o.put(JsonLdConsts.ID, idUtils.buildAbsoluteUrl(instanceId.getUuid()).getId());
                    instanceIds.add(instanceId);
                    updatedObjects.computeIfAbsent(instanceId.getUuid(), x -> new HashSet<>()).add(o);
                }
            });
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
                    if (doc != null) {
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

    public ScopeElement getScopeForInstance(UUID id, DataStage stage, boolean returnPermissions, boolean applyRestrictions) {
        InstanceId instanceId = ids.resolveId(stage, id);
        if (instanceId != null) {
            ScopeElement scope = graphDBScopes.getScopeForInstance(instanceId.getSpace().getName(), instanceId.getUuid(), stage, applyRestrictions);
            if (returnPermissions) {
                enrichWithPermissionInformation(stage, scope, authContext.getUserWithRoles().getPermissions());
            }
            return scope;
        }
        return null;
    }

    public void removeSpace(NormalizedJsonLd instance) {
        instance.keySet().removeIf(CoreInstanceController::isSpaceKey);
    }

    public static boolean isSpaceKey(String key) {
        return key.equals(EBRAINSVocabulary.META_SPACE);
    }

    public GraphEntity getNeighbors(UUID id, DataStage stage) {
        InstanceId instanceId = ids.resolveId(stage, id);
        if (instanceId != null) {
            return graphDBInstances.getNeighbors(instanceId.getSpace().getName(), instanceId.getUuid(), stage);
        }
        return null;
    }
}

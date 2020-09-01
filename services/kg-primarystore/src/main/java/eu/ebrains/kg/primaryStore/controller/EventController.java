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

package eu.ebrains.kg.primaryStore.controller;

import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permissions.controller.PermissionSvc;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.primaryStore.serviceCall.PrimaryStoreToGraphDB;
import eu.ebrains.kg.primaryStore.serviceCall.PrimaryStoreToIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class EventController {

    private final PermissionSvc permissionSvc;
    private final PrimaryStoreToIds idsSvc;
    private final EventRepository eventRepository;
    private final IdUtils idUtils;
    private final PrimaryStoreToGraphDB graphDBSvc;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EventController(PermissionSvc permissionSvc, PrimaryStoreToIds idsSvc, EventRepository eventRepository, IdUtils idUtils, PrimaryStoreToGraphDB graphDBSvc) {
        this.permissionSvc = permissionSvc;
        this.idsSvc = idsSvc;
        this.eventRepository = eventRepository;
        this.idUtils = idUtils;
        this.graphDBSvc = graphDBSvc;
    }

    private boolean isEventOfType(PersistedEvent event, String type) {
        return event.getData() != null && event.getData().getTypes() != null ? event.getData().getTypes().contains(type) : false;
    }

    private void checkPermission(AuthTokens authTokens, PersistedEvent event, DataStage stage, Event.Type type, Space space, UUID uuid, UserWithRoles userWithRoles) {
        boolean hasPermission = false;
        List<String> semantics = event.getData() != null && event.getData().getTypes() != null ? event.getData().getTypes() : Collections.emptyList();
        Functionality functionality;
        switch (type) {
            case DELETE:
                functionality = Functionality.withSemanticsForOperation(semantics, type, Functionality.DELETE);
                hasPermission = permissionSvc.hasPermission(userWithRoles, functionality, space, uuid);
                break;
            case INSERT:
                NormalizedJsonLd sp = graphDBSvc.getSpace(space, stage, authTokens);
                if (sp == null) {
                    //The space doesn't exist - this means the user has to have space creation rights to execute this insertion.
                    boolean spaceCreationPermission = permissionSvc.hasPermission(userWithRoles, Functionality.CREATE_SPACE, space);
                    if (!spaceCreationPermission) {
                        throw new ForbiddenException(String.format("The creation of this instance involves the creation of the non-existing space %s - you don't have the according rights to do so!", space.getName()));
                    }
                }
                functionality = Functionality.withSemanticsForOperation(semantics, type, Functionality.CREATE);
                hasPermission = permissionSvc.hasPermission(userWithRoles, functionality, space, uuid);
                break;
            case UPDATE:
                functionality = Functionality.withSemanticsForOperation(semantics, type, Functionality.WRITE);
                hasPermission = permissionSvc.hasPermission(userWithRoles, functionality, space, uuid);
                if (!hasPermission && functionality == Functionality.WRITE) {
                    hasPermission = permissionSvc.hasPermission(userWithRoles, Functionality.SUGGEST, space, uuid);
                    event.setSuggestion(true);
                }
                break;
            case RELEASE:
                hasPermission = permissionSvc.hasPermission(userWithRoles, Functionality.withSemanticsForOperation(semantics, type, Functionality.RELEASE), space, uuid);
                break;
            case UNRELEASE:
                hasPermission = permissionSvc.hasPermission(userWithRoles, Functionality.withSemanticsForOperation(semantics, type, Functionality.UNRELEASE), space, uuid);
                break;
        }
        if (!hasPermission) {
            throw new ForbiddenException();
        }
    }

    public PersistedEvent persistEvent(AuthTokens authTokens, Event event, DataStage dataStage, UserWithRoles userWithRoles, User resolvedUser) {
        PersistedEvent persistedEvent = new PersistedEvent(event, dataStage, resolvedUser);
        ensureInternalIdInPayload(userWithRoles, persistedEvent);

        checkPermission(authTokens, persistedEvent, dataStage, event.getType(), event.getSpace(), event.getDocumentId(), userWithRoles);
        if (persistedEvent.getType() == Event.Type.DELETE) {
            idsSvc.deprecateInstance(persistedEvent.getDocumentId(), authTokens);
        } else {
            if (dataStage == DataStage.IN_PROGRESS) {
                ensureMergeOfIdentifiers(authTokens, persistedEvent, dataStage);
                List<JsonLdId> mergedIds = idsSvc.upsert(dataStage, new IdWithAlternatives(persistedEvent.getDocumentId(), persistedEvent.getSpace(), persistedEvent.getData().getIdentifiers()), authTokens);
                if (mergedIds != null) {
                    persistedEvent.setMergedIds(mergedIds);
                }
            }
            addMetaInformationToData(dataStage, persistedEvent);
        }
        eventRepository.insert(persistedEvent);
        return persistedEvent;
    }

    private void ensureInternalIdInPayload(UserWithRoles user, PersistedEvent persistedEvent) {
        if (persistedEvent.getData() != null) {
            JsonLdId idFromPayload = persistedEvent.getData().getId();
            if (idFromPayload != null) {
                //Save the original id as an "identifier"
                persistedEvent.getData().addIdentifiers(idFromPayload.getId());
            }
            persistedEvent.getData().setId(idUtils.buildAbsoluteUrl(persistedEvent.getDocumentId()));
            //In the native space, we store the document separately for every user - this means the documents are actual contributions to an instance.
            if (persistedEvent.getDataStage() == DataStage.NATIVE) {
                persistedEvent.setInstance(persistedEvent.getSpace(), UUID.nameUUIDFromBytes(String.format("%s-%s", persistedEvent.getDocumentId(), persistedEvent.getUser() != null ? persistedEvent.getUser().getNativeId() : user.getUser().getNativeId()).getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    private void ensureMergeOfIdentifiers(AuthTokens authTokens, PersistedEvent persistedEvent, DataStage dataStage) {
        //If we're in the inProgress stage, we look up if there are merges needed
        Set<JsonLdId> resolvedIds = idsSvc.resolveIds(dataStage, persistedEvent.getDocumentId(), persistedEvent.getData().getIdentifiers(), persistedEvent.getSpace(), authTokens);
        if (resolvedIds != null && !resolvedIds.isEmpty()) {
            if (resolvedIds.size() > 1) {
                logger.debug("Found an ambiguous id - this means a new instance id will be created and the ids will be merged...");
                persistedEvent.getData().addIdentifiers(idUtils.buildAbsoluteUrl(persistedEvent.getDocumentId()).getId());
                persistedEvent.getData().addIdentifiers(resolvedIds.stream().map(JsonLdId::getId).distinct().toArray(String[]::new));
                persistedEvent.setInstance(persistedEvent.getSpace(), UUID.randomUUID());
            }
        }
    }

    private void addMetaInformationToData(DataStage dataStage, PersistedEvent event) {
        IndexedJsonLdDoc data = IndexedJsonLdDoc.from(event.getData());
        //We don't need the document id except for the native space.
        data.setDocumentId(dataStage == DataStage.NATIVE ? event.getDocumentId() : null);
        data.setTimestamp(event.getReportedTimeStampInMs());
        data.setIndexTimestamp(event.getIndexedTimestamp());
        if (dataStage == DataStage.RELEASED) {
            data.getDoc().put(EBRAINSVocabulary.META_LAST_RELEASED_AT, ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT));
        }
    }

}

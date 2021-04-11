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

package eu.ebrains.kg.primaryStore.controller;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.GraphDBSpaces;
import eu.ebrains.kg.commons.api.Ids;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class EventController {

    private final Permissions permissions;
    private final Ids.Client ids;
    private final EventRepository eventRepository;
    private final IdUtils idUtils;
    private final GraphDBSpaces.Client graphDBSpaces;
    private final UserResolver userResolver;
    private final AuthContext authContext;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EventController(Permissions permissions, Ids.Client ids, EventRepository eventRepository, IdUtils idUtils, GraphDBSpaces.Client graphDBSpaces, UserResolver userResolver, AuthContext authContext) {
        this.permissions = permissions;
        this.ids = ids;
        this.eventRepository = eventRepository;
        this.idUtils = idUtils;
        this.graphDBSpaces = graphDBSpaces;
        this.userResolver = userResolver;
        this.authContext = authContext;
    }

    private void checkPermission(PersistedEvent event) {
        boolean hasPermission = false;
        List<String> semantics = event.getData() != null && event.getData().types() != null ? event.getData().types() : Collections.emptyList();
        Functionality functionality;
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        switch (event.getType()) {
            case DELETE:
                functionality = Functionality.withSemanticsForOperation(semantics, event.getType(), Functionality.DELETE);
                hasPermission = permissions.hasPermission(userWithRoles, functionality, event.getSpaceName(), event.getDocumentId());
                break;
            case INSERT:
                if (event.getSpace() == null) {
                    //The space doesn't exist - this means the user has to have space creation rights to execute this insertion.
                    boolean spaceCreationPermission = permissions.hasPermission(userWithRoles, Functionality.CREATE_SPACE, event.getSpaceName());
                    if (!spaceCreationPermission) {
                        throw new ForbiddenException(String.format("The creation of this instance involves the creation of the non-existing space %s - you don't have the according rights to do so!", event.getSpaceName()));
                    }
                }
                functionality = Functionality.withSemanticsForOperation(semantics, event.getType(), Functionality.CREATE);
                hasPermission = permissions.hasPermission(userWithRoles, functionality, event.getSpaceName(),  event.getDocumentId());
                break;
            case UPDATE:
                functionality = Functionality.withSemanticsForOperation(semantics, event.getType(), Functionality.WRITE);
                hasPermission = permissions.hasPermission(userWithRoles, functionality, event.getSpaceName(),  event.getDocumentId());
                if (!hasPermission && functionality == Functionality.WRITE) {
                    hasPermission = permissions.hasPermission(userWithRoles, Functionality.SUGGEST, event.getSpaceName(),  event.getDocumentId());
                    event.setSuggestion(true);
                }
                break;
            case RELEASE:
                hasPermission = permissions.hasPermission(userWithRoles, Functionality.withSemanticsForOperation(semantics, event.getType(), Functionality.RELEASE), event.getSpaceName(),  event.getDocumentId());
                break;
            case UNRELEASE:
                hasPermission = permissions.hasPermission(userWithRoles, Functionality.withSemanticsForOperation(semantics, event.getType(), Functionality.UNRELEASE), event.getSpaceName(),  event.getDocumentId());
                break;
            case META_DEPRECATION:
                hasPermission = permissions.hasGlobalPermission(userWithRoles, Functionality.DEFINE_TYPES);
                break;
        }
        if (!hasPermission) {
            throw new ForbiddenException();
        }
    }

    public PersistedEvent persistEvent(Event event, DataStage dataStage) {
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        logger.info(String.format("Received event of type %s for instance %s in space %s by user %s via client %s", event.getType().name(), event.getDocumentId(), event.getSpaceName() != null ? event.getSpaceName().getName() : null, userWithRoles != null && userWithRoles.getUser() != null ? userWithRoles.getUser().getUserName() : "anonymous", userWithRoles != null && userWithRoles.getClientId() != null ? userWithRoles.getClientId() : "unknown"));
        User user = userResolver.resolveUser(event);
        PersistedEvent persistedEvent = new PersistedEvent(event, dataStage, user, Space.fromJsonLd(graphDBSpaces.getSpace(dataStage, event.getSpaceName() != null ? event.getSpaceName().getName() : null)));
        ensureInternalIdInPayload(persistedEvent, userWithRoles);
        checkPermission(persistedEvent);
        if (persistedEvent.getType() == Event.Type.DELETE) {
            ids.deprecateId(DataStage.IN_PROGRESS, persistedEvent.getDocumentId(), false);
        } else {
            switch (dataStage){
                case IN_PROGRESS:
                case RELEASED:
                    ensureMergeOfIdentifiers(persistedEvent, dataStage);
                    List<JsonLdId> mergedIds = ids.createOrUpdateId(new IdWithAlternatives(persistedEvent.getDocumentId(), persistedEvent.getSpaceName(), persistedEvent.getData().identifiers()), dataStage);
                    if (mergedIds != null) {
                        persistedEvent.setMergedIds(mergedIds);
                    }
                    break;
            }
            addMetaInformationToData(dataStage, persistedEvent);
        }
        eventRepository.insert(persistedEvent);
        return persistedEvent;
    }

    private void ensureInternalIdInPayload(PersistedEvent persistedEvent, UserWithRoles userWithRoles) {
        if (persistedEvent.getData() != null) {
            JsonLdId idFromPayload = persistedEvent.getData().id();
            if (idFromPayload != null) {
                //Save the original id as an "identifier"
                persistedEvent.getData().addIdentifiers(idFromPayload.getId());
            }
            persistedEvent.getData().setId(idUtils.buildAbsoluteUrl(persistedEvent.getDocumentId()));
            //In the native space, we store the document separately for every user - this means the documents are actual contributions to an instance.
            if (persistedEvent.getDataStage() == DataStage.NATIVE) {
                UUID userSpecificUUID = idUtils.getDocumentIdForUserAndInstance(persistedEvent.getUser() != null ? persistedEvent.getUser().getNativeId() : userWithRoles.getUser().getNativeId(), persistedEvent.getDocumentId());
                persistedEvent.setInstance(persistedEvent.getSpaceName(), userSpecificUUID);
            }
        }
    }

    private void ensureMergeOfIdentifiers(PersistedEvent persistedEvent, DataStage dataStage) {
        //If we're in the inProgress stage, we look up if there are merges needed
        Set<JsonLdId> resolvedIds = resolveIds(dataStage, persistedEvent.getDocumentId(), persistedEvent.getData().identifiers(), persistedEvent.getSpaceName());
        if (resolvedIds != null && !resolvedIds.isEmpty()) {
            if (resolvedIds.size() > 1) {
                logger.debug("Found an ambiguous id - this means a new instance id will be created and the ids will be merged...");
                persistedEvent.getData().addIdentifiers(idUtils.buildAbsoluteUrl(persistedEvent.getDocumentId()).getId());
                persistedEvent.getData().addIdentifiers(resolvedIds.stream().map(JsonLdId::getId).distinct().toArray(String[]::new));
                persistedEvent.setInstance(persistedEvent.getSpaceName(), UUID.randomUUID());
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


    private Set<JsonLdId> resolveIds(DataStage stage, UUID id, Set<String> alternativeIds, SpaceName space) {
        IdWithAlternatives idWithAlternatives = new IdWithAlternatives();
        idWithAlternatives.setId(id);
        idWithAlternatives.setSpace(space.getName());
        idWithAlternatives.setAlternatives(alternativeIds);
        List<JsonLdIdMapping> result = ids.resolveId(Collections.singletonList(idWithAlternatives), stage);
        if (result == null || result.size() == 0) {
            return null;
        }
        if (result.size() > 1) {
            throw new RuntimeException("Received multiple responses although I was only asking for a single id. There is something totally wrong!");
        }
        JsonLdIdMapping jsonLdIdMapping = result.get(0);
        String absoluteId = idUtils.buildAbsoluteUrl(id).getId();
        if (jsonLdIdMapping.getRequestedId() == null || !jsonLdIdMapping.getRequestedId().equals(id)) {
            throw new RuntimeException(String.format("Did receive a result - but instead of id %s, I received a value for %s", absoluteId, jsonLdIdMapping.getRequestedId()));
        } else {
            if (jsonLdIdMapping.getResolvedIds() == null || jsonLdIdMapping.getResolvedIds().isEmpty()) {
                return null;
            }
            return jsonLdIdMapping.getResolvedIds();
        }
    }

}

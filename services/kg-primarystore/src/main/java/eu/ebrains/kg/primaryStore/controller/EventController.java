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

package eu.ebrains.kg.primaryStore.controller;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.GraphDBSpaces;
import eu.ebrains.kg.commons.api.Ids;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class EventController {

    private final Permissions permissions;
    private final Ids.Client ids;
    private final EventRepository eventRepository;
    private final IdUtils idUtils;
    private final GraphDBSpaces.Client graphDBSpaces;
    private final AuthContext authContext;
    private final UsersRepository usersRepository;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EventController(Permissions permissions, Ids.Client ids, EventRepository eventRepository, IdUtils idUtils, GraphDBSpaces.Client graphDBSpaces, UsersRepository usersRepository, AuthContext authContext) {
        this.permissions = permissions;
        this.ids = ids;
        this.eventRepository = eventRepository;
        this.idUtils = idUtils;
        this.graphDBSpaces = graphDBSpaces;
        this.usersRepository = usersRepository;
        this.authContext = authContext;
    }

    public void checkPermissionsForRerunEvents(){
        if(!permissions.hasGlobalPermission(authContext.getUserWithRolesWithoutTermsCheck(), Functionality.RERUN_EVENTS_FOR_SPACE)){
            throw new UnauthorizedException("You are not allowed to rerun the events of a space!");
        }
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
                    boolean spaceCreationPermission = permissions.hasPermission(userWithRoles, Functionality.MANAGE_SPACE, event.getSpaceName());
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
        }
        if (!hasPermission) {
            throw new ForbiddenException();
        }
    }

    public PersistedEvent persistEvent(Event event, DataStage dataStage) {
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        logger.info(String.format("Received event of type %s for instance %s in space %s by user %s via client %s", event.getType().name(), event.getDocumentId(), event.getSpaceName() != null ? event.getSpaceName().getName() : null, userWithRoles != null && userWithRoles.getUser() != null ? userWithRoles.getUser().getUserName() : "anonymous", userWithRoles != null && userWithRoles.getClientId() != null ? userWithRoles.getClientId() : "direct access"));
        if(userWithRoles==null){
            throw new UnauthorizedException("Can not persist an event without user information");
        }
        if(dataStage == event.getType().getStage()){
            //We only update the representation of the user at its original stage since otherwise, we might end up having the user updated multiple times (once per stage)
            usersRepository.updateUserRepresentation(userWithRoles.getUser());
        }
        if(dataStage==DataStage.NATIVE && (event.getType() == Event.Type.INSERT || event.getType() == Event.Type.UPDATE)){
            //For insert and update, we need to ensure that the user information is also present in the native payload to properly calculate the alternatives
            event.getData().put(EBRAINSVocabulary.META_USER, idUtils.buildAbsoluteUrl(usersRepository.getUserUUID(userWithRoles.getUser())));
        }
        PersistedEvent persistedEvent = new PersistedEvent(event, dataStage, userWithRoles.getUser(), graphDBSpaces.getSpace(event.getSpaceName()));
        ensureInternalIdInPayload(persistedEvent, userWithRoles);
        checkPermission(persistedEvent);
        handleIds(dataStage, persistedEvent);
        eventRepository.insert(persistedEvent);
        return persistedEvent;
    }

    public void handleIds(DataStage dataStage, PersistedEvent persistedEvent) {
        if (persistedEvent.getType() == Event.Type.DELETE) {
            ids.removeId(DataStage.IN_PROGRESS, persistedEvent.getDocumentId());
        } else {
            switch (dataStage){
                case IN_PROGRESS:
                case RELEASED:
                    ids.createOrUpdateId(new IdWithAlternatives(persistedEvent.getDocumentId(), persistedEvent.getSpaceName(), persistedEvent.getData().identifiers()), dataStage);
                    break;
            }
            addMetaInformationToData(dataStage, persistedEvent);
        }
    }

    private void ensureInternalIdInPayload(@NonNull PersistedEvent persistedEvent, UserWithRoles userWithRoles) {
        if (persistedEvent.getData() != null) {
            JsonLdId idFromPayload = persistedEvent.getData().id();
            if (idFromPayload != null) {
                //Save the original id as an "identifier"
                persistedEvent.getData().addIdentifiers(idFromPayload.getId());
            }
            persistedEvent.getData().setId(idUtils.buildAbsoluteUrl(persistedEvent.getDocumentId()));
            //In the native space, we store the document separately for every user - this means the documents are actual contributions to an instance.
            if (persistedEvent.getDataStage() == DataStage.NATIVE) {
                if(userWithRoles==null){
                    throw new UnauthorizedException("It is not possible to persist an event without authentication information");
                }
                UUID userSpecificUUID = idUtils.getDocumentIdForUserAndInstance(persistedEvent.getUserId(), persistedEvent.getDocumentId());
                persistedEvent.setInstance(persistedEvent.getSpaceName(), userSpecificUUID);
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
            final String indexTimestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(event.getIndexedTimestamp()), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT);
            final String firstRelease = eventRepository.getFirstRelease(event.getDocumentId());
            data.getDoc().put(EBRAINSVocabulary.META_FIRST_RELEASED_AT, firstRelease == null ? indexTimestamp : firstRelease);
            data.getDoc().put(EBRAINSVocabulary.META_LAST_RELEASED_AT, indexTimestamp);
        }
    }

}

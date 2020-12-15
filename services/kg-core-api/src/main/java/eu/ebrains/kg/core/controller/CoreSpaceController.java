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

import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.permissions.controller.PermissionSvc;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.core.serviceCall.CoreSpacesToGraphDB;
import eu.ebrains.kg.core.serviceCall.CoreToAuthentication;
import eu.ebrains.kg.core.serviceCall.CoreToPrimaryStore;
import eu.ebrains.kg.core.serviceCall.CoreTypesToGraphDB;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CoreSpaceController {

    private final CoreInstanceController instanceController;
    private final CoreTypesToGraphDB typesGraphDBSvc;
    private final CoreToPrimaryStore primaryStoreSvc;
    private final CoreSpacesToGraphDB graphDbSvc;
    private final AuthContext authContext;
    private final PermissionSvc permissionSvc;
    private final CoreToAuthentication coreToAuthentication;

    public CoreSpaceController(CoreInstanceController instanceController, CoreTypesToGraphDB typesGraphDBSvc, CoreToPrimaryStore primaryStoreSvc, CoreSpacesToGraphDB graphDbSvc, AuthContext authContext, PermissionSvc permissionSvc, CoreToAuthentication coreToAuthentication) {
        this.instanceController = instanceController;
        this.typesGraphDBSvc = typesGraphDBSvc;
        this.primaryStoreSvc = primaryStoreSvc;
        this.graphDbSvc = graphDbSvc;
        this.authContext = authContext;
        this.permissionSvc = permissionSvc;
        this.coreToAuthentication = coreToAuthentication;
    }

    public NormalizedJsonLd getSpace(ExposedStage stage, String space, boolean permissions) {
        NormalizedJsonLd sp = graphDbSvc.getSpace(new SpaceName(space), stage.getStage());
        if (sp != null && permissions) {
            UserWithRoles userWithRoles = authContext.getUserWithRoles();
            String spaceIdentifier = sp.getAs(SchemaOrgVocabulary.IDENTIFIER, String.class, null);
            if (spaceIdentifier != null) {
                List<Functionality> applyingFunctionalities = userWithRoles.getPermissions().stream().filter(f -> (f.getFunctionality().getStage() == null || f.getFunctionality().getStage() == stage.getStage()) && f.getFunctionality().getFunctionalityGroup() == Functionality.FunctionalityGroup.INSTANCE && f.appliesTo(new SpaceName(spaceIdentifier), null)).map(FunctionalityInstance::getFunctionality).collect(Collectors.toList());
                sp.put(EBRAINSVocabulary.META_PERMISSIONS, applyingFunctionalities);
            }
        }
        return sp;
    }


    public NormalizedJsonLd createSpaceDefinition(Space space, boolean global) {
        if (permissionSvc.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.CREATE_SPACE)) {
            NormalizedJsonLd spacePayload = space.toJsonLd();
            primaryStoreSvc.postEvent(Event.createUpsertEvent(global ? InternalSpace.GLOBAL_SPEC : space.getName(), UUID.nameUUIDFromBytes(spacePayload.id().getId().getBytes(StandardCharsets.UTF_8)), Event.Type.INSERT, spacePayload), false, authContext.getAuthTokens());
            coreToAuthentication.createRoles(Arrays.stream(RoleMapping.values()).filter(r -> r != RoleMapping.IS_CLIENT).map(r -> r.toRole(space.getName())).collect(Collectors.toList()));
            return spacePayload;
        } else {
            throw new ForbiddenException();
        }
    }

    public void removeSpaceDefinition(SpaceName space, boolean removeRoles) {
        if (permissionSvc.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.DELETE_SPACE)) {
            if (removeRoles) {
                coreToAuthentication.removeRoles(FunctionalityInstance.getRolePatternForSpace(space));
            }
            JsonLdId id = Space.createId(space);
            primaryStoreSvc.postEvent(Event.createDeleteEvent(InternalSpace.GLOBAL_SPEC, UUID.nameUUIDFromBytes(id.getId().getBytes(StandardCharsets.UTF_8)), id), false, authContext.getAuthTokens());
        } else {
            throw new ForbiddenException();
        }
    }


    public void removeTypeInSpaceLink(SpaceName space, Type type) {
        //For this, we need the permission to define types in the given space
        if (permissionSvc.hasPermission(authContext.getUserWithRoles(), Functionality.DEFINE_TYPES, space) && permissionSvc.hasPermission(authContext.getUserWithRoles(), Functionality.READ, space)) {
            if (!isTypeInSpaceEmpty(space, type)) {
                throw new IllegalStateException("Type in space is not empty");
            }
            NormalizedJsonLd payload = new NormalizedJsonLd();
            payload.addTypes(EBRAINSVocabulary.META_TYPE_IN_SPACE_DEFINITION_TYPE);
            payload.put(EBRAINSVocabulary.META_SPACE, space);
            payload.put(EBRAINSVocabulary.META_TYPE, new JsonLdId(type.getName()));
            JsonLdId type2space = EBRAINSVocabulary.createIdForStructureDefinition("type2space", space.getName(), type.getName());
            UUID id = UUID.nameUUIDFromBytes(type2space.getId().getBytes(StandardCharsets.UTF_8));
            Event deprecateSpace = new Event(space, id, payload, Event.Type.META_DEPRECATION, new Date());
            primaryStoreSvc.postEvent(deprecateSpace, false, authContext.getAuthTokens());

        } else {
            throw new ForbiddenException();
        }
    }


    public void removeSpaceLinks(SpaceName space) {
        //For this, we need global permissions for both - deleting spaces and reading (the latter to ensure that we actually can see all potential values)
        if (permissionSvc.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.DELETE_SPACE) && permissionSvc.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.READ)) {
            if (!isSpaceEmpty(space.getName())) {
                throw new IllegalStateException("Space is not empty");
            }
            NormalizedJsonLd payload = new NormalizedJsonLd();
            payload.addTypes(EBRAINSVocabulary.META_SPACEDEFINITION_TYPE);
            payload.put(EBRAINSVocabulary.META_SPACE, space);
            Event deprecateSpace = new Event(space, UUID.nameUUIDFromBytes(EBRAINSVocabulary.createIdForStructureDefinition("spaces", space.getName()).getId().getBytes(StandardCharsets.UTF_8)), payload, Event.Type.META_DEPRECATION, new Date());
            primaryStoreSvc.postEvent(deprecateSpace, false, authContext.getAuthTokens());
        } else {
            throw new ForbiddenException();
        }
    }

    public boolean isTypeInSpaceEmpty(SpaceName space, Type type) {
        return instanceController.getInstances(DataStage.IN_PROGRESS, type, space, null, new ResponseConfiguration().setReturnPayload(false), new PaginationParam().setFrom(0).setSize(0L)).getTotalResults() == 0;

    }

    public boolean isSpaceEmpty(String space) {
        return typesGraphDBSvc.getTypes(DataStage.IN_PROGRESS, new SpaceName(space), false, false, new PaginationParam()).getTotalResults() == 0;
    }

}

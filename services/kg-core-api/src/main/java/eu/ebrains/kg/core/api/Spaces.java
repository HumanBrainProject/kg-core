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

import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.controller.CoreSpaceController;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.core.serviceCall.CoreSpacesToGraphDB;
import eu.ebrains.kg.core.serviceCall.CoreToPrimaryStore;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The spaces API provides information about existing KG spaces
 */
@RestController
@RequestMapping(Version.API + "/spaces")
public class Spaces {

    private final CoreSpacesToGraphDB graphDbSvc;
    private final CoreSpaceController spaceController;
    private final AuthContext authContext;
    private final CoreToPrimaryStore primaryStoreSvc;

    public Spaces(CoreSpacesToGraphDB graphDbSvc, CoreSpaceController spaceController, AuthContext authContext, CoreToPrimaryStore primaryStoreSvc) {
        this.graphDbSvc = graphDbSvc;
        this.spaceController = spaceController;
        this.authContext = authContext;
        this.primaryStoreSvc = primaryStoreSvc;
    }

    @GetMapping("{space}")
    public Result<NormalizedJsonLd> getSpace(@RequestParam("stage") ExposedStage stage, @PathVariable("space") String space, @RequestParam(value = "permissions", defaultValue = "false") boolean permissions) {
        NormalizedJsonLd s = spaceController.getSpace(stage, space, permissions);
        if(s!=null){
            s.removeAllInternalProperties();
        }
        return Result.ok(s);
    }

    @GetMapping
    public PaginatedResult<NormalizedJsonLd> getSpaces(@RequestParam("stage") ExposedStage stage, @ParameterObject PaginationParam paginationParam, @RequestParam(value = "permissions", defaultValue = "false") boolean permissions) {
        Paginated<NormalizedJsonLd> spaces = graphDbSvc.getSpaces(stage.getStage(), paginationParam);
        if (permissions) {
            UserWithRoles userWithRoles = authContext.getUserWithRoles();
            spaces.getData().forEach(space -> {
                List<Functionality> applyingFunctionalities = userWithRoles.getPermissions().stream().filter(f -> (f.getFunctionality().getStage() == null || f.getFunctionality().getStage() == stage.getStage()) && f.getFunctionality().getFunctionalityGroup() == Functionality.FunctionalityGroup.INSTANCE && f.appliesTo(Space.fromJsonLd(space).getName(), null)).map(FunctionalityInstance::getFunctionality).collect(Collectors.toList());
                space.put(EBRAINSVocabulary.META_PERMISSIONS, applyingFunctionalities);
            });
        }
        spaces.getData().forEach(NormalizedJsonLd::removeAllInternalProperties);
        return PaginatedResult.ok(spaces);
    }


    @Operation(summary = "Assign a type to a space")
    @PutMapping("{space}/types")
    public ResponseEntity<Result<Void>> assignTypeToSpace(@RequestParam("stage") ExposedStage stage, @PathVariable("space") String space, @RequestParam("type") String type) {
        NormalizedJsonLd payload = new NormalizedJsonLd();
        payload.addTypes(EBRAINSVocabulary.META_TYPE_IN_SPACE_DEFINITION_TYPE);
        Type t = new Type(type);
        payload.addProperty(EBRAINSVocabulary.META_TYPE, new JsonLdId(t.getName()));
        payload.addProperty(EBRAINSVocabulary.META_SPACES, Collections.singletonList(space));
        payload.setId(EBRAINSVocabulary.createIdForStructureDefinition("type2space", space, t.getName()));
        primaryStoreSvc.postEvent(Event.createUpsertEvent(InternalSpace.GLOBAL_SPEC, UUID.nameUUIDFromBytes(payload.id().getId().getBytes(StandardCharsets.UTF_8)), Event.Type.INSERT, payload), false, authContext.getAuthTokens());
        return ResponseEntity.ok(Result.ok());
    }

}

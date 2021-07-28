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

import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.GraphDBSpaces;
import eu.ebrains.kg.commons.api.PrimaryStoreEvents;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.config.openApiGroups.Advanced;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesInputWithoutEnrichedSensitiveData;
import eu.ebrains.kg.commons.markers.ExposesSpace;
import eu.ebrains.kg.commons.markers.ExposesType;
import eu.ebrains.kg.commons.markers.WritesData;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.core.controller.CoreSpaceController;
import eu.ebrains.kg.core.model.ExposedStage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    private final GraphDBSpaces.Client graphDBSpaces;
    private final CoreSpaceController spaceController;
    private final AuthContext authContext;
    private final PrimaryStoreEvents.Client primaryStoreEvents;

    public Spaces(GraphDBSpaces.Client graphDBSpaces, CoreSpaceController spaceController, AuthContext authContext, PrimaryStoreEvents.Client primaryStoreEvents) {
        this.graphDBSpaces = graphDBSpaces;
        this.spaceController = spaceController;
        this.authContext = authContext;
        this.primaryStoreEvents = primaryStoreEvents;
    }

    @GetMapping("{space}")
    @ExposesSpace
    @Advanced
    public Result<NormalizedJsonLd> getSpace(@RequestParam("stage") ExposedStage stage, @PathVariable("space") @Parameter(description = "The space to be read or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam(value = "permissions", defaultValue = "false") boolean permissions) {
        NormalizedJsonLd s = spaceController.getSpace(stage, space, permissions);
        if (s != null) {
            s.removeAllInternalProperties();
        }
        return Result.ok(s);
    }

    @GetMapping
    @ExposesSpace
    @Advanced
    public PaginatedResult<NormalizedJsonLd> getSpaces(@RequestParam("stage") ExposedStage stage, @ParameterObject PaginationParam paginationParam, @RequestParam(value = "permissions", defaultValue = "false") boolean permissions) {
        Paginated<NormalizedJsonLd> spaces = graphDBSpaces.getSpaces(stage.getStage(), paginationParam);
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
    @WritesData
    @Admin
    public ResponseEntity<Result<Void>> assignTypeToSpace(@PathVariable("space") @Parameter(description = "The space be linked to or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam("type") String type) {
        NormalizedJsonLd payload = new NormalizedJsonLd();
        SpaceName sp = authContext.resolveSpaceName(space);
        payload.addTypes(EBRAINSVocabulary.META_TYPE_IN_SPACE_DEFINITION_TYPE);
        Type t = new Type(type);
        payload.addProperty(EBRAINSVocabulary.META_TYPE, new JsonLdId(t.getName()));
        payload.addProperty(EBRAINSVocabulary.META_SPACES, Collections.singletonList(sp.getName()));
        payload.setId(EBRAINSVocabulary.createIdForStructureDefinition("type2space", sp.getName(), t.getName()));
        primaryStoreEvents.postEvent(Event.createUpsertEvent(InternalSpace.GLOBAL_SPEC, UUID.nameUUIDFromBytes(payload.id().getId().getBytes(StandardCharsets.UTF_8)), Event.Type.INSERT, payload), false);
        return ResponseEntity.ok(Result.ok());
    }

    @Operation(summary = "Remove a type in space definition")
    @DeleteMapping("{space}/types")
    @WritesData
    @Admin
    public ResponseEntity<Result<Void>> removeTypeFromSpace(@PathVariable("space") @Parameter(description = "The space the type shall be removed from or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam("type") String type) {
        SpaceName spaceName = authContext.resolveSpaceName(space);
        spaceController.removeTypeInSpaceLink(spaceName, new Type(type));
        return ResponseEntity.ok(Result.ok());
    }


    @Operation(summary = "Explicitly specify a space (incl. creation of roles in the authentication system)")
    @PutMapping("{space}/specification")
    @Admin
    @ExposesInputWithoutEnrichedSensitiveData
    public ResponseEntity<Result<NormalizedJsonLd>> createSpaceDefinition(@PathVariable(value = "space") @Parameter(description = "The space the definition is valid for. Please note that you can't do so for your private space (\"" + SpaceName.PRIVATE_SPACE + "\")") String space, @RequestParam(value = "autorelease", required = false, defaultValue = "false") boolean autoRelease, @RequestParam(value = "clientSpace", required = false, defaultValue = "false") boolean clientSpace) {
        SpaceName spaceName = authContext.resolveSpaceName(space);
        if (spaceName != null) {
            if(spaceName.equals(authContext.getUserWithRoles().getPrivateSpace())){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.nok(HttpStatus.BAD_REQUEST.value(), "Your private space is configured by default - you can't do so yourself."));
            }
        }
        return ResponseEntity.ok(Result.ok(spaceController.createSpaceDefinition(new Space(spaceName, autoRelease, clientSpace), true)));
    }


    @Operation(summary = "Remove a space definition (this does not mean that its links are removed - it therefore can still appear in the type queries)")
    @DeleteMapping("{space}/specification")
    @Admin
    @ExposesInputWithoutEnrichedSensitiveData
    public ResponseEntity<Result<Void>> removeSpaceDefinition(@PathVariable(value = "space") @Parameter(description = "The space the definition should be removed for. Please note that you can't do so for your private space (\"" + SpaceName.PRIVATE_SPACE + "\")") String space) {
        SpaceName spaceName = authContext.resolveSpaceName(space);
        if (spaceName != null) {
            if(spaceName.equals(authContext.getUserWithRoles().getPrivateSpace())){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.nok(HttpStatus.BAD_REQUEST.value(), "Your private space is configured by default - you can't do so yourself."));
            }
        }
        spaceController.removeSpaceDefinition(new SpaceName(space));
        return ResponseEntity.ok(Result.ok());
    }


    @Operation(summary = "Remove all links of a space (if it is empty) so it is disappearing from the meta database. This does not remove the explicit specifications but keeps them e.g. if it is going to be reintroudced).")
    @DeleteMapping("{space}/links")
    @Admin
    public void removeSpaceLinks(@PathVariable(value = "space") @Parameter(description = "The space links should be removed from or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space.") String space) {
        spaceController.removeSpaceLinks(authContext.resolveSpaceName(space));
    }


    @Operation(summary = "List candidate spaces for removal", description = "Returns a list of spaces which potentially could be removed because they are not in use (have no types)")
    @GetMapping("/candidates/forRemoval")
    @ExposesType
    @Admin
    public Result<List<String>> candidatesForDeprecation() {
        List<NormalizedJsonLd> allSpaces = getSpaces(ExposedStage.IN_PROGRESS, new PaginationParam(), false).getData();
        if (allSpaces != null) {
            return Result.ok(allSpaces.stream().map(space -> space.getAs(SchemaOrgVocabulary.IDENTIFIER, String.class)).filter(spaceController::isSpaceEmpty).collect(Collectors.toList()));
        }
        return Result.ok(Collections.emptyList());
    }


    @Operation(summary = "Remove candidates", description = "Remove all candidates (instances without occurrences) in one go")
    @DeleteMapping("/candidates/forRemoval")
    @WritesData
    @Admin
    public ResponseEntity<List<Result<String>>> deprecateAllCandidates(@RequestParam(value = "removeRoles", required = false, defaultValue = "false") boolean removeRoles) {
        Result<List<String>> listResult = candidatesForDeprecation();
        if (listResult != null && listResult.getData() != null) {
            List<Result<String>> result = new ArrayList<>();
            listResult.getData().forEach(d -> {
                try {
                    removeSpaceLinks(d);
                    removeSpaceDefinition(d);
                    result.add(Result.ok(d));
                } catch (Exception e) {
                    result.add(Result.nok(HttpStatus.CONFLICT.value(), String.format("%s - %s", d, e.getMessage())));
                }
            });
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }

}

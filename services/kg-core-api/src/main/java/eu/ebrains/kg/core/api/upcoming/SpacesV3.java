/*
 * Copyright 2022 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.core.api.upcoming;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.exception.InstanceNotFoundException;
import eu.ebrains.kg.commons.exception.InvalidRequestException;
import eu.ebrains.kg.commons.markers.ExposesInputWithoutEnrichedSensitiveData;
import eu.ebrains.kg.commons.markers.ExposesSpace;
import eu.ebrains.kg.commons.markers.WritesData;
import eu.ebrains.kg.commons.model.PaginatedResult;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.external.spaces.SpaceInformation;
import eu.ebrains.kg.commons.model.external.spaces.SpaceSpecification;
import eu.ebrains.kg.core.api.common.Spaces;
import eu.ebrains.kg.core.controller.CoreInferenceController;
import eu.ebrains.kg.core.controller.CoreSpaceController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

/**
 * The spaces API provides information about existing KG spaces
 */
@RestController
@RequestMapping(Version.UPCOMING + "/spaces")
public class SpacesV3 extends Spaces {

    private final CoreInferenceController inferenceController;
    private final AuthContext authContext;
    private final CoreSpaceController spaceController;

    public SpacesV3(CoreInferenceController inferenceController, AuthContext authContext, CoreSpaceController spaceController) {
        super();
        this.inferenceController = inferenceController;
        this.authContext = authContext;
        this.spaceController = spaceController;
    }


    @GetMapping("{space}")
    @ExposesSpace
    @Tag(name = TAG)
    public Result<SpaceInformation> getSpace(@PathVariable("space") @Parameter(description = "The space to be read or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam(value = "permissions", defaultValue = "false") boolean permissions) {
        SpaceInformation s = spaceController.getSpace(SpaceName.fromString(space), permissions);
        if(s != null){
            return Result.ok(s);
        }
        throw new InstanceNotFoundException(String.format("Space %s was not found", space));
    }

    @GetMapping
    @ExposesSpace
    @Tag(name = TAG)
    public PaginatedResult<SpaceInformation> listSpaces(@ParameterObject PaginationParam paginationParam, @RequestParam(value = "permissions", defaultValue = "false") boolean permissions) {
        return PaginatedResult.ok(spaceController.listSpaces(paginationParam, permissions));
    }


    @Operation(summary = "Assign a type to a space")
    @PutMapping("{space}/types")
    @WritesData
    @Admin
    @Tag(name = TAG)
    public void assignTypeToSpace(@PathVariable("space") @Parameter(description = "The space be linked to or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam("type") String type) {
        spaceController.addTypeToSpace(SpaceName.fromString(space), type);
    }

    @Operation(summary = "Remove a type in space definition")
    @DeleteMapping("{space}/types")
    @WritesData
    @Admin
    @Tag(name = TAG)
    public void removeTypeFromSpace(@PathVariable("space") @Parameter(description = "The space the type shall be removed from or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam("type") String type) {
        spaceController.removeTypeFromSpace(SpaceName.fromString(space), type);
    }


    @Operation(summary = "Explicitly specify a space")
    @PutMapping("{space}/specification")
    @Admin
    @ExposesInputWithoutEnrichedSensitiveData
    @Tag(name = TAG)
    public void createSpaceDefinition(@PathVariable(value = "space") @Parameter(description = "The space the definition is valid for. Please note that you can't do so for your private space (\"" + SpaceName.PRIVATE_SPACE + "\")") String space, @RequestParam(value = "autorelease", required = false, defaultValue = "false") boolean autoRelease, @RequestParam(value = "clientSpace", required = false, defaultValue = "false") boolean clientSpace, @RequestParam(value = "deferCache", required = false, defaultValue = "false") boolean deferCache) {
        if(space == null){
            throw new InvalidRequestException("You need to provide a space name to execute this functionality");
        }
        SpaceSpecification spaceSpecification = new SpaceSpecification();
        spaceSpecification.setName(space);
        spaceSpecification.setIdentifier(space);
        spaceSpecification.setAutoRelease(autoRelease);
        spaceSpecification.setDeferCache(deferCache);
        spaceSpecification.setClientSpace(clientSpace);
        spaceController.createSpaceDefinition(spaceSpecification);
    }


    @Operation(summary = "Remove a space definition")
    @DeleteMapping("{space}/specification")
    @Admin
    @ExposesInputWithoutEnrichedSensitiveData
    @Tag(name = TAG)
    public void removeSpaceDefinition(@PathVariable(value = "space") @Parameter(description = "The space the definition should be removed for. Please note that you can't do so for your private space (\"" + SpaceName.PRIVATE_SPACE + "\")") String space) {
        spaceController.removeSpaceDefinition(SpaceName.fromString(space));
    }

    @Operation(summary = "Trigger a rerun of the events of this space")
    @PutMapping("{space}/eventHistory")
    @Admin
    @Tag(name = TAG)
    public void rerunEvents(@PathVariable(value = "space") @Parameter(description = "The space the event rerun shall be executed for.") String space) {
        spaceController.rerunEvents(SpaceName.fromString(space));
    }

    @Operation(summary = "Triggers the inference of all documents of the given space")
    @Admin
    @PostMapping("/{space}/inference")
    @Tag(name = TAG)
    public void triggerInference(@PathVariable(value = "space") String space, @RequestParam(value = "identifier", required = false) String identifier, @RequestParam(value = "async", required = false, defaultValue = "false") boolean async) {
        SpaceName spaceName = authContext.resolveSpaceName(space);
        if (async) {
            inferenceController.asyncTriggerInference(spaceName, identifier);
        } else {
            inferenceController.triggerInference(spaceName, identifier);
        }
    }


}

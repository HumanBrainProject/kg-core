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

import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.config.openApiGroups.Advanced;
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
import eu.ebrains.kg.core.controller.CoreSpaceController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The spaces API provides information about existing KG spaces
 */
@RestController
@RequestMapping(Version.API + "/spaces")
public class Spaces {

    private final CoreSpaceController spaceController;

    public Spaces(CoreSpaceController spaceController) {
        this.spaceController = spaceController;
    }

    @GetMapping("{space}")
    @ExposesSpace
    @Advanced
    public Result<SpaceInformation> getSpace(@PathVariable("space") @Parameter(description = "The space to be read or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam(value = "permissions", defaultValue = "false") boolean permissions) {
        SpaceInformation s = spaceController.getSpace(SpaceName.fromString(space), permissions);
        return s != null ? Result.ok(s) : null;
    }

    @GetMapping
    @ExposesSpace
    @Advanced
    public PaginatedResult<SpaceInformation> getSpaces(@ParameterObject PaginationParam paginationParam, @RequestParam(value = "permissions", defaultValue = "false") boolean permissions) {
        return PaginatedResult.ok(spaceController.getSpaces(paginationParam, permissions));
    }


    @Operation(summary = "Assign a type to a space")
    @PutMapping("{space}/types")
    @WritesData
    @Admin
    public void assignTypeToSpace(@PathVariable("space") @Parameter(description = "The space be linked to or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam("type") String type) {
        spaceController.addTypeToSpace(SpaceName.fromString(space), type);
    }

    @Operation(summary = "Remove a type in space definition")
    @DeleteMapping("{space}/types")
    @WritesData
    @Admin
    public void removeTypeFromSpace(@PathVariable("space") @Parameter(description = "The space the type shall be removed from or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam("type") String type) {
        spaceController.removeTypeFromSpace(SpaceName.fromString(space), type);
    }


    @Operation(summary = "Explicitly specify a space")
    @PutMapping("{space}/specification")
    @Admin
    @ExposesInputWithoutEnrichedSensitiveData
    public void createSpaceDefinition(@PathVariable(value = "space") @Parameter(description = "The space the definition is valid for. Please note that you can't do so for your private space (\"" + SpaceName.PRIVATE_SPACE + "\")") String space, @RequestParam(value = "autorelease", required = false, defaultValue = "false") boolean autoRelease, @RequestParam(value = "clientSpace", required = false, defaultValue = "false") boolean clientSpace) {
        if(space == null){
            throw new InvalidRequestException("You need to provide a space name to execute this functionality");
        }
        SpaceSpecification spaceSpecification = new SpaceSpecification();
        spaceSpecification.setName(space);
        spaceSpecification.setIdentifier(space);
        spaceSpecification.setAutoRelease(autoRelease);
        spaceSpecification.setClientSpace(clientSpace);
        spaceController.createSpaceDefinition(spaceSpecification);
    }


    @Operation(summary = "Remove a space definition")
    @DeleteMapping("{space}/specification")
    @Admin
    @ExposesInputWithoutEnrichedSensitiveData
    public void removeSpaceDefinition(@PathVariable(value = "space") @Parameter(description = "The space the definition should be removed for. Please note that you can't do so for your private space (\"" + SpaceName.PRIVATE_SPACE + "\")") String space) {
        spaceController.removeSpaceDefinition(new SpaceName(space));
    }

}

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

package eu.ebrains.kg.graphdb.structure.controller;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.internal.spaces.Space;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class SpaceController {

    private final StructureRepository structureRepository;
    private final PermissionsController permissionsController;
    private final AuthContext authContext;

    public SpaceController(StructureRepository structureRepository, PermissionsController permissionsController, AuthContext authContext) {
        this.structureRepository = structureRepository;
        this.permissionsController = permissionsController;
        this.authContext = authContext;
    }

    public List<Space> getSpaces(DataStage stage){
        final List<SpaceName> reflectedSpaces = this.structureRepository.reflectSpaces(stage);
        final List<Space> spaceSpecifications = this.structureRepository.getSpaceSpecifications();
        final Set<SpaceName> spacesWithSpecifications = spaceSpecifications.stream().map(Space::getName).collect(Collectors.toSet());
        final Stream<Space> allSpaces = Stream.concat(spaceSpecifications.stream(), reflectedSpaces.stream().filter(s -> !spacesWithSpecifications.contains(s))
                .map(s -> new Space(s, false, false).setReflected(true)))
                .peek(s -> {if(reflectedSpaces.contains(s.getName())){s.setExistsInDB(true);}});
        Set<SpaceName> whitelistedSpaces = permissionsController.whitelistedSpaceReads(authContext.getUserWithRoles());
        List<Space> spaceDefinitions;
        if(whitelistedSpaces!=null) {
            spaceDefinitions = allSpaces.filter(s -> !whitelistedSpaces.contains(s.getName())).collect(Collectors.toList());
        }
        else{
            spaceDefinitions = allSpaces.collect(Collectors.toList());
        }
        return spaceDefinitions;
    }
}

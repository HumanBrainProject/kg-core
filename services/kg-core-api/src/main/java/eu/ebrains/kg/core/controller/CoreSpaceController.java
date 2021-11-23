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

package eu.ebrains.kg.core.controller;

import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.api.GraphDBSpaces;
import eu.ebrains.kg.commons.api.GraphDBTypes;
import eu.ebrains.kg.commons.api.PrimaryStoreEvents;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.exception.InvalidRequestException;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.model.external.spaces.SpaceInformation;
import eu.ebrains.kg.commons.model.external.spaces.SpaceSpecification;
import eu.ebrains.kg.commons.model.internal.spaces.Space;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import eu.ebrains.kg.core.model.ExposedStage;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CoreSpaceController {

    private final GraphDBSpaces.Client graphDBSpaces;
    private final PrimaryStoreEvents.Client primaryStoreEvents;
    private final AuthContext authContext;

    public CoreSpaceController(GraphDBSpaces.Client graphDBSpaces, PrimaryStoreEvents.Client primaryStoreEvents, AuthContext authContext) {
        this.graphDBSpaces = graphDBSpaces;
        this.primaryStoreEvents = primaryStoreEvents;
        this.authContext = authContext;
    }

    private SpaceInformation translateSpaceToSpaceInformation(Space space, boolean permissions) {
        final SpaceInformation spaceInformation = space.toSpaceInformation();
        if (permissions) {
            UserWithRoles userWithRoles = authContext.getUserWithRoles();
            String spaceIdentifier = spaceInformation.getIdentifier();
            if (spaceIdentifier != null) {
                List<Functionality> applyingFunctionalities = userWithRoles.getPermissions().stream().
                        filter(f -> (
                                f.getFunctionality().getFunctionalityGroup() == Functionality.FunctionalityGroup.INSTANCE
                                || f.getFunctionality().getFunctionalityGroup() == Functionality.FunctionalityGroup.QUERY)
                                && f.appliesTo(new SpaceName(spaceIdentifier), null)
                        ).map(FunctionalityInstance::getFunctionality).distinct().collect(Collectors.toList());
                spaceInformation.setPermissions(applyingFunctionalities);
            }
        }
        return spaceInformation;
    }


    public Paginated<SpaceInformation> getSpaces(PaginationParam pagination, boolean permissions) {
        Paginated<Space> sp = graphDBSpaces.getSpaces(pagination);
        final List<SpaceInformation> spaceInformations = sp.getData().stream().map(s -> translateSpaceToSpaceInformation(s, permissions)).collect(Collectors.toList());
        return new Paginated<>(spaceInformations, sp.getTotalResults(), sp.getSize(), sp.getFrom());
    }


    public SpaceInformation getSpace(SpaceName space, boolean permissions) {
        Space sp = graphDBSpaces.getSpace(space);
        return sp != null ? translateSpaceToSpaceInformation(sp, permissions) : null;
    }


    public void createSpaceDefinition(SpaceSpecification spaceSpec) {
        graphDBSpaces.specifySpace(spaceSpec);
    }

    public void removeSpaceDefinition(SpaceName space) {
        graphDBSpaces.removeSpaceSpecification(space);
    }

    public void addTypeToSpace(SpaceName space, String type){
        graphDBSpaces.addTypeToSpace(space, type);
    }

    public void removeTypeFromSpace(SpaceName space, String type){
        graphDBSpaces.removeTypeFromSpace(space, type);
    }

    public void rerunEvents(SpaceName space){
        primaryStoreEvents.rerunEvents(space.getName());
    }

}

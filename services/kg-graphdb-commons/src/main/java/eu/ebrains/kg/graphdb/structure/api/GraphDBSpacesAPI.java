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

package eu.ebrains.kg.graphdb.structure.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.api.GraphDBSpaces;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.exception.InvalidRequestException;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.external.spaces.SpaceSpecification;
import eu.ebrains.kg.commons.model.internal.spaces.Space;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import eu.ebrains.kg.graphdb.structure.controller.MetaDataController;
import eu.ebrains.kg.graphdb.structure.controller.StructureRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class GraphDBSpacesAPI implements GraphDBSpaces.Client {


    private final StructureRepository structureRepository;
    private final MetaDataController metaDataController;
    private final PermissionsController permissionsController;
    private final AuthContext authContext;

    public GraphDBSpacesAPI(StructureRepository structureRepository, MetaDataController metaDataController, PermissionsController permissionsController, AuthContext authContext) {
        this.structureRepository = structureRepository;
        this.metaDataController = metaDataController;
        this.permissionsController = permissionsController;
        this.authContext = authContext;
    }

    private static Space createSpaceRepresentation(String name){
        return new Space(new SpaceName(name), false, false, false);
    }
    private List<Space> getSpaces(){
        List<Space> spaces = this.metaDataController.getSpaces(DataStage.IN_PROGRESS, authContext.getUserWithRoles());
        final SpaceName privateSpace = authContext.getUserWithRoles().getPrivateSpace();
        final Optional<Space> existingPrivateSpace = spaces.stream().filter(s -> s.getName().equals(privateSpace)).findAny();
        if (existingPrivateSpace.isPresent()) {
            //Rename the existing private space
            existingPrivateSpace.get().setName(SpaceName.fromString(SpaceName.PRIVATE_SPACE));
            existingPrivateSpace.get().setIdentifier(SpaceName.PRIVATE_SPACE);
        }
        else {
            //The private space doesn't exist yet -> we create it virtually.
            spaces.add(createSpaceRepresentation(SpaceName.PRIVATE_SPACE));
        }
        if(authContext.getUserWithRoles().hasInvitations()){
            spaces.add(createSpaceRepresentation(SpaceName.REVIEW_SPACE));
        }
        spaces.sort(Comparator.comparing(s -> s.getName().getName()));
        return spaces;
    }

    @Override
    public Space getSpace(SpaceName space) {
        return getSpaces().stream().filter(s -> s.getName().equals(space)).findFirst().orElse(null);
    }

    @Override
    public Paginated<Space> getSpaces(PaginationParam paginationParam) {
        return PaginationParam.paginate(getSpaces(), paginationParam);
    }

    @Override
    public void specifySpace(SpaceSpecification spaceSpecification) {
        if(permissionsController.canManageSpaces(authContext.getUserWithRoles())) {
            switch(spaceSpecification.getName()){
                case SpaceName.PRIVATE_SPACE:
                    throw new InvalidRequestException("You can't provide a definition for your private space");
                case SpaceName.REVIEW_SPACE:
                    throw new InvalidRequestException("You can't provide a definition for the invitation space");
            }
            structureRepository.createOrUpdateSpaceDocument(spaceSpecification);
            structureRepository.evictSpaceSpecificationCache();
        }
        else{
            throw new ForbiddenException("You don't have the required rights to manage spaces");
        }
    }

    @Override
    public void removeSpaceSpecification(SpaceName spaceName) {
        if(permissionsController.canManageSpaces(authContext.getUserWithRoles())) {
            switch(spaceName.getName()){
                case SpaceName.PRIVATE_SPACE:
                    throw new InvalidRequestException("You can't remove your private space");
                case SpaceName.REVIEW_SPACE:
                    throw new InvalidRequestException("You can't remove the invitation space");
            }
            structureRepository.removeSpaceDocument(spaceName);
            structureRepository.evictSpaceSpecificationCache();
        }
        else{
            throw new ForbiddenException("You don't have the required rights to manage spaces");
        }
    }

    @Override
    public void addTypeToSpace(SpaceName spaceName, String typeName) {
        if(permissionsController.canManageSpaces(authContext.getUserWithRoles())) {
            structureRepository.addLinkBetweenSpaceAndType(spaceName, typeName);
            structureRepository.evictTypesInSpaceBySpecification(spaceName);
        }
        else{
            throw new ForbiddenException("You don't have the required rights to manage spaces");
        }
    }

    @Override
    public void removeTypeFromSpace(SpaceName spaceName, String typeName) {
        if(permissionsController.canManageSpaces(authContext.getUserWithRoles())) {
            structureRepository.removeLinkBetweenSpaceAndType(spaceName, typeName);
            structureRepository.evictTypesInSpaceBySpecification(spaceName);
        }
        else{
            throw new ForbiddenException("You don't have the required rights to manage spaces");
        }
    }
}

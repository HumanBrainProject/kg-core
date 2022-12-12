/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

package eu.ebrains.kg.graphdb.structure.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.api.GraphDBTypes;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.model.external.types.TypeInformation;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import eu.ebrains.kg.graphdb.instances.controller.DocumentsRepository;
import eu.ebrains.kg.graphdb.structure.controller.MetaDataController;
import eu.ebrains.kg.graphdb.structure.controller.StructureRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GraphDBTypesAPI implements GraphDBTypes.Client {

    private static final String NO_RIGHTS_TO_DEFINE_TYPES = "You don't have the required rights to define types";
    private final AuthContext authContext;
    private final StructureRepository structureRepository;
    private final MetaDataController metaDataController;
    private final PermissionsController permissionsController;
    private final DocumentsRepository documents;

    public GraphDBTypesAPI(AuthContext authContext, StructureRepository structureRepository, MetaDataController metaDataController, PermissionsController permissionsController, DocumentsRepository documents) {
        this.authContext = authContext;
        this.structureRepository = structureRepository;
        this.metaDataController = metaDataController;
        this.permissionsController = permissionsController;
        this.documents = documents;
    }

    @Override
    public Paginated<TypeInformation> listTypes(DataStage stage, String space, boolean withProperties,
                                                boolean withIncomingLinks, PaginationParam paginationParam) {
        return PaginationParam.paginate(metaDataController.readMetaDataStructure(stage, space, null, withProperties, withIncomingLinks, authContext.getUserWithRoles(), authContext.getClientSpace() != null ? authContext.getClientSpace().getName() : null, authContext.getUserWithRolesWithoutTermsCheck().getPrivateSpace(), documents.getInvitationDocuments()), paginationParam);
    }

    @Override
    public Map<String, Result<TypeInformation>> getTypesByName(List<String> types, DataStage stage, String space,
                                                               boolean withProperties, boolean withIncomingLinks) {
        return metaDataController.getTypesByName(types, stage, space, withProperties, withIncomingLinks, authContext.getUserWithRoles(), authContext.getClientSpace()!=null ? authContext.getClientSpace().getName() : null, documents.getInvitationDocuments());
    }

    @Override
    public void specifyType(JsonLdId typeName, NormalizedJsonLd normalizedJsonLd, boolean global) {
        if (permissionsController.canManageTypesAndProperties(authContext.getUserWithRoles())) {
            if(global) {
                structureRepository.createOrUpdateTypeDocument(typeName, normalizedJsonLd, null);
                structureRepository.evictTypeSpecification(typeName.getId());
            }
            else{
                final SpaceName clientSpace = getClientSpaceOrThrowException();
                structureRepository.createOrUpdateTypeDocument(typeName, normalizedJsonLd, clientSpace);
                structureRepository.evictClientSpecificTypeSpecification(typeName.getId(), clientSpace);
            }
        } else {
            throw new ForbiddenException(NO_RIGHTS_TO_DEFINE_TYPES);
        }
    }

    @Override
    public void removeTypeSpecification(JsonLdId typeName, boolean global) {
        if (permissionsController.canManageTypesAndProperties(authContext.getUserWithRoles())) {

            if(global){
                structureRepository.removeTypeDocument(typeName, null);
                structureRepository.evictTypeSpecification(typeName.getId());
            }
            else{
                final SpaceName clientSpace = getClientSpaceOrThrowException();
                structureRepository.removeTypeDocument(typeName, clientSpace);
                structureRepository.evictClientSpecificTypeSpecification(typeName.getId(), clientSpace);
            }
        } else {
            throw new ForbiddenException(NO_RIGHTS_TO_DEFINE_TYPES);
        }
    }

    private SpaceName getClientSpaceOrThrowException(){
        final SpaceName clientSpace = authContext.getClientSpace() != null ? authContext.getClientSpace().getName() : null;
        if(clientSpace==null){
            throw new IllegalArgumentException("You need to be logged in with a client to be able to specify a type non-globally");
        }
        return clientSpace;
    }


    @Override
    public void specifyProperty(JsonLdId propertyName, NormalizedJsonLd normalizedJsonLd, boolean global) {
        if (permissionsController.canManageTypesAndProperties(authContext.getUserWithRoles())) {
            if(global) {
                structureRepository.createOrUpdatePropertyDocument(propertyName, normalizedJsonLd, null);
                structureRepository.evictPropertySpecificationCache(propertyName.getId());
            }
            else{
                final SpaceName clientSpace = getClientSpaceOrThrowException();
                structureRepository.createOrUpdatePropertyDocument(propertyName, normalizedJsonLd,  clientSpace);
                structureRepository.evictClientSpecificPropertySpecificationCache(propertyName.getId(), clientSpace);
            }
        } else {
            throw new ForbiddenException(NO_RIGHTS_TO_DEFINE_TYPES);
        }
    }

    @Override
    public void removePropertySpecification(JsonLdId propertyName, boolean global) {
        if (permissionsController.canManageTypesAndProperties(authContext.getUserWithRoles())) {
            if(global) {
                structureRepository.removePropertyDocument(propertyName, null);
                structureRepository.evictPropertySpecificationCache(propertyName.getId());
            }
            else{
                SpaceName clientSpace = getClientSpaceOrThrowException();
                structureRepository.removePropertyDocument(propertyName, clientSpace);
                structureRepository.evictClientSpecificPropertySpecificationCache(propertyName.getId(), clientSpace);
            }
        } else {
            throw new ForbiddenException(NO_RIGHTS_TO_DEFINE_TYPES);
        }
    }

    @Override
    public void addOrUpdatePropertyToType(String typeName, String propertyName, NormalizedJsonLd payload,
                                          boolean global) {
        if (permissionsController.canManageTypesAndProperties(authContext.getUserWithRoles())) {
            if(global) {
                structureRepository.addLinkBetweenTypeAndProperty(typeName, propertyName, payload, null);
                structureRepository.evictPropertiesInTypeBySpecificationCache(typeName);
            }
            else{
                SpaceName clientSpace = getClientSpaceOrThrowException();
                structureRepository.addLinkBetweenTypeAndProperty(typeName, propertyName, payload, clientSpace);
                structureRepository.evictClientSpecificPropertiesInTypeBySpecificationCache(typeName, clientSpace);
            }
        } else {
            throw new ForbiddenException(NO_RIGHTS_TO_DEFINE_TYPES);
        }
    }

    @Override
    public void removePropertyFromType(String typeName, String propertyName, boolean global) {
        if (permissionsController.canManageTypesAndProperties(authContext.getUserWithRoles())) {
            if(global) {
                structureRepository.removeLinkBetweenTypeAndProperty(typeName, propertyName, null);
                structureRepository.evictPropertiesInTypeBySpecificationCache(typeName);
            }
            else{
                SpaceName clientSpace = getClientSpaceOrThrowException();
                structureRepository.removeLinkBetweenTypeAndProperty(typeName, propertyName, clientSpace);
                structureRepository.evictClientSpecificPropertiesInTypeBySpecificationCache(typeName, clientSpace);
            }
        } else {
            throw new ForbiddenException(NO_RIGHTS_TO_DEFINE_TYPES);
        }
    }
}

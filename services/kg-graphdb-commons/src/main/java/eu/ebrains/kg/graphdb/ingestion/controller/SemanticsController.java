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

package eu.ebrains.kg.graphdb.ingestion.controller;

import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.ingestion.controller.semantics.*;
import eu.ebrains.kg.graphdb.ingestion.model.DBOperation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class SemanticsController {


    private final List<? extends SemanticsHandler> handlers;

    public SemanticsController(ClientSemanticsHandler clientSemanticsHandler, TypeDefinitionSemanticsHandler typeDefinitionSemanticsHandler, PropertyDefinitionSemanticsHandler propertyDefinitionSemanticsHandler, PropertyInTypeDefinitionSemanticsHandler propertyInTypeDefinitionSemanticsHandler, SpaceDefinitionSemanticsHandler spaceDefinitionSemanticsHandler, TypeInSpaceDefinitionSemanticsHandler typeInSpaceDefinitionSemanticsHandler) {
        this.handlers = Arrays.asList(clientSemanticsHandler, typeDefinitionSemanticsHandler, propertyDefinitionSemanticsHandler, propertyInTypeDefinitionSemanticsHandler, spaceDefinitionSemanticsHandler, typeInSpaceDefinitionSemanticsHandler);
    }

    List<DBOperation> createUpsertOperations(DataStage stage, ArangoDocumentReference rootDocumentRef, ArangoDocument document) {
         List<DBOperation> operations = new ArrayList<>();
        for (SemanticsHandler handler : handlers) {
            operations.addAll(handler.createUpsertOperations(stage, rootDocumentRef, document));
        }
        return operations;
    }

    List<DBOperation> createMetaDeprecationOperations(SpaceName space, NormalizedJsonLd document) {
         List<DBOperation> operations = new ArrayList<>();
        for (SemanticsHandler handler : handlers) {
            operations.addAll(handler.createMetaDeprecateOperations(space, document));
        }
        return operations;
    }


}

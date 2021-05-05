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

package eu.ebrains.kg.graphdb.ingestion.controller.semantics;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.ingestion.controller.structure.StaticStructureController;
import eu.ebrains.kg.graphdb.ingestion.model.DBOperation;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class TypeDefinitionSemanticsHandler extends SemanticsHandler {


    @Override
    public List<DBOperation> createUpsertOperations(DataStage stage, ArangoDocumentReference rootDocumentReference, ArangoDocument document) {
        if(document.getDoc().getTypes()!=null && document.getDoc().getTypes().contains(EBRAINSVocabulary.META_TYPEDEFINITION_TYPE)){
            JsonLdId typeReference = document.getDoc().getAs(EBRAINSVocabulary.META_TYPE, JsonLdId.class);
            if(typeReference!=null){
                ArangoDocumentReference targetType = StaticStructureController.createDocumentRefForMetaRepresentation(typeReference.getId(), ArangoCollectionReference.fromSpace(InternalSpace.TYPES_SPACE));
                return handleOverrideReference(rootDocumentReference, document, typeReference, targetType, ArangoCollectionReference.fromSpace(new Space(EBRAINSVocabulary.META_TYPE), true));
            }
        }
        return Collections.emptyList();
    }
}

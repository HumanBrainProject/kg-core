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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PropertyDefinitionSemanticsHandler extends SemanticsHandler {

    @Override
    public List<DBOperation> createUpsertOperations(DataStage stage, ArangoDocumentReference rootDocumentRef, ArangoDocument document) {
        if (document.getDoc().getTypes() != null && document.getDoc().getTypes().contains(EBRAINSVocabulary.META_PROPERTY_DEFINITION_TYPE)) {
            JsonLdId propertyReference = document.getDoc().getAs(EBRAINSVocabulary.META_PROPERTY, JsonLdId.class);
            if (propertyReference != null) {
                ArangoDocumentReference propertyRef = StaticStructureController.createDocumentRefForMetaRepresentation(propertyReference.getId(), ArangoCollectionReference.fromSpace(InternalSpace.PROPERTIES_SPACE));
                List<DBOperation> dbOperations = new ArrayList<>();
                dbOperations.addAll(handleOverrideReference(rootDocumentRef, document, propertyReference, propertyRef, ArangoCollectionReference.fromSpace(new Space(EBRAINSVocabulary.META_PROPERTY), true)));
                dbOperations.addAll(handleExplicitlyStatedTargetTypes(document, propertyRef));
                return dbOperations;
            }
        }
        return Collections.emptyList();
    }
}

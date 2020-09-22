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
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.TypeUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.commons.model.MetaRepresentation;
import eu.ebrains.kg.graphdb.ingestion.controller.structure.StaticStructureController;
import eu.ebrains.kg.graphdb.ingestion.model.DBOperation;
import eu.ebrains.kg.graphdb.ingestion.model.UpsertOperation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class SpaceDefinitionSemanticsHandler extends SemanticsHandler {

    private final ArangoDatabases databases;
    final IdUtils idUtils;

    public SpaceDefinitionSemanticsHandler(TypeUtils typeUtils, ArangoDatabases databases, IdUtils idUtils) {
        super(typeUtils);
        this.databases = databases;
        this.idUtils = idUtils;
    }

    @Override
    public List<DBOperation> createUpsertOperations(DataStage stage, ArangoDocumentReference rootDocumentReference, ArangoDocument document) {
        if(document.getDoc().types()!=null && document.getDoc().types().contains(EBRAINSVocabulary.META_SPACEDEFINITION_TYPE)){
            String spaceReference = document.getDoc().getAs(SchemaOrgVocabulary.NAME, String.class);
            ArangoDocumentReference targetSpace = StaticStructureController.createDocumentRefForMetaRepresentation(spaceReference, ArangoCollectionReference.fromSpace(InternalSpace.SPACES_SPACE));
            UpsertOperation upsertSpaceOperation = null;
            if(!databases.getByStage(stage).collection(targetSpace.getArangoCollectionReference().getCollectionName()).documentExists(targetSpace.getDocumentId().toString())){
                MetaRepresentation spaceMetaRepresentation = StaticStructureController.createMetaRepresentation(spaceReference, targetSpace.getArangoCollectionReference());
                upsertSpaceOperation = new UpsertOperation(null, new NormalizedJsonLd(typeUtils.translate(spaceMetaRepresentation, Map.class)), spaceMetaRepresentation.getIdRef(), false, false);
            }
            List<DBOperation> dbOperations = handleOverrideReference(rootDocumentReference, document, null, targetSpace, ArangoCollectionReference.fromSpace(new Space(EBRAINSVocabulary.META_SPACE), true));
            if(upsertSpaceOperation!=null){
                List<DBOperation> result = new ArrayList<>();
                result.add(upsertSpaceOperation);
                result.addAll(dbOperations);
                return result;
            }
            return dbOperations;
        }
        return Collections.emptyList();
    }
}

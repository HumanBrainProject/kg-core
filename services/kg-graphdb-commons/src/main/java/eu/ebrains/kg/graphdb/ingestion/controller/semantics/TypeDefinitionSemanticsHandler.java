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

package eu.ebrains.kg.graphdb.ingestion.controller.semantics;

import com.arangodb.ArangoDatabase;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.TypeUtils;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.ArangoUtils;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.commons.model.MetaRepresentation;
import eu.ebrains.kg.graphdb.ingestion.controller.structure.StaticStructureController;
import eu.ebrains.kg.graphdb.ingestion.model.DBOperation;
import eu.ebrains.kg.graphdb.ingestion.model.DeleteInstanceOperation;
import eu.ebrains.kg.graphdb.ingestion.model.UpsertOperation;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class TypeDefinitionSemanticsHandler extends SemanticsHandler {

    private final ArangoDatabases databases;
    private final ArangoUtils arangoUtils;

    public TypeDefinitionSemanticsHandler(TypeUtils typeUtils, ArangoDatabases databases, ArangoUtils arangoUtils) {
        super(typeUtils);
        this.databases = databases;
        this.arangoUtils = arangoUtils;
    }

    @Override
    public List<DBOperation> createMetaDeprecateOperations(SpaceName documentSpace, NormalizedJsonLd document) {
        if(document.types()!=null && document.types().contains(EBRAINSVocabulary.META_TYPEDEFINITION_TYPE)){
            List<DBOperation> operations = new ArrayList<>();
            String typeToBeDeprecated = document.getAs(EBRAINSVocabulary.META_TYPE, String.class);
            ArangoDocumentReference typeReference = StaticStructureController.createDocumentRefForMetaRepresentation(typeToBeDeprecated, ArangoCollectionReference.fromSpace(InternalSpace.TYPES_SPACE));
            operations.add(new DeleteInstanceOperation(typeReference));
            ArangoDatabase metaByStage = this.databases.getMetaByStage(DataStage.IN_PROGRESS);
            AQL relationEdges = new AQL();
            Map<String, Object> bindVars = new HashMap<>();
            relationEdges.addLine(AQL.trust("LET type = DOCUMENT(@typeDoc)"));
            bindVars.put("typeDoc", typeReference.getId());
            relationEdges.addLine(AQL.trust("LET globalPropertyRelation = (FOR property, type2property IN 1..1 OUTBOUND type `"+InternalSpace.GLOBAL_TYPE_TO_PROPERTY_EDGE_COLLECTION.getCollectionName()+"` RETURN type2property._id)"));
            relationEdges.addLine(AQL.trust("LET spacePropertyRelation = FLATTEN ("));
            relationEdges.addLine(AQL.trust("FOR space, space2type IN 1..1 INBOUND type `"+InternalSpace.SPACE_TO_TYPE_EDGE_COLLECTION.getCollectionName()+"`"));
            relationEdges.addLine(AQL.trust("LET propertyRelationBySpace = (FOR property, type2property IN 1..1 OUTBOUND space2type `"+InternalSpace.TYPE_TO_PROPERTY_EDGE_COLLECTION.getCollectionName()+"` RETURN type2property."+ ArangoVocabulary.ID+")"));
            relationEdges.addLine(AQL.trust("RETURN APPEND(propertyRelationBySpace, space2type."+ArangoVocabulary.ID+")"));
            relationEdges.addLine(AQL.trust(") RETURN UNION_DISTINCT(globalPropertyRelation, spacePropertyRelation)"));
            operations.addAll(metaByStage.query(relationEdges.build().getValue(), bindVars, String[].class).asListRemaining().stream().flatMap(Arrays::stream).map(id -> new DeleteInstanceOperation(ArangoDocumentReference.fromArangoId(id, true))).collect(Collectors.toList()));
            return operations;
        }
        return Collections.emptyList();
    }

    @Override
    public List<DBOperation> createUpsertOperations(DataStage stage, ArangoDocumentReference rootDocumentReference, ArangoDocument document) {
        if(document.getDoc().types()!=null && document.getDoc().types().contains(EBRAINSVocabulary.META_TYPEDEFINITION_TYPE)){
            JsonLdId typeReference = document.getDoc().getAs(EBRAINSVocabulary.META_TYPE, JsonLdId.class);
            if(typeReference!=null){
                ArangoDocumentReference targetType = StaticStructureController.createDocumentRefForMetaRepresentation(typeReference.getId(), ArangoCollectionReference.fromSpace(InternalSpace.TYPES_SPACE));
                UpsertOperation upsertTypeOperation = null;
                if(!databases.getByStage(stage).collection(targetType.getArangoCollectionReference().getCollectionName()).documentExists(targetType.getDocumentId().toString())){
                    MetaRepresentation typeMetaRepresentation = StaticStructureController.createMetaRepresentation(typeReference.getId(), targetType.getArangoCollectionReference());
                    upsertTypeOperation = new UpsertOperation(null, new NormalizedJsonLd(typeUtils.translate(typeMetaRepresentation, Map.class)), typeMetaRepresentation.getIdRef(), false, false);
                }
                List<DBOperation> dbOperations = handleOverrideReference(rootDocumentReference, document, typeReference, targetType, ArangoCollectionReference.fromSpace(new SpaceName(EBRAINSVocabulary.META_TYPE), true));
                if(upsertTypeOperation!=null){
                    List<DBOperation> result = new ArrayList<>();
                    result.add(upsertTypeOperation);
                    result.addAll(dbOperations);
                    return result;
                }
                return dbOperations;
            }
        }
        return Collections.emptyList();
    }
}

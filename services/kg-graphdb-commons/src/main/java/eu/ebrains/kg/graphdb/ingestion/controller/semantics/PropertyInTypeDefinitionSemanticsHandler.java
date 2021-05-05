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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.graphdb.ingestion.controller.semantics;

import com.arangodb.ArangoDatabase;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.TypeUtils;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.commons.model.ArangoEdge;
import eu.ebrains.kg.graphdb.ingestion.controller.IdFactory;
import eu.ebrains.kg.graphdb.ingestion.controller.structure.StaticStructureController;
import eu.ebrains.kg.graphdb.ingestion.model.DBOperation;
import eu.ebrains.kg.graphdb.ingestion.model.DeleteInstanceOperation;
import eu.ebrains.kg.graphdb.ingestion.model.UpsertOperation;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PropertyInTypeDefinitionSemanticsHandler extends SemanticsHandler {
    private final IdUtils idUtils;
    private final ArangoDatabases databases;

    public PropertyInTypeDefinitionSemanticsHandler(TypeUtils typeUtils, IdUtils idUtils, ArangoDatabases databases) {
        super(typeUtils);
        this.idUtils = idUtils;
        this.databases = databases;
    }

    @Override
    public List<DBOperation> createUpsertOperations(DataStage stage, ArangoDocumentReference rootDocumentRef, ArangoDocument document) {
        if (document.getDoc().types() != null && document.getDoc().types().contains(EBRAINSVocabulary.META_PROPERTY_IN_TYPE_DEFINITION_TYPE)) {
            JsonLdId propertyReference = document.getDoc().getAs(EBRAINSVocabulary.META_PROPERTY, JsonLdId.class);
            JsonLdId typeReference = document.getDoc().getAs(EBRAINSVocabulary.META_TYPE, JsonLdId.class);
            if (propertyReference != null && typeReference != null) {
                List<DBOperation> dbOperations = new ArrayList<>();
                ArangoDocumentReference globalTypeToPropertyEdgeRef = IdFactory.createDocumentRefForGlobalTypeToPropertyEdge(typeReference.getId(), propertyReference.getId());
                dbOperations.add(ensureGlobalTypeToPropertyEdge(globalTypeToPropertyEdgeRef, typeReference, propertyReference));
                dbOperations.addAll(handleOverrideReference(rootDocumentRef, document, propertyReference, globalTypeToPropertyEdgeRef, InternalSpace.CLIENT_TYPE_PROPERTY_EDGE_COLLECTION));
                dbOperations.addAll(handleExplicitlyStatedTargetTypes(document, globalTypeToPropertyEdgeRef));
                return dbOperations;
            }
        }
        return Collections.emptyList();
    }

    private UpsertOperation ensureGlobalTypeToPropertyEdge(ArangoDocumentReference globalTypeToPropertyEdgeRef, JsonLdId typeReference, JsonLdId propertyReference){
        ArangoEdge edge = new ArangoEdge();
        edge.redefineId(globalTypeToPropertyEdgeRef);
        edge.setTo(StaticStructureController.createDocumentRefForMetaRepresentation(propertyReference.getId(), ArangoCollectionReference.fromSpace(InternalSpace.PROPERTIES_SPACE)));
        edge.setFrom(StaticStructureController.createDocumentRefForMetaRepresentation(typeReference.getId(), ArangoCollectionReference.fromSpace(InternalSpace.TYPES_SPACE)));
        return new UpsertOperation(globalTypeToPropertyEdgeRef, typeUtils.translate(edge.getPayload(), NormalizedJsonLd.class), globalTypeToPropertyEdgeRef, false, false);
    }

    @Override
    public List<DBOperation> createMetaDeprecateOperations(SpaceName documentSpace, NormalizedJsonLd document) {
        if (document.types() != null && document.types().contains(EBRAINSVocabulary.META_PROPERTY_IN_TYPE_DEFINITION_TYPE)) {
            Boolean forcedRemoval = document.getAs(EBRAINSVocabulary.META_FORCED_REMOVAL, Boolean.class);
            if(forcedRemoval!=null && forcedRemoval){
                JsonLdId property = document.getAs(EBRAINSVocabulary.META_PROPERTY, JsonLdId.class);
                JsonLdId type = document.getAs(EBRAINSVocabulary.META_TYPE, JsonLdId.class);
                if(property!=null && type!=null) {
                    ArangoDocumentReference propertyDocument = StaticStructureController.createDocumentRefForMetaRepresentation(property.getId(), ArangoCollectionReference.fromSpace(InternalSpace.PROPERTIES_SPACE));
                    ArangoDocumentReference typeDocument = StaticStructureController.createDocumentRefForMetaRepresentation(type.getId(), ArangoCollectionReference.fromSpace(InternalSpace.TYPES_SPACE));
                    ArangoDocumentReference globalTypeToPropertyRef = IdFactory.createDocumentRefForGlobalTypeToPropertyEdge(type.getId(), property.getId());
                    List<DBOperation> operations = new ArrayList<>();
                    operations.add(new DeleteInstanceOperation(globalTypeToPropertyRef));
                    ArangoDatabase metaByStage = this.databases.getMetaByStage(DataStage.IN_PROGRESS);
                    AQL query = new AQL();
                    Map<String, Object> bindVars = new HashMap<>();
                    bindVars.put("property", propertyDocument.getId());
                    bindVars.put("type", typeDocument.getId());
                    query.addLine(AQL.trust("FOR d IN "+InternalSpace.TYPE_TO_PROPERTY_EDGE_COLLECTION.getCollectionName()));
                    query.addLine(AQL.trust("FILTER d._to == @property"));
                    query.addLine(AQL.trust("LET spaceType = DOCUMENT(d._from)"));
                    query.addLine(AQL.trust("FILTER spaceType._to == @type"));
                    query.addLine(AQL.trust("RETURN d._id"));
                    List<String> ids = metaByStage.query(query.build().getValue(), bindVars, String.class).asListRemaining();
                    if(ids!=null){
                        ids.stream().map(id -> ArangoDocumentReference.fromArangoId(id, true)).forEach(doc -> operations.add(new DeleteInstanceOperation(doc)));
                    }
                    return operations;
                }
            }
            else {
                ArangoDocumentReference instanceRef = new ArangoDocumentReference(ArangoCollectionReference.fromSpace(documentSpace), idUtils.getUUID(document.id()));
                return Collections.singletonList(new DeleteInstanceOperation(instanceRef));
            }
        }
        return Collections.emptyList();
    }

}

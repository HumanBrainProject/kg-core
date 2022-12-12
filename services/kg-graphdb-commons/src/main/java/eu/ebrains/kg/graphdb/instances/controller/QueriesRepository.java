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

package eu.ebrains.kg.graphdb.instances.controller;

import com.arangodb.ArangoDatabase;
import eu.ebrains.kg.arango.commons.ArangoQueries;
import eu.ebrains.kg.arango.commons.aqlbuilder.AQL;
import eu.ebrains.kg.arango.commons.model.*;
import eu.ebrains.kg.commons.*;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.markers.*;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.queries.model.spec.GraphQueryKeys;
import eu.ebrains.kg.graphdb.structure.controller.MetaDataController;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class QueriesRepository extends AbstractRepository{
    private final DocumentsRepository documents;
    private final ArangoDatabases databases;
    private final AuthContext authContext;
    private final PermissionsController permissionsController;
    private final MetaDataController metaDataController;
    private final EmbeddedAndAlternativesRepository embeddedAndAlternatives;

    public QueriesRepository(DocumentsRepository documents, ArangoDatabases databases, AuthContext authContext, PermissionsController permissionsController, MetaDataController metaDataController, EmbeddedAndAlternativesRepository embeddedAndAlternatives) {
        this.documents = documents;
        this.databases = databases;
        this.authContext = authContext;
        this.permissionsController = permissionsController;
        this.metaDataController = metaDataController;
        this.embeddedAndAlternatives = embeddedAndAlternatives;
    }

    @ExposesData
    public NormalizedJsonLd getQuery(SpaceName space, UUID id) {
        ArangoDocument document = documents.getDocument(DataStage.IN_PROGRESS, ArangoCollectionReference.fromSpace(space).doc(id));
        if (document == null || !document.getDoc().types().contains(EBRAINSVocabulary.META_QUERY_TYPE)) {
            //If it's not a query, it's not exposed...
            return null;
        }
        //We explicitly do not check for permissions because queries can be read by everyone
        final NormalizedJsonLd query = document.getDoc();
        query.removeAllInternalProperties();
        query.remove(EBRAINSVocabulary.META_ALTERNATIVE);
        return query;
    }

    @ExposesQuery
    public Paginated<NormalizedJsonLd> getQueriesByRootType(DataStage stage, PaginationParam paginationParam, String search, boolean embedded, boolean alternatives, String typeFilter) {
        ArangoDatabase database = databases.getByStage(stage);
        if (database.collection(InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName()).exists()) {
            Map<String, Object> bindVars = new HashMap<>();
            AQL aql = new AQL();
            final UserWithRoles userWithRoles = authContext.getUserWithRoles();
            Map<String, Object> whitelistFilter = permissionsController.whitelistFilterForReadInstances(metaDataController.getSpaceNames(stage, userWithRoles), userWithRoles, stage);
            if (whitelistFilter != null) {
                aql.specifyWhitelist();
                bindVars.putAll(whitelistFilter);
            }
            iterateThroughTypeList(Collections.singletonList(new Type(EBRAINSVocabulary.META_QUERY_TYPE)), null, bindVars, aql);
            aql.indent().addLine(AQL.trust("FOR v IN 1..1 OUTBOUND typeDefinition.type @@typeRelationCollection"));
            if (whitelistFilter != null) {
                aql.addDocumentFilterWithWhitelistFilter(AQL.trust("v"));
            }
            if (typeFilter != null && !typeFilter.isBlank()) {
                aql.addLine(AQL.trust("FILTER v.`" + GraphQueryKeys.GRAPH_QUERY_META.getFieldName() + "`.`" + GraphQueryKeys.GRAPH_QUERY_TYPE.getFieldName() + "` == @typeFilter"));
                bindVars.put("typeFilter", typeFilter);
            }
            if (search != null && !search.isBlank()) {
                aql.addLine(AQL.trust("FILTER LIKE(v.`" + GraphQueryKeys.GRAPH_QUERY_META.getFieldName() + "`.`" + GraphQueryKeys.GRAPH_QUERY_NAME.getFieldName() + "`, @search, true)"));
                aql.addLine(AQL.trust("OR LIKE(v.`" + GraphQueryKeys.GRAPH_QUERY_LABEL.getFieldName() + "`, @search, true)"));
                aql.addLine(AQL.trust("OR LIKE(v.`" + GraphQueryKeys.GRAPH_QUERY_DESCRIPTION.getFieldName() + "`, @search, true)"));
                bindVars.put("search", "%" + search + "%");
            }
            aql.addPagination(paginationParam);
            aql.addLine(AQL.trust("RETURN v"));
            bindVars.put("@typeRelationCollection", InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName());
            Paginated<NormalizedJsonLd> normalizedJsonLdPaginated = ArangoQueries.queryDocuments(database, new AQLQuery(aql, bindVars), null);
            embeddedAndAlternatives.handleAlternativesAndEmbedded(normalizedJsonLdPaginated.getData(), stage, alternatives, embedded);
            exposeRevision(normalizedJsonLdPaginated.getData());
            normalizedJsonLdPaginated.getData().forEach(NormalizedJsonLd::removeAllInternalProperties);
            return normalizedJsonLdPaginated;
        }
        return new Paginated<>(Collections.emptyList(), 0L, 0, 0);
    }



}

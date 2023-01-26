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

package eu.ebrains.kg.graphdb.queries.controller;

import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import eu.ebrains.kg.arango.commons.ArangoQueries;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.Tuple;
import eu.ebrains.kg.commons.exception.LimitExceededException;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.QueryResult;
import eu.ebrains.kg.commons.model.StreamedQueryResult;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.GraphDBArangoUtils;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import eu.ebrains.kg.graphdb.queries.model.spec.Specification;
import eu.ebrains.kg.graphdb.queries.utils.DataQueryBuilder;
import eu.ebrains.kg.graphdb.queries.utils.SpecificationToScopeQueryAdapter;
import eu.ebrains.kg.graphdb.structure.controller.MetaDataController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class QueryController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SpecificationInterpreter specificationInterpreter;

    private final PermissionsController permissionsController;

    private final ArangoDatabases arangoDatabases;

    private final GraphDBArangoUtils graphDBArangoUtils;

    private final MetaDataController metaDataController;

    private final Double maxMemoryForQuery;

    public QueryController(SpecificationInterpreter specificationInterpreter, ArangoDatabases arangoDatabases, PermissionsController permissionsController, GraphDBArangoUtils graphDBArangoUtils, @Value("${eu.ebrains.kg.arango.maxMemory:#{null}}") Double maxMemoryForQuery, MetaDataController metaDataController) {
        this.specificationInterpreter = specificationInterpreter;
        this.arangoDatabases = arangoDatabases;
        this.graphDBArangoUtils = graphDBArangoUtils;
        this.permissionsController = permissionsController;
        this.maxMemoryForQuery = maxMemoryForQuery;
        this.metaDataController = metaDataController;
    }


    public QueryResult query(UserWithRoles userWithRoles, KgQuery query, PaginationParam paginationParam, Map<String, String> filterValues, boolean scopeMode) {
        ArangoDatabase database = arangoDatabases.getByStage(query.getStage());
        final Tuple<AQLQuery, Specification> q = query(database, userWithRoles, query, paginationParam, filterValues, scopeMode);
        try {
            return new QueryResult(ArangoQueries.queryDocuments(database, q.getA(), maxMemoryForQuery), q.getB().getResponseVocab());
        } catch (ArangoDBException ex) {
            logger.error(String.format("Was not able to execute query: %s", q.getA()));
            // Test if the exception error num is for "Query use more memory than allowed"
            if (ex.getErrorNum() == 32) {
                throw new LimitExceededException(String.format("%s - Bandwidth Limit Exceeded - %s", HttpStatus.BANDWIDTH_LIMIT_EXCEEDED.value(), ex.getErrorNum()));
            } else {
                throw ex;
            }
        }
    }

    public StreamedQueryResult queryToStream(UserWithRoles userWithRoles, KgQuery query, PaginationParam paginationParam, Map<String, String> filterValues, boolean scopeMode) {
        ArangoDatabase database = arangoDatabases.getByStage(query.getStage());
        final Tuple<AQLQuery, Specification> q = query(database, userWithRoles, query, paginationParam, filterValues, scopeMode);
        try {
            return new StreamedQueryResult(ArangoQueries.queryDocumentsAsStream(database, q.getA(), maxMemoryForQuery), q.getB().getResponseVocab());
        } catch (ArangoDBException ex) {
            logger.error(String.format("Was not able to execute query: %s", q.getA()));
            throw ex;
        }
    }


    private Tuple<AQLQuery, Specification> query(ArangoDatabase database, UserWithRoles userWithRoles, KgQuery query, PaginationParam paginationParam, Map<String, String> filterValues, boolean scopeMode) {
        Specification specification = specificationInterpreter.readSpecification(query.getPayload());
        Map<String, Object> whitelistFilter;
        if(scopeMode){
            specification = new SpecificationToScopeQueryAdapter(specification).translate();
            // In scope mode, we don't apply the whitelist filter since we're only exposing ids and it is important
            // that we have the full scope of an instance
            whitelistFilter = null;
        }
        else {
            whitelistFilter = permissionsController.whitelistFilterForReadInstances(metaDataController.getSpaceNames(query.getStage(), userWithRoles), userWithRoles, query.getStage());
        }
        graphDBArangoUtils.getOrCreateArangoCollection(database, ArangoCollectionReference.fromSpace(InternalSpace.TYPE_SPACE));
        graphDBArangoUtils.getOrCreateArangoCollection(database, InternalSpace.TYPE_EDGE_COLLECTION);
        final List<String> spaceRestrictions = query.getRestrictToSpaces() == null ? null : query.getRestrictToSpaces().stream().filter(Objects::nonNull).map(ArangoCollectionReference::fromSpace).map(ArangoCollectionReference::getCollectionName).collect(Collectors.toList());
        AQLQuery aql = new DataQueryBuilder(specification, paginationParam, whitelistFilter, spaceRestrictions,  filterValues, database.getCollections().stream().map(c -> new ArangoCollectionReference(c.getName(), c.getType() == CollectionType.EDGES)).collect(Collectors.toList())).build();
        aql.addBindVar("idRestriction", query.getIdRestrictions() == null ? Collections.emptyList() : query.getIdRestrictions().stream().filter(Objects::nonNull).map(UUID::toString).collect(Collectors.toList()));
        return new Tuple<>(aql, specification);
    }



}

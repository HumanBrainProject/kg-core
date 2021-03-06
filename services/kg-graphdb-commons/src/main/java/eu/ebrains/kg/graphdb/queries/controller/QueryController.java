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

package eu.ebrains.kg.graphdb.queries.controller;

import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.AqlQueryOptions;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.QueryResult;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.controller.ArangoUtils;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import eu.ebrains.kg.graphdb.queries.model.spec.Specification;
import eu.ebrains.kg.graphdb.queries.utils.DataQueryBuilder;
import eu.ebrains.kg.graphdb.queries.utils.SpecificationToScopeQueryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class QueryController {

    private final static AqlQueryOptions QUERY_OPTIONS = new AqlQueryOptions();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SpecificationInterpreter specificationInterpreter;

    private final ArangoRepositoryCommons arangoRepositoryCommons;
    private final PermissionsController permissionsController;

    private final ArangoDatabases arangoDatabases;

    private final ArangoUtils arangoUtils;

    public QueryController(SpecificationInterpreter specificationInterpreter, ArangoDatabases arangoDatabases, ArangoRepositoryCommons arangoRepositoryCommons, PermissionsController permissionsController, ArangoUtils arangoUtils) {
        this.specificationInterpreter = specificationInterpreter;
        this.arangoDatabases = arangoDatabases;
        this.arangoUtils = arangoUtils;
        this.arangoRepositoryCommons = arangoRepositoryCommons;
        this.permissionsController = permissionsController;
    }


    public QueryResult query(UserWithRoles userWithRoles, KgQuery query, PaginationParam paginationParam, Map<String, String> filterValues, boolean scopeMode) {
        ArangoDatabase database = arangoDatabases.getByStage(query.getStage());
        Specification specification = specificationInterpreter.readSpecification(query.getPayload(), null);
        if(scopeMode){
            specification = new SpecificationToScopeQueryAdapter(specification).translate();
        }
        Map<String, Object> whitelistFilter = permissionsController.whitelistFilterForReadInstances(userWithRoles, query.getStage());
        arangoUtils.getOrCreateArangoCollection(database, ArangoCollectionReference.fromSpace(InternalSpace.TYPE_SPACE));
        arangoUtils.getOrCreateArangoCollection(database, InternalSpace.TYPE_EDGE_COLLECTION);
        AQLQuery aql = new DataQueryBuilder(specification, paginationParam, whitelistFilter, filterValues, database.getCollections().stream().map(c -> new ArangoCollectionReference(c.getName(), c.getType() == CollectionType.EDGES)).collect(Collectors.toList())).build();
        aql.addBindVar("idRestriction", query.getIdRestrictions() == null ? Collections.emptyList() : query.getIdRestrictions().stream().filter(Objects::nonNull).map(UUID::toString).collect(Collectors.toList()));
        try {
            return new QueryResult(arangoRepositoryCommons.queryDocuments(database, aql), specification.getResponseVocab());
        } catch (ArangoDBException ex) {
            logger.error(String.format("Was not able to execute query: %s", aql));
            throw ex;
        }
    }

}

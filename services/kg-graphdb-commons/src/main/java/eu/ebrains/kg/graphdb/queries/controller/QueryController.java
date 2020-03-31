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

package eu.ebrains.kg.graphdb.queries.controller;

import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.AqlQueryOptions;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import eu.ebrains.kg.graphdb.queries.model.spec.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class QueryController {

    private final static AqlQueryOptions QUERY_OPTIONS = new AqlQueryOptions();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SpecificationInterpreter specificationInterpreter;

    private final ArangoRepositoryCommons arangoRepositoryCommons;
    private final PermissionsController permissionsController;
    private final AuthContext authContext;

    private final ArangoDatabases arangoDatabases;

    private final IdUtils idUtils;

    public QueryController(SpecificationInterpreter specificationInterpreter, ArangoDatabases arangoDatabases, IdUtils idUtils, ArangoRepositoryCommons arangoRepositoryCommons, PermissionsController permissionsController, AuthContext authContext) {
        this.specificationInterpreter = specificationInterpreter;
        this.arangoDatabases = arangoDatabases;
        this.idUtils = idUtils;
        this.arangoRepositoryCommons = arangoRepositoryCommons;
        this.permissionsController = permissionsController;
        this.authContext = authContext;
    }


    public Paginated<NormalizedJsonLd> query(UserWithRoles userWithRoles, KgQuery query, PaginationParam paginationParam, Map<String, String> filterValues) {
        ArangoDatabase database = arangoDatabases.getByStage(query.getStage());
        Specification specification = specificationInterpreter.readSpecification(query.getPayload(), null);
        Map<String, Object> whitelistFilter = permissionsController.whitelistFilter(userWithRoles, query.getStage());
        AQLQuery aql = new DataQueryBuilder(specification, paginationParam, whitelistFilter, filterValues, database.getCollections().stream().map(c -> new ArangoCollectionReference(c.getName(), c.getType() == CollectionType.EDGES)).collect(Collectors.toList())).build();
        aql.addBindVar("idRestriction", query.getIdRestrictions() == null ? Collections.emptyList() : query.getIdRestrictions());
        try {
            return arangoRepositoryCommons.queryDocuments(database, aql);
        } catch (ArangoDBException ex) {
            logger.error(String.format("Was not able to execute query: %s", aql));
            throw ex;
        }
    }

}

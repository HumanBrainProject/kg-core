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

package eu.ebrains.kg.graphdb.queries.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.api.GraphDBQueries;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.StreamedQueryResult;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.graphdb.queries.controller.QueryController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GraphDBQueriesAPI implements GraphDBQueries.Client {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AuthContext authContext;
    private final QueryController queryController;

    public GraphDBQueriesAPI(AuthContext authContext, QueryController queryController) {
        this.queryController = queryController;
        this.authContext = authContext;
    }

    @Override
    public StreamedQueryResult executeQuery(KgQuery query, Map<String, String> params, PaginationParam paginationParam){
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        checkPermissionForQueryExecution(userWithRoles);
        return queryController.queryToStream(userWithRoles, query, paginationParam, params, false);
    }

    private void checkPermissionForQueryExecution(UserWithRoles userWithRoles){
        //TODO this is a client permission, not a user permission... let's see how we can handle this.
        //Functionality executeQuery = graphDBMode.isSync() ? Functionality.EXECUTE_SYNC_QUERY : Functionality.EXECUTE_QUERY;
//        if (!permissions.hasPermission(Functionality.EXECUTE_QUERY)) {
//            throw new ForbiddenException();
//        }
    }

}

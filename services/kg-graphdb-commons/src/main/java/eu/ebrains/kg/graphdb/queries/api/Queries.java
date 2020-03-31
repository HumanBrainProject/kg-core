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

package eu.ebrains.kg.graphdb.queries.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.graphdb.queries.controller.QueryController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/queries")
public class Queries {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AuthContext authContext;


    private final QueryController queryController;

    public Queries(AuthContext authContext, QueryController queryController) {
        this.queryController = queryController;
        this.authContext = authContext;
    }


    @PostMapping
    public Paginated<NormalizedJsonLd> executeQuery(@RequestBody KgQuery query, PaginationParam paginationParam){
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        checkPermissionForQueryExecution(userWithRoles);
        return queryController.query(userWithRoles, query, paginationParam, null);
    }

    private void checkPermissionForQueryExecution(UserWithRoles userWithRoles){
        //TODO this is a client permission, not a user permission... let's see how we can handle this.
        //Functionality executeQuery = graphDBMode.isSync() ? Functionality.EXECUTE_SYNC_QUERY : Functionality.EXECUTE_QUERY;
//        if (!permissions.hasPermission(Functionality.EXECUTE_QUERY)) {
//            throw new ForbiddenException();
//        }
    }

}

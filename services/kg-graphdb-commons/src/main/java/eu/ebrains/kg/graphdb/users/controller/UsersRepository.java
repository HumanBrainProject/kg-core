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

package eu.ebrains.kg.graphdb.users.controller;

import com.arangodb.ArangoDatabase;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class UsersRepository {

    private final ArangoRepositoryCommons arangoRepositoryCommons;
    private final ArangoDatabases databases;
    private final Permissions permissions;
    private final AuthContext authContext;

    public UsersRepository(ArangoRepositoryCommons arangoRepositoryCommons, ArangoDatabases databases, Permissions permissions, AuthContext authContext) {
        this.arangoRepositoryCommons = arangoRepositoryCommons;
        this.databases = databases;
        this.permissions = permissions;
        this.authContext = authContext;
    }

    public Paginated<NormalizedJsonLd> getUsers(PaginationParam pagination) {
        if(!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.LIST_USERS, null)){
            throw new ForbiddenException("No right to list users");
        }
        ArangoDatabase db = databases.getByStage(DataStage.NATIVE);
        ArangoCollectionReference userCollection = ArangoCollectionReference.fromSpace(InternalSpace.USERS_SPACE);
        if (db.collection(userCollection.getCollectionName()).exists()) {
            AQLQuery userQuery = createUserQuery(pagination,  db);
            return arangoRepositoryCommons.queryDocuments(db, userQuery);
        }
        return new Paginated<>(Collections.emptyList(), 0, 0, 0);
    }

    private AQLQuery createUserQuery(PaginationParam paginationParam, ArangoDatabase db) {
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR user IN @@userCollection"));
        bindVars.put("@userCollection", ArangoCollectionReference.fromSpace(InternalSpace.USERS_SPACE).getCollectionName());
        aql.addPagination(paginationParam);
        aql.addLine(AQL.trust("RETURN user"));
        return new AQLQuery(aql, bindVars);
    }
}

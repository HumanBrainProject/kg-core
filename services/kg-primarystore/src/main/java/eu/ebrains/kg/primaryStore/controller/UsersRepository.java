/*
 * Copyright 2022 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.primaryStore.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.model.DocumentCreateOptions;
import eu.ebrains.kg.arango.commons.ArangoQueries;
import eu.ebrains.kg.arango.commons.aqlbuilder.AQL;
import eu.ebrains.kg.arango.commons.aqlbuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.*;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.SetupLogic;
import eu.ebrains.kg.commons.exception.AmbiguousException;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.ReducedUserInformation;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import eu.ebrains.kg.commons.semantics.vocabularies.HBPVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class UsersRepository implements SetupLogic {

    private final ArangoDatabaseProxy arangoDatabase;
    private final Permissions permissions;
    private final AuthContext authContext;
    private final IdUtils idUtils;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public UsersRepository(@Qualifier("userDB") ArangoDatabaseProxy arangoDatabase, Permissions permissions, AuthContext authContext, IdUtils idUtils) {
        this.arangoDatabase = arangoDatabase;
        this.permissions = permissions;
        this.authContext = authContext;
        this.idUtils = idUtils;
    }

    @PostConstruct
    public void setup() {
        arangoDatabase.createIfItDoesntExist();
        arangoDatabase.createCollectionIfItDoesntExist(ArangoCollectionReference.fromSpace(InternalSpace.USERS_SPACE));
    }

    private ArangoCollection getUserCollection() {
        return arangoDatabase.getOrCreate().collection(ArangoCollectionReference.fromSpace(InternalSpace.USERS_SPACE).getCollectionName());
    }

    public Paginated<NormalizedJsonLd> getUsers(PaginationParam pagination) {
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.LIST_USERS, null)) {
            throw new ForbiddenException("No right to list users");
        }
        final AQLQuery query = createUserQuery(pagination);
        return ArangoQueries.queryDocuments(arangoDatabase.getOrCreate(), query, null);
    }

    private static class ReducedUserInformationLookup extends HashMap<String, ReducedUserInformation>{}

    public Map<String, ReducedUserInformation> getUsers(Set<UUID> uuids){
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("RETURN {"));
        int counter = 0;
        for (UUID uuid : uuids) {
            String doc = String.format("u%d", counter);
            bindVars.put(doc, uuid);
            aql.addLine(AQL.trust(String.format("@%s: KEEP(DOCUMENT(\"%s\", @%s), [\"@id\", \"http://schema.org/name\", \"http://schema.org/alternateName\", \"http://schema.org/identifier\"])", doc, ArangoCollectionReference.fromSpace(InternalSpace.USERS_SPACE).getCollectionName(), doc)));
            if(counter<uuids.size()){
                aql.add(AQL.trust(", "));
            }
            counter++;
        }
        aql.addLine(AQL.trust("}"));

        final List<ReducedUserInformationLookup> reducedUserInformationLookups = arangoDatabase.getOrCreate().query(aql.build().getValue(), bindVars, ReducedUserInformationLookup.class).asListRemaining();
        if(reducedUserInformationLookups.isEmpty()){
            return Collections.emptyMap();
        }
        else if(reducedUserInformationLookups.size()==1){
            return reducedUserInformationLookups.get(0);
        }
        throw new AmbiguousException("The lookup of user information created too many results");
    }


    public Paginated<NormalizedJsonLd> getUsersWithLimitedInfo(PaginationParam pagination, String id) {
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.LIST_USERS_LIMITED, null)) {
            throw new ForbiddenException("No right to list users");
        }
        final AQLQuery query = createUserLimitedQuery(pagination, id);
        return ArangoQueries.queryDocuments(arangoDatabase.getOrCreate(), query, null);
    }

    private AQLQuery createUserQuery(PaginationParam paginationParam) {
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR user IN @@userCollection"));
        bindVars.put("@userCollection", ArangoCollectionReference.fromSpace(InternalSpace.USERS_SPACE).getCollectionName());
        aql.addPagination(paginationParam);
        aql.addLine(AQL.trust("RETURN user"));
        return new AQLQuery(aql, bindVars);
    }

    private AQLQuery createUserLimitedQuery(PaginationParam paginationParam, String id) {
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR user IN @@userCollection"));
        bindVars.put("@userCollection", ArangoCollectionReference.fromSpace(InternalSpace.USERS_SPACE).getCollectionName());
        aql.addPagination(paginationParam);
        if (StringUtils.isNotBlank(id)) {
            aql.addLine(AQL.trust("FILTER user.`" + HBPVocabulary.NAMESPACE + "users/nativeId" + "` == @id"));
            bindVars.put("id", id);
        }
        aql.addLine(AQL.trust("RETURN UNSET(user, "));
        aql.addLine(AQL.trust("\"" + SchemaOrgVocabulary.EMAIL + "\")"));
        return new AQLQuery(aql, bindVars);
    }


    public void updateUserRepresentation(User user) {
        final UUID userInstanceId = getUserUUID(user);
        final NormalizedJsonLd userDocument = getUserCollection().getDocument(userInstanceId.toString(), NormalizedJsonLd.class);
        if (userDocument == null || !new User(userDocument).isEqual(user)) {
            logger.info(String.format("Creating / updating user profile for %s", user.getNativeId()));
            user.setId(idUtils.buildAbsoluteUrl(userInstanceId));
            user.put(ArangoVocabulary.KEY, userInstanceId);
            getUserCollection().insertDocument(user, new DocumentCreateOptions().overwrite(true));
        }
    }


    @NotNull
    public UUID getUserUUID(User user) {
        try {
            return UUID.fromString(user.getNativeId());
        } catch (IllegalArgumentException e) {
            //If the native id is not a UUID on its own, we build one based on the string.
            return UUID.nameUUIDFromBytes(user.getNativeId().getBytes(StandardCharsets.UTF_8));
        }
    }
}

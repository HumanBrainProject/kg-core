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

package eu.ebrains.kg.graphdb.spaces.controller;

import com.arangodb.ArangoDatabase;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.exception.AmbiguousException;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ArangoRepositorySpaces {

    private final ArangoDatabases databases;
    private final Permissions permissions;
    private final PermissionsController permissionsController;
    private final AuthContext authContext;
    private final ArangoRepositoryCommons arangoRepositoryCommons;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ArangoRepositorySpaces(ArangoDatabases databases, Permissions permissions, PermissionsController permissionsController, AuthContext authContext, ArangoRepositoryCommons arangoRepositoryCommons) {
        this.databases = databases;
        this.permissions = permissions;
        this.permissionsController = permissionsController;
        this.authContext = authContext;
        this.arangoRepositoryCommons = arangoRepositoryCommons;
    }

    public NormalizedJsonLd getSpace(SpaceName space, DataStage stage) {
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.READ_SPACE, space)){
            throw new ForbiddenException(String.format("You don't have the right to read the space %s", space.getName()));
        }
        ArangoDatabase db = databases.getMetaByStage(stage);
        ArangoCollectionReference spaceCollection = ArangoCollectionReference.fromSpace(InternalSpace.SPACES_SPACE);
        if (db.collection(spaceCollection.getCollectionName()).exists()) {
            AQLQuery spaceQuery = createSpaceQuery(null, space.getName(), db);
            Paginated<NormalizedJsonLd> normalizedJsonLds = arangoRepositoryCommons.queryDocuments(db, spaceQuery);
            if (normalizedJsonLds.getData().size() == 0) {
                return null;
            } else if (normalizedJsonLds.getData().size() == 1) {
                return normalizedJsonLds.getData().get(0);
            } else {
                throw new AmbiguousException("Found too many instances for this name");
            }
        }
        return null;
    }

    public Paginated<NormalizedJsonLd> getSpaces(DataStage stage, PaginationParam pagination) {
        ArangoDatabase db = databases.getMetaByStage(stage);
        ArangoCollectionReference spaceCollection = ArangoCollectionReference.fromSpace(InternalSpace.SPACES_SPACE);
        if (db.collection(spaceCollection.getCollectionName()).exists()) {
            AQLQuery spaceQuery = createSpaceQuery(pagination, null, db);
            return arangoRepositoryCommons.queryDocuments(db, spaceQuery);
        }
        return new Paginated<>(Collections.emptyList(), 0, 0, 0);
    }


    private AQLQuery createSpaceQuery(PaginationParam param, String filterBySpace, ArangoDatabase db) {
        Set<SpaceName> whitelistedSpaces = permissionsController.whitelistedSpaceReads(authContext.getUserWithRoles());
        ArangoCollectionReference extensionSpace = ArangoCollectionReference.fromSpace(new SpaceName(EBRAINSVocabulary.META_SPACE), true);
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR space IN @@spaceCollection"));
        aql.addPagination(param);
        if(whitelistedSpaces!=null){
            aql.addLine(AQL.trust("FILTER space.@schemaorgname IN @whitelistedSpaces"));
            bindVars.put("whitelistedSpaces", whitelistedSpaces.stream().map(SpaceName::getName).collect(Collectors.toSet()));
            bindVars.put("schemaorgname", SchemaOrgVocabulary.NAME);
        }
        if (filterBySpace != null) {
            aql.addLine(AQL.trust("FILTER space.@schemaorgname == @filterName"));
            bindVars.put("schemaorgname", SchemaOrgVocabulary.NAME);
            bindVars.put("filterName", filterBySpace);
        }
        if (db.collection(extensionSpace.getCollectionName()).exists()) {
            aql.addLine(AQL.trust("LET overrides = ("));
            aql.addLine(AQL.trust("FOR o IN 1..1 INBOUND space @extensionRef"));
            aql.addLine(AQL.trust("LET att = (FOR a IN ATTRIBUTES(o)"));
            aql.addLine(AQL.trust("FILTER LIKE(a, @filter)"));
            aql.addLine(AQL.trust("RETURN {"));
            aql.addLine(AQL.trust("name: a,"));
            aql.addLine(AQL.trust("value: o[a]"));
            aql.addLine(AQL.trust("})"));
            aql.addLine(AQL.trust("RETURN ZIP(att[*].name, att[*].value))"));
            bindVars.put("extensionRef", extensionSpace.getCollectionName());
            bindVars.put("filter", EBRAINSVocabulary.META_SPACE + "/%");
        }
        else{
            aql.addLine(AQL.trust("LET overrides = []"));
        }
        aql.addLine(AQL.trust("RETURN MERGE(PUSH(overrides, space))"));
        bindVars.put("@spaceCollection", ArangoCollectionReference.fromSpace(InternalSpace.SPACES_SPACE).getCollectionName());
        return new AQLQuery(aql, bindVars);
    }

}

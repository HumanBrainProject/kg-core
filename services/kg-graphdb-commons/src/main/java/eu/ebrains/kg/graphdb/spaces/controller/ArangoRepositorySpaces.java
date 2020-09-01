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

package eu.ebrains.kg.graphdb.spaces.controller;

import com.arangodb.ArangoDatabase;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.exception.AmbiguousException;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class ArangoRepositorySpaces {

    private final ArangoDatabases databases;
    private final ArangoRepositoryCommons arangoRepositoryCommons;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ArangoRepositorySpaces(ArangoDatabases databases, ArangoRepositoryCommons arangoRepositoryCommons) {
        this.databases = databases;
        this.arangoRepositoryCommons = arangoRepositoryCommons;
    }

    public NormalizedJsonLd getSpace(Space space, DataStage stage) {
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
        ArangoCollectionReference extensionSpace = ArangoCollectionReference.fromSpace(new Space(EBRAINSVocabulary.META_SPACE), true);
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR space IN @@spaceCollection"));
        aql.addPagination(param);
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

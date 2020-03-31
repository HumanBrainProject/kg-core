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
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.ingestion.controller.structure.StaticStructureController;
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

    public NormalizedJsonLd getSpace(Space space, DataStage stage){
        ArangoDatabase db = databases.getMetaByStage(stage);
        ArangoCollectionReference spaceCollection = ArangoCollectionReference.fromSpace(InternalSpace.SPACES_SPACE);
        if(db.collection(spaceCollection.getCollectionName()).exists()){
            ArangoDocumentReference spaceDocumentReference = StaticStructureController.createDocumentRefForMetaRepresentation(space.getName(), spaceCollection);
            NormalizedJsonLd document = db.collection(spaceCollection.getCollectionName()).getDocument(spaceDocumentReference.getDocumentId().toString(), NormalizedJsonLd.class);
            return document != null ? document.removeAllInternalProperties() : null;
        }
        return null;
    }

    public Paginated<NormalizedJsonLd> getSpaces(DataStage stage, PaginationParam pagination) {
        ArangoDatabase db = databases.getMetaByStage(stage);
        ArangoCollectionReference spaceCollection = ArangoCollectionReference.fromSpace(InternalSpace.SPACES_SPACE);
        if(db.collection(spaceCollection.getCollectionName()).exists()) {
            AQLQuery spaceQuery = createSpaceQuery(pagination);
            return arangoRepositoryCommons.queryDocuments(db, spaceQuery, NormalizedJsonLd::removeAllInternalProperties);
        }
        return new Paginated<>(Collections.emptyList(), 0, 0, 0);
    }

    private AQLQuery createSpaceQuery(PaginationParam param){
        AQL aql = new AQL();
        aql.addLine(AQL.trust("FOR space IN @@spaceCollection"));
        aql.addPagination(param);
        aql.addLine(AQL.trust("    RETURN space"));
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("@spaceCollection", ArangoCollectionReference.fromSpace(InternalSpace.SPACES_SPACE).getCollectionName());
        return new AQLQuery(aql, bindVars);
    }

}

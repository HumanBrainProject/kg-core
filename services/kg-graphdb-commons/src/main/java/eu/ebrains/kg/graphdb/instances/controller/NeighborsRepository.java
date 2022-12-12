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

package eu.ebrains.kg.graphdb.instances.controller;

import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import eu.ebrains.kg.arango.commons.aqlbuilder.AQL;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.exception.AmbiguousException;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.markers.ExposesMinimalData;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.GraphEntity;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class NeighborsRepository extends AbstractRepository{

    private final Permissions permissions;
    private final AuthContext authContext;
    private final PermissionsController permissionsController;
    private final ArangoDatabases databases;

    public NeighborsRepository(Permissions permissions, AuthContext authContext, PermissionsController permissionsController, ArangoDatabases databases) {
        this.permissions = permissions;
        this.authContext = authContext;
        this.permissionsController = permissionsController;
        this.databases = databases;
    }

    @ExposesMinimalData
    public GraphEntity getNeighbors(DataStage stage, SpaceName space, UUID id) {
        if (!permissions.hasPermission(authContext.getUserWithRoles(), permissionsController.getReadFunctionality(stage), space, id)) {
            throw new ForbiddenException();
        }
        //FIXME: Do we have to restrict this to the instances with minimal read access?
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        ArangoDatabase db = databases.getByStage(stage);
        //The edges are injection-safe since they have been checked beforehand - so we can trust these values.
        String edges = String.join(", ", getAllEdgeCollections(db));

        //For now, we're hardcoding the number of investigated levels for simplicity. this could be done differently if we want to make it parametrized
        aql.addLine(AQL.trust("LET doc = DOCUMENT(@id)"));
        bindVars.put("id", String.format("%s/%s", ArangoCollectionReference.fromSpace(space).getCollectionName(), id));

        if (!edges.isEmpty()) {
            aql.addLine(AQL.trust("LET inbnd = (FOR inbnd IN 1..1 INBOUND doc " + edges));
            aql.addLine(AQL.trust("    RETURN { \"id\": inbnd._key, \"name\": inbnd._label, \"types\": inbnd.`@type`, \"space\": inbnd.`" + EBRAINSVocabulary.META_SPACE + "`})"));
            aql.addLine(AQL.trust("LET outbnd = (FOR outbnd IN 1..1 OUTBOUND doc " + edges));
            aql.addLine(AQL.trust("    LET outbnd2 = (FOR outbnd2 IN 1..1 OUTBOUND outbnd " + edges));
            aql.addLine(AQL.trust("    RETURN {\"id\": outbnd2._key, \"name\": outbnd2._label, \"types\": outbnd2.`@type`, \"space\": outbnd2.`" + EBRAINSVocabulary.META_SPACE + "`})"));
            aql.addLine(AQL.trust("    RETURN {\"id\": outbnd._key,  \"name\": outbnd._label, \"outbound\": outbnd2, \"types\": outbnd.`@type`, \"space\": outbnd.`" + EBRAINSVocabulary.META_SPACE + "` })"));
        } else {
            aql.addLine(AQL.trust("LET inbnd = []"));
            aql.addLine(AQL.trust("LET outbnd = []"));
        }
        aql.addLine(AQL.trust("RETURN {\"id\": doc._key, \"name\": doc._label, \"inbound\" : inbnd, \"outbound\": outbnd, \"types\": doc.`@type`, \"space\": doc.`" + EBRAINSVocabulary.META_SPACE + "` }"));

        List<GraphEntity> graphEntities = db.query(aql.build().getValue(), bindVars, new AqlQueryOptions(), GraphEntity.class).asListRemaining();
        if (graphEntities.isEmpty()) {
            return null;
        } else if (graphEntities.size() == 1) {
            return graphEntities.get(0);
        } else {
            throw new AmbiguousException(String.format("Did find multiple instances for the id %s", id));
        }
    }
}

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
import com.arangodb.entity.CollectionType;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import eu.ebrains.kg.arango.commons.aqlbuilder.AQL;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.TypeUtils;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.DynamicJson;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.markers.ExposesReleaseStatus;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.ReleaseStatus;
import eu.ebrains.kg.commons.model.ScopeElement;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ReleaseStatusRepository extends AbstractRepository {

    private final AuthContext authContext;
    private final Permissions permissions;
    private final ScopeRepository scope;
    private final ArangoDatabases databases;
    private final JsonAdapter jsonAdapter;

    public ReleaseStatusRepository(AuthContext authContext, Permissions permissions, ScopeRepository scope, ArangoDatabases databases, JsonAdapter jsonAdapter) {
        this.authContext = authContext;
        this.permissions = permissions;
        this.scope = scope;
        this.databases = databases;
        this.jsonAdapter = jsonAdapter;
    }

    @ExposesReleaseStatus
    public Map<UUID, ReleaseStatus> getIndividualReleaseStatus(List<InstanceId> instanceIds, ReleaseTreeScope releaseTreeScope) {
        final UserWithRoles userWithRoles = authContext.getUserWithRoles();
        // Depending on the number of i nstance ids, the permission check can take a while. To make things faster,
        // we memorize if there are space permissions available and only do explicit checks for individual instances if needed.
        Set<SpaceName> permissionsForSpace = new HashSet<>();
        instanceIds.forEach(id -> {
            if (!permissionsForSpace.contains(id.getSpace())) {
                if (!permissions.hasPermission(userWithRoles, Functionality.RELEASE_STATUS, id.getSpace())) {
                    if (!permissions.hasPermission(userWithRoles, Functionality.RELEASE_STATUS, id.getSpace(), id.getUuid())) {
                        throw new ForbiddenException();
                    }
                } else if (id.getSpace() != null) {
                    permissionsForSpace.add(id.getSpace());
                }
            }
        });
        if(releaseTreeScope == ReleaseTreeScope.TOP_INSTANCE_ONLY){
            return TypeUtils.splitList(instanceIds, 2000).stream().map(chunk -> getTopInstanceReleaseStatus(chunk).entrySet()).flatMap(Collection::stream).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        //TODO optimize in terms of bulk queries
        Map<UUID, ReleaseStatus> result = new HashMap<>();
        instanceIds.forEach(i -> {
            ReleaseStatus releaseStatus = getReleaseStatus(i.getSpace(), i.getUuid(), releaseTreeScope);
            result.put(i.getUuid(), releaseStatus);
        });
        return result;

    }

    @ExposesReleaseStatus
    public ReleaseStatus getReleaseStatus(SpaceName space, UUID id, ReleaseTreeScope treeScope) {
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.RELEASE_STATUS, space, id)) {
            throw new ForbiddenException();
        }
        switch (treeScope) {
            case TOP_INSTANCE_ONLY:
                return getTopInstanceReleaseStatus(Collections.singletonList(new InstanceId(id, space))).get(id);
            case CHILDREN_ONLY:
            case CHILDREN_ONLY_RESTRICTED:
                //FIXME restrict exposed release status based on permissions.
                ScopeElement scopeForInstance = scope.getScopeForInstance(space, id, DataStage.IN_PROGRESS, treeScope == ReleaseTreeScope.CHILDREN_ONLY_RESTRICTED);
                if (scopeForInstance.getChildren() == null || scopeForInstance.getChildren().isEmpty()) {
                    return null;
                }
                Set<InstanceId> instanceIds = fetchInvolvedInstances(scopeForInstance, new HashSet<>());
                //Ignore top instance
                instanceIds.remove(new InstanceId(id, space));
                AQL aql = new AQL();
                aql.addLine(AQL.trust("FOR id IN @ids"));
                Map<String, Object> bindVars = new HashMap<>();
                bindVars.put("ids", instanceIds.stream().map(instanceId -> ArangoDocumentReference.fromInstanceId(instanceId).getId()).collect(Collectors.toList()));
                aql.addLine(AQL.trust("LET doc = DOCUMENT(id)"));
                aql.addLine(AQL.trust("LET status = FIRST((FOR v IN 1..1 INBOUND  doc @@releaseStatusCollection"));
                bindVars.put("@releaseStatusCollection", InternalSpace.RELEASE_STATUS_EDGE_COLLECTION.getCollectionName());
                aql.addLine(AQL.trust("RETURN v.`" + SchemaOrgVocabulary.NAME + "`))"));
                aql.addLine(AQL.trust("RETURN status"));
                ArangoDatabase db = databases.getByStage(DataStage.IN_PROGRESS);
                List<String> status = db.query(aql.build().getValue(), bindVars, String.class).asListRemaining();
                if (status.contains(null) || status.contains(ReleaseStatus.UNRELEASED.name())) {
                    return ReleaseStatus.UNRELEASED;
                } else if (status.contains(ReleaseStatus.HAS_CHANGED.name())) {
                    return ReleaseStatus.HAS_CHANGED;
                } else {
                    return ReleaseStatus.RELEASED;
                }
            default:
                throw new RuntimeException("Release tree scope unknown");
        }
    }

    private Map<UUID, ReleaseStatus> getTopInstanceReleaseStatus(List<InstanceId> instanceId) {
        ArangoDatabase db = databases.getByStage(DataStage.IN_PROGRESS);
        ArangoCollectionReference releaseStatusCollection = InternalSpace.RELEASE_STATUS_EDGE_COLLECTION;
        if (!db.collection(releaseStatusCollection.getCollectionName()).exists()) {
            db.createCollection(releaseStatusCollection.getCollectionName(), new CollectionCreateOptions().type(CollectionType.EDGES));
        }
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR id in @ids"));
        bindVars.put("ids", instanceId.stream().map(id -> ArangoDocumentReference.fromInstanceId(id).getId()).collect(Collectors.toList()));
        aql.addLine(AQL.trust("LET doc = DOCUMENT(id)"));
        aql.addLine(AQL.trust("RETURN {\"id\": doc._key, \"status\": NOT_NULL(FIRST(FOR v IN 1..1 INBOUND doc @@releaseStatusCollection"));
        aql.addLine(AQL.trust("RETURN v.`" + SchemaOrgVocabulary.NAME + "`), \"" + ReleaseStatus.UNRELEASED.name() + "\")}"));
        bindVars.put("@releaseStatusCollection", releaseStatusCollection.getCollectionName());
        List<String> data = db.query(aql.build().getValue(), bindVars, new AqlQueryOptions(), String.class).asListRemaining();
        Map<UUID, ReleaseStatus> result = new HashMap<>();
        data.stream().map(d -> jsonAdapter.fromJson(d, DynamicJson.class)).forEach(d -> {
            result.put(UUID.fromString(d.getAs("id", String.class)), ReleaseStatus.valueOf(d.getAs("status", String.class)));
        });
        return result;
    }

    private Set<InstanceId> fetchInvolvedInstances(ScopeElement element, Set<InstanceId> collector) {
        collector.add(new InstanceId(element.getId(), new SpaceName(element.getSpace())));
        if (element.getChildren() != null) {
            element.getChildren().forEach(c -> fetchInvolvedInstances(c, collector));
        }
        return collector;
    }
}

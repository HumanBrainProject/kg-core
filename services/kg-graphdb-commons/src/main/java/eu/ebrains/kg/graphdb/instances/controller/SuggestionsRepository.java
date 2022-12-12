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

import eu.ebrains.kg.arango.commons.ArangoQueries;
import eu.ebrains.kg.arango.commons.aqlbuilder.AQL;
import eu.ebrains.kg.arango.commons.aqlbuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.*;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.markers.*;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import eu.ebrains.kg.graphdb.structure.controller.MetaDataController;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SuggestionsRepository extends AbstractRepository {

    private final InstancesRepository instances;
    private final  IdUtils idUtils;
    private final  AuthContext authContext;
    private final  PermissionsController permissionsController;
    private final  MetaDataController metaDataController;
    private final  ArangoDatabases databases;

    public SuggestionsRepository(InstancesRepository instances, IdUtils idUtils, AuthContext authContext, PermissionsController permissionsController, MetaDataController metaDataController, ArangoDatabases databases) {
        this.instances = instances;
        this.idUtils = idUtils;
        this.authContext = authContext;
        this.permissionsController = permissionsController;
        this.metaDataController = metaDataController;
        this.databases = databases;
    }

    private Paginated<SuggestedLink> getSuggestedLinkById(DataStage stage, InstanceId instanceId, List<UUID> excludeIds) {
        if (excludeIds == null || !excludeIds.contains(instanceId.getUuid())) {
            //It is a lookup for an instance id -> let's do a shortcut.
            NormalizedJsonLd result = instances.getInstance(stage, instanceId.getSpace(), instanceId.getUuid(), false, false, false, false, null);
            if (result != null) {
                final List<String> types = result.types();
                if (!types.isEmpty()) {
                    SuggestedLink l = new SuggestedLink();
                    UUID uuid = idUtils.getUUID(result.id());
                    l.setId(uuid);
                    l.setLabel(result.getAs(IndexedJsonLdDoc.LABEL, String.class));
                    l.setType(types.get(0));
                    l.setSpace(result.getAs(EBRAINSVocabulary.META_SPACE, String.class, null));
                    return new Paginated<>(Collections.singletonList(l), 1L, 1, 0);
                }
            }
        }
        return new Paginated<>(Collections.emptyList(), 0L, 0, 0);
    }


    @ExposesData
    //FIXME: Do we want to return suggested links for RELEASED stage?
    public Paginated<SuggestedLink> getSuggestionsByTypes(DataStage stage, PaginationParam paginationParam, List<Type> type, Map<String, List<String>> searchablePropertiesByType, String search, List<UUID> excludeIds) {
        // Suggestions are special in terms of permissions: We even allow instances to show up which are in spaces the
        // user doesn't have read access for. This is only acceptable because we're returning a restricted result with
        // minimal information.
        if (search != null) {
            InstanceId instanceId = InstanceId.deserialize(search);
            if (instanceId != null) {
                //This is a shortcut: If the search term is an instance id, we can directly
                return getSuggestedLinkById(stage, instanceId, excludeIds);
            }
        }
        Map<String, Object> bindVars = new HashMap<>();
        AQL aql = new AQL();
        // ATTENTION: We are only allowed to search by "label" fields but not by "searchable" fields if the user has no read rights
        // for those instances since otherwise, information could be extracted by doing searches. We therefore don't provide additional search fields.
        iterateThroughTypeList(type, null, bindVars, aql);
        if (searchablePropertiesByType == null) {
            searchablePropertiesByType = Collections.emptyMap();
        }

        //For suggestions, we're a little more strict. We only show additional information if the user has read rights for the space - individual instance permissions are not reflected.
        final UserWithRoles userWithRoles = authContext.getUserWithRoles();
        final Map<String, Object> whitelistFilter = permissionsController.whitelistFilterForReadInstances(metaDataController.getSpaceNames(stage, userWithRoles), userWithRoles, stage);
        if (whitelistFilter != null) {
            aql.addLine(AQL.trust("LET restrictedSpaces = @restrictedSpaces"));
            bindVars.put("restrictedSpaces", whitelistFilter.get(AQL.READ_ACCESS_BY_SPACE));
        }
        aql.addLine(AQL.trust("LET searchableProperties = @searchableProperties"));
        bindVars.put("searchableProperties", searchablePropertiesByType);
        if (type.size() == 1 && type.get(0).getSpaces().size() == 1) {
            // If there is only one type and one space for this type, we have the chance to optimize the query... Please
            // note that we're not restricting the spaces to the ones the user can read because the suggestions are
            // working with minimal data and are not affected by the read rights.
            aql.indent().addLine(AQL.trust(String.format("FOR v IN @@singleSpace OPTIONS {indexHint: \"%s\"}", ArangoDatabaseProxy.BROWSE_AND_SEARCH_INDEX)));
            aql.addLine(AQL.trust(String.format("FILTER @typeFilter IN v.`%s` AND v.`%s` == null", JsonLdConsts.TYPE, IndexedJsonLdDoc.EMBEDDED)));
            bindVars.put("typeFilter", type.get(0).getName());
            bindVars.put("@singleSpace", ArangoCollectionReference.fromSpace(type.get(0).getSpacesForInternalUse(userWithRoles.getPrivateSpace()).iterator().next()).getCollectionName());
        } else {
            aql.indent().addLine(AQL.trust("FOR v IN 1..1 OUTBOUND typeDefinition.type @@typeRelationCollection"));
            bindVars.put("@typeRelationCollection", InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName());
        }
        if (!excludeIds.isEmpty()) {
            aql.addLine(AQL.trust("FILTER v." + ArangoVocabulary.KEY + " NOT IN @excludeIds"));
            bindVars.put("excludeIds", excludeIds);
        }
        addSearchFilter(bindVars, aql, search, false);
        aql.addLine(AQL.trust(String.format("SORT v.%s", IndexedJsonLdDoc.LABEL)));
        aql.addPagination(paginationParam);

        aql.addLine(AQL.trust("LET additionalInfo = "));
        if (whitelistFilter != null) {
            aql.addLine(AQL.trust("v.`" + EBRAINSVocabulary.META_SPACE + "` NOT IN restrictedSpaces ? null : "));
        }
        aql.addLine(AQL.trust("CONCAT_SEPARATOR(\", \", (FOR s IN NOT_NULL(searchableProperties[typeDefinition.typeName], []) RETURN v[s]))"));
        aql.addLine(AQL.trust("LET attWithMeta = [{name: \"" + JsonLdConsts.ID + "\", value: v.`" + JsonLdConsts.ID + "`}, {name: \"" + EBRAINSVocabulary.LABEL + "\", value: v." + IndexedJsonLdDoc.LABEL + "},  {name: \"" + EBRAINSVocabulary.ADDITIONAL_INFO + "\", value: additionalInfo}, {name: \"" + EBRAINSVocabulary.META_TYPE + "\", value: typeDefinition.typeName}, {name: \"" + EBRAINSVocabulary.META_SPACE + "\", value: v.`" + EBRAINSVocabulary.META_SPACE + "`}]"));
        aql.addLine(AQL.trust("RETURN ZIP(attWithMeta[*].name, attWithMeta[*].value)"));
        Paginated<NormalizedJsonLd> normalizedJsonLdPaginated = ArangoQueries.queryDocuments(databases.getByStage(stage), new AQLQuery(aql, bindVars), null);
        List<SuggestedLink> links = normalizedJsonLdPaginated.getData().stream().map(payload -> {
            SuggestedLink link = new SuggestedLink();
            UUID uuid = idUtils.getUUID(payload.id());
            link.setId(uuid);
            link.setLabel(payload.getAs(EBRAINSVocabulary.LABEL, String.class, uuid != null ? uuid.toString() : null));
            link.setType(payload.getAs(EBRAINSVocabulary.META_TYPE, String.class, null));
            link.setSpace(payload.getAs(EBRAINSVocabulary.META_SPACE, String.class, null));
            link.setAdditionalInformation(payload.getAs(EBRAINSVocabulary.ADDITIONAL_INFO, String.class, null));
            return link;
        }).collect(Collectors.toList());
        return new Paginated<>(links, normalizedJsonLdPaginated.getTotalResults(), normalizedJsonLdPaginated.getSize(), normalizedJsonLdPaginated.getFrom());
    }
}

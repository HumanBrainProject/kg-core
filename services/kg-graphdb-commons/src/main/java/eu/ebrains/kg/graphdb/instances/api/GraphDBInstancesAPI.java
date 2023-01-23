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

package eu.ebrains.kg.graphdb.instances.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.GraphDBInstances;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesData;
import eu.ebrains.kg.commons.markers.ExposesMinimalData;
import eu.ebrains.kg.commons.markers.ExposesQuery;
import eu.ebrains.kg.commons.markers.ExposesReleaseStatus;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.model.external.types.Property;
import eu.ebrains.kg.commons.model.external.types.TargetType;
import eu.ebrains.kg.commons.model.external.types.TypeInformation;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.instances.controller.*;
import eu.ebrains.kg.graphdb.instances.model.ArangoRelation;
import eu.ebrains.kg.graphdb.structure.api.GraphDBTypesAPI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GraphDBInstancesAPI implements GraphDBInstances.Client {

    private final InstancesRepository instances;
    private final IncomingLinksRepository incomingLinks;
    private final QueriesRepository queries;
    private final DocumentsRepository documents;
    private final NeighborsRepository neighbors;
    private final SuggestionsRepository suggestions;
    private final ReleaseStatusRepository releaseStatus;
    private final AuthContext authContext;
    private final IdUtils idUtils;
    private final GraphDBTypesAPI types;

    public GraphDBInstancesAPI(InstancesRepository instances, IncomingLinksRepository incomingLinks, QueriesRepository queries, DocumentsRepository documents, NeighborsRepository neighbors, SuggestionsRepository suggestions, ReleaseStatusRepository releaseStatus, AuthContext authContext, IdUtils idUtils, GraphDBTypesAPI types) {
        this.instances = instances;
        this.incomingLinks = incomingLinks;
        this.queries = queries;
        this.documents = documents;
        this.neighbors = neighbors;
        this.suggestions = suggestions;
        this.releaseStatus = releaseStatus;
        this.authContext = authContext;
        this.idUtils = idUtils;
        this.types = types;
    }

    @Override
    public Paginated<NormalizedJsonLd> getIncomingLinks(String space, UUID id, DataStage stage, String property, String type, PaginationParam paginationParam) {
        return incomingLinks.getIncomingLinks(stage, new SpaceName(space), id, property, type, paginationParam, documents.getInvitationDocuments());
    }

    @Override
    public NormalizedJsonLd getInstanceById(String space, UUID id, DataStage stage, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize, boolean removeInternalProperties) {
        return instances.getInstance(stage, new SpaceName(space), id, returnEmbedded, removeInternalProperties, returnAlternatives, returnIncomingLinks, incomingLinksPageSize);
    }

    @Override
    public NormalizedJsonLd getInstanceByIdWithoutPayload(DataStage stage, String space, UUID id, boolean removeInternalProperties, boolean returnIncomingLinks, Long incomingLinksPageSize, boolean returnPermissions) {
        return instances.getInstanceWithoutPayload(stage, new SpaceName(space), id, removeInternalProperties, returnIncomingLinks, incomingLinksPageSize, returnPermissions);
    }

    @Override
    @ExposesQuery
    public NormalizedJsonLd getQueryById(String space, UUID id) {
        return queries.getQuery(new SpaceName(space), id);
    }

    @Override
    @ExposesData
    public Paginated<NormalizedJsonLd> getInstancesByType(DataStage stage, String typeName, String space, String searchByLabel, String filterProperty, String filterValue, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam) {
        Type type = new Type(typeName);
        List<String> searchableProperties = null;
        if ((searchByLabel != null && !searchByLabel.isBlank())) {
            //Since we're either sorting or searching by label, we need to reflect on the type -> we therefore have to resolve the type in the database first...
            final Result<TypeInformation> typeInformation = types.getTypesByName(Collections.singletonList(typeName), stage, space, false, false).get(typeName);
            if(typeInformation!=null && typeInformation.getData()!=null){
                type = Type.fromPayload(typeInformation.getData());
                if(space!=null) {
                    type.getSpaces().add(SpaceName.fromString(space));
                }
                //We're also interested in the properties which are marked as "searchable"
                searchableProperties = typeInformation.getData().getProperties().stream()
                        .filter(p -> {
                            Boolean searchable = p.getAs(EBRAINSVocabulary.META_PROPERTY_SEARCHABLE, Boolean.class);
                            return searchable != null && searchable;
                        }).map(Property::getIdentifier).filter(Objects::nonNull).collect(Collectors.toList());
            }
        } else {
            final Result<TypeInformation> typeInformation = types.getTypesByName(Collections.singletonList(typeName), stage, space, false, false).get(typeName);
            if(typeInformation!=null && typeInformation.getData()!=null) {
                type = Type.fromPayload(typeInformation.getData());
                if(space!=null) {
                    type.getSpaces().add(SpaceName.fromString(space));
                }
            }
        }
        return documents.getDocumentsByTypes(stage, type, SpaceName.PRIVATE_SPACE.equals(space) ? authContext.getUserWithRolesWithoutTermsCheck().getPrivateSpace() : SpaceName.fromString(space), filterProperty, filterValue, paginationParam, searchByLabel, returnEmbedded, returnAlternatives, searchableProperties);
    }

    @Override
    @ExposesQuery
    public Paginated<NormalizedJsonLd> getQueriesByType(DataStage stage, String searchByLabel, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam, String rootType) {
        return queries.getQueriesByRootType(stage, paginationParam, searchByLabel, returnEmbedded, returnAlternatives, rootType == null ? null : URLDecoder.decode(rootType, StandardCharsets.UTF_8));
    }

    @Override
    @ExposesData
    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(List<String> ids, DataStage stage, String typeRestriction, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize) {
        List<InstanceId> instanceIds = ids.stream().map(InstanceId::deserialize).filter(Objects::nonNull).collect(Collectors.toList());
        return documents.getDocumentsByIdList(stage, instanceIds, typeRestriction, returnEmbedded, returnAlternatives, returnIncomingLinks, incomingLinksPageSize);
    }

    @Override
    @ExposesMinimalData
    public Map<UUID, String> getLabels(List<String> ids, DataStage stage) {
        Set<InstanceId> instanceIds = ids.stream().map(InstanceId::deserialize).filter(Objects::nonNull).collect(Collectors.toSet());
        return instances.getLabelsForInstances(stage, instanceIds);
    }

    @Override
    @ExposesData
    public List<NormalizedJsonLd> getInstancesByIdentifier(String identifier, String space, DataStage stage) {
        return documents.getDocumentsByIdentifiers(Collections.singleton(identifier), stage, new SpaceName(space), false, false);
    }

    @Override
    @ExposesData
    public List<NormalizedJsonLd> getDocumentWithRelatedInstancesByIdentifiers(String space, UUID id, DataStage stage, boolean returnEmbedded, boolean returnAlternatives) {
        return documents.getDocumentsBySharedIdentifiers(stage, new SpaceName(space), id, returnEmbedded, returnAlternatives);
    }

    @Override
    @ExposesData
    public List<NormalizedJsonLd> getDocumentWithIncomingRelatedInstances(String space, UUID id, DataStage stage, String relation, boolean useOriginalTo, boolean returnEmbedded, boolean returnAlternatives) {
        return documents.getDocumentsByIncomingRelation(stage, new SpaceName(space), id, new ArangoRelation(URLDecoder.decode(relation, StandardCharsets.UTF_8)), useOriginalTo, returnEmbedded, returnAlternatives);
    }

    @Override
    @ExposesMinimalData
    public GraphEntity getNeighbors(String space, UUID id, DataStage stage) {
        return neighbors.getNeighbors(stage, new SpaceName(space), id);
    }

    @Override
    @ExposesReleaseStatus
    public ReleaseStatus getReleaseStatus(String space, UUID id, ReleaseTreeScope treeScope) {
        return releaseStatus.getReleaseStatus(new SpaceName(space), id, treeScope);
    }

    @Override
    @ExposesReleaseStatus
    public Map<UUID, ReleaseStatus> getIndividualReleaseStatus(List<InstanceId> instanceIds, ReleaseTreeScope releaseTreeScope) {
        return releaseStatus.getIndividualReleaseStatus(instanceIds, releaseTreeScope);
    }


    @Override
    @ExposesMinimalData
    public SuggestionResult getSuggestedLinksForProperty(NormalizedJsonLd payload, DataStage stage, String space, UUID id, String propertyName, String sourceType, String targetType, String search, PaginationParam paginationParam) {
        List<Type> sourceTypes = null;
        List<UUID> existingLinks = Collections.emptyList();

        if (StringUtils.isNotBlank(sourceType)) {
            sourceTypes = Collections.singletonList(new Type(URLDecoder.decode(sourceType, StandardCharsets.UTF_8)));
        } else {
            if (payload == null) {
                payload = instances.getInstance(stage, new SpaceName(space), id, true, true, false, false, null);
            }
            if (payload != null) {
                sourceTypes = payload.types().stream().map(Type::new).collect(Collectors.toList());
                existingLinks = payload.getAsListOf(propertyName, JsonLdId.class, true).stream().map(idUtils::getUUID).filter(Objects::nonNull).collect(Collectors.toList());
            }
        }
        SuggestionResult suggestionResult = new SuggestionResult();
        if (sourceTypes == null || sourceTypes.isEmpty()) {
            //We don't have any clue about the type so we can't give any suggestions
            return null;
        }

        final Map<String, Result<TypeInformation>> sourceTypeInformation = types.getTypesByName(sourceTypes.stream().map(Type::getName).collect(Collectors.toList()), stage, space, true, false);
        final List<String> targetTypesOfProperty = sourceTypeInformation.values().stream().map(s -> s.getData().getProperties()).flatMap(Collection::stream).filter(p -> propertyName.equals(p.getIdentifier()))
                .map(Property::getTargetTypes).flatMap(Collection::stream).map(TargetType::getType).filter(t -> targetType == null || t.equals(targetType)).distinct().collect(Collectors.toList());
        final Map<String, Result<TypeInformation>> targetTypeInformation = types.getTypesByName(targetTypesOfProperty, stage, null, true, false);
        suggestionResult.setTypes(targetTypeInformation.values().stream().collect(Collectors.toMap(k -> k.getData().getIdentifier(), Result::getData)));
        final Map<String, List<String>> searchablePropertiesByType = new HashMap<>();
        targetTypeInformation.keySet().forEach(t -> {
            final Result<TypeInformation> typeInformation = targetTypeInformation.get(t);
            if(typeInformation!=null && typeInformation.getData()!=null){
                final List<String> searchableProperties = typeInformation.getData().getProperties().stream().filter(p -> {
                    final Boolean searchable = p.getAs(EBRAINSVocabulary.META_PROPERTY_SEARCHABLE, Boolean.class);
                    return searchable != null && searchable;
                }).map(Property::getIdentifier).distinct().collect(Collectors.toList());
                searchablePropertiesByType.put(t, searchableProperties);
            }
        });
        final List<Type> targetTypes = suggestionResult.getTypes().values().stream().map(Type::fromPayload).collect(Collectors.toList());
        Paginated<SuggestedLink> documentsByTypes = suggestions.getSuggestionsByTypes(stage, paginationParam, targetTypes, searchablePropertiesByType, search, existingLinks);

        //We check for those instances that do not have additional information, if we can get something from the released endpoint
        final List<InstanceId> instancesWithoutAdditionalInfo = documentsByTypes.getData().stream().filter(d -> d.getAdditionalInformation() == null).map(d -> {
            if (!CollectionUtils.isEmpty(searchablePropertiesByType.get(d.getType()))) {
                return new InstanceId(d.getId(), SpaceName.fromString(d.getSpace()));
            }
            return null;
        }).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        final Map<UUID, Result<NormalizedJsonLd>> documentsByIdList = documents.getDocumentsByIdList(DataStage.RELEASED, instancesWithoutAdditionalInfo, null, false, false, false, null);
        documentsByTypes.getData().stream().filter(d -> d.getAdditionalInformation() == null).forEach(d -> {
            final List<String> searchableProperties = searchablePropertiesByType.get(d.getType());
            if (!CollectionUtils.isEmpty(searchableProperties)) {
                final Result<NormalizedJsonLd> document = documentsByIdList.get(d.getId());
                if(document!=null && document.getData()!=null){
                    final String additionalInformation = searchableProperties.stream().map(p -> document.getData().get(p)).filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(", "));
                    if(StringUtils.isNotBlank(additionalInformation)){
                        d.setAdditionalInformation(additionalInformation.trim());
                    }
                }
            }
        });

        suggestionResult.setSuggestions(documentsByTypes);
        suggestionResult.getTypes().values().forEach( t -> {
            t.clearProperties();
            if(t.getSpaces()!=null) {
                t.getSpaces().forEach(s -> s.setProperties(null));
            }
        });
        return suggestionResult;
    }


}

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
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
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
import eu.ebrains.kg.graphdb.instances.controller.ArangoRepositoryInstances;
import eu.ebrains.kg.graphdb.instances.model.ArangoRelation;
import eu.ebrains.kg.graphdb.structure.api.GraphDBTypesAPI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GraphDBInstancesAPI implements GraphDBInstances.Client {

    private final ArangoRepositoryInstances repository;
    private final AuthContext authContext;
    private final IdUtils idUtils;
    private final GraphDBTypesAPI types;

    public GraphDBInstancesAPI(ArangoRepositoryInstances repository, AuthContext authContext, IdUtils idUtils, GraphDBTypesAPI types) {
        this.repository = repository;
        this.authContext = authContext;
        this.idUtils = idUtils;
        this.types = types;
    }

    @Override
    public Paginated<NormalizedJsonLd> getIncomingLinks(String space, UUID id, DataStage stage, String property, String type, PaginationParam paginationParam) {
        return repository.getIncomingLinks(stage, new SpaceName(space), id, property, type, paginationParam);
    }

    @Override
    public NormalizedJsonLd getInstanceById(String space, UUID id, DataStage stage, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize, boolean removeInternalProperties) {
        return repository.getInstance(stage, new SpaceName(space), id, returnEmbedded, removeInternalProperties, returnAlternatives, returnIncomingLinks, incomingLinksPageSize);
    }

    @Override
    @ExposesQuery
    public NormalizedJsonLd getQueryById(String space, UUID id, DataStage stage) {
        return repository.getQuery(stage, new SpaceName(space), id);
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
                Type typeFromPayload = Type.fromPayload(typeInformation.getData());
                type = typeFromPayload;
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
            }
        }
        return repository.getDocumentsByTypes(stage, type, space != null && !space.isBlank() ? new SpaceName(space) : null, filterProperty, filterValue, paginationParam, searchByLabel, returnEmbedded, returnAlternatives, searchableProperties);
    }

    @Override
    @ExposesQuery
    public Paginated<NormalizedJsonLd> getQueriesByType(DataStage stage, String searchByLabel, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam, String rootType) {
        return repository.getQueriesByRootType(stage, paginationParam, searchByLabel, returnEmbedded, returnAlternatives, rootType == null ? null : URLDecoder.decode(rootType, StandardCharsets.UTF_8));
    }

    @Override
    @ExposesData
    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(List<String> ids, DataStage stage, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize) {
        List<InstanceId> instanceIds = ids.stream().map(InstanceId::deserialize).filter(Objects::nonNull).collect(Collectors.toList());
        return repository.getDocumentsByIdList(stage, instanceIds, returnEmbedded, returnAlternatives, returnIncomingLinks, incomingLinksPageSize);
    }

    @Override
    @ExposesMinimalData
    public Map<UUID, String> getLabels(List<String> ids, DataStage stage) {
        Set<InstanceId> instanceIds = ids.stream().map(InstanceId::deserialize).filter(Objects::nonNull).collect(Collectors.toSet());
        return repository.getLabelsForInstances(stage, instanceIds);
    }

    @Override
    @ExposesData
    public List<NormalizedJsonLd> getInstancesByIdentifier(String identifier, String space, DataStage stage) {
        return repository.getDocumentsByIdentifiers(Collections.singleton(identifier), stage, new SpaceName(space), false, false);
    }

    @Override
    @ExposesData
    public List<NormalizedJsonLd> getDocumentWithRelatedInstancesByIdentifiers(String space, UUID id, DataStage stage, boolean returnEmbedded, boolean returnAlternatives) {
        return repository.getDocumentsBySharedIdentifiers(stage, new SpaceName(space), id, returnEmbedded, returnAlternatives);
    }

    @Override
    @ExposesData
    public List<NormalizedJsonLd> getDocumentWithIncomingRelatedInstances(String space, UUID id, DataStage stage, String relation, boolean useOriginalTo, boolean returnEmbedded, boolean returnAlternatives) {
        return repository.getDocumentsByIncomingRelation(stage, new SpaceName(space), id, new ArangoRelation(URLDecoder.decode(relation, StandardCharsets.UTF_8)), useOriginalTo, returnEmbedded, returnAlternatives);
    }

    @Override
    @ExposesMinimalData
    public GraphEntity getNeighbors(String space, UUID id, DataStage stage) {
        return repository.getNeighbors(stage, new SpaceName(space), id);
    }

    @Override
    @ExposesReleaseStatus
    public ReleaseStatus getReleaseStatus(String space, UUID id, ReleaseTreeScope treeScope) {
        return repository.getReleaseStatus(new SpaceName(space), id, treeScope);
    }

    @Override
    @ExposesMinimalData
    public SuggestionResult getSuggestedLinksForProperty(NormalizedJsonLd payload, DataStage stage, String space, UUID id, String propertyName, String sourceType, String targetType, String search, PaginationParam paginationParam) {
        List<Type> sourceTypes = null;
        List<UUID> existingLinks = Collections.emptyList();
        ;
        if (StringUtils.isNotBlank(sourceType)) {
            sourceTypes = Collections.singletonList(new Type(URLDecoder.decode(sourceType, StandardCharsets.UTF_8)));
        } else {
            if (payload == null) {
                payload = repository.getInstance(stage, new SpaceName(space), id, true, true, false, false, null);
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
                .map(Property::getTargetTypes).flatMap(Collection::stream).map(TargetType::getType).distinct().collect(Collectors.toList());
        final Map<String, Result<TypeInformation>> targetTypeInformation = types.getTypesByName(targetTypesOfProperty, stage, null, false, false);
        suggestionResult.setTypes(targetTypeInformation.values().stream().collect(Collectors.toMap(k -> k.getData().getIdentifier(), Result::getData)));
        final List<Type> targetTypes = suggestionResult.getTypes().values().stream().map(Type::fromPayload).filter(t -> targetType == null || t.getName().equals(targetType)).collect(Collectors.toList());
        Paginated<SuggestedLink> documentsByTypes = repository.getSuggestionsByTypes(stage, paginationParam, targetTypes, search, existingLinks);
        suggestionResult.setSuggestions(documentsByTypes);
        return suggestionResult;
    }


}

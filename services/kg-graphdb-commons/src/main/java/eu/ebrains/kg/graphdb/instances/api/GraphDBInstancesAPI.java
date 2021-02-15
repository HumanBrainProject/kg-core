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
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.instances.controller.ArangoRepositoryInstances;
import eu.ebrains.kg.graphdb.instances.model.ArangoRelation;
import eu.ebrains.kg.graphdb.types.controller.ArangoRepositoryTypes;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GraphDBInstancesAPI implements GraphDBInstances.Client {

    private final ArangoRepositoryInstances repository;
    private final ArangoRepositoryTypes typeRepository;
    private final AuthContext authContext;
    private final IdUtils idUtils;

    public GraphDBInstancesAPI(AuthContext authContext, ArangoRepositoryInstances repository, ArangoRepositoryTypes typeRepository, IdUtils idUtils) {
        this.repository = repository;
        this.typeRepository = typeRepository;
        this.authContext = authContext;
        this.idUtils = idUtils;
    }

    @Override
    public NormalizedJsonLd getInstanceById(String space, UUID id, DataStage stage, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, boolean removeInternalProperties) {
        return repository.getInstance(stage, new SpaceName(space), id, returnEmbedded, removeInternalProperties, returnAlternatives, returnIncomingLinks);
    }

    @Override
    @ExposesData
    public Paginated<NormalizedJsonLd> getInstancesByType(DataStage stage, String type, String space, String searchByLabel, boolean returnAlternatives, boolean returnEmbedded, boolean sortByLabel, PaginationParam paginationParam) {
        List<Type> types = Collections.singletonList(new Type(type));
        Map<String, List<String>> searchableProperties = null;
        if (sortByLabel || (searchByLabel != null && !searchByLabel.isBlank())) {
            //Since we're either sorting or searching by label, we need to reflect on the type -> we therefore have to resolve the type in the database first...
            List<NormalizedJsonLd> typeInformation = typeRepository.getTypes(authContext.getUserWithRoles().getClientId(), stage, types, true, false, false);
            types = typeInformation.stream().map(Type::fromPayload).collect(Collectors.toList());

            //We're also interested in the properties which are marked as "searchable"
            searchableProperties = typeInformation.stream().collect(Collectors.toMap(JsonLdDoc::primaryIdentifier, v -> {
                List<NormalizedJsonLd> properties = v.getAsListOf(EBRAINSVocabulary.META_PROPERTIES, NormalizedJsonLd.class);
                String labelProperty = Type.fromPayload(v).getLabelProperty();
                return properties.stream().filter(p -> {
                    Boolean searchable = p.getAs(EBRAINSVocabulary.META_PROPERTY_SEARCHABLE, Boolean.class);
                    return searchable != null && searchable;
                }).map(JsonLdDoc::primaryIdentifier).filter(p -> !p.equals(labelProperty)).collect(Collectors.toList());
            }));
        }
        return repository.getDocumentsByTypes(stage, types, space != null && !space.isBlank() ? new SpaceName(space) : null, paginationParam, searchByLabel, returnEmbedded, returnAlternatives, sortByLabel, searchableProperties);
    }

    @Override
    @ExposesQuery
    public Paginated<NormalizedJsonLd> getQueriesByType(DataStage stage, String searchByLabel, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam, String rootType) {
        return repository.getQueriesByRootType(stage, paginationParam, searchByLabel, returnEmbedded, returnAlternatives, rootType == null ? null : URLDecoder.decode(rootType, StandardCharsets.UTF_8));
    }

    @Override
    @ExposesData
    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(List<String> ids, DataStage stage, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks) {
        List<InstanceId> instanceIds = ids.stream().map(InstanceId::deserialize).filter(Objects::nonNull).collect(Collectors.toList());
        return repository.getDocumentsByIdList(stage, instanceIds, returnEmbedded, returnAlternatives, returnIncomingLinks);
    }

    @Override
    @ExposesMinimalData
    public Map<UUID, String> getLabels(List<String> ids, DataStage stage) {
        Set<InstanceId> instanceIds = ids.stream().map(InstanceId::deserialize).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Type> types = getInstancesByIds(ids, stage, false, false, false).values().stream().map(Result::getData).map(JsonLdDoc::types).flatMap(Collection::stream).distinct().map(Type::new).collect(Collectors.toSet());
        List<Type> extendedTypes = typeRepository.getTypeInformation(authContext.getUserWithRoles().getClientId(), stage, types);
        return repository.getLabelsForInstances(stage, instanceIds, extendedTypes);
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
//
//    @Override
//    @ExposesData
//    public List<NormalizedJsonLd> getDocumentWithOutgoingRelatedInstances(String space, UUID id, DataStage stage, String relation, boolean returnEmbedded, boolean returnAlternatives) {
//        return repository.getDocumentsByOutgoingRelation(stage, new SpaceName(space), id, new ArangoRelation(URLDecoder.decode(relation, StandardCharsets.UTF_8)), returnEmbedded, returnAlternatives);
//    }

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
    public SuggestionResult getSuggestedLinksForProperty(NormalizedJsonLd payload, DataStage stage, String space, UUID id, String propertyName, String type, String search, PaginationParam paginationParam) {

        List<Type> types;
        List<UUID> existingLinks;
        if (StringUtils.isNotBlank(type)) {
            types = Collections.singletonList(new Type(URLDecoder.decode(type, StandardCharsets.UTF_8)));
            existingLinks = Collections.emptyList();
        } else {
            if (payload == null) {
                payload = repository.getInstance(stage, new SpaceName(space), id, true, true, false, false);
            }
            types = payload.types().stream().map(Type::new).collect(Collectors.toList());
            existingLinks = payload.getAsListOf(propertyName, JsonLdId.class, true).stream().map(idUtils::getUUID).filter(Objects::nonNull).collect(Collectors.toList());
        }
        SuggestionResult suggestionResult = new SuggestionResult();
        List<NormalizedJsonLd> targetTypesForProperty = typeRepository.getTargetTypesForProperty(authContext.getUserWithRoles().getClientId(), stage, types, propertyName);
        List<Type> typesWithLabelInfo = ArangoRepositoryTypes.extractExtendedTypeInformationFromPayload(targetTypesForProperty);
        suggestionResult.setTypes(targetTypesForProperty.stream().collect(Collectors.toMap(JsonLdDoc::primaryIdentifier, t -> t)));
        Paginated<SuggestedLink> documentsByTypes = repository.getSuggestionsByTypes(stage, typesWithLabelInfo, paginationParam, search, existingLinks);
        suggestionResult.setSuggestions(documentsByTypes);
        return suggestionResult;
    }


}

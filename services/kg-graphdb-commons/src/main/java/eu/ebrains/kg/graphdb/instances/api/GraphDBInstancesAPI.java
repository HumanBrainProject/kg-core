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
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/graphdb/{stage}")
public class GraphDBInstancesAPI {

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


    @GetMapping("instances/{space}/{id}")
    @ExposesData
    public NormalizedJsonLd getInstanceById(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam(value = "returnEmbedded", defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnIncomingLinks", required = false, defaultValue = "false") boolean returnIncomingLinks, @RequestParam(value = "removeInternalProperties", required = false, defaultValue = "true") boolean removeInternalProperties) {
        return repository.getInstance(stage, new SpaceName(space), id, returnEmbedded, removeInternalProperties, returnAlternatives, returnIncomingLinks);
    }

    @GetMapping("instancesByType")
    @ExposesData
    public Paginated<NormalizedJsonLd> getInstancesByType(@PathVariable("stage") DataStage stage, @RequestParam("type") String type, @RequestParam(value = "space", required = false) String space, @RequestParam(value = "searchByLabel", required = false) String searchByLabel, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "sortByLabel", required = false, defaultValue = "false") boolean sortByLabel, PaginationParam paginationParam) {
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

    @GetMapping("queriesByType")
    @ExposesQuery
    public Paginated<NormalizedJsonLd> getQueriesByType(@PathVariable("stage") DataStage stage, @RequestParam(value = "searchByLabel", required = false) String searchByLabel, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, PaginationParam paginationParam, @RequestParam("rootType") String rootType) {
        return repository.getQueriesByRootType(stage, paginationParam, searchByLabel, returnEmbedded, returnAlternatives, rootType == null ? null : URLDecoder.decode(rootType, StandardCharsets.UTF_8));
    }

    @PostMapping("instancesByIds")
    @ExposesData
    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(@RequestBody List<String> ids, @PathVariable("stage") DataStage stage, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnIncomingLinks", required = false, defaultValue = "false") boolean returnIncomingLinks) {
        List<InstanceId> instanceIds = ids.stream().map(InstanceId::deserialize).filter(Objects::nonNull).collect(Collectors.toList());
        return repository.getDocumentsByIdList(stage, instanceIds, returnEmbedded, returnAlternatives, returnIncomingLinks);
    }

    @PostMapping("instancesByIds/labels")
    @ExposesMinimalData
    public Map<UUID, String> getLabels(@RequestBody List<String> ids, @PathVariable("stage") DataStage stage) {
        Set<InstanceId> instanceIds = ids.stream().map(InstanceId::deserialize).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Type> types = getInstancesByIds(ids, stage, false, false, false).values().stream().map(Result::getData).map(JsonLdDoc::types).flatMap(Collection::stream).distinct().map(Type::new).collect(Collectors.toSet());
        List<Type> extendedTypes = typeRepository.getTypeInformation(authContext.getUserWithRoles().getClientId(), stage, types);
        return repository.getLabelsForInstances(stage, instanceIds, extendedTypes);
    }

    @GetMapping("instancesByIdentifier/{space}")
    @ExposesData
    public List<NormalizedJsonLd> getInstancesByIdentifier(@RequestParam("identifier") String identifier, @PathVariable("space") String space, @PathVariable("stage") DataStage stage) {
        return repository.getDocumentsByIdentifiers(Collections.singleton(identifier), stage, new SpaceName(space), false, false);
    }

    @GetMapping("instances/{space}/{id}/relatedByIdentifier")
    @ExposesData
    public List<NormalizedJsonLd> getDocumentWithRelatedInstancesByIdentifiers(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives) {
        return repository.getDocumentsBySharedIdentifiers(stage, new SpaceName(space), id, returnEmbedded, returnAlternatives);
    }

    @GetMapping("instances/{space}/{id}/relatedByIncomingRelation")
    @ExposesData
    public List<NormalizedJsonLd> getDocumentWithIncomingRelatedInstances(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam("relation") String relation, @RequestParam(value = "useOriginalTo", defaultValue = "false") boolean useOriginalTo, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives) {
        return repository.getDocumentsByIncomingRelation(stage, new SpaceName(space), id, new ArangoRelation(URLDecoder.decode(relation, StandardCharsets.UTF_8)), useOriginalTo, returnEmbedded, returnAlternatives);
    }

    @GetMapping("instances/{space}/{id}/relatedByOutgoingRelation")
    @ExposesData
    public List<NormalizedJsonLd> getDocumentWithOutgoingRelatedInstances(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam("relation") String relation, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives) {
        return repository.getDocumentsByOutgoingRelation(stage, new SpaceName(space), id, new ArangoRelation(URLDecoder.decode(relation, StandardCharsets.UTF_8)), returnEmbedded, returnAlternatives);
    }

    @GetMapping("instances/{space}/{id}/neighbors")
    @ExposesMinimalData
    public GraphEntity getNeighbors(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage) {
        return repository.getNeighbors(stage, new SpaceName(space), id);
    }

    @GetMapping("instances/{space}/{id}/releaseStatus")
    @ExposesReleaseStatus
    public ReleaseStatus getReleaseStatus(@PathVariable("space") String space, @PathVariable("id") UUID id, @RequestParam("releaseTreeScope") ReleaseTreeScope treeScope) {
        return repository.getReleaseStatus(new SpaceName(space), id, treeScope);
    }


    @PostMapping("instances/{space}/{id}/suggestedLinksForProperty")
    @ExposesMinimalData
    public SuggestionResult getSuggestedLinksForProperty(@RequestBody(required = false) NormalizedJsonLd payload, @PathVariable("stage") DataStage stage, @PathVariable("space") String space, @PathVariable("id") UUID id, @RequestParam(value = "property") String propertyName, @Parameter(description = "Allows to define the source type of the given field and therefore to skip reflection on the source payload (e.g. useful for embedded instances)") @RequestParam(value = "type", required = false) String type, @RequestParam(value = "search", required = false) String search, PaginationParam paginationParam) {

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

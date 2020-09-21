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

package eu.ebrains.kg.graphdb.instances.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.graphdb.instances.controller.ArangoRepositoryInstances;
import eu.ebrains.kg.graphdb.instances.model.ArangoRelation;
import eu.ebrains.kg.graphdb.types.controller.ArangoRepositoryTypes;
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
    public NormalizedJsonLd getInstanceById(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam(value = "returnEmbedded", defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "removeInternalProperties", required = false, defaultValue = "true") boolean removeInternalProperties) {
        return repository.getInstance(stage, new Space(space), id, returnEmbedded, removeInternalProperties, returnAlternatives);
    }

    @GetMapping("instancesByType")
    public Paginated<NormalizedJsonLd> getInstancesByType(@PathVariable("stage") DataStage stage, @RequestParam("type") String type, @RequestParam(value = "searchByLabel", required = false) String searchByLabel, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, PaginationParam paginationParam) {
        List<Type> types = Collections.singletonList(new Type(type));
        if(searchByLabel!=null && !searchByLabel.isBlank()){
            //Since we're searching by label, we need to reflect on the type -> we therefore have to resolve the type in the database first...
            types = typeRepository.getTypeInformation(authContext.getUserWithRoles().getClientId(), stage, types);
        }
        return repository.getDocumentsByTypes(stage, types, paginationParam, searchByLabel, returnEmbedded, returnAlternatives);
    }

    @GetMapping("queriesByType")
    public Paginated<NormalizedJsonLd> getQueriesByType(@PathVariable("stage") DataStage stage, @RequestParam(value = "searchByLabel", required = false) String searchByLabel, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, PaginationParam paginationParam, @RequestParam("rootType") String rootType) {
        return repository.getQueriesByRootType(stage, paginationParam, searchByLabel, returnEmbedded, returnAlternatives, rootType == null ? null : URLDecoder.decode(rootType, StandardCharsets.UTF_8));
    }

    @PostMapping("instancesByIds")
    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(@RequestBody List<String> ids, @PathVariable("stage") DataStage stage, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives) {
        List<InstanceId> instanceIds = ids.stream().map(InstanceId::deserialize).filter(Objects::nonNull).collect(Collectors.toList());
        return repository.getDocumentsByIdList(stage, instanceIds, returnEmbedded, returnAlternatives);
  }

    @PostMapping("instancesByIds/labels")
    public Map<UUID, String> getLabels(@RequestBody List<String> ids, @PathVariable("stage") DataStage stage) {
        Set<InstanceId> instanceIds = ids.stream().map(InstanceId::deserialize).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Type> types = getInstancesByIds(ids, stage, false, false).values().stream().map(Result::getData).map(JsonLdDoc::types).flatMap(Collection::stream).distinct().map(Type::new).collect(Collectors.toSet());
        List<Type> extendedTypes = typeRepository.getTypeInformation(authContext.getUserWithRoles().getClientId(), stage, types);
        return repository.getLabelsForInstances(stage, instanceIds, extendedTypes);
    }

  @GetMapping("instancesByIdentifier/{space}")
  public List<NormalizedJsonLd> getInstancesByIdentifier(@RequestParam("identifier") String identifier, @PathVariable("space") String space, @PathVariable("stage") DataStage stage) {
        return repository.getDocumentsByIdentifiers(Collections.singleton(identifier), stage, new Space(space), false, false);
  }

    @GetMapping("instances/{space}/{id}/relatedByIdentifier")
    public List<NormalizedJsonLd> getDocumentWithRelatedInstancesByIdentifiers(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives) {
        return repository.getDocumentsBySharedIdentifiers(stage, new Space(space), id, returnEmbedded, returnAlternatives);
    }

    @GetMapping("instances/{space}/{id}/relatedByIncomingRelation")
    public List<NormalizedJsonLd> getDocumentWithIncomingRelatedInstances(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam("relation") String relation, @RequestParam(value = "useOriginalTo", defaultValue = "false") boolean useOriginalTo, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives) {
        return repository.getDocumentsByIncomingRelation(stage, new Space(space), id, new ArangoRelation(URLDecoder.decode(relation, StandardCharsets.UTF_8)), useOriginalTo, returnEmbedded, returnAlternatives);
    }

    @GetMapping("instances/{space}/{id}/relatedByOutgoingRelation")
    public List<NormalizedJsonLd> getDocumentWithOutgoingRelatedInstances(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam("relation") String relation, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives) {
        return repository.getDocumentsByOutgoingRelation(stage, new Space(space), id, new ArangoRelation(URLDecoder.decode(relation, StandardCharsets.UTF_8)), returnEmbedded, returnAlternatives);
    }

    @GetMapping("instances/{space}/{id}/neighbors")
    public GraphEntity getNeighbors(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage) {
        return repository.getNeighbors(stage, new Space(space), id);
    }

    @GetMapping("instances/{space}/{id}/releaseStatus")
    public ReleaseStatus getReleaseStatus(@PathVariable("space") String space, @PathVariable("id") UUID id, @RequestParam("releaseTreeScope") ReleaseTreeScope treeScope) {
        return repository.getReleaseStatus(new Space(space), id, treeScope);
    }


    @PostMapping("instances/{space}/{id}/suggestedLinksForProperty")
    public SuggestionResult getSuggestedLinksForProperty(@RequestBody(required = false) NormalizedJsonLd payload, @PathVariable("stage") DataStage stage, @PathVariable("space") String space, @PathVariable("id") UUID id, @RequestParam(value = "property") String propertyName, @RequestParam(value = "type", required = false) String type, @RequestParam(value = "search", required = false) String search, PaginationParam paginationParam) {
        if (payload == null) {
            payload = repository.getInstance(stage, new Space(space), id, true, true, false);
        }
        SuggestionResult suggestionResult = new SuggestionResult();
        ArangoRepositoryTypes.TargetsForProperties properties = new ArangoRepositoryTypes.TargetsForProperties(propertyName, payload.types());
        Paginated<NormalizedJsonLd> targetTypesForProperty = typeRepository.getTargetTypesForProperty(authContext.getUserWithRoles().getClientId(), stage, properties, false, false,null);
        List<Type> typesWithLabelInfo = ArangoRepositoryTypes.extractExtendedTypeInformationFromPayload(targetTypesForProperty.getData());
        if(type != null && !type.isBlank()){
            typesWithLabelInfo = typesWithLabelInfo.stream().filter(t -> type.equals(t.getName())).collect(Collectors.toList());
        }
        suggestionResult.setTypes(targetTypesForProperty.getData().stream().collect(Collectors.toMap(JsonLdDoc::primaryIdentifier, t -> t)));
        List<UUID> existingLinks = payload.getAsListOf(propertyName, JsonLdId.class, true).stream().map(idUtils::getUUID).filter(Objects::nonNull).collect(Collectors.toList());
        Paginated<SuggestedLink> documentsByTypes = repository.getSuggestionsByTypes(stage, typesWithLabelInfo, paginationParam, search, existingLinks);
        suggestionResult.setSuggestions(documentsByTypes);
        return suggestionResult;
    }


}

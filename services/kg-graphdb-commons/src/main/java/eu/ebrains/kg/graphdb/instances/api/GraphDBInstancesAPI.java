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
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
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
            types = extractExtendedTypeInformationFromPayload(typeRepository.getTypes(authContext.getUserWithRoles().getClientId(), stage, types, true));
        }
        return repository.getDocumentsByTypes(stage, types, paginationParam, searchByLabel, returnEmbedded, returnAlternatives);
    }

    @GetMapping("queriesByType")
    public Paginated<NormalizedJsonLd> getQueriesByType(@PathVariable("stage") DataStage stage, @RequestParam(value = "searchByLabel", required = false) String searchByLabel, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, PaginationParam paginationParam, @RequestParam("rootType") String rootType) {
        return repository.getQueriesByRootType(stage, paginationParam, searchByLabel, returnEmbedded, returnAlternatives, rootType);
    }

    @PostMapping("instancesByIds")
    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(@RequestBody List<String> ids, @PathVariable("stage") DataStage stage, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives) {
        List<InstanceId> instanceIds = ids.stream().map(InstanceId::deserialize).filter(Objects::nonNull).collect(Collectors.toList());
        return repository.getDocumentsByIdList(stage, instanceIds, returnEmbedded, returnAlternatives);
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
    public List<NormalizedJsonLd> getDocumentWithOutgoingRelatedInstances(@RequestParam("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam("relation") String relation, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives) {
        return repository.getDocumentsByOutgoingRelation(stage, new Space(space), id, new ArangoRelation(URLDecoder.decode(relation, StandardCharsets.UTF_8)), returnEmbedded, returnAlternatives);
    }

    @GetMapping("instances/{space}/{id}/releaseStatus")
    public ReleaseStatus getReleaseStatus(@PathVariable("space") String space, @PathVariable("id") UUID id, @RequestParam("releaseTreeScope") ReleaseTreeScope treeScope) {
        return repository.getReleaseStatus(new Space(space), id, treeScope);
    }


    @PostMapping("instances/{space}/{id}/suggestedLinksForProperty")
    public SuggestionResult getSuggestedLinksForProperty(@RequestBody(required = false) NormalizedJsonLd payload, @PathVariable("stage") DataStage stage, @PathVariable("space") String space, @PathVariable("id") UUID id, @RequestParam(value = "property") String propertyName, @RequestParam(value = "type", required = false) String type, @RequestParam(value = "search", required = false) String search, PaginationParam paginationParam) {
        if (payload == null) {
            //TODO Load from database (assume unchanged payload)
            payload = new NormalizedJsonLd();
        }
        SuggestionResult suggestionResult = new SuggestionResult();
        Paginated<NormalizedJsonLd> targetTypesForProperty = typeRepository.getTargetTypesForProperty(authContext.getUserWithRoles().getClientId(), stage, propertyName, true, null);
        List<Type> typesWithLabelInfo = extractExtendedTypeInformationFromPayload(targetTypesForProperty.getData());
        if(type != null && !type.isBlank()){
            typesWithLabelInfo = typesWithLabelInfo.stream().filter(t -> type.equals(t.getName())).collect(Collectors.toList());
        }
        suggestionResult.setTypes(targetTypesForProperty.getData().stream().collect(Collectors.toMap(JsonLdDoc::getPrimaryIdentifier, t -> t)));
        List<UUID> existingLinks = payload.getAsListOf(propertyName, JsonLdId.class).stream().map(idUtils::getUUID).filter(Objects::nonNull).collect(Collectors.toList());
        Paginated<SuggestedLink> documentsByTypes = repository.getSuggestionsByTypes(stage, typesWithLabelInfo, paginationParam, search, existingLinks);
        suggestionResult.setSuggestions(documentsByTypes);
        return suggestionResult;
    }


    private List<Type> extractExtendedTypeInformationFromPayload(List<NormalizedJsonLd> payload){
        return payload.stream().map(t -> {
            //TODO this can probably be solved in a more optimized way - we don't need all properties but only the labels...
            Type targetType = new Type(t.getPrimaryIdentifier());
            List<NormalizedJsonLd> properties = t.getAsListOf(EBRAINSVocabulary.META_PROPERTIES, NormalizedJsonLd.class);
            List<String> labelProperties = properties.stream().filter(p -> p.getAs(EBRAINSVocabulary.META_LABELPROPERTY, Boolean.class, false)).map(p -> p.getAs(SchemaOrgVocabulary.IDENTIFIER, String.class)).collect(Collectors.toList());
            t.remove(EBRAINSVocabulary.META_PROPERTIES);
            targetType.setLabelProperties(labelProperties);
            return targetType;
        }).collect(Collectors.toList());
    }


}
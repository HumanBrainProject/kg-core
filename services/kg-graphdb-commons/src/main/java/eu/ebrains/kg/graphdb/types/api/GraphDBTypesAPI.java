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

package eu.ebrains.kg.graphdb.types.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.types.controller.ArangoRepositoryTypes;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/graphdb/{stage}")
public class GraphDBTypesAPI {

    private final AuthContext authContext;

    private final ArangoRepositoryTypes repositoryTypes;

    public GraphDBTypesAPI(AuthContext authContext, ArangoRepositoryTypes repositoryTypes) {
        this.authContext = authContext;
        this.repositoryTypes = repositoryTypes;
    }

    @GetMapping("/types")
    public Paginated<NormalizedJsonLd> getTypes(@PathVariable("stage") DataStage stage, @RequestParam(value = "space", required = false) String space, PaginationParam paginationParam) {
        return getTypes(stage, space, false, false, paginationParam);
    }

    private Paginated<NormalizedJsonLd> getTypes(DataStage stage, String space, boolean withProperties, boolean withCount, PaginationParam paginationParam) {
        if (space != null && !space.isEmpty()) {
            return repositoryTypes.getTypesForSpace(authContext.getUserWithRoles().getClientId(), stage, new Space(space), withProperties, withCount, paginationParam);
        } else {
            return repositoryTypes.getAllTypes(authContext.getUserWithRoles().getClientId(), stage, withProperties, withCount, paginationParam);
        }
    }

    @GetMapping("/typesWithProperties")
    public Paginated<NormalizedJsonLd> getTypesWithProperties(@PathVariable("stage") DataStage stage, @RequestParam(value = "space", required = false) String space, @RequestParam(value = "withCounts", required = false, defaultValue = "true") boolean withCounts, PaginationParam paginationParam) {
        Paginated<NormalizedJsonLd> types = getTypes(stage, space, true, withCounts, paginationParam);
        types.getData().forEach(this::promoteLabelFields);
        return types;
    }

    private void promoteLabelFields(NormalizedJsonLd normalizedJsonLd) {
        List<Map> properties = normalizedJsonLd.getAsListOf(EBRAINSVocabulary.META_PROPERTIES, Map.class);
        List<Object> labelFieldNames = properties.stream().filter(m -> new NormalizedJsonLd(m).getAs(EBRAINSVocabulary.META_LABELPROPERTY, Boolean.class, false)).map(m -> m.get(SchemaOrgVocabulary.IDENTIFIER)).collect(Collectors.toList());
        normalizedJsonLd.put(EBRAINSVocabulary.META_LABELPROPERTIES, labelFieldNames);
    }

    @PostMapping("/typesByName")
    public Map<String, Result<NormalizedJsonLd>> getTypesByName(@RequestBody List<String> types, @PathVariable("stage") DataStage stage, @RequestParam(value = "space", required = false) String space) {
        return getTypesByName(types, stage, space, false, true);
    }

    @PostMapping("/typesWithPropertiesByName")
    public Map<String, Result<NormalizedJsonLd>> getTypesWithPropertiesByName(@RequestBody List<String> types, @PathVariable("stage") DataStage stage, @RequestParam(value = "withCounts", required = false, defaultValue = "true") boolean withCounts, @RequestParam(value = "space", required = false) String space) {
        Map<String, Result<NormalizedJsonLd>> typesByName = getTypesByName(types, stage, space, true, withCounts);
        typesByName.forEach((k, v) -> {
            if(v.getData() != null) {
                promoteLabelFields(v.getData());
            }
        });
        return typesByName;
    }

    private Map<String, Result<NormalizedJsonLd>> getTypesByName(List<String> types, DataStage stage, String space, boolean withProperties, boolean withCounts) {
        List<Type> typeList = types.stream().map(Type::new).collect(Collectors.toList());
        List<NormalizedJsonLd> typeObjects;
        if (space != null && !space.isBlank()) {
            typeObjects = repositoryTypes.getTypesForSpace(authContext.getUserWithRoles().getClientId(), stage, new Space(space), typeList, withProperties, withCounts);
        } else {
            typeObjects = repositoryTypes.getTypes(authContext.getUserWithRoles().getClientId(), stage, typeList, withProperties, withCounts);
        }
        Map<String, Result<NormalizedJsonLd>> type2Map = typeObjects.stream().map(NormalizedJsonLd::removeAllInternalProperties).collect(Collectors.toMap(JsonLdDoc::getPrimaryIdentifier, Result::ok));
        for (String type : types) {
            if(!type2Map.containsKey(type)){
                type2Map.put(type, Result.nok(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase()));
            }
        }
        return type2Map;
    }
}

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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.graphdb.types.api;

import eu.ebrains.kg.commons.api.GraphDBTypes;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/graphdb/{stage}")
public class GraphDBTypesAPIRest implements GraphDBTypes {

    private final GraphDBTypesAPI graphDBTypesAPI;

    public GraphDBTypesAPIRest(GraphDBTypesAPI graphDBTypesAPI) {
        this.graphDBTypesAPI = graphDBTypesAPI;
    }

    @GetMapping("/types")
    public Paginated<NormalizedJsonLd> getTypes(@PathVariable("stage") DataStage stage, @RequestParam(value = "space", required = false) String space, @RequestParam(value = "withIncomingLinks", required = false, defaultValue = "false") boolean withIncomingLinks, PaginationParam paginationParam) {
        return graphDBTypesAPI.getTypes(stage, space, withIncomingLinks, paginationParam);
    }

    @GetMapping("/typesWithProperties")
    public Paginated<NormalizedJsonLd> getTypesWithProperties(@PathVariable("stage") DataStage stage, @RequestParam(value = "space", required = false) String space, @RequestParam(value = "withCounts", required = false, defaultValue = "true") boolean withCounts, @RequestParam(value = "withIncomingLinks", required = false, defaultValue = "true") boolean withIncomingLinks, PaginationParam paginationParam) {
        return graphDBTypesAPI.getTypesWithProperties(stage, space, withCounts, withIncomingLinks, paginationParam);
    }

    @PostMapping("/typesByName")
    public Map<String, Result<NormalizedJsonLd>> getTypesByName(@RequestBody List<String> types, @PathVariable("stage") DataStage stage, @RequestParam(value = "space", required = false) String space) {
       return graphDBTypesAPI.getTypesByName(types, stage, space);
    }

    @PostMapping("/typesWithPropertiesByName")
    public Map<String, Result<NormalizedJsonLd>> getTypesWithPropertiesByName(@RequestBody List<String> types, @PathVariable("stage") DataStage stage, @RequestParam(value = "withCounts", required = false, defaultValue = "true") boolean withCounts, @RequestParam(value = "withIncomingLinks", required = false, defaultValue = "true") boolean withIncomingLinks, @RequestParam(value = "space", required = false) String space) {
        return graphDBTypesAPI.getTypesWithPropertiesByName(types, stage, withCounts, withIncomingLinks, space);
    }

}

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

package eu.ebrains.kg.graphdb.spaces.api;

import eu.ebrains.kg.commons.api.GraphDBSpaces;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/graphdb/{stage}")
public class GraphDBSpacesAPIRest implements GraphDBSpaces {

    private final GraphDBSpacesAPI graphDBSpacesAPI;

    public GraphDBSpacesAPIRest(GraphDBSpacesAPI graphDBSpacesAPI) {
        this.graphDBSpacesAPI = graphDBSpacesAPI;
    }

    @Override
    @GetMapping("/spaces/{space}")
    public NormalizedJsonLd getSpace(@PathVariable("stage") DataStage stage, @PathVariable("space") String space) {
        return graphDBSpacesAPI.getSpace(stage, space);
    }

    @Override
    @GetMapping("/spaces")
    public Paginated<NormalizedJsonLd> getSpaces(@PathVariable("stage") DataStage stage, PaginationParam paginationParam) {
        return graphDBSpacesAPI.getSpaces(stage, paginationParam);
    }
}
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

import eu.ebrains.kg.commons.markers.ExposesMinimalData;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.ScopeElement;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.graphdb.instances.controller.ArangoRepositoryInstances;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/graphdb/{stage}/scopes")
public class GraphDBScopesAPI {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ArangoRepositoryInstances repository;

    public GraphDBScopesAPI(ArangoRepositoryInstances repository) {
        this.repository = repository;
    }

    @GetMapping("/{space}/{id}")
    @ExposesMinimalData
    public ScopeElement getScopeForInstance(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam(value = "fetchLabels", defaultValue = "true") boolean fetchLabels){
       return this.repository.getScopeForInstance(new SpaceName(space), id, stage, fetchLabels);
    }

}

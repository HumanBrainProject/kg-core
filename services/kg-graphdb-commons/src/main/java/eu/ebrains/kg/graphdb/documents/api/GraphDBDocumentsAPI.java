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

package eu.ebrains.kg.graphdb.documents.api;

import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.graphdb.instances.controller.ArangoRepositoryInstances;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/graphdb/")
public class GraphDBDocumentsAPI {

    private final ArangoRepositoryInstances repository;

    public GraphDBDocumentsAPI(ArangoRepositoryInstances repository) {
        this.repository = repository;
    }

    @GetMapping("documentIds/{space}")
    public List<String> getDocumentIdsBySpace(@PathVariable("space") String space) {
        return repository.getDocumentIdsBySpace(new SpaceName(space));
    }

}

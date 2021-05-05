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

package eu.ebrains.kg.graphdb.documents.api;

import eu.ebrains.kg.commons.api.GraphDBDocuments;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/graphdb")
public class GraphDBDocumentsAPIRest implements GraphDBDocuments {

    private final GraphDBDocumentsAPI graphDBDocumentsAPI;

    public GraphDBDocumentsAPIRest(GraphDBDocumentsAPI graphDBDocumentsAPI) {
        this.graphDBDocumentsAPI = graphDBDocumentsAPI;
    }

    @GetMapping("/documentIds/{space}")
    public List<String> getDocumentIdsBySpace(@PathVariable("space") String space) {
        return graphDBDocumentsAPI.getDocumentIdsBySpace(space);
    }

}

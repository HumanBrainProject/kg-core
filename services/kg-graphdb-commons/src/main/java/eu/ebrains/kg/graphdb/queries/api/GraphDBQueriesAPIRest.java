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

package eu.ebrains.kg.graphdb.queries.api;

import eu.ebrains.kg.commons.api.GraphDBQueries;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.query.KgQuery;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/graphdb/queries")
public class GraphDBQueriesAPIRest implements GraphDBQueries {

    private final GraphDBQueriesAPI graphDBQueriesAPI;

    public GraphDBQueriesAPIRest(GraphDBQueriesAPI graphDBQueriesAPI) {
        this.graphDBQueriesAPI = graphDBQueriesAPI;
    }

    @Override
    @PostMapping
    public Paginated<NormalizedJsonLd> executeQuery(@RequestBody KgQuery query, PaginationParam paginationParam){
        return graphDBQueriesAPI.executeQuery(query, paginationParam);
    }

}

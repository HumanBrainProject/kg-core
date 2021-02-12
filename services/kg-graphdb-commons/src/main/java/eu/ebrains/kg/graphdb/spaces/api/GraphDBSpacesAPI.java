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
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.spaces.controller.ArangoRepositorySpaces;
import org.springframework.stereotype.Component;

@Component
public class GraphDBSpacesAPI implements GraphDBSpaces.Client {

    private final ArangoRepositorySpaces repositorySpaces;

    public GraphDBSpacesAPI(ArangoRepositorySpaces repositorySpaces) {
        this.repositorySpaces = repositorySpaces;
    }

    @Override
    public NormalizedJsonLd getSpace(DataStage stage, String space) {
        return repositorySpaces.getSpace(new SpaceName(space), stage);
    }

    @Override
    public Paginated<NormalizedJsonLd> getSpaces(DataStage stage, PaginationParam paginationParam) {
        Paginated<NormalizedJsonLd> spaces = repositorySpaces.getSpaces(stage, paginationParam);
        spaces.getData().forEach(e -> e.remove(EBRAINSVocabulary.META_SPACE));
        return spaces;
    }
}

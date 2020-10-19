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

package eu.ebrains.kg.graphdb.spaces.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.permissions.controller.PermissionSvc;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.spaces.controller.ArangoRepositorySpaces;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/graphdb/{stage}")
public class GraphDBSpacesAPI {

    private final AuthContext authContext;

    private final ArangoRepositorySpaces repositorySpaces;

    private final PermissionSvc permissionSvc;

    public GraphDBSpacesAPI(AuthContext authContext, ArangoRepositorySpaces repositorySpaces, PermissionSvc permissionSvc) {
        this.authContext = authContext;
        this.repositorySpaces = repositorySpaces;
        this.permissionSvc = permissionSvc;
    }

    @GetMapping("/spaces/{space}")
    public NormalizedJsonLd getSpace(@PathVariable("stage") DataStage stage, @PathVariable("space") String space) {
        return repositorySpaces.getSpace(new SpaceName(space), stage);
    }

//    @PutMapping("/spaces/{space}")
//    public NormalizedJsonLd defineSpace(@RequestBody NormalizedJsonLd payload, @PathVariable("stage") DataStage stage, @PathVariable("space") String space) {
//        permissionSvc.hasPermission(authContext.getUserWithRoles(), Functionality.CREATE_SPACE, new Space(space));
//        return repositorySpaces.defineSpace(new Space(space), payload, stage);
//    }

    @GetMapping("/spaces")
    public Paginated<NormalizedJsonLd> getSpaces(@PathVariable("stage") DataStage stage, PaginationParam paginationParam) {
        Paginated<NormalizedJsonLd> spaces = repositorySpaces.getSpaces(stage, paginationParam);
        spaces.getData().forEach(e -> e.remove(EBRAINSVocabulary.META_SPACE));
        return spaces;
    }
}

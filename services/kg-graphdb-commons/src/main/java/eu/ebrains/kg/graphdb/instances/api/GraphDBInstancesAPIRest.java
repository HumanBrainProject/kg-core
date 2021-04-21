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

package eu.ebrains.kg.graphdb.instances.api;

import eu.ebrains.kg.commons.api.GraphDBInstances;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesData;
import eu.ebrains.kg.commons.markers.ExposesMinimalData;
import eu.ebrains.kg.commons.markers.ExposesQuery;
import eu.ebrains.kg.commons.markers.ExposesReleaseStatus;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/graphdb/{stage}")
public class GraphDBInstancesAPIRest implements GraphDBInstances {

    private final GraphDBInstancesAPI graphDBInstancesAPI;

    public GraphDBInstancesAPIRest(GraphDBInstancesAPI graphDBInstancesAPI) {
        this.graphDBInstancesAPI = graphDBInstancesAPI;
    }

    @GetMapping("instances/{space}/{id}/incomingLinks")
    @ExposesData
    public Paginated<NormalizedJsonLd> getIncomingLinks(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam(value = "property") String property, @RequestParam(value = "type") String type, PaginationParam paginationParam) {
        return graphDBInstancesAPI.getIncomingLinks(space!= null ? URLDecoder.decode(space, StandardCharsets.UTF_8) : null, id, stage, property, type != null ? URLDecoder.decode(type, StandardCharsets.UTF_8) : null, paginationParam);
    }

    @GetMapping("instances/{space}/{id}")
    @ExposesData
    public NormalizedJsonLd getInstanceById(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam(value = "returnEmbedded", defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnIncomingLinks", required = false, defaultValue = "false") boolean returnIncomingLinks, @RequestParam(value = "incomingLinksPageSize", required = false) Long incomingLinksPageSize, @RequestParam(value = "removeInternalProperties", required = false, defaultValue = "true") boolean removeInternalProperties) {
        return graphDBInstancesAPI.getInstanceById(space, id, stage, returnEmbedded, returnAlternatives, returnIncomingLinks, incomingLinksPageSize, removeInternalProperties);
    }

    @GetMapping("instancesByType")
    @ExposesData
    public Paginated<NormalizedJsonLd> getInstancesByType(@PathVariable("stage") DataStage stage, @RequestParam("type") String type, @RequestParam(value = "space", required = false) String space, @RequestParam(value = "searchByLabel", required = false) String searchByLabel, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "sortByLabel", required = false, defaultValue = "false") boolean sortByLabel, PaginationParam paginationParam) {
        return graphDBInstancesAPI.getInstancesByType(stage, type, space, searchByLabel, returnAlternatives, returnEmbedded, sortByLabel, paginationParam);
    }

    @GetMapping("queriesByType")
    @ExposesQuery
    public Paginated<NormalizedJsonLd> getQueriesByType(@PathVariable("stage") DataStage stage, @RequestParam(value = "searchByLabel", required = false) String searchByLabel, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, PaginationParam paginationParam, @RequestParam("rootType") String rootType) {
        return graphDBInstancesAPI.getQueriesByType(stage, searchByLabel, returnAlternatives, returnEmbedded, paginationParam, rootType);
    }

    @PostMapping("instancesByIds")
    @ExposesData
    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(@RequestBody List<String> ids, @PathVariable("stage") DataStage stage, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives, @RequestParam(value = "returnIncomingLinks", required = false, defaultValue = "false") boolean returnIncomingLinks, @RequestParam(value = "incomingLinksPageSize", required = false) Long incomingLinksPageSize) {
        return graphDBInstancesAPI.getInstancesByIds(ids, stage, returnEmbedded, returnAlternatives, returnIncomingLinks, incomingLinksPageSize);
    }

    @PostMapping("instancesByIds/labels")
    @ExposesMinimalData
    public Map<UUID, String> getLabels(@RequestBody List<String> ids, @PathVariable("stage") DataStage stage) {
       return graphDBInstancesAPI.getLabels(ids, stage);
    }

    @GetMapping("instancesByIdentifier/{space}")
    @ExposesData
    public List<NormalizedJsonLd> getInstancesByIdentifier(@RequestParam("identifier") String identifier, @PathVariable("space") String space, @PathVariable("stage") DataStage stage) {
       return graphDBInstancesAPI.getInstancesByIdentifier(identifier, space, stage);

    }

    @GetMapping("instances/{space}/{id}/relatedByIdentifier")
    @ExposesData
    public List<NormalizedJsonLd> getDocumentWithRelatedInstancesByIdentifiers(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives) {
        return graphDBInstancesAPI.getDocumentWithRelatedInstancesByIdentifiers(space, id, stage, returnEmbedded, returnAlternatives);
    }

    @GetMapping("instances/{space}/{id}/relatedByIncomingRelation")
    @ExposesData
    public List<NormalizedJsonLd> getDocumentWithIncomingRelatedInstances(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam("relation") String relation, @RequestParam(value = "useOriginalTo", defaultValue = "false") boolean useOriginalTo, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives) {
        return graphDBInstancesAPI.getDocumentWithIncomingRelatedInstances(space, id, stage, relation, useOriginalTo, returnEmbedded, returnAlternatives);
    }
//
//    @GetMapping("instances/{space}/{id}/relatedByOutgoingRelation")
//    @ExposesData
//    public List<NormalizedJsonLd> getDocumentWithOutgoingRelatedInstances(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage, @RequestParam("relation") String relation, @RequestParam(value = "returnEmbedded", required = false, defaultValue = "false") boolean returnEmbedded, @RequestParam(value = "returnAlternatives", required = false, defaultValue = "false") boolean returnAlternatives) {
//        return graphDBInstancesAPI.getDocumentWithOutgoingRelatedInstances(space, id, stage, relation, returnEmbedded, returnAlternatives);
//    }

    @GetMapping("instances/{space}/{id}/neighbors")
    @ExposesMinimalData
    public GraphEntity getNeighbors(@PathVariable("space") String space, @PathVariable("id") UUID id, @PathVariable("stage") DataStage stage) {
        return graphDBInstancesAPI.getNeighbors(space, id, stage);
    }

    @GetMapping("instances/{space}/{id}/releaseStatus")
    @ExposesReleaseStatus
    public ReleaseStatus getReleaseStatus(@PathVariable("space") String space, @PathVariable("id") UUID id, @RequestParam("releaseTreeScope") ReleaseTreeScope treeScope) {
        return graphDBInstancesAPI.getReleaseStatus(space, id, treeScope);
    }


    @PostMapping("instances/{space}/{id}/suggestedLinksForProperty")
    @ExposesMinimalData
    public SuggestionResult getSuggestedLinksForProperty(@RequestBody(required = false) NormalizedJsonLd payload, @PathVariable("stage") DataStage stage, @PathVariable("space") String space, @PathVariable("id") UUID id, @RequestParam(value = "property") String propertyName, @RequestParam(value = "type", required = false) String type, @RequestParam(value = "search", required = false) String search, PaginationParam paginationParam) {
       return graphDBInstancesAPI.getSuggestedLinksForProperty(payload, stage, space, id, propertyName, type, search, paginationParam);
    }


}

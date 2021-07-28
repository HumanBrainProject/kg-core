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
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.commons.api;

import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesData;
import eu.ebrains.kg.commons.markers.ExposesMinimalData;
import eu.ebrains.kg.commons.markers.ExposesQuery;
import eu.ebrains.kg.commons.markers.ExposesReleaseStatus;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface GraphDBInstances {

    interface Client extends GraphDBInstances {}

    @ExposesData
    Paginated<NormalizedJsonLd> getIncomingLinks(String space, UUID id, DataStage stage, String property, String type, PaginationParam paginationParam);

    @ExposesData
    NormalizedJsonLd getInstanceById(String space, UUID id, DataStage stage, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize, boolean removeInternalProperties);

    @ExposesData
    NormalizedJsonLd getQueryById(String space, UUID id,  DataStage stage);

    @ExposesData
    Paginated<NormalizedJsonLd> getInstancesByType(DataStage stage, String type, String space, String searchByLabel, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam);

    @ExposesQuery
    Paginated<NormalizedJsonLd> getQueriesByType(DataStage stage, String searchByLabel, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam, String rootType);

    @ExposesData
    Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(List<String> instanceIds, DataStage stage, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize);

    @ExposesMinimalData
    Map<UUID, String> getLabels(List<String> instanceIds, DataStage stage);

    @ExposesData
    List<NormalizedJsonLd> getInstancesByIdentifier(String identifier, String space, DataStage stage);

    @ExposesData
    List<NormalizedJsonLd> getDocumentWithRelatedInstancesByIdentifiers(String space, UUID id, DataStage stage, boolean returnEmbedded, boolean returnAlternatives);

    @ExposesData
    List<NormalizedJsonLd> getDocumentWithIncomingRelatedInstances(String space, UUID id, DataStage stage, String relation, boolean useOriginalTo, boolean returnEmbedded, boolean returnAlternatives);
//
//    @ExposesData
//    List<NormalizedJsonLd> getDocumentWithOutgoingRelatedInstances(String space, UUID id, DataStage stage, String relation, boolean returnEmbedded, boolean returnAlternatives);

    @ExposesMinimalData
    GraphEntity getNeighbors(String space, UUID id, DataStage stage);

    @ExposesReleaseStatus
    ReleaseStatus getReleaseStatus(String space, UUID id, ReleaseTreeScope treeScope);

    @ExposesMinimalData
    SuggestionResult getSuggestedLinksForProperty(NormalizedJsonLd payload, DataStage stage, String space, UUID id, String propertyName, String sourceType, String targetType, String search, PaginationParam paginationParam);
}

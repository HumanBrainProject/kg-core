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

package eu.ebrains.kg.systemTest.controller.investigation;

import com.arangodb.model.AqlQueryOptions;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.InferredJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import eu.ebrains.kg.systemTest.serviceCall.IdsSvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class InstanceHistory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final IdsSvc idsSvc;

    private final IdUtils idUtils;

    private ArangoDatabaseProxy events;

    public InstanceHistory(IdUtils idUtils, IdsSvc idsSvc,  @Qualifier("events") ArangoDatabaseProxy events) {
        this.events = events;
        this.idsSvc = idsSvc;
        this.idUtils = idUtils;
    }

    public List<NormalizedJsonLd> loadHistoryOfInstance(UUID uuid) {
        List<JsonLdIdMapping> jsonLdIdMappings = idsSvc.resolveIds(DataStage.LIVE, Collections.singletonList(new IdWithAlternatives(uuid, null, null)));
        if (jsonLdIdMappings.isEmpty()) {
            return null;
        } else {
            Set<JsonLdId> resolvedIds = jsonLdIdMappings.get(0).getResolvedIds();
            if (resolvedIds.isEmpty()) {
                return null;
            }
            if (resolvedIds.size() > 1) {
                logger.warn(String.format("The id %s resolved to %d ids - there is something wrong!", uuid, resolvedIds.size()));
            }
            Set<UUID> resolvedUUIDs = resolvedIds.stream().map(idUtils::getUUID).filter(Objects::nonNull).collect(Collectors.toSet());
            List<NormalizedJsonLd> liveEventsByResolvedIds = resolvedUUIDs.stream().map(id -> getEvents(id, DataStage.LIVE)).flatMap(Collection::stream).collect(Collectors.toList());
            Set<UUID> involvedLiveIds = liveEventsByResolvedIds.stream().map(e -> e.getAs("data", NormalizedJsonLd.class).getIdentifiers().stream().map(identifier -> JsonLdId.cast(identifier, null)).filter(Objects::nonNull).map(idUtils::getUUID).filter(Objects::nonNull).collect(Collectors.toSet())).flatMap(Collection::stream).collect(Collectors.toSet());
            Set<UUID> allLiveIds = Stream.concat(resolvedUUIDs.stream(), involvedLiveIds.stream()).collect(Collectors.toSet());
            List<NormalizedJsonLd> liveEvents = allLiveIds.stream().map(id -> getEvents(id, DataStage.LIVE)).flatMap(Collection::stream).collect(Collectors.toList());
            Set<UUID> involvedNativeUUIDs = liveEvents.stream().map(e -> {
                NormalizedJsonLd data = e.getAs("data", NormalizedJsonLd.class);
                return InferredJsonLdDoc.from(data).getInferenceOf().stream().map(idUtils::getUUID).filter(Objects::nonNull).collect(Collectors.toSet());
            }).flatMap(Collection::stream).collect(Collectors.toSet());
            List<NormalizedJsonLd> nativeEvents = involvedNativeUUIDs.stream().map(id -> getEvents(id, DataStage.NATIVE)).flatMap(Collection::stream).collect(Collectors.toList());
            return Stream.concat(liveEvents.stream(), nativeEvents.stream()).sorted((o1, o2)-> {
                Long o1ts = o1==null ? null : o1.getAs("indexedTimestamp", Long.class);
                Long o2ts = o2==null ? null : o2.getAs("indexedTimestamp", Long.class);
                return o1ts != null ? o2ts != null ? o1ts.compareTo(o2ts) : 1 : 0;
            }).collect(Collectors.toList());
        }
    }


    private List<NormalizedJsonLd> getEvents(UUID uuid, DataStage stage) {
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR event IN " + stage.name().toLowerCase() + "_events"));
        aql.addLine(AQL.trust("FILTER event.documentId==@documentId"));
        aql.addLine(AQL.trust("RETURN event"));
        bindVars.put("documentId", uuid);
        return events.getOrCreate().query(aql.build().getValue(), bindVars, new AqlQueryOptions(), NormalizedJsonLd.class).asListRemaining();
    }

}

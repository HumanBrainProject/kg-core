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

package eu.ebrains.kg.nexusv1.controller;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.nexusv1.models.NexusV1Event;
import eu.ebrains.kg.nexusv1.serviceCall.IdsSvc;
import eu.ebrains.kg.nexusv1.serviceCall.JsonLdSvc;
import eu.ebrains.kg.nexusv1.serviceCall.NexusSvc;
import eu.ebrains.kg.nexusv1.serviceCall.PrimaryStoreSvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class EventDispatcher {

    private Map<String, String> orgMap;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    PrimaryStoreSvc primaryStoreSvc;

    IdsSvc idsSvc;

    NexusSvc nexusSvc;
    private JsonLdSvc jsonLdSvc;

    PayloadNormalizer payloadNormalizer;

    NexusV1EventTracker eventTracker;

    IdUtils idUtils;

    @Value("${eu.ebrains.kg.nexus.auth.token}")
    private String clientAuthToken;

    public EventDispatcher(PrimaryStoreSvc primaryStoreSvc, PayloadNormalizer payloadNormalizer, NexusV1EventTracker eventTracker, NexusSvc nexusSvc, JsonLdSvc jsonLdSvc, IdsSvc idsSvc, IdUtils idUtils) {
        this.primaryStoreSvc = primaryStoreSvc;
        this.payloadNormalizer = payloadNormalizer;
        this.eventTracker = eventTracker;
        this.nexusSvc = nexusSvc;
        this.jsonLdSvc = jsonLdSvc;
        this.idsSvc = idsSvc;
        this.idUtils = idUtils;
        this.orgMap = new HashMap<>();
    }

    private String getOrgLabel(String orgId) {
        if (this.orgMap.containsKey(orgId)) {
            return this.orgMap.get(orgId);
        } else {
            ClientAuthToken authToken = new ClientAuthToken(this.clientAuthToken);
            JsonLdDoc orgInfo = nexusSvc.getOrgInfo(orgId, authToken);
            String orgLabel = (String) orgInfo.get("_label");
            this.orgMap.put(orgId, orgLabel);
            return orgLabel;
        }
    }

    private void processEvent(String org, NexusV1Event event) {
        NormalizedJsonLd normalizedJsonLd = payloadNormalizer.normalizePayload(event);
        try {
            Event e = new Event(new Space(org), UUID.nameUUIDFromBytes(normalizedJsonLd.getId().getId().getBytes(StandardCharsets.UTF_8)), normalizedJsonLd, event.toEventType(), event.getDate());
            doDispatchEvent(e, org, event.getResourceId());
            this.eventTracker.updateLastSeenEventId(event.getEventId());
        } catch (ParseException ex) {
            logger.error(String.format("Could not parse date from nexus v1 event"), ex.getMessage());
        } catch (NexusV1Event.UnHandleEventTypeException ex) {
            logger.error(String.format("Could not interprete event type from nexus v1 event"), ex.getMessage());
        }
    }

    void dispatchEvent(NexusV1Event event) {
        if (event != null) {
            String orgId = event.getOrganization();
            String orgLabel = getOrgLabel(orgId);
            processEvent(orgLabel, event);
        }
    }

    private void doDispatchEvent(Event event, String organization, String resourceId) {
        switch (event.getType()) {
            case UPDATE:
            case INSERT:
                insertOrUpdateEvent(event, organization, resourceId);
                break;
            case DELETE:
                deleteInstance(event, organization, resourceId);
                break;
        }
    }

    private void insertOrUpdateEvent(Event event, String organization, String resourceId) {
        Space space = new Space(organization);
        if (!PayloadNormalizer.isIgnored(space)) {
            PayloadNormalizer.normalizeIfSuffixed(space);
            //FIXME interpret timestamp from parameter
            if (resourceId != null) {
                event.getData().addProperty(JsonLdConsts.ID, resourceId);
            }
            primaryStoreSvc.pushToStore(event, space);
        }
    }

    private void deleteInstance(Event event, String organization, String address) {
        Space space = new Space(organization);
        if (!PayloadNormalizer.isIgnored(space)) {
            Space normalizedSpace = PayloadNormalizer.normalizeIfSuffixed(space);
            //Fetch the KG id of the instance
            JsonLdId toResolve = new JsonLdId(address);
            JsonLdIdMapping jsonLdIdMapping = idsSvc.resolveId(DataStage.NATIVE, toResolve.getId());
            if(jsonLdIdMapping != null && jsonLdIdMapping.getResolvedIds()!=null && jsonLdIdMapping.getResolvedIds().size()==1){
                NormalizedJsonLd deletePayload = new NormalizedJsonLd();
                deletePayload.setId(jsonLdIdMapping.getResolvedIds().iterator().next());
                primaryStoreSvc.pushToStore(new Event(jsonLdIdMapping.getSpace(), idUtils.getUUID(deletePayload.getId()), deletePayload, Event.Type.DELETE, new Date(event.getReportedTimeStampInMs())), normalizedSpace);
            }else {
                logger.error(String.format("Could not resolve id to delete - %s", address));
            }
        }
    }

}

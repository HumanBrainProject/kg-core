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

package eu.ebrains.kg.core.controller;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.GraphDBDocuments;
import eu.ebrains.kg.commons.api.GraphDBInstances;
import eu.ebrains.kg.commons.api.PrimaryStoreEvents;
import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
/**
 * The inference controller contains the orchestration logic for the inference operations
 */
@Component
public class CoreInferenceController {

    private final GraphDBInstances.Client graphDBInstances;
    private final GraphDBDocuments.Client graphDBDocuments;
    private final IdUtils idUtils;
    private final PrimaryStoreEvents.Client primaryStoreEvents;
    private final IdsController ids;

    public CoreInferenceController(GraphDBInstances.Client graphDBInstances, GraphDBDocuments.Client graphDBDocuments, IdUtils idUtils, PrimaryStoreEvents.Client primaryStoreEvents, IdsController ids) {
        this.graphDBInstances = graphDBInstances;
        this.graphDBDocuments = graphDBDocuments;
        this.idUtils = idUtils;
        this.primaryStoreEvents = primaryStoreEvents;
        this.ids = ids;
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Async
    public void asyncTriggerInference(SpaceName space, String identifier){
        triggerInference(space, identifier);
    }

    public void triggerInference(SpaceName space, String identifier) {
        List<UUID> uuids;
        if(space == null){
            throw new IllegalArgumentException("No space provided");
        }
        if (identifier == null) {
            uuids = graphDBDocuments.getDocumentIdsBySpace(space.getName()).stream().map(UUID::fromString).collect(Collectors.toList());
        } else {
            uuids = graphDBInstances.getInstancesByIdentifier(identifier, space.getName(), DataStage.NATIVE).stream().map(d -> IndexedJsonLdDoc.from(d).getDocumentId()).collect(Collectors.toList());
            if (uuids.isEmpty()) {
                //We can't find any uuids - it could be that the passed identifier is the id of the IN_PROGRESS stage -> we therefore have to try to look it up...
                UUID uuid = extractInternalUUID(identifier);
                if (uuid != null) {
                    InstanceId instanceId = ids.resolveId(DataStage.IN_PROGRESS, uuid);
                    if (instanceId != null) {
                        NormalizedJsonLd instance = graphDBInstances.getInstanceById(instanceId.getSpace().getName(), instanceId.getUuid(), DataStage.IN_PROGRESS,  false, false, false, null, true);
                        List<JsonLdId> inferenceOf = InferredJsonLdDoc.from(instance).getInferenceOf();
                        //To be sure, we re-infer all of the previous sources...
                        uuids = inferenceOf.stream().map(idUtils::getUUID).filter(Objects::nonNull).collect(Collectors.toList());
                    }
                }
            }
        }

        for (UUID documentId : uuids) {
            try {
                logger.info(String.format("Inferring document with UUID %s in space %s", documentId, space.getName()));
                primaryStoreEvents.infer(space.getName(), documentId);
            } catch (Exception e) {
                logger.error(String.format("Was not able to infer document with UUID %s", documentId), e);
                if (e instanceof UnauthorizedException) {
                    //Since an unauthorized exception is not recoverable - we can stop here...
                    throw e;
                }
            }
        }
    }

    private UUID extractInternalUUID(String identifier) {
        UUID uuid;
        try {
            uuid = idUtils.getUUID(new JsonLdId(identifier));
        }
        catch (IllegalArgumentException e){
            uuid = null;
        }
        if (uuid == null) {
            try {
                uuid = UUID.fromString(identifier);
            } catch (IllegalArgumentException e) {
                //The passed identifier was not a uuid
            }
        }
        return uuid;
    }
}

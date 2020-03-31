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

package eu.ebrains.kg.core.controller;

import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.core.serviceCall.GraphDB4ExtraOperationsSvc;
import eu.ebrains.kg.core.serviceCall.Ids4ExtraOperationsSvc;
import eu.ebrains.kg.core.serviceCall.PrimaryStoreSvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class InferenceController {


    private final GraphDB4ExtraOperationsSvc graphSvc;
    private final IdUtils idUtils;
    private final PrimaryStoreSvc primaryStoreSvc;
    private final Ids4ExtraOperationsSvc idsSvc;

    public InferenceController(GraphDB4ExtraOperationsSvc graphSvc, IdUtils idUtils, PrimaryStoreSvc primaryStoreSvc, Ids4ExtraOperationsSvc idsSvc) {
        this.graphSvc = graphSvc;
        this.idUtils = idUtils;
        this.primaryStoreSvc = primaryStoreSvc;
        this.idsSvc = idsSvc;
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Async
    public void asyncTriggerInference(Space space, String identifier, AuthTokens authTokens){
        triggerInference(space, identifier, authTokens);
    }

    public void triggerInference(Space space, String identifier, AuthTokens authTokens) {
        List<UUID> uuids;
        if (identifier == null) {
            uuids = graphSvc.getDocumentIdsBySpace(space, DataStage.NATIVE, authTokens).stream().map(UUID::fromString).collect(Collectors.toList());
        } else {
            uuids = graphSvc.getRelatedInstancesByIdentifiers(space, identifier, DataStage.NATIVE, authTokens).stream().map(IndexedJsonLdDoc::getDocumentId).collect(Collectors.toList());
            if (uuids.isEmpty()) {
                //We can't find any uuids - it could be that the passed identifier is the id of the LIVE stage -> we therefore have to try to look it up...
                UUID uuid = extractInternalUUID(identifier);
                if (uuid != null) {
                    InstanceId instanceId = idsSvc.resolveId(DataStage.LIVE, uuid, authTokens);
                    if (instanceId != null) {
                        NormalizedJsonLd instance = graphSvc.getInstance(DataStage.LIVE, instanceId, false, false, false, authTokens);
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
                primaryStoreSvc.inferInstance(space, documentId, authTokens);
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

    public void triggerDeferredInference(AuthTokens authTokens) {
        primaryStoreSvc.inferDeferred(authTokens);
    }
}

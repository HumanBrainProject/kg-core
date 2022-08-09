/*
 * Copyright 2022 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.core.api.common;

import eu.ebrains.kg.commons.api.JsonLd;
import eu.ebrains.kg.commons.exception.InvalidRequestException;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.ExtendedResponseConfiguration;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.core.controller.CoreInstanceController;
import eu.ebrains.kg.core.controller.IdsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.UUID;

public abstract class Instances {

    protected static final String TAG = "instances";
    protected static final String TAG_ADV = "instances - advanced";
    protected static final String TAG_EXTRA = "xtra - instances";
    protected final JsonLd.Client jsonLd;
    protected final IdsController idsController;
    protected final CoreInstanceController instanceController;
    protected  final Logger logger = LoggerFactory.getLogger(getClass());

    public Instances(JsonLd.Client jsonLd, IdsController idsController, CoreInstanceController instanceController) {
        this.jsonLd = jsonLd;
        this.idsController = idsController;
        this.instanceController = instanceController;
    }

    protected NormalizedJsonLd normalizePayload(JsonLdDoc jsonLdDoc, boolean requiresTypeAtRootLevel){
        try{
            jsonLdDoc.normalizeTypes();
            jsonLdDoc.validate(requiresTypeAtRootLevel);
        }
        catch (InvalidRequestException e){
            //There have been validation errors -> we're going to normalize and validate again...
            final NormalizedJsonLd normalized = jsonLd.normalize(jsonLdDoc, true);
            normalized.validate(requiresTypeAtRootLevel);
            return normalized;
        }
        return new NormalizedJsonLd(jsonLdDoc);
    }


    protected ResponseEntity<Result<NormalizedJsonLd>> contributeToInstance(NormalizedJsonLd normalizedJsonLd, UUID id, ExtendedResponseConfiguration responseConfiguration, boolean removeNonDeclaredFields) {
        Date startTime = new Date();
        logger.debug(String.format("Contributing to instance with id %s", id));
        final InstanceId instanceId = idsController.findId(id, normalizedJsonLd.identifiers());
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        }
        ResponseEntity<Result<NormalizedJsonLd>> resultResponseEntity = instanceController.contributeToInstance(normalizedJsonLd, instanceId, removeNonDeclaredFields, responseConfiguration);
        logger.debug(String.format("Done contributing to instance with id %s", id));
        Result<NormalizedJsonLd> body = resultResponseEntity.getBody();
        if(body!=null){
            body.setExecutionDetails(startTime, new Date());
        }
        return resultResponseEntity;
    }


    protected String enrichSearchTermIfItIsAUUID(String search){
        if(search!=null) {
            try {
                //The search string is a UUID -> let's try to resolve it - if we're successful, we can shortcut the lookup process.
                UUID uuid = UUID.fromString(search);
                InstanceId resolvedSearchId = idsController.resolveId(DataStage.IN_PROGRESS, uuid);
                if(resolvedSearchId!=null){
                    return resolvedSearchId.serialize();
                }
            }
            catch(IllegalArgumentException e){
                //The search string is not an id -> we therefore don't treat it.
            }
        }
        return search;
    }

}

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

package eu.ebrains.kg.nexusv0.controller;

import com.google.gson.Gson;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.exception.AmbiguousException;
import eu.ebrains.kg.commons.exception.InstanceNotFoundException;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.nexusv0.serviceCall.CoreSvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class NexusV0Importer {

    private final PayloadNormalizer payloadNormalizer;

    private final Gson gson;

    private final IdUtils idUtils;

    private final CoreSvc coreSvc;

    public NexusV0Importer(PayloadNormalizer payloadNormalizer, Gson gson, IdUtils idUtils, CoreSvc coreSvc) {
        this.payloadNormalizer = payloadNormalizer;
        this.gson = gson;
        this.idUtils = idUtils;
        this.coreSvc = coreSvc;
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public final static String RELEASING_TYPE = "https://schema.hbp.eu/Release";

    private void releaseInstance(JsonLdId jsonLdId, boolean deferInference, String nexusEndpoint) {
        String idTranslatedToEndpoint = payloadNormalizer.translateToCurrentEndpoint(jsonLdId.getId(), nexusEndpoint);
        if (deferInference) {
            Space spaceFromUrl = payloadNormalizer.getSpaceFromUrl(jsonLdId.getId());
            if (spaceFromUrl != null) {
                //When releasing, we first ensure that the object actually is inferred...
                logger.info(String.format("Now inferring the instance in space %s because there is a release waiting...", spaceFromUrl.getName()));
                coreSvc.inferInstance(spaceFromUrl, idTranslatedToEndpoint);
            } else {
                logger.error(String.format("Was not able to extract space from the id %s", jsonLdId.getId()));
            }
        }
        Result<List<NormalizedJsonLd>> instanceByIdentifier = coreSvc.getInstancesByIdentifiers(Collections.singleton(idTranslatedToEndpoint));
        if (instanceByIdentifier != null && instanceByIdentifier.getData() != null) {
            if (instanceByIdentifier.getData().size() == 1) {
                NormalizedJsonLd normalizedJsonLd = instanceByIdentifier.getData().get(0);
                UUID uuid = idUtils.getUUID(normalizedJsonLd.getId());
                String revision = normalizedJsonLd.getAs(EBRAINSVocabulary.META_REVISION, String.class);
                logger.info(String.format("Releasing instance %s in revision %s", uuid, revision));
                coreSvc.releaseInstance(uuid, revision);
            } else if (instanceByIdentifier.getData().isEmpty()) {
                throw new InstanceNotFoundException(String.format("Was not able to release the instance %s, because it can not be found in the database.", idTranslatedToEndpoint));
            } else {
                throw new AmbiguousException(String.format("Received multiple instances for identifier %s - this is an invalid state when releasing an instance...", idTranslatedToEndpoint));
            }
        } else {
            throw new IllegalArgumentException(String.format("Was not able to resolve the an instance with the identifier %s", idTranslatedToEndpoint));
        }
    }

    public void insertOrUpdateEvent(JsonLdDoc payload, String organization, String domain, String schema, String version, String timestamp, String user, String id, boolean deferInference, String nexusEndpoint) {
        logger.info(String.format("Received insert/update payload from Nexus v0 for instance id %s/%s/%s/%s/%s", organization, domain, schema, version, id));
        Space space = PayloadNormalizer.normalizeIfSuffixed(new Space(organization));
        logger.trace(String.format("Found space %s for instance id %s/%s/%s/%s/%s ", space.getName(), organization, domain, schema, version, id));
        NormalizedJsonLd normalizedJsonLdDoc;
        String userId = resolveUserId(user, organization);
        ZonedDateTime eventTime = timestamp != null ? ZonedDateTime.parse(timestamp) : null;
        try {
            normalizedJsonLdDoc = payloadNormalizer.normalizePayload(payload, organization, domain, schema, version, id, nexusEndpoint);
            logger.debug(String.format("Successfully normalized the payload for instance id %s/%s/%s/%s/%s", organization, domain, schema, version, id));
        } catch (Exception e) {
            logger.error(String.format("Was not able to normalize the received payload for instance id %s/%s/%s/%s/%s:\n\n%s", organization, domain, schema, version, id, gson.toJson(payload)), e);
            throw e;
        }
        if (PayloadNormalizer.isInferred(organization)) {
            //If the document is an inferred instance, we are only interested in the id and identifiers to be able to reference it...
            NormalizedJsonLd inferredPayload = new NormalizedJsonLd();
            inferredPayload.put(JsonLdConsts.ID, normalizedJsonLdDoc.get(JsonLdConsts.ID));
            inferredPayload.put(SchemaOrgVocabulary.IDENTIFIER, normalizedJsonLdDoc.get(SchemaOrgVocabulary.IDENTIFIER));
            normalizedJsonLdDoc = inferredPayload;
        } else if (normalizedJsonLdDoc.getTypes().contains(RELEASING_TYPE)) {
            Object releaseInstance = normalizedJsonLdDoc.get("https://schema.hbp.eu/release/instance");
            if (releaseInstance instanceof Map && ((Map) releaseInstance).containsKey(JsonLdConsts.ID)) {
                releaseInstance(new JsonLdId((new NormalizedJsonLd((Map) releaseInstance).getAs(JsonLdConsts.ID, String.class))), deferInference, nexusEndpoint);
            }
        }
        Result<List<NormalizedJsonLd>> instancesByIdentifiers = coreSvc.getInstancesByIdentifiers(normalizedJsonLdDoc.getAllIdentifiersIncludingId());
        if (instancesByIdentifiers.getData() != null && !instancesByIdentifiers.getData().isEmpty()) {
            //The document already exists... we need to check if it exists in the current space...
            NormalizedJsonLd existingInstanceInSpace = null;
            for (NormalizedJsonLd instanceByIdentifier : instancesByIdentifiers.getData()) {
                String spaceOfInstance = instanceByIdentifier.getAs(EBRAINSVocabulary.META_SPACE, String.class);
                if (space.getName().equals(spaceOfInstance)) {
                    existingInstanceInSpace = instanceByIdentifier;
                    break;
                }
            }
            if (existingInstanceInSpace != null) {
                UUID uuidForContribution = idUtils.getUUID(existingInstanceInSpace.getId());
                coreSvc.replaceContribution(normalizedJsonLdDoc, uuidForContribution, userId, eventTime, deferInference);
                //We're done after replacing the contribution
                return;
            }
        }
        coreSvc.createInstance(normalizedJsonLdDoc, space, userId, eventTime, deferInference);
    }


    public void deleteInstance(String organization, String domain, String schema, String schemaVersion, String id, Integer rev, String authorId, String timestamp, String nexusEndpoint) {
        String userId = resolveUserId(authorId, organization);
        ZonedDateTime eventTime = timestamp != null ? ZonedDateTime.parse(timestamp) : null;
        String absoluteNexusUrl = payloadNormalizer.getAbsoluteNexusUrl(organization, domain, schema, schemaVersion, id, true, nexusEndpoint);
        Result<List<NormalizedJsonLd>> instanceByIdentifier = coreSvc.getInstancesByIdentifiers(Collections.singleton(absoluteNexusUrl));
        if (instanceByIdentifier != null && instanceByIdentifier.getData() != null) {

            instanceByIdentifier.getData().forEach(instance -> {
                if (instance.getTypes().contains(RELEASING_TYPE)) {
                    //It was a releasing instance - we unrelease the linked element...
                    JsonLdId releasedInstance = instance.getAs("https://schema.hbp.eu/release/instance", JsonLdId.class);
                    Result<List<NormalizedJsonLd>> releasedInstances = coreSvc.getInstancesByIdentifiers(Collections.singleton(releasedInstance.getId()));
                    if (releasedInstances != null && releasedInstances.getData() != null) {
                        releasedInstances.getData().forEach(r -> coreSvc.unreleaseInstance(idUtils.getUUID(r.getId())));
                    }
                }
                //The delete for Nexus is actually a replacement with an empty payload (it's a revert of the contribution in fact)...
                Result<NormalizedJsonLd> resultingDocument = coreSvc.replaceContribution(new JsonLdDoc(), idUtils.getUUID(instance.getId()), userId, eventTime, false);
            });
        }
    }

    private String resolveUserId(String userId, String organization) {
        //The expected author id looks like this: https://nexus-iam.humanbrainproject.org/v0/realms/HBP/users/305629
        String result;
        if (userId != null && userId.contains("realms/HBP/users")) {
            String[] split = userId.split("/");
            User userByMidId = coreSvc.getUserByMidId(split[split.length - 1]);
            result = userByMidId != null ? userByMidId.getNativeId() : userId;
        } else {
            result = "unknown";
        }
        return PayloadNormalizer.isInferred(organization) ? result + "-inference" : result;
    }
}

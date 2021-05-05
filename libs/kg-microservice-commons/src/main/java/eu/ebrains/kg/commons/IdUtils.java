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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.commons;

import eu.ebrains.kg.commons.jsonld.JsonLdId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * The id utils service is a helper bean to translate between different representations of the ids (internal / external). It allows to parse absolute identifiers into internal UUID and vice-versa.
 */
@Component
public class IdUtils {
    private final String namespace;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static UUID createMetaRepresentationUUID(String name){
        return UUID.nameUUIDFromBytes(("metaRepresentation" + name).getBytes(StandardCharsets.UTF_8));
    }

    public IdUtils(@Value("${eu.ebrains.kg.namespace}") String namespace) {
        this.namespace = namespace != null ? namespace.toLowerCase() : null;
    }

    /**
     * @return the absolute URL of an internal instance id
     */
    public JsonLdId buildAbsoluteUrl(UUID instanceId){
        return instanceId == null ? null : new JsonLdId(String.format("%s%s", namespace, instanceId));
    }

    /**
     * EBRAINS KG stores the source documents originating from different users separately. We therefore generate new ids for these user-specific representations by calculating a new UUID based on the concatenation of the user id and the instance id.
     *
     * @return a new, reproducible UUID (based on the user id and the instance id)
     */
    public UUID getDocumentIdForUserAndInstance(String userId, UUID instanceId){
        return UUID.nameUUIDFromBytes(String.format("%s-%s", instanceId, userId).getBytes(StandardCharsets.UTF_8));
    }

    public boolean isInternalId(String id){
        return id.startsWith(namespace);
    }

    /**
     * @return the UUID extracted from the given absolute id or null if the passed JsonLdId is not an internal id of the EBRAINS KG.
     */
    public UUID getUUID(JsonLdId jsonLdId) {
        if (jsonLdId != null && jsonLdId.getId() != null && isInternalId(jsonLdId.getId())) {
            try{
                return UUID.fromString(jsonLdId.getId().substring(namespace.length()));
            }
            catch (IllegalArgumentException ex){
                logger.warn(String.format("Was not able to extract a uuid from the id %s", jsonLdId.getId()));
                return null;
            }
        }
        return null;
    }
}

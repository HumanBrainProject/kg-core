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

package eu.ebrains.kg.commons;

import eu.ebrains.kg.commons.jsonld.JsonLdId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class IdUtils {
    private final String namespace;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public IdUtils(@Value("${eu.ebrains.kg.namespace}") String namespace) {
        this.namespace = namespace != null ? namespace.toLowerCase() : null;
    }

    public JsonLdId buildAbsoluteUrl(UUID instanceId){
        return instanceId == null ? null : new JsonLdId(String.format("%s%s", namespace, instanceId));
    }

    public UUID getDocumentIdForUserAndInstance(String userId, UUID instanceId){
        return UUID.nameUUIDFromBytes(String.format("%s-%s", instanceId, userId).getBytes(StandardCharsets.UTF_8));
    }

    public UUID getUUID(JsonLdId jsonLdId) {
        if (jsonLdId != null && jsonLdId.getId() != null && jsonLdId.getId().startsWith(namespace)) {
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

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

package eu.ebrains.kg.nexusv1.serviceCall;

import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.ExecutionException;

@Component
public class NexusSvc {

    @Autowired
    @Qualifier("external")
    WebClient.Builder nexusWebClient;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${eu.ebrains.kg.nexus.endpoint}")
    String nexusEndpoint;


    public JsonLdDoc getOrgInfo(String orgId, ClientAuthToken authToken) {
        WebClient.RequestHeadersSpec<?> spec = nexusWebClient.build().get()
                .uri(String.format("%s/%s/%s", nexusEndpoint, "orgs", orgId))
                .header("Authorization", authToken.getBearerToken())
                .accept(MediaType.APPLICATION_JSON);
        try {
            return spec.retrieve().bodyToMono(JsonLdDoc.class).toFuture().get();
        } catch (InterruptedException e) {
            logger.error(String.format("Could not request org info - %s", e.getMessage()));
        } catch (ExecutionException e) {
            logger.error(String.format("Could not request org info - %s", e.getMessage()));
        }
        return  null;
    }
}

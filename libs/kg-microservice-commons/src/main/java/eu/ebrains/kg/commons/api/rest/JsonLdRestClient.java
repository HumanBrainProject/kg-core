/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.commons.api.rest;

import eu.ebrains.kg.commons.AuthTokenContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.api.JsonLd;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@RestClient
public class JsonLdRestClient implements JsonLd.Client {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static String SERVICE_URL = "http://kg-jsonld/internal/jsonld";
    private final ServiceCall serviceCall;
    private final AuthTokenContext authTokenContext;

    public JsonLdRestClient(ServiceCall serviceCall, AuthTokenContext authTokenContext) {
        this.serviceCall = serviceCall;
        this.authTokenContext = authTokenContext;
    }

    @Override
    public NormalizedJsonLd normalize(JsonLdDoc payload, boolean keepNullValues) {
        return serviceCall.post(SERVICE_URL,
                payload,
                authTokenContext.getAuthTokens(),
                NormalizedJsonLd.class);
    }

    @Override
    public List<JsonLdDoc> applyVocab(List<NormalizedJsonLd> documents, String vocab) {
        JsonLdDoc[] result = serviceCall.post(String.format("%s/withVocab?vocab=%s", SERVICE_URL, URLEncoder.encode(vocab, StandardCharsets.UTF_8)),
                documents,
                authTokenContext.getAuthTokens(),
                JsonLdDoc[].class);
        return result!=null ? Arrays.asList(result): null;
    }
}

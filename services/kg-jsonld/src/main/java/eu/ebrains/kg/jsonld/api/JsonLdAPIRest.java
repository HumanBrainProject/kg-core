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

package eu.ebrains.kg.jsonld.api;

import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/jsonld")
public class JsonLdAPIRest implements eu.ebrains.kg.commons.api.JsonLd {

    private final JsonLdAPI jsonLdAPI;

    public JsonLdAPIRest(JsonLdAPI jsonLdAPI) {
        this.jsonLdAPI = jsonLdAPI;
    }

    @PostMapping
    public NormalizedJsonLd normalize(@RequestBody JsonLdDoc payload, @RequestParam(value = "keepNullValues", required = false, defaultValue = "true") boolean keepNullValues) {
        return this.jsonLdAPI.normalize(payload, keepNullValues);
    }

    @PostMapping("/withVocab")
    public List<Map<?,?>> applyVocab(@RequestBody List<NormalizedJsonLd> documents, @RequestParam(value = "vocab") String vocab) {
        return this.jsonLdAPI.applyVocab(documents, URLDecoder.decode(vocab, StandardCharsets.UTF_8));
    }

}

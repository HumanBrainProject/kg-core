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

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.config.openApiGroups.Simple;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesInputWithoutEnrichedSensitiveData;
import eu.ebrains.kg.core.serviceCall.CoreToJsonLd;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The spaces API provides information about existing KG spaces
 */
@RestController
@RequestMapping(Version.API + "/jsonld")
public class JsonLD {
    private final CoreToJsonLd coreToJsonLd;

    public JsonLD(CoreToJsonLd coreToJsonLd) {
        this.coreToJsonLd = coreToJsonLd;
    }

    @Operation(summary = "Normalizes the passed payload according to the EBRAINS KG conventions")
    @PostMapping("/normalizedPayload")
    @ExposesInputWithoutEnrichedSensitiveData
    @Simple
    public NormalizedJsonLd normalizePayload(@RequestBody JsonLdDoc payload) {
        return coreToJsonLd.toNormalizedJsonLd(payload);
    }

}
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

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.core.controller.InferenceController;
import eu.ebrains.kg.core.serviceCall.JsonLdSvc;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * Extra operations are exposing specific functionality for closely integrated clients (such as adapters).
 */
@RestController
@RequestMapping(Version.API)
public class ExtraOperations {
    private final JsonLdSvc jsonLdSvc;
    private final AuthContext authContext;
    private final InferenceController inferenceController;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ExtraOperations(JsonLdSvc jsonLdSvc, AuthContext authContext, InferenceController inferenceController) {
        this.jsonLdSvc = jsonLdSvc;
        this.authContext = authContext;
        this.inferenceController = inferenceController;
    }

    @ApiOperation("Triggers the inference of all documents of the given space")
    @PostMapping("/extra/inference/{space}")
    public void triggerInference(@PathVariable(value = "space") String space, @RequestParam(value = "identifier", required = false) String identifier, @RequestParam(value = "async", required = false, defaultValue = "false") boolean  async) {
        if(async) {
            inferenceController.asyncTriggerInference(new Space(space), identifier, authContext.getAuthTokens());
        }
        else {
            inferenceController.triggerInference(new Space(space), identifier, authContext.getAuthTokens());
        }
    }

    @ApiOperation("Triggers the inference of all documents which have been tagged to be deferred as part of their creation/contribution")
    @PostMapping("/extra/inference/deferred/all")
    public void triggerDeferredInference() {
        inferenceController.triggerDeferredInference(authContext.getAuthTokens());
    }

    @ApiOperation("Normalizes the passed payload according to the EBRAINS KG conventions")
    @PostMapping("/extra/normalizedPayload")
    public NormalizedJsonLd normalizePayload(@RequestBody JsonLdDoc payload) {
        return jsonLdSvc.toNormalizedJsonLd(payload);
    }

}

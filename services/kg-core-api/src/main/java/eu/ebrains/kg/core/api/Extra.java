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
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.core.controller.CoreInferenceController;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.core.serviceCall.*;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Extra operations are exposing specific functionality for closely integrated clients (such as adapters).
 */
@RestController
@RequestMapping(Version.API)
public class Extra {
    private final CoreToJsonLd coreToJsonLd;
    private final AuthContext authContext;
    private final CoreInferenceController inferenceController;
    private final CoreToIds idsSvc;
    private final CoreExtraToGraphDB graphDB4ExtraSvc;
    private final CoreToAuthentication authenticationSvc;
    private final CoreToAdmin coreToAdmin;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Extra(CoreToJsonLd coreToJsonLd, AuthContext authContext, CoreInferenceController inferenceController, CoreToIds idsSvc, CoreExtraToGraphDB graphDB4ExtraSvc, CoreToAuthentication authenticationSvc, CoreToAdmin coreToAdmin) {
        this.coreToJsonLd = coreToJsonLd;
        this.authContext = authContext;
        this.inferenceController = inferenceController;
        this.idsSvc = idsSvc;
        this.graphDB4ExtraSvc = graphDB4ExtraSvc;
        this.authenticationSvc = authenticationSvc;
        this.coreToAdmin = coreToAdmin;
    }

    @Operation(summary = "Triggers the inference of all documents of the given space")
    @PostMapping("/extra/inference/{space}")
    public void triggerInference(@PathVariable(value = "space") String space, @RequestParam(value = "identifier", required = false) String identifier, @RequestParam(value = "async", required = false, defaultValue = "false") boolean async) {
        if (async) {
            inferenceController.asyncTriggerInference(new SpaceName(space), identifier, authContext.getAuthTokens());
        } else {
            inferenceController.triggerInference(new SpaceName(space), identifier, authContext.getAuthTokens());
        }
    }

    @Operation(summary = "Triggers the inference of all documents which have been tagged to be deferred as part of their creation/contribution")
    @PostMapping("/extra/inference/deferred/{space}")
    public void triggerDeferredInference(@RequestParam(value = "sync", required = false, defaultValue = "false") boolean sync, @PathVariable(value = "space") String space) {
        inferenceController.triggerDeferredInference(authContext.getAuthTokens(), new SpaceName(space), sync);
    }

    @Operation(summary = "Normalizes the passed payload according to the EBRAINS KG conventions")
    @PostMapping("/extra/normalizedPayload")
    public NormalizedJsonLd normalizePayload(@RequestBody JsonLdDoc payload) {
        return coreToJsonLd.toNormalizedJsonLd(payload);
    }

    @Operation(summary = "Returns suggestions for an instance to be linked by the given property (e.g. for the KG Editor)")
    @GetMapping("/extra/instances/{id}/suggestedLinksForProperty")
    public Result<SuggestionResult> getSuggestedLinksForProperty(@RequestParam("stage") ExposedStage stage, @PathVariable("id") UUID id, @RequestParam(value = "property") String propertyName, @RequestParam(value = "type", required = false) String type, @RequestParam(value = "search", required = false) String search, @ParameterObject PaginationParam paginationParam) {
        return getSuggestedLinksForProperty(null, stage, propertyName, id, type, search, paginationParam);
    }

    @Operation(summary = "Returns suggestions for an instance to be linked by the given property (e.g. for the KG Editor) - and takes into account the passed payload (already chosen values, reflection on dependencies between properties - e.g. providing only parcellations for an already chosen brain atlas)")
    @PostMapping("/extra/instances/{id}/suggestedLinksForProperty")
    public Result<SuggestionResult> getSuggestedLinksForProperty(@RequestBody NormalizedJsonLd payload, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "property") String propertyName, @PathVariable("id") UUID id, @RequestParam(value = "type", required = false) String type, @RequestParam(value = "search", required = false) String search, @ParameterObject PaginationParam paginationParam) {
        InstanceId instanceId = idsSvc.resolveId(DataStage.IN_PROGRESS, id);
        return Result.ok(graphDB4ExtraSvc.getSuggestedLinksForProperty(payload, stage.getStage(), instanceId, id, propertyName, type != null && !type.isBlank() ? new Type(type) : null, search, paginationParam, authContext.getAuthTokens()));
    }

    @Operation(summary = "Retrieve user information based on a keycloak attribute (excluding detailed information such as e-mail address)")
    @GetMapping("/extra/users/byAttribute/{attribute}/{value}")
    public ResponseEntity<List<User>> getUsersByAttribute(@PathVariable("attribute") String attribute, @PathVariable("value") String value) {
        List<User> users = authenticationSvc.getUsersByAttribute(attribute, value);
        return users != null ? ResponseEntity.ok(users) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Register a client in EBRAINS KG")
    @PutMapping("/extra/clients/{id}")
    public ResponseEntity<String> addClient(@PathVariable("id") String id) {
        coreToAdmin.addClient(id);
        return ResponseEntity.ok(String.format("Successfully inserted the client with id %s", id));
    }


    @Operation(summary = "Define a space")
    @PutMapping("/extra/spaces/{space}")
    public Result<NormalizedJsonLd> defineSpace(@PathVariable(value = "space") String space, @RequestParam("autorelease") boolean autoRelease) {
        return Result.ok(coreToAdmin.addSpace(space, autoRelease).toJsonLd());
    }
}

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
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.core.controller.CoreInferenceController;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

/**
 * The spaces API provides information about existing KG spaces
 */
@RestController
@RequestMapping(Version.API + "/inference")
@Admin
public class Inference {
    private final CoreInferenceController inferenceController;
    private final AuthContext authContext;

    public Inference(CoreInferenceController inferenceController, AuthContext authContext) {
        this.inferenceController = inferenceController;
        this.authContext = authContext;
    }

    @Operation(summary = "Triggers the inference of all documents of the given space")
    @PostMapping("/{space}")
    public void triggerInference(@PathVariable(value = "space") String space, @RequestParam(value = "identifier", required = false) String identifier, @RequestParam(value = "async", required = false, defaultValue = "false") boolean async) {
        SpaceName spaceName = authContext.resolveSpaceName(space);
        if (async) {
            inferenceController.asyncTriggerInference(spaceName, identifier);
        } else {
            inferenceController.triggerInference(spaceName, identifier);
        }
    }

}

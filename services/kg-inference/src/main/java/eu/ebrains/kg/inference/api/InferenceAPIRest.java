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

package eu.ebrains.kg.inference.api;

import eu.ebrains.kg.commons.api.Inference;
import eu.ebrains.kg.commons.model.Event;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/internal/inference")
public class InferenceAPIRest implements Inference {

    private final InferenceAPI inferenceAPI;

    public InferenceAPIRest(InferenceAPI inferenceAPI) {
        this.inferenceAPI = inferenceAPI;
    }

    @GetMapping("/{space}/{id}")
    public List<Event> infer(@PathVariable("space") String space, @PathVariable("id") UUID id) {
        return this.inferenceAPI.infer(space, id);
    }

}

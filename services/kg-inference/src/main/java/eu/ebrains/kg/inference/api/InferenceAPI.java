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
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.inference.controller.Reconcile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class InferenceAPI implements Inference.Client {

    private final Reconcile reconcile;

    public InferenceAPI(Reconcile reconcile) {
        this.reconcile = reconcile;
    }

    @Override
    public List<Event> infer(String space, UUID id) {
        //Something happened (insert / update / delete) to the native document referenced by the id.
        return this.reconcile.reconcile(new SpaceName(space), id);
    }

}

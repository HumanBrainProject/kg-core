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

package eu.ebrains.kg.inference.api;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.inference.controller.Reconcile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/inference")
public class Inference {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    ;

    @Autowired
    Reconcile reconcile;

    @Autowired
    IdUtils idUtils;

    @GetMapping("/{space}/{id}")
    public List<Event> infer(@PathVariable("space") String space, @PathVariable("id") UUID id) {
        //Something happened (insert / update / delete) to the native document referenced by the id.
        return this.reconcile.reconcile(new Space(space), id);
    }

}

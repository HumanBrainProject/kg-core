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

package eu.ebrains.kg.graphdb.scopes.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.model.ScopeElement;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.graphdb.scopes.controller.ScopesController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/graphdb/scopes")
public class GraphDBScopesAPI {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AuthContext authContext;
    private final ScopesController scopesController;

    public GraphDBScopesAPI(AuthContext authContext, ScopesController scopesController) {
        this.scopesController = scopesController;
        this.authContext = authContext;
    }


    @GetMapping("/{space}/{id}")
    public ScopeElement getScopeForInstance(@PathVariable("space") String space, @PathVariable("id") UUID id){
       return this.scopesController.getScopeForInstance(new Space(space), id);
    }

}

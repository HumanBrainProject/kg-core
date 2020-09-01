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

package eu.ebrains.kg.graphdb.scopes.controller;

import eu.ebrains.kg.commons.model.ScopeElement;
import eu.ebrains.kg.commons.model.Space;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScopesController {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ScopeElement getScopeForInstance(Space space, UUID id){
        //fetch all relevant queries

        //simplify queries

        return null;
    }

}

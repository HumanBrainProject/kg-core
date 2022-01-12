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

package eu.ebrains.kg.graphdb.instances.api;

import eu.ebrains.kg.commons.api.GraphDBScopes;
import eu.ebrains.kg.commons.markers.ExposesMinimalData;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.ScopeElement;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.graphdb.instances.controller.ArangoRepositoryInstances;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GraphDBScopesAPI implements GraphDBScopes.Client {

    private final ArangoRepositoryInstances repository;

    public GraphDBScopesAPI(ArangoRepositoryInstances repository) {
        this.repository = repository;
    }

    @Override
    @ExposesMinimalData
    public ScopeElement getScopeForInstance(String space, UUID id, DataStage stage, boolean applyRestrictions){
       return this.repository.getScopeForInstance(new SpaceName(space), id, stage, applyRestrictions);
    }

}

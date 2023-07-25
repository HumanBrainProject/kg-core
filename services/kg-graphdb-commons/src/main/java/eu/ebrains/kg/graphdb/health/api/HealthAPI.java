/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

package eu.ebrains.kg.graphdb.health.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.api.GraphDBHealth;
import eu.ebrains.kg.commons.exception.UnauthorizedException;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import eu.ebrains.kg.graphdb.health.controller.RelationConsistency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HealthAPI implements GraphDBHealth.Client {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AuthContext authContext;
    private final RelationConsistency relationConsistency;
    private final Permissions permissions;

    public HealthAPI(AuthContext authContext, RelationConsistency relationConsistency, Permissions permissions) {
        this.authContext = authContext;
        this.relationConsistency = relationConsistency;
        this.permissions = permissions;
    }

    public Map<String, List<String>> healthStatus(){
        logger.info("Checking health status of DB");
        if(!permissions.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.CHECK_HEALTH_STATUS)){
            throw new UnauthorizedException("The current user doesn't have the rights to run the health status check");
        }
        Map<String, List<String>> resultCollector = new HashMap<>();
        relationConsistency.checkRelationConsistency(DataStage.RELEASED, resultCollector);
        return resultCollector;
    }

}

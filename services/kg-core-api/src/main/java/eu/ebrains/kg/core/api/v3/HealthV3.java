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

package eu.ebrains.kg.core.api.v3;

import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.GraphDBHealth;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(Version.V3 +"/health")
public class HealthV3{

    private final GraphDBHealth.Client graphDBHealth;

    public HealthV3(GraphDBHealth.Client graphDBHealth) {
        this.graphDBHealth = graphDBHealth;
    }


    @Admin
    @GetMapping
    public void healthStatus(){
        graphDBHealth.healthStatus();
    }

}

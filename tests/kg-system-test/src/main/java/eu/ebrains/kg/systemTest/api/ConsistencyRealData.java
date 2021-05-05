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

package eu.ebrains.kg.systemTest.api;

import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.systemTest.controller.consistency4real.InternalTypeInstanceComparison;
import eu.ebrains.kg.systemTest.controller.consistency4real.PropertiesComparison;
import eu.ebrains.kg.systemTest.model.ComparisonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tests/consistencyOfRealData")
public class ConsistencyRealData {

    private final InternalTypeInstanceComparison internalTypeInstanceComparison;
    private final PropertiesComparison propertiesComparison;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ConsistencyRealData(InternalTypeInstanceComparison internalTypeInstanceComparison, PropertiesComparison propertiesComparison) {
        this.internalTypeInstanceComparison = internalTypeInstanceComparison;
        this.propertiesComparison = propertiesComparison;
    }


    @GetMapping("typesAndInstances")
    public Map<String, ComparisonResult<Long>> compareStatsBetweenTypeAndInstancesAPIs(@RequestParam(defaultValue = "false") boolean failingOnly, @RequestParam(defaultValue = "false") boolean analyzeFailing){
        return internalTypeInstanceComparison.compareTypesWithInstances(DataStage.IN_PROGRESS, failingOnly, analyzeFailing);
    }

    @GetMapping("properties")
    public Map<String, Map<String, ComparisonResult<Long>>> checkProperties(@RequestParam(defaultValue = "0") int from, @RequestParam(required = false) Integer size,  @RequestParam(defaultValue = "false") boolean unsuccessfulOnly) {
        return propertiesComparison.compareProperties(DataStage.IN_PROGRESS, unsuccessfulOnly, from ,size);
    }

    @PostMapping("propertiesByType")
    public Map<String, Map<String, ComparisonResult<Long>>> checkPropertiesByType(@RequestBody List<String> listOfTypeNames, @RequestParam(defaultValue = "false") boolean unsuccessfulOnly) {
        return propertiesComparison.comparePropertiesByType(DataStage.IN_PROGRESS, unsuccessfulOnly, listOfTypeNames);
    }

}

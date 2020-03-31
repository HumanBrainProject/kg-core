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

package eu.ebrains.kg.systemTest.api;

import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Type;
import eu.ebrains.kg.systemTest.controller.consistency4real.InternalTypeInstanceComparison;
import eu.ebrains.kg.systemTest.controller.investigation.InstanceHistory;
import eu.ebrains.kg.systemTest.model.ComparisonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/tests/investigation")
public class Investigation {

    private final InternalTypeInstanceComparison internalTypeInstanceComparison;
    private final InstanceHistory instanceHistory;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Investigation(InternalTypeInstanceComparison internalTypeInstanceComparison, InstanceHistory instanceHistory) {
        this.internalTypeInstanceComparison = internalTypeInstanceComparison;
        this.instanceHistory = instanceHistory;
    }

    @GetMapping("instance/{id}")
    public List<NormalizedJsonLd> getHistoryOfInstance(@PathVariable("id") UUID instanceId){
        return instanceHistory.loadHistoryOfInstance(instanceId);
    }

    @GetMapping("analyzeType")
    public Map<String, Set<UUID>> analyzeType(@RequestParam String type){
        return internalTypeInstanceComparison.analyzeType(DataStage.LIVE, new Type(type));
    }

    @PutMapping("selfHeal")
    public Map<String, Set<UUID>> selfHeal(@RequestParam String type){
        return internalTypeInstanceComparison.selfHeal(DataStage.LIVE, new Type(type));
    }

    @PutMapping("selfHealAll")
    public void selfHealAll(){
        Map<String, ComparisonResult<Long>> failingTypes = internalTypeInstanceComparison.compareTypesWithInstances(DataStage.LIVE, true, false);
        failingTypes.keySet().forEach(type -> {
            internalTypeInstanceComparison.selfHeal(DataStage.LIVE, new Type(type));
        });
    }
}

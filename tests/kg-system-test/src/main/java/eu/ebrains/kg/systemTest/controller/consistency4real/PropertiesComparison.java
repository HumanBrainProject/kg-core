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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.systemTest.controller.consistency4real;

import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.PaginatedResult;
import eu.ebrains.kg.commons.model.Type;
import eu.ebrains.kg.systemTest.model.ComparisonResult;
import eu.ebrains.kg.systemTest.serviceCall.SystemTestToCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PropertiesComparison {
    private final SystemTestToCore coreSvc;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final List<String> PROPERTIES_TO_SKIP = Arrays.asList("https://kg.ebrains.eu/vocab/meta/alternative", "https://kg.ebrains.eu/vocab/meta/revision");

    public PropertiesComparison(SystemTestToCore coreSvc) {
        this.coreSvc = coreSvc;
    }

    public  Map<String, Map<String, ComparisonResult<Long>>> comparePropertiesByType(DataStage stage, boolean unsuccessfulOnly, List<String> listOfTypes) {
        Map<String, Map<String, ComparisonResult<Long>>> result = new HashMap<>();
        Map<Type, Map<String, Long>> typesFilteredByOccurences = coreSvc.getTypesFilteredByOccurencesByType(stage, listOfTypes);

        getComparedProperties(stage, unsuccessfulOnly, result, typesFilteredByOccurences);
        return result;
    }

    public Map<String, Map<String, ComparisonResult<Long>>> compareProperties(DataStage stage, boolean unsuccessfulOnly, int from, Integer size) {
        Map<String, Map<String, ComparisonResult<Long>>> result = new HashMap<>();
        Map<Type, Map<String, Long>> typesFilteredByOccurences = coreSvc.getTypesFilteredByOccurences(stage, from, size);
        getComparedProperties(stage, unsuccessfulOnly, result, typesFilteredByOccurences);
        return result;
    }

    private void getComparedProperties(DataStage stage, boolean unsuccessfulOnly, Map<String, Map<String, ComparisonResult<Long>>> result, Map<Type, Map<String, Long>> typesFilteredByOccurences) {
        for (Map.Entry<Type, Map<String, Long>> entry : typesFilteredByOccurences.entrySet()) {
            Type type = entry.getKey();
            Map<String, Long> typeProperties = entry.getValue();
            Set<String> propertyNames = new HashSet<>(typeProperties.keySet());

            Map<String, Long> propertiesCount = new HashMap<>();
            PaginatedResult<NormalizedJsonLd> instances = coreSvc.getInstances(type, stage);

            instances.getData().forEach(instance -> {
                for (Map.Entry<String, ?> props : instance.entrySet()) {
                    String propertyName = props.getKey();
                    propertyNames.add(propertyName);
                    if (propertiesCount.containsKey(propertyName)) {
                        propertiesCount.put(propertyName, propertiesCount.get(propertyName) + 1);
                    } else {
                        propertiesCount.put(propertyName, 1l);
                    }
                }
            });

            Map<String, ComparisonResult<Long>> propertyComparisonResult = new HashMap<>();
            result.put(type.getName(), propertyComparisonResult);

            propertyNames.forEach(name -> {
                if (!PROPERTIES_TO_SKIP.contains(name)) {
                    ComparisonResult<Long> r = new ComparisonResult<>();
                    Long actualValue = typeProperties.get(name);
                    Long expectedValue = propertiesCount.get(name);
                    r.setActualValue(actualValue);
                    r.setExpectedValue(expectedValue);
                    if (actualValue != null && expectedValue != null) {
                        r.setCorrect(r.getActualValue().equals(r.getExpectedValue()));
                    } else {
                        r.setCorrect(false);
                    }
                    if (!unsuccessfulOnly || !r.isCorrect()) {
                        propertyComparisonResult.put(name, r);
                    }
                }

            });
        }
    }
}

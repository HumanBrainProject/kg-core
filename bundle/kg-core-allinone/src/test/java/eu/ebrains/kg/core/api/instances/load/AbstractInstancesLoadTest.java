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

package eu.ebrains.kg.core.api.instances.load;

import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.IngestConfiguration;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.core.api.Inference;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.metrics.PerformanceTestUtils;
import eu.ebrains.kg.testutils.AbstractLoadTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class AbstractInstancesLoadTest extends AbstractLoadTest {

    @Autowired
    protected Instances instances;

    @Autowired
    protected Inference inference;


    // INSERTION
    protected void testInsert(int numberOfFields, int numberOfIterations, boolean parallelize, boolean deferInference, boolean normalize, PerformanceTestUtils.Link link) throws IOException {
        StringBuilder title = new StringBuilder();
        switch(numberOfFields){
            case smallPayload:
                title.append("Small");
                break;
            case averagePayload:
                title.append("Average");
                break;
            case bigPayload:
                title.append("Big");
                break;
            default:
                title.append(String.format("%d fields", numberOfFields));
                break;
        }
        title.append(", ");
        title.append(link == null ? "no link" : link == PerformanceTestUtils.Link.PREVIOUS ? "immediate link" : "deferred link");
        title.append(", ");
        title.append(deferInference ? "deferred / async" : "sync");
        title.append(", ");
        title.append(normalize ? "normalization" : "no normalization");
        utils.addSection(title.toString());

        // When
        List<ResponseEntity<Result<NormalizedJsonLd>>> results = utils.executeMany(numberOfFields, normalize, numberOfIterations, parallelize, link, p -> instances.createNewInstance(p, "test", DEFAULT_RESPONSE_CONFIG, new IngestConfiguration().setNormalizePayload(normalize).setDeferInference(deferInference), null));

        //Then
        for (int i = 0; i < results.size(); i++) {
            System.out.printf("Result %d: %d ms%n", i, Objects.requireNonNull(results.get(i).getBody()).getDurationInMs());
        }

        if (deferInference) {
            triggerDeferredInference();
        }
    }

    protected List<NormalizedJsonLd> getAllInstancesFromInProgress(ExposedStage stage) {
        return this.instances.getInstances(stage, type, null, null, null, null, DEFAULT_RESPONSE_CONFIG, EMPTY_PAGINATION).getData();
    }

    protected void triggerDeferredInference() {
        Instant start = Instant.now();
        System.out.println("Trigger inference");
        inference.triggerDeferredInference(true, "test");
        Instant end = Instant.now();
        System.out.printf("Inference handled in %d ms%n", Duration.between(start, end).toMillis());
    }

}

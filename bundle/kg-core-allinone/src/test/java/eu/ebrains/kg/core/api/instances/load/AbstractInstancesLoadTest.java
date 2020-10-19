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

package eu.ebrains.kg.core.api.instances.load;

import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.IngestConfiguration;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.core.api.Extra;
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
    protected Extra extra;


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
        List<ResponseEntity<Result<NormalizedJsonLd>>> results = utils.executeMany(numberOfFields, normalize, numberOfIterations, parallelize, link, p -> instances.createNewInstance(p, "test", DEFAULT_RESPONSE_CONFIG, new IngestConfiguration().setNormalizePayload(normalize).setDeferInference(deferInference), null), authTokens);

        //Then
        for (int i = 0; i < results.size(); i++) {
            System.out.printf("Result %d: %d ms%n", i, Objects.requireNonNull(results.get(i).getBody()).getDurationInMs());
        }

        if (deferInference) {
            triggerDeferredInference();
        }
    }

    protected List<NormalizedJsonLd> getAllInstancesFromInProgress(ExposedStage stage) {
        return this.instances.getInstances(stage, type, null, null, DEFAULT_RESPONSE_CONFIG, EMPTY_PAGINATION).getData();
    }

    protected void triggerDeferredInference() {
        Instant start = Instant.now();
        System.out.println("Trigger inference");
        extra.triggerDeferredInference(true, "test");
        Instant end = Instant.now();
        System.out.printf("Inference handled in %d ms%n", Duration.between(start, end).toMillis());
    }

}

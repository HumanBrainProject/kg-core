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

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.AbstractSystemTest;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DeleteSystemTest extends AbstractSystemTest {

    @Autowired
    private IdUtils idUtils;

    @Test
    public void testDeleteSingleAverageNoLink() throws IOException {
        //Given
        testInsert(averagePayload, 1, false, false, false, null);
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);
        //When
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            ResponseEntity<Result<Void>> resultResponseEntity = instances.deleteInstance(idUtils.getUUID(allInstancesFromInProgress.get(i).getId()), null);
            System.out.printf("Result %d: %d ms%n", i, resultResponseEntity.getBody().getDurationInMs());
        }
    }

    @Test
    public void testDeleteSingleAverageNoLinkFullParallelism() throws IOException, InterruptedException {
        //Given
        testInsert(averagePayload, 1, false, false, false, null);
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);

        //When
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            int finalI = i;
            executorService.execute(() -> {
                ResponseEntity<Result<Void>> resultResponseEntity = instances.deleteInstance(idUtils.getUUID(allInstancesFromInProgress.get(finalI).getId()), null);
                System.out.printf("Result %d: %d ms%n", finalI, resultResponseEntity.getBody().getDurationInMs());
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.HOURS);
    }

}

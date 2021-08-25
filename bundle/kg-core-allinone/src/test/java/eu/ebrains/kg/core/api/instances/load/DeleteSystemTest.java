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

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.core.model.ExposedStage;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DeleteSystemTest extends AbstractInstancesLoadTest {

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
            ResponseEntity<Result<Void>> resultResponseEntity = instances.deleteInstance(idUtils.getUUID(allInstancesFromInProgress.get(i).id()));
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
                ResponseEntity<Result<Void>> resultResponseEntity = instances.deleteInstance(idUtils.getUUID(allInstancesFromInProgress.get(finalI).id()));
                System.out.printf("Result %d: %d ms%n", finalI, resultResponseEntity.getBody().getDurationInMs());
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.HOURS);
    }

}

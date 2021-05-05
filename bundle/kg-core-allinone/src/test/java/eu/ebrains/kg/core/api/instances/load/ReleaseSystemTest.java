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
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.TestDataFactory;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ReleaseSystemTest extends AbstractInstancesLoadTest {

    @Autowired
    private Instances instances;

    @Autowired
    private IdUtils idUtils;

    // RELEASING
    @Test
    public void testReleaseSingleAverageNoLink() throws IOException {
        //Given
        testInsert(averagePayload, 1, false, false, false, null);
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);
        //When
        long startTime = new Date().getTime();
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            IndexedJsonLdDoc from = IndexedJsonLdDoc.from(allInstancesFromInProgress.get(i));
            ResponseEntity<Result<Void>> resultResponseEntity = instances.releaseInstance(idUtils.getUUID(allInstancesFromInProgress.get(i).id()), from.getRevision());
            System.out.printf("Result %d: %d ms%n", i, resultResponseEntity.getBody().getDurationInMs());
        }
        System.out.printf("Total time for %d releases: %d ms%n", batchInsertion, new Date().getTime() - startTime);
    }

    @Test
    public void testReleaseSingleAverageNoLinkfullParallelism() throws IOException, InterruptedException {
        //Given
        testInsert(averagePayload, 1, false, false, false, null);
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);

        //When
        long startTime = new Date().getTime();
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            int finalI = i;
            executorService.execute(() -> {
                IndexedJsonLdDoc from = IndexedJsonLdDoc.from(allInstancesFromInProgress.get(finalI));
                ResponseEntity<Result<Void>> resultResponseEntity = instances.releaseInstance(idUtils.getUUID(allInstancesFromInProgress.get(finalI).id()), from.getRevision());
                System.out.printf("Result %d: %d ms%n", finalI, resultResponseEntity.getBody().getDurationInMs());
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.HOURS);
        System.out.printf("Total time for %d releases: %d ms%n", batchInsertion, new Date().getTime() - startTime);
    }


    @Test
    public void testReleaseSingleBigNoLink() {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(bigPayload, true, 0, null);
        List<ResponseEntity<Result<NormalizedJsonLd>>> l = new ArrayList<>();
        for (int i = 0; i < smallBatchInsertion; i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);
            l.add(instance);
        }

        //When
        long startTime = new Date().getTime();
        for (int i = 0; i < l.size(); i++) {
            JsonLdId id = l.get(i).getBody().getData().id();
            IndexedJsonLdDoc from = IndexedJsonLdDoc.from(l.get(i).getBody().getData());
            ResponseEntity<Result<Void>> resultResponseEntity = instances.releaseInstance(idUtils.getUUID(id), from.getRevision());
            System.out.println(String.format("Result %d: %d ms", i, resultResponseEntity.getBody().getDurationInMs()));
        }
        System.out.println(String.format("Total time for %d releases: %d ms", batchInsertion, new Date().getTime() - startTime));
    }

    @Test
    public void testUnReleaseSingleAverageNoLink() {
        //Given
        TestDataFactory.createTestData(averagePayload, true, 0, null);
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);
        //When
        long startTime = new Date().getTime();
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            ResponseEntity<Result<Void>> resultResponseEntity = instances.unreleaseInstance(idUtils.getUUID(allInstancesFromInProgress.get(i).id()));
            System.out.println(String.format("Result %d: %d ms", i, resultResponseEntity.getBody().getDurationInMs()));
        }
        System.out.println(String.format("Total time for %d unreleases: %d ms", batchInsertion, new Date().getTime() - startTime));
    }

}

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

package eu.ebrains.kg.core.apiCurrent.instances.load;

import eu.ebrains.kg.metrics.PerformanceTestUtils;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;


@TestPropertySource(properties = {"logging.level.eu.ebrains.kg=WARN"})
public class InsertSystemTest extends AbstractInstancesLoadTest {

    @Test
    public void testInsertSmallNoLink() throws IOException {
        testInsert(smallPayload, batchInsertion, true,  false, null);
    }

    @Test
    public void testInsertSmallImmediateLink() throws IOException {
        testInsert(smallPayload, batchInsertion, true,  false, PerformanceTestUtils.Link.PREVIOUS);
    }

    @Test
    public void testInsertSmallNoLinkNormalize() throws IOException {
        testInsert(smallPayload, batchInsertion, true,  true, null);
    }

    @Test
    public void testInsertAverageNoLink() throws IOException {
        testInsert(averagePayload, batchInsertion, true,  false, null);
    }

    @Test
    public void testInsertAverageNoLinkNormalize() throws IOException {
        testInsert(averagePayload, batchInsertion, true,  true, null);
    }


    @Test
    public void testInsertBigNoLinkInference() throws IOException {
        testInsert(bigPayload, batchInsertion, true,  false, null);
    }

    @Test
    public void testInsertBigNoLinkInferenceNormalize() throws IOException {
        testInsert(bigPayload, batchInsertion, true, true, null);
    }


}

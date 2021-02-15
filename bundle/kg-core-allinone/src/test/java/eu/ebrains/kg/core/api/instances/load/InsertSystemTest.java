/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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

import eu.ebrains.kg.metrics.PerformanceTestUtils;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;


@TestPropertySource(properties = {"logging.level.eu.ebrains.kg=WARN"})
public class InsertSystemTest extends AbstractInstancesLoadTest {

    @Test
    public void testInsertSmallNoLink() throws IOException {
        testInsert(smallPayload, batchInsertion, true, false, false, null);
    }

    @Test
    public void testInsertSmallImmediateLink() throws IOException {
        testInsert(smallPayload, batchInsertion, true, false, false, PerformanceTestUtils.Link.PREVIOUS);
    }

    @Test
    public void testInsertSmallDeferredLink() throws IOException {
        testInsert(smallPayload, batchInsertion, true, false, false, PerformanceTestUtils.Link.NEXT);
    }

    @Test
    public void testInsertNoLinkDeferred() throws IOException {
        testInsert(smallPayload, batchInsertion, true, true, false, null);
    }

    @Test
    public void testInsertSmallNoLinkNormalize() throws IOException {
        testInsert(smallPayload, batchInsertion, true, false, true, null);
    }

    @Test
    public void testInsertSmallDeferredLinkNormalize() throws IOException {
        testInsert(smallPayload, batchInsertion, true, false, true, PerformanceTestUtils.Link.NEXT);
    }

    @Test
    public void testInsertSmallNoLinkDeferredNormalize() throws IOException {
        testInsert(smallPayload, batchInsertion, true, true, true, null);
    }

    @Test
    public void testInsertAverageNoLink() throws IOException {
        testInsert(averagePayload, batchInsertion, true, false, false, null);
    }

    @Test
    public void testInsertAverageNoLinkDeferred() throws IOException {
        testInsert(averagePayload, batchInsertion, true, true, false, null);
    }

    @Test
    public void testInsertAverageNoLinkNormalize() throws IOException {
        testInsert(averagePayload, batchInsertion, true, false, true, null);
    }

    @Test
    public void testInsertAverageNoLinkDeferredNormalize() throws IOException {
        testInsert(averagePayload, batchInsertion, true, true, true, null);
    }

    @Test
    public void testInsertBigNoLinkInference() throws IOException {
        testInsert(bigPayload, batchInsertion, true, false, false, null);
    }

    @Test
    public void testInsertBigNoLinkInferenceDeferred() throws IOException {
        testInsert(bigPayload, batchInsertion, true, true, false, null);
    }

    @Test
    public void testInsertBigNoLinkInferenceNormalize() throws IOException {
        testInsert(bigPayload, batchInsertion, true, false, true, null);
    }

    @Test
    public void testInsertBigNoLinkInferenceDeferredNormalize() throws IOException {
        testInsert(bigPayload, batchInsertion, true, true, true, null);
    }

}

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

@TestPropertySource(properties = {"eu.ebrains.kg.metrics=true", "logging.level.eu.ebrains.kg=WARN"})
public class InsertMethodInspectionTest extends AbstractInstancesLoadTest {

    @Test
    public void methodInspectionInsertSmallNoLink() throws IOException {
        testInsert(smallPayload, batchInsertion, false, false, false, null);

    }

    @Test
    public void methodInspectionInsertSmallNoLinkNormalize() throws IOException {
        testInsert(smallPayload, batchInsertion, false, false, true, null);

    }

    @Test
    public void methodInspectionInsertAverageImmediateLink() throws IOException {
        testInsert(averagePayload, batchInsertion, false, false, true, PerformanceTestUtils.Link.PREVIOUS);
    }

}

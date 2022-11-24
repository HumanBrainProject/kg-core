/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

package eu.ebrains.kg.core.api.instances.tests;

import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.ReleaseStatus;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.core.api.instances.TestContext;
import eu.ebrains.kg.core.api.v3.InstancesV3;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("java:S2187") //We don't add "tests" to these classes because they are test abstractions and are used in other tests
public class GetReleaseStatusByIdsTest extends AbstractInstanceTest {

    public Result<Map<UUID, Result<ReleaseStatus>>> response;
    public NormalizedJsonLd originalInstanceA;
    public UUID uuidA;
    public UUID uuidB;
    public NormalizedJsonLd originalInstanceB;

    public GetReleaseStatusByIdsTest(TestContext testContext, InstancesV3 instances) {
        super(testContext, instances);
    }

    @Override
    protected void setup() {
        originalInstanceA = createInstanceWithServerDefinedUUID(0);
        uuidA = testContext.getIdUtils().getUUID(originalInstanceA.id());

        originalInstanceB = createInstanceWithServerDefinedUUID(1);
        uuidB = testContext.getIdUtils().getUUID(originalInstanceB.id());

        instances.releaseInstance(uuidA, null);
    }

    @Override
    protected void run() {
        response = instances.getReleaseStatusByIds(Arrays.asList(uuidA, uuidB), ReleaseTreeScope.TOP_INSTANCE_ONLY);
    }
}


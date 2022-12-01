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

package eu.ebrains.kg.core.api.spaces.test;

import eu.ebrains.kg.commons.model.ExtendedResponseConfiguration;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.external.spaces.SpaceInformation;
import eu.ebrains.kg.core.api.AbstractTest;
import eu.ebrains.kg.core.api.instances.TestContext;
import eu.ebrains.kg.core.api.v3.InstancesV3;
import eu.ebrains.kg.core.api.v3.SpacesV3;
import eu.ebrains.kg.testutils.TestDataFactory;

@SuppressWarnings("java:S2187") //We don't add "tests" to these classes because they are test abstractions and are used in other tests
public class GetSpaceWithPermissionsTest extends AbstractTest {

    private final InstancesV3 instances;
    private final SpacesV3 spaces;
    public Result<SpaceInformation> space;

    public GetSpaceWithPermissionsTest(TestContext testContext, InstancesV3 instances, SpacesV3 spaces) {
        super(testContext);
        this.instances = instances;
        this.spaces = spaces;
    }

    @Override
    protected void setup() {
        // We create a new instance so the space is implicitly created.
        instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), "functionalityTest", new ExtendedResponseConfiguration());
    }

    @Override
    protected void run() {
        space = spaces.getSpace("functionalityTest", true);
    }
}

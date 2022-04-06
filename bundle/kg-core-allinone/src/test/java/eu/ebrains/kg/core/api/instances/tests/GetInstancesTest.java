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

package eu.ebrains.kg.core.api.instances.tests;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.authentication.api.AuthenticationAPI;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.PaginatedResult;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.api.instances.TestContext;
import eu.ebrains.kg.core.model.ExposedStage;

public class GetInstancesTest extends AbstractInstanceTest {

    public PaginatedResult<NormalizedJsonLd> response;
    public NormalizedJsonLd originalInstanceA;
    public NormalizedJsonLd originalInstanceB;
    private String type;
    private String space = null;

    private GetInstancesTest(TestContext testContext, Instances instances) {
        super(testContext, instances);
    }

    public static GetInstancesTest getInstancesWithExistingType(TestContext testContext, Instances instances){
        return new GetInstancesTest(testContext, instances);
    }


    public static GetInstancesTest getInstancesByType(TestContext testContext, Instances instances, String type){
        final GetInstancesTest test = getInstancesWithExistingType(testContext, instances);
        test.type = type;
        return test;
    }

    public static GetInstancesTest getInstancesByTypeAndSpace(TestContext testContext, Instances instances, String type, String space){
        final GetInstancesTest test = getInstancesByType(testContext, instances, type);
        test.space = space;
        return test;
    }

    @Override
    protected void setup() {
        originalInstanceA = createInstanceWithServerDefinedUUID(0);
        originalInstanceB = createInstanceWithServerDefinedUUID(1);
        if(type==null){
            type = originalInstanceA.types().get(0);
        }
    }

    @Override
    protected void run() {
        response = this.instances.getInstances(ExposedStage.IN_PROGRESS, this.type, this.space, null, null, null, defaultResponseConfiguration, defaultPaginationParam);
    }
}

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

package eu.ebrains.kg.core.api.types.test;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.authentication.api.AuthenticationAPI;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.IngestConfiguration;
import eu.ebrains.kg.commons.model.ResponseConfiguration;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.core.api.AbstractTest;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.api.Types;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.TestDataFactory;

import java.util.Collections;
import java.util.Map;

public class GetTypesByNameTest extends AbstractTest {
    private final Types types;
    private final Instances instances;
    private final SpaceName spaceName;
    private final boolean withProperties;

    public  Result<Map<String, Result<NormalizedJsonLd>>> response;

    public GetTypesByNameTest(ArangoDB.Builder database, AuthenticationAPI authenticationAPI,  SpaceName spaceName, boolean withProperties, RoleMapping[] roles, Types types, Instances instances) {
        super(database, authenticationAPI,  roles);
        this.instances = instances;
        this.types = types;
        this.spaceName = spaceName;
        this.withProperties = withProperties;
    }

    @Override
    protected void setup() {
        // We create a new instance so the type is implicitly created.
        instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), "functionalityTest", new ResponseConfiguration(), new IngestConfiguration(), null);

    }

    @Override
    protected void run() {
         response = this.types.getTypesByName(Collections.singletonList(TestDataFactory.TEST_TYPE), ExposedStage.IN_PROGRESS, withProperties, spaceName == null ? null : spaceName.getName());
    }
}

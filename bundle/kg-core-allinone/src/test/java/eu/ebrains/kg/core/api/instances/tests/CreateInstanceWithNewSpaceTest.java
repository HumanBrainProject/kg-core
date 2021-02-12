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

package eu.ebrains.kg.core.api.instances.tests;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.authentication.api.AuthenticationAPI;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.testutils.TestDataFactory;
import org.springframework.http.ResponseEntity;

public class CreateInstanceWithNewSpaceTest extends AbstractInstanceTest {

    public JsonLdDoc testData = TestDataFactory.createTestData(smallPayload, 0, true);
    public ResponseEntity<Result<NormalizedJsonLd>> response;

    public CreateInstanceWithNewSpaceTest(ArangoDB.Builder database, AuthenticationAPI authenticationAPI, Instances instances, RoleMapping[] roles) {
        super(database, authenticationAPI,  instances, roles);
    }

    @Override
    protected void setup() {
    }

    @Override
    protected void run(){
        response = instances.createNewInstance(testData, "functionalityTest", defaultResponseConfiguration, defaultIngestConfiguration, null);
    }
}

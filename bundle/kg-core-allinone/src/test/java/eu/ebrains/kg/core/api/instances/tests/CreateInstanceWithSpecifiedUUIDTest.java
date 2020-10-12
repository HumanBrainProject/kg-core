/*
 * Copyright 2020 EPFL/Human Brain Project PCO
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
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.testutils.TestDataFactory;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public class CreateInstanceWithSpecifiedUUIDTest extends AbstractInstanceTest {

    public JsonLdDoc testData = TestDataFactory.createTestData(smallPayload, 1, true);
    public ResponseEntity<Result<NormalizedJsonLd>> response;
    public UUID clientSpecifiedUUID = UUID.randomUUID();

    public CreateInstanceWithSpecifiedUUIDTest(ArangoDB.Builder database, ToAuthentication authenticationSvc, Instances instances, RoleMapping[] roles) {
        super(database, authenticationSvc, instances, roles);
    }

    @Override
    protected void setup() {
        //We create an instance already so the space exists. The creation of an instance in a non-existing space is a different use case
        createInstanceWithServerDefinedUUID(0);
    }

    @Override
    protected void run(){
        response = instances.createNewInstance(testData, clientSpecifiedUUID, "functionalityTest", defaultResponseConfiguration, defaultIngestConfiguration, null);
    }

}

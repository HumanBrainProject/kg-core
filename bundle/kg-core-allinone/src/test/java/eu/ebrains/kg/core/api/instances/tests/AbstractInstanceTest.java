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
import eu.ebrains.kg.core.api.AbstractTest;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.testutils.TestDataFactory;
import org.springframework.http.ResponseEntity;

public abstract class AbstractInstanceTest extends AbstractTest {

    protected final Instances instances;

    public AbstractInstanceTest(ArangoDB.Builder database, ToAuthentication authenticationSvc, Instances instances, RoleMapping[] roles) {
        super(database, authenticationSvc, roles);
        this.instances = instances;
    }

    protected NormalizedJsonLd createInstanceWithServerDefinedUUID(int iteration) {
        JsonLdDoc originalDoc = TestDataFactory.createTestData(smallPayload, iteration, true);
        ResponseEntity<Result<NormalizedJsonLd>> response = instances.createNewInstance(originalDoc, "functionalityTest", defaultResponseConfiguration, defaultIngestConfiguration, null);
        return response.getBody().getData();
    }


}

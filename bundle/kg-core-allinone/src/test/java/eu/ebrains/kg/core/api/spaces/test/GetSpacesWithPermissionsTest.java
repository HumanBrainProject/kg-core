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

package eu.ebrains.kg.core.api.spaces.test;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.authentication.api.AuthenticationAPI;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.IngestConfiguration;
import eu.ebrains.kg.commons.model.PaginatedResult;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.ResponseConfiguration;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.core.api.AbstractTest;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.api.Spaces;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.TestDataFactory;

public class GetSpacesWithPermissionsTest extends AbstractTest {

    private final Instances instances;
    private final Spaces spaces;
    public final String space = "functionalitytest";
    public PaginatedResult<NormalizedJsonLd> response;


    public GetSpacesWithPermissionsTest(ArangoDB.Builder database, AuthenticationAPI authenticationAPI,  RoleMapping[] roles, Instances instances, Spaces spaces) {
        super(database, authenticationAPI,  roles);
        this.instances = instances;
        this.spaces = spaces;
    }

    @Override
    protected void setup() {
        // We create a new instance so the space is implicitly created.
        instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), space, new ResponseConfiguration(), new IngestConfiguration(), null);
    }

    @Override
    protected void run() {
        response = this.spaces.getSpaces(ExposedStage.IN_PROGRESS, new PaginationParam(), true);
    }
}

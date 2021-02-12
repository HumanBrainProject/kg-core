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

package eu.ebrains.kg.core.api.types.test;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.authentication.api.AuthenticationAPI;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.core.api.AbstractTest;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.api.Types;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.TestDataFactory;

public class GetTypesTest extends AbstractTest {
    private final Types types;
    private final Instances instances;
    private final SpaceName spaceName;
    private final boolean withProperties;
    private final boolean withIncomingLinks;

    public PaginatedResult<NormalizedJsonLd> response;

    public GetTypesTest(ArangoDB.Builder database, AuthenticationAPI authenticationAPI,  SpaceName spaceName, boolean withProperties, boolean withIncomingLinks, RoleMapping[] roles, Types types, Instances instances) {
        super(database, authenticationAPI,  roles);
        this.instances = instances;
        this.types = types;
        this.spaceName = spaceName;
        this.withProperties = withProperties;
        this.withIncomingLinks = withIncomingLinks;
    }

    @Override
    protected void setup() {
        // We create a new instance so the type is implicitly created.
        instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), "functionalityTest", new ResponseConfiguration(), new IngestConfiguration(), null);

    }

    @Override
    protected void run() {
        response = this.types.getTypes(ExposedStage.IN_PROGRESS, spaceName == null ? null : spaceName.getName(), withProperties, withIncomingLinks, new PaginationParam());
    }
}

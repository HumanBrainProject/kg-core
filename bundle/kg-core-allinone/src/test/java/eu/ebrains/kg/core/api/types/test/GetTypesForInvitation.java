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

import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.model.external.types.TypeInformation;
import eu.ebrains.kg.core.api.AbstractTest;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.api.Types;
import eu.ebrains.kg.core.api.instances.TestContext;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.TestDataFactory;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@SuppressWarnings("java:S2187") //We don't add "tests" to these classes because they are test abstractions and are used in other tests
public class GetTypesForInvitation extends AbstractTest {
    private final Types types;
    private final Instances instances;
    private final boolean withProperties;
    private final boolean withIncomingLinks;
    public PaginatedResult<TypeInformation> response;
    private final String space;

    public GetTypesForInvitation(TestContext testContext, boolean withProperties, boolean withIncomingLinks, Types types, String space, Instances instances) {
        super(testContext);
        this.instances = instances;
        this.types = types;
        this.withProperties = withProperties;
        this.withIncomingLinks = withIncomingLinks;
        this.space = space;
    }

    @Override
    protected void setup() {
        // We create a new instance so the type is implicitly created.
        final ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), "functionalityTest", new ExtendedResponseConfiguration());
        instances.inviteUserForInstance(testContext.getIdUtils().getUUID(instance.getBody().getData().id()), USER_ID);
    }

    @Override
    protected void run() {
        response = this.types.getTypes(ExposedStage.IN_PROGRESS, space, withProperties, withIncomingLinks, new PaginationParam());
    }
}

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
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.ReleaseStatus;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.testutils.TestDataFactory;

import java.util.UUID;

public class GetReleaseStatusTest extends AbstractInstanceTest {

    public NormalizedJsonLd originalInstance;
    public IdUtils idUtils;
    public UUID uuid;
    public ReleaseStatus releaseStatusAfterCreation;
    public ReleaseStatus releaseStatusAfterReleasing;
    public ReleaseStatus releaseStatusAfterChange;
    public ReleaseStatus releaseStatusAfterUnreleasing;

    public GetReleaseStatusTest(ArangoDB.Builder database, ToAuthentication authenticationSvc, IdUtils idUtils, Instances instances, RoleMapping[] roles) {
        super(database, authenticationSvc, instances, roles);
        this.idUtils = idUtils;
    }

    @Override
    protected void setup() {
        originalInstance = createInstanceWithServerDefinedUUID(0);

        uuid = idUtils.getUUID(originalInstance.id());

    }

    @Override
    protected void run() {
        releaseStatusAfterCreation = assureValidPayload(instances.getReleaseStatus(uuid, ReleaseTreeScope.TOP_INSTANCE_ONLY));

        beAdmin();
        instances.releaseInstance(uuid, null);
        beInCurrentRole();

        releaseStatusAfterReleasing = assureValidPayload(instances.getReleaseStatus(uuid, ReleaseTreeScope.TOP_INSTANCE_ONLY));

        beAdmin();
        instances.contributeToInstancePartialReplacement(TestDataFactory.createTestData(smallPayload, 0, true), uuid, false, defaultResponseConfiguration, defaultIngestConfiguration, null);
        beInCurrentRole();

        releaseStatusAfterChange = assureValidPayload(instances.getReleaseStatus(uuid, ReleaseTreeScope.TOP_INSTANCE_ONLY));

        beAdmin();
        instances.unreleaseInstance(uuid);
        beInCurrentRole();

        releaseStatusAfterUnreleasing = assureValidPayload(instances.getReleaseStatus(uuid, ReleaseTreeScope.TOP_INSTANCE_ONLY));

    }
}

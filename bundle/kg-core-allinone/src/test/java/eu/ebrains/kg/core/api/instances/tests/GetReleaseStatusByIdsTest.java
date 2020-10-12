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
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
import eu.ebrains.kg.core.api.Instances;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class GetReleaseStatusByIdsTest extends AbstractInstanceTest {

    public Result<Map<UUID, Result<ReleaseStatus>>> response;
    public NormalizedJsonLd originalInstanceA;
    public UUID uuidA;
    public UUID uuidB;
    public NormalizedJsonLd originalInstanceB;

    public IdUtils idUtils;

    public GetReleaseStatusByIdsTest(ArangoDB.Builder database, ToAuthentication authenticationSvc, IdUtils idUtils, Instances instances, RoleMapping[] roles) {
        super(database, authenticationSvc, instances, roles);
        this.idUtils = idUtils;
    }

    @Override
    protected void setup() {
        originalInstanceA = createInstanceWithServerDefinedUUID(0);
        uuidA = idUtils.getUUID(originalInstanceA.id());

        originalInstanceB = createInstanceWithServerDefinedUUID(1);
        uuidB = idUtils.getUUID(originalInstanceB.id());

        instances.releaseInstance(uuidA, null);
    }

    @Override
    protected void run() {
        response = instances.getReleaseStatusByIds(Arrays.asList(uuidA, uuidB), ReleaseTreeScope.TOP_INSTANCE_ONLY);
    }
}


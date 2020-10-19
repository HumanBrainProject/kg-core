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
import eu.ebrains.kg.commons.model.PaginatedResult;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.model.ExposedStage;

public class GetInstancesTest extends AbstractInstanceTest {

    public PaginatedResult<NormalizedJsonLd> response;
    public NormalizedJsonLd originalInstanceA;
    public NormalizedJsonLd originalInstanceB;
    public IdUtils idUtils;

    public GetInstancesTest(ArangoDB.Builder database, ToAuthentication authenticationSvc, IdUtils idUtils, Instances instances, RoleMapping[] roles) {
        super(database, authenticationSvc, instances, roles);
        this.idUtils = idUtils;
    }

    @Override
    protected void setup() {
        originalInstanceA = createInstanceWithServerDefinedUUID(0);
        originalInstanceB = createInstanceWithServerDefinedUUID(1);
    }

    @Override
    protected void run() {
        response = this.instances.getInstances(ExposedStage.IN_PROGRESS, originalInstanceA.types().get(0), null, null, defaultResponseConfiguration, defaultPaginationParam);
    }
}

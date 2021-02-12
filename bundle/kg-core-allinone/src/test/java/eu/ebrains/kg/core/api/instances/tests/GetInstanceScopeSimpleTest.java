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
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.ScopeElement;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.model.ExposedStage;
import org.springframework.http.ResponseEntity;

public class GetInstanceScopeSimpleTest extends AbstractInstanceTest {

    public ResponseEntity<Result<ScopeElement>> response;
    public NormalizedJsonLd originalInstance;
    public IdUtils idUtils;

    public GetInstanceScopeSimpleTest(ArangoDB.Builder database, AuthenticationAPI authenticationAPI, IdUtils idUtils, Instances instances, RoleMapping[] roles) {
        super(database, authenticationAPI,  instances, roles);
        this.idUtils = idUtils;
    }

    @Override
    protected void setup() {
        originalInstance = createInstanceWithServerDefinedUUID(0);
    }

    @Override
    protected void run(){
        response = instances.getInstanceScope(idUtils.getUUID(originalInstance.id()), ExposedStage.IN_PROGRESS, false);
    }
}

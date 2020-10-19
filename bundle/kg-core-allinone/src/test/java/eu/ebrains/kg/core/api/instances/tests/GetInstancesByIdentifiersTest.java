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
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.model.ExposedStage;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Map;

public class GetInstancesByIdentifiersTest extends AbstractInstanceTest {

    public Result<Map<String, Result<NormalizedJsonLd>>> response;
    public ResponseEntity<Result<NormalizedJsonLd>> updateResult;
    public NormalizedJsonLd originalInstance;
    public String identifier = "https://foo/bar";
    public JsonLdDoc doc;
    public IdUtils idUtils;

    public GetInstancesByIdentifiersTest(ArangoDB.Builder database, ToAuthentication authenticationSvc, IdUtils idUtils, Instances instances, RoleMapping[] roles) {
        super(database, authenticationSvc, instances, roles);
        this.idUtils = idUtils;
    }

    @Override
    protected void setup() {
        originalInstance = createInstanceWithServerDefinedUUID(0);
        doc = new JsonLdDoc();
        doc.addIdentifiers(identifier);
        updateResult = instances.contributeToInstancePartialReplacement(doc, idUtils.getUUID(originalInstance.id()), false, defaultResponseConfiguration, defaultIngestConfiguration, null);

    }

    @Override
    protected void run() {
        response = instances.getInstancesByIdentifiers(Collections.singletonList(identifier), ExposedStage.IN_PROGRESS, defaultResponseConfiguration);
    }
}

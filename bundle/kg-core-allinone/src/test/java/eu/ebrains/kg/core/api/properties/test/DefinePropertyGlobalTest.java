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

package eu.ebrains.kg.core.api.properties.test;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.authentication.api.AuthenticationAPI;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.ExtendedResponseConfiguration;
import eu.ebrains.kg.commons.model.ResponseConfiguration;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.core.api.AbstractTest;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.api.Properties;
import eu.ebrains.kg.testutils.TestDataFactory;

public class DefinePropertyGlobalTest extends AbstractTest {

    private final Instances instances;
    private final Properties properties;

    public NormalizedJsonLd instance;
    public String property;
    public String type;


    public DefinePropertyGlobalTest(ArangoDB.Builder database, AuthenticationAPI authenticationAPI, RoleMapping[] roles, Instances instances, Properties properties) {
        super(database, authenticationAPI, roles);
        this.instances = instances;
        this.properties = properties;
    }

    @Override
    protected void setup() {
        // We create a new instance so the type and its properties are implicitly created.
        instance = assureValidPayload(instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), "functionalityTest", new ExtendedResponseConfiguration()));
        property = instance.keySet().stream().filter(k -> k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX)).findFirst().orElse(null);
        type = instance.types().get(0);
    }

    @Override
    protected void run() {
        NormalizedJsonLd propertyDefinition = createPropertyDefinition();
        properties.defineProperty(propertyDefinition, true, property);
    }


    NormalizedJsonLd createPropertyDefinition() {
        NormalizedJsonLd payload = new NormalizedJsonLd();
        payload.addProperty("http://foo", "bar");
        return payload;
    }

}

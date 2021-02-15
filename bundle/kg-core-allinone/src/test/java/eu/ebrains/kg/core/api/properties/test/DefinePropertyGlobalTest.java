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

package eu.ebrains.kg.core.api.properties.test;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.authentication.api.AuthenticationAPI;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.IngestConfiguration;
import eu.ebrains.kg.commons.model.ResponseConfiguration;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
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
        instance = assureValidPayload(instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), "functionalityTest", new ResponseConfiguration(), new IngestConfiguration(), null));
        property = instance.keySet().stream().filter(k -> k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX)).findFirst().orElse(null);
        type = instance.types().get(0);
    }

    @Override
    protected void run() {
        NormalizedJsonLd propertyDefinition = createPropertyDefinition(property, null);
        properties.defineProperty(propertyDefinition, true);
    }


    NormalizedJsonLd createPropertyDefinition(String property, String type) {
        NormalizedJsonLd payload = new NormalizedJsonLd();
        if (type != null) {
            payload.addTypes(EBRAINSVocabulary.META_PROPERTY_IN_TYPE_DEFINITION_TYPE);
            payload.addProperty(EBRAINSVocabulary.META_TYPE, new JsonLdId(type));
        } else {
            payload.addTypes(EBRAINSVocabulary.META_PROPERTY_DEFINITION_TYPE);
        }
        payload.addProperty(EBRAINSVocabulary.META_PROPERTY, new JsonLdId(property));
        payload.addProperty("http://foo", "bar");
        return payload;
    }

}

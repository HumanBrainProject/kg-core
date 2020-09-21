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

package eu.ebrains.kg.core.api.instances.functionality;

import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.IngestConfiguration;
import eu.ebrains.kg.commons.model.ResponseConfiguration;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.api.Properties;
import eu.ebrains.kg.core.api.Types;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.AbstractFunctionalityTest;
import eu.ebrains.kg.testutils.TestDataFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@TestPropertySource(properties = {"eu.ebrains.kg.core.metadata.synchronous=true"})
public class PropertiesFunctionalityTest extends AbstractFunctionalityTest {

    @Autowired
    Types types;

    @Autowired
    Properties properties;

    @Autowired
    Instances instances;

    @Override
    protected void authenticate() {
        beAdmin();
    }



    NormalizedJsonLd createPropertyDefinition(String property, String type) {
        NormalizedJsonLd payload = new NormalizedJsonLd();
        if(type!=null) {
            payload.addTypes(EBRAINSVocabulary.META_PROPERTY_IN_TYPE_DEFINITION_TYPE);
            payload.addProperty(EBRAINSVocabulary.META_TYPE, new JsonLdId(type));
        }
        else{
            payload.addTypes(EBRAINSVocabulary.META_PROPERTY_DEFINITION_TYPE);
        }
        payload.addProperty(EBRAINSVocabulary.META_PROPERTY, new JsonLdId(property));
        payload.addProperty("http://foo", "bar");
        return payload;
    }

    @Test
    public void definePropertyGlobal() {
        //Given
        // We create a new instance so the type and its properties are implicitly created.
        NormalizedJsonLd instance = assureValidPayload(instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), "functionalityTest", new ResponseConfiguration(), new IngestConfiguration(), null));
        String property = instance.keySet().stream().filter(k -> k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX)).findFirst().orElse(null);
        String type = instance.types().get(0);

        //When
        NormalizedJsonLd propertyDefinition = createPropertyDefinition(property, null);
        properties.defineProperty(propertyDefinition, true);

        //Then
        Map<String, Result<NormalizedJsonLd>> result = assureValidPayload(this.types.getTypesByName(Collections.singletonList(type), ExposedStage.IN_PROGRESS, true, null));
        NormalizedJsonLd typeDefinition = assureValidPayload(result.get(type));
        List<NormalizedJsonLd> properties = typeDefinition.getAsListOf(EBRAINSVocabulary.META_PROPERTIES, NormalizedJsonLd.class);
        NormalizedJsonLd propertydef = properties.stream().filter(p -> p.getAs(SchemaOrgVocabulary.IDENTIFIER, String.class).equals(property)).findFirst().orElse(null);
        assertEquals("bar", propertydef.getAs("http://foo", String.class));
    }

}

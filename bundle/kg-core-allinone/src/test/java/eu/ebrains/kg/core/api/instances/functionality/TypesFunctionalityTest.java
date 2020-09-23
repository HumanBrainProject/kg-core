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
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.api.Instances;
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
public class TypesFunctionalityTest extends AbstractFunctionalityTest {

    @Autowired
    Types types;

    @Autowired
    Instances instances;

    @Override
    protected void authenticate() {
        beAdmin();
    }

    private final static String TYPE_NAME = "https://core.kg.ebrains.eu/Test";

    NormalizedJsonLd createTypeDefinition() {
        NormalizedJsonLd payload = new NormalizedJsonLd();
        payload.addProperty(EBRAINSVocabulary.META_TYPE, new JsonLdId(TYPE_NAME));
        payload.addProperty("http://foo", "bar");
        return payload;
    }


    @Test
    public void defineType() {
        //Given
        NormalizedJsonLd typeDefinition = createTypeDefinition();

        //When
        types.defineType(typeDefinition, true);

        //Then
        Result<Map<String, Result<NormalizedJsonLd>>> typesByName = types.getTypesByName(Collections.singletonList(TYPE_NAME), ExposedStage.IN_PROGRESS, false, null);
        NormalizedJsonLd data = typesByName.getData().get(TYPE_NAME).getData();
        assertNotNull(data);
        assertTrue(data.identifiers().contains(TYPE_NAME));
        assertEquals("bar", data.get("http://foo"));
    }


    @Test
    public void getTypes() {
        //Given
        // We create a new instance so the type is implicitly created.
        instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), "functionalityTest", new ResponseConfiguration(), new IngestConfiguration(), null);

        //When
        PaginatedResult<NormalizedJsonLd> types = this.types.getTypes(ExposedStage.IN_PROGRESS, null, false, new PaginationParam());

        //Then
        List<NormalizedJsonLd> response = assureValidPayload(types);
        assertEquals(1, response.size());
        assertTrue(response.get(0).identifiers().contains(TestDataFactory.TEST_TYPE));
    }

    @Test
    public void getTypesBySpace() {
        //Given
        // We create a new instance so the type is implicitly created.
        instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), "functionalityTest", new ResponseConfiguration(), new IngestConfiguration(), null);

        //When
        PaginatedResult<NormalizedJsonLd> types = this.types.getTypes(ExposedStage.IN_PROGRESS, "functionalityTest", false, new PaginationParam());

        //Then
        List<NormalizedJsonLd> response = assureValidPayload(types);
        assertEquals(1, response.size());
        assertTrue(response.get(0).identifiers().contains(TestDataFactory.TEST_TYPE));
    }


    @Test
    public void getTypesWithProperties() {
        //Given
        // We create a new instance so the type is implicitly created.
        instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), "functionalityTest", new ResponseConfiguration(), new IngestConfiguration(), null);

        //When
        PaginatedResult<NormalizedJsonLd> types = this.types.getTypes(ExposedStage.IN_PROGRESS, null, true, new PaginationParam());

        //Then
        List<NormalizedJsonLd> response = assureValidPayload(types);
        assertEquals(1, response.size());
        assertTrue(response.get(0).identifiers().contains(TestDataFactory.TEST_TYPE));
        List<NormalizedJsonLd> properties = response.get(0).getAsListOf(EBRAINSVocabulary.META_PROPERTIES, NormalizedJsonLd.class);
        assertNotNull(properties);
        assertTrue(properties.size()>smallPayload);
    }

    @Test
    public void getTypesBySpaceWithProperties() {
        //Given
        // We create a new instance so the type is implicitly created.
        instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), "functionalityTest", new ResponseConfiguration(), new IngestConfiguration(), null);

        //When
        PaginatedResult<NormalizedJsonLd> types = this.types.getTypes(ExposedStage.IN_PROGRESS, "functionalityTest", true, new PaginationParam());

        //Then
        List<NormalizedJsonLd> response = assureValidPayload(types);
        assertEquals(1, response.size());
        assertTrue(response.get(0).identifiers().contains(TestDataFactory.TEST_TYPE));
    }

    @Test
    public void getTypesByName() {
        //Given
        // We create a new instance so the type is implicitly created.
        instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), "functionalityTest", new ResponseConfiguration(), new IngestConfiguration(), null);

        //When
        Result<Map<String, Result<NormalizedJsonLd>>> typesByName = this.types.getTypesByName(Collections.singletonList(TestDataFactory.TEST_TYPE), ExposedStage.IN_PROGRESS, false, null);

        //Then
        Map<String, Result<NormalizedJsonLd>> map = assureValidPayload(typesByName);
        assureValidPayload(map.get(TestDataFactory.TEST_TYPE));
    }

}

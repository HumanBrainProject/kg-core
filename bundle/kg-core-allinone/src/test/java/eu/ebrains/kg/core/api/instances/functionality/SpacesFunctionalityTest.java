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

import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.api.Spaces;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.AbstractFunctionalityTest;
import eu.ebrains.kg.testutils.TestDataFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.Assert.*;
@TestPropertySource(properties = {"eu.ebrains.kg.core.metadata.synchronous=true"})
public class SpacesFunctionalityTest extends AbstractFunctionalityTest {

    @Autowired
    Spaces spaces;

    @Autowired
    Instances instances;

    @Override
    protected void authenticate() {
        beAdmin();
    }

    private final static String SPACE = "functionalitytest";
    @Test
    public void getSpaceWithPermissions() {
        //Given

        // We create a new instance so the space is implicitly created.
        instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), SPACE, new ResponseConfiguration(), new IngestConfiguration(), null);

        //When
        Result<NormalizedJsonLd> space = spaces.getSpace(ExposedStage.IN_PROGRESS, SPACE, true);

        //Then
        NormalizedJsonLd normalizedJsonLd = assureValidPayload(space);
        assertNotNull(normalizedJsonLd);
    }

    @Test
    public void getSpacesWithPermissions() {
        //Given

        // We create a new instance so the space is implicitly created.
        instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), SPACE, new ResponseConfiguration(), new IngestConfiguration(), null);

        //When
        PaginatedResult<NormalizedJsonLd> spaces = this.spaces.getSpaces(ExposedStage.IN_PROGRESS, new PaginationParam(), true);

        //Then
        List<NormalizedJsonLd> normalizedJsonLd = assureValidPayload(spaces);
        assertNotNull(normalizedJsonLd);
        assertEquals(1, normalizedJsonLd.size());
        assertEquals(SPACE, normalizedJsonLd.get(0).getAs(SchemaOrgVocabulary.NAME, String.class));
    }


}

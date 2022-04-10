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

package eu.ebrains.kg.core.api.spaces;

import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.model.external.spaces.SpaceInformation;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.api.Spaces;
import eu.ebrains.kg.core.api.spaces.test.GetSpaceWithPermissionsTest;
import eu.ebrains.kg.core.api.spaces.test.GetSpacesWithPermissionsTest;
import eu.ebrains.kg.testutils.AbstractFunctionalityTest;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.Assert.*;

@TestPropertySource(properties = {"eu.ebrains.kg.core.metadata.synchronous=true"})
public class SpacesTest extends AbstractFunctionalityTest {

    @Autowired
    Spaces spaces;

    @Autowired
    Instances instances;

    private final static RoleMapping[] NON_READ_SPACE_ROLES = new RoleMapping[]{null};
    private final static RoleMapping[] READ_SPACE_ROLES = RoleMapping.getRemainingUserRoles(NON_READ_SPACE_ROLES);

    @Test
    @Ignore //TODO fix test
    public void getSpaceWithPermissionsOk() {
        //Given
        GetSpaceWithPermissionsTest test = new GetSpaceWithPermissionsTest(ctx(READ_SPACE_ROLES), instances, spaces);

        //When
        test.execute(() -> {
            //Then
            SpaceInformation spaceInformation = test.assureValidPayload(test.space);
            assertNotNull(spaceInformation);
        });
    }

    @Test
    @Ignore //TODO fix test
    public void getSpaceWithPermissionsForbidden() {
        //Given
        GetSpaceWithPermissionsTest test = new GetSpaceWithPermissionsTest(ctx(NON_READ_SPACE_ROLES), instances, spaces);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    @Ignore //TODO fix test
    public void getSpacesWithPermissionsOk() {
        //Given
        GetSpacesWithPermissionsTest test = new GetSpacesWithPermissionsTest(ctx(READ_SPACE_ROLES), instances, spaces);

        //When
        test.execute(() -> {
            //Then
            List<SpaceInformation> spaceInformations = test.assureValidPayload(test.response);
            assertNotNull(spaceInformations);
            assertEquals(1, spaceInformations.size());
            assertEquals(test.space, spaceInformations.get(0).getAs(SchemaOrgVocabulary.NAME, String.class));
        });
    }

    @Test
    @Ignore //TODO fix test
    public void getSpacesWithPermissionsForbidden() {
        //Given
        GetSpacesWithPermissionsTest test = new GetSpacesWithPermissionsTest(ctx(NON_READ_SPACE_ROLES), instances, spaces);

        //When
        test.execute(()->{
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }


}

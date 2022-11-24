/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

package eu.ebrains.kg.core.api.types;

import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.external.types.TypeInformation;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.api.v3.InstancesV3;
import eu.ebrains.kg.core.api.v3.TypesV3;
import eu.ebrains.kg.core.api.types.test.DefineTypeTest;
import eu.ebrains.kg.core.api.types.test.GetTypesByNameTest;
import eu.ebrains.kg.core.api.types.test.GetTypesForInvitation;
import eu.ebrains.kg.core.api.types.test.GetTypesTest;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.AbstractFunctionalityTest;
import eu.ebrains.kg.testutils.TestDataFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {"eu.ebrains.kg.core.metadata.synchronous=true"})
@Disabled //TODO fix tests
class TypesTest extends AbstractFunctionalityTest {

    @Autowired
    TypesV3 types;

    @Autowired
    InstancesV3 instances;

    //TODO we are currently allowing even unauthenticated users to read type information. is this ok?
    private final static RoleMapping[] NON_READ_TYPES_IN_PROGRESS_ROLES = new RoleMapping[]{};
    private final static RoleMapping[] READ_TYPES_IN_PROGRESS_ROLES = RoleMapping.getRemainingUserRoles(NON_READ_TYPES_IN_PROGRESS_ROLES);

    private final static RoleMapping[] TYPE_DEFINITION_ROLES = new RoleMapping[]{RoleMapping.ADMIN};
    private final static RoleMapping[] NON_TYPE_DEFINITION_ROLES = RoleMapping.getRemainingUserRoles(TYPE_DEFINITION_ROLES);


    @Test
    void defineTypeOk() {
        //Given
        DefineTypeTest test = new DefineTypeTest(ctx(TYPE_DEFINITION_ROLES), types);

        //When
        test.execute(() -> {
            Result<Map<String, Result<TypeInformation>>> typesByName = types.getTypesByName(Collections.singletonList(test.typeName), ExposedStage.IN_PROGRESS, false, false,null);
            TypeInformation data = typesByName.getData().get(test.typeName).getData();
            assertNotNull(data);
            assertEquals(test.typeName, data.getIdentifier());
            assertEquals("bar", data.get("http://foo"));
        });
    }

    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void defineTypeForbidden() {
        //Given
        DefineTypeTest test = new DefineTypeTest(ctx(NON_TYPE_DEFINITION_ROLES), types);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    void getTypesOk() {
        //Given
        GetTypesTest test = new GetTypesTest(ctx(READ_TYPES_IN_PROGRESS_ROLES), null, false, false, false, types, instances);

        //When
        test.execute(() -> {
            List<TypeInformation> response = test.assureValidPayload(test.response);
            assertEquals(1, response.size());
            assertEquals(response.get(0).getIdentifier(), TestDataFactory.TEST_TYPE);
        });
    }

    @Test
    void getTypesForbidden() {
        //Given
        GetTypesTest test = new GetTypesTest(ctx(NON_READ_TYPES_IN_PROGRESS_ROLES), null, false, false, false, types, instances);

        //When
        test.execute(() -> {
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }


    @Test
    void getTypesBySpaceOk() {
        //Given
        GetTypesTest test = new GetTypesTest(ctx(NON_READ_TYPES_IN_PROGRESS_ROLES), new SpaceName("functionalityTest"), false, false,false,  types, instances);

        //When
        test.execute(() -> {
            //Then
            List<TypeInformation> response = test.assureValidPayload(test.response);
            assertEquals(1, response.size());
            assertEquals(response.get(0).getIdentifier(), TestDataFactory.TEST_TYPE);
        });

    }

    @Test
    void getTypesBySpaceForbidden() {
        //Given
        GetTypesTest test = new GetTypesTest(ctx(NON_READ_TYPES_IN_PROGRESS_ROLES), new SpaceName("functionalityTest"), false, false,false, types, instances);

        //When
        test.execute(() -> {
            //Then
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }


    @Test
    void getTypesWithPropertiesOk() {
        //Given
        GetTypesTest test = new GetTypesTest(ctx(READ_TYPES_IN_PROGRESS_ROLES), null, true, false, true, types, instances);

        //When
        test.execute(() -> {
            //Then
            List<TypeInformation> response = test.assureValidPayload(test.response);
            assertEquals(1, response.size());
            assertEquals(response.get(0).getIdentifier(), TestDataFactory.TEST_TYPE);
            List<NormalizedJsonLd> properties = response.get(0).getAsListOf(EBRAINSVocabulary.META_PROPERTIES, NormalizedJsonLd.class);
            assertNotNull(properties);
            assertTrue(properties.size() > smallPayload);
        });
    }

    @Test
    void getTypesWithPropertiesForbidden() {
        //Given
        GetTypesTest test = new GetTypesTest(ctx(NON_READ_TYPES_IN_PROGRESS_ROLES), null, true, false, true, types, instances);

        //When
        test.execute(() -> {
            //Then
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }

    @Test
    void getTypesBySpaceWithPropertiesOk() {
        //Given
        GetTypesTest test = new GetTypesTest(ctx(READ_TYPES_IN_PROGRESS_ROLES), new SpaceName("functionalityTest"), true, false, true, types, instances);

        //When
        test.execute(() -> {
            //Then
            List<TypeInformation> response = test.assureValidPayload(test.response);
            assertEquals(1, response.size());
            assertEquals(response.get(0).getIdentifier(), TestDataFactory.TEST_TYPE);
        });
    }

    @Test
    void getTypesBySpaceWithPropertiesForbidden() {
        //Given
        GetTypesTest test = new GetTypesTest(ctx(NON_READ_TYPES_IN_PROGRESS_ROLES), new SpaceName("functionalityTest"), true, false, true, types, instances);

        //When
        test.execute(() -> {
            //Then
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }



    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void getTypesByNameOk() {
        //Given
        GetTypesByNameTest test = new GetTypesByNameTest(ctx(READ_TYPES_IN_PROGRESS_ROLES), null, false, types, instances);

        //When
        test.execute(()->{

            //Then
            Map<String, Result<TypeInformation>> map = test.assureValidPayload(test.response);
            test.assureValidPayload(map.get(TestDataFactory.TEST_TYPE));
        });
    }

    @Test
    void getTypesByNameForbidden() {
        //Given
        GetTypesByNameTest test = new GetTypesByNameTest(ctx(NON_READ_TYPES_IN_PROGRESS_ROLES),  null, false, types, instances);

        //When
        test.execute(()->{
            //Then
            Result.Error error = test.response.getData().get(TestDataFactory.TEST_TYPE).getError();
            assertNotNull(error);
            assertEquals(403, error.getCode());
        });
    }

    @Test
    void getTypesForInvitationInReviewSpace() {
        //Given
        GetTypesForInvitation test = new GetTypesForInvitation(ctx(NON_READ_TYPES_IN_PROGRESS_ROLES),  false, false, types, SpaceName.REVIEW_SPACE, instances);

        //When
        test.execute(()->{
            //Then
            test.assureValidPayload(test.response);
            assertEquals(1, test.response.getData().size());
        });
    }

    @Test
    void getTypesForInvitation() {
        //Given
        GetTypesForInvitation test = new GetTypesForInvitation(ctx(NON_READ_TYPES_IN_PROGRESS_ROLES),  false, false, types, null, instances);

        //When
        test.execute(()->{
            //Then
            test.assureValidPayload(test.response);
            assertEquals(1, test.response.getData().size());
        });
    }

}

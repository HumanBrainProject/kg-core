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

package eu.ebrains.kg.core.api.types;

import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.api.Types;
import eu.ebrains.kg.core.api.types.test.DefineTypeTest;
import eu.ebrains.kg.core.api.types.test.GetTypesByNameTest;
import eu.ebrains.kg.core.api.types.test.GetTypesTest;
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
public class TypesTest extends AbstractFunctionalityTest {

    @Autowired
    Types types;

    @Autowired
    Instances instances;

    //TODO we are currently allowing even unauthenticated users to read type information. is this ok?
    private final static RoleMapping[] NON_READ_TYPES_IN_PROGRESS_ROLES = new RoleMapping[]{};
    private final static RoleMapping[] READ_TYPES_IN_PROGRESS_ROLES = RoleMapping.getRemainingUserRoles(NON_READ_TYPES_IN_PROGRESS_ROLES);

    private final static RoleMapping[] TYPE_DEFINITION_ROLES = new RoleMapping[]{RoleMapping.ADMIN};
    private final static RoleMapping[] NON_TYPE_DEFINITION_ROLES = RoleMapping.getRemainingUserRoles(TYPE_DEFINITION_ROLES);


    @Test
    public void defineTypeOk() {
        //Given
        DefineTypeTest test = new DefineTypeTest(database, authenticationAPI, TYPE_DEFINITION_ROLES, types);

        //When
        test.execute(() -> {
            Result<Map<String, Result<NormalizedJsonLd>>> typesByName = types.getTypesByName(Collections.singletonList(test.typeName), ExposedStage.IN_PROGRESS, false, false, null);
            NormalizedJsonLd data = typesByName.getData().get(test.typeName).getData();
            assertNotNull(data);
            assertTrue(data.identifiers().contains(test.typeName));
            assertEquals("bar", data.get("http://foo"));
        });
    }

    @Test
    public void defineTypeForbidden() {
        //Given
        DefineTypeTest test = new DefineTypeTest(database, authenticationAPI, NON_TYPE_DEFINITION_ROLES, types);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    public void getTypesOk() {
        //Given
        GetTypesTest test = new GetTypesTest(database, authenticationAPI, null, false, false, false, READ_TYPES_IN_PROGRESS_ROLES, types, instances);

        //When
        test.execute(() -> {
            List<NormalizedJsonLd> response = test.assureValidPayload(test.response);
            assertEquals(1, response.size());
            assertTrue(response.get(0).identifiers().contains(TestDataFactory.TEST_TYPE));
        });
    }

    @Test
    public void getTypesForbidden() {
        //Given
        GetTypesTest test = new GetTypesTest(database, authenticationAPI, null, false, false, false, NON_READ_TYPES_IN_PROGRESS_ROLES, types, instances);

        //When
        test.execute(() -> {
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }


    @Test
    public void getTypesBySpaceOk() {
        //Given
        GetTypesTest test = new GetTypesTest(database, authenticationAPI, new SpaceName("functionalityTest"), false, false,false,  READ_TYPES_IN_PROGRESS_ROLES, types, instances);

        //When
        test.execute(() -> {
            //Then
            List<NormalizedJsonLd> response = test.assureValidPayload(test.response);
            assertEquals(1, response.size());
            assertTrue(response.get(0).identifiers().contains(TestDataFactory.TEST_TYPE));
        });

    }

    @Test
    public void getTypesBySpaceForbidden() {
        //Given
        GetTypesTest test = new GetTypesTest(database, authenticationAPI, new SpaceName("functionalityTest"), false, false,false, NON_READ_TYPES_IN_PROGRESS_ROLES, types, instances);

        //When
        test.execute(() -> {
            //Then
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }


    @Test
    public void getTypesWithPropertiesOk() {
        //Given
        GetTypesTest test = new GetTypesTest(database, authenticationAPI, null, true, false, true, READ_TYPES_IN_PROGRESS_ROLES, types, instances);

        //When
        test.execute(() -> {
            //Then
            List<NormalizedJsonLd> response = test.assureValidPayload(test.response);
            assertEquals(1, response.size());
            assertTrue(response.get(0).identifiers().contains(TestDataFactory.TEST_TYPE));
            List<NormalizedJsonLd> properties = response.get(0).getAsListOf(EBRAINSVocabulary.META_PROPERTIES, NormalizedJsonLd.class);
            assertNotNull(properties);
            assertTrue(properties.size() > smallPayload);
        });
    }

    @Test
    public void getTypesWithPropertiesForbidden() {
        //Given
        GetTypesTest test = new GetTypesTest(database, authenticationAPI, null, true, false, true, NON_READ_TYPES_IN_PROGRESS_ROLES, types, instances);

        //When
        test.execute(() -> {
            //Then
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }

    @Test
    public void getTypesBySpaceWithPropertiesOk() {
        //Given
        GetTypesTest test = new GetTypesTest(database, authenticationAPI, new SpaceName("functionalityTest"), true, false, true, READ_TYPES_IN_PROGRESS_ROLES, types, instances);

        //When
        test.execute(() -> {
            //Then
            List<NormalizedJsonLd> response = test.assureValidPayload(test.response);
            assertEquals(1, response.size());
            assertTrue(response.get(0).identifiers().contains(TestDataFactory.TEST_TYPE));
        });
    }

    @Test
    public void getTypesBySpaceWithPropertiesForbidden() {
        //Given
        GetTypesTest test = new GetTypesTest(database, authenticationAPI, new SpaceName("functionalityTest"), true, false, true, NON_READ_TYPES_IN_PROGRESS_ROLES, types, instances);

        //When
        test.execute(() -> {
            //Then
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }



    @Test
    public void getTypesByNameOk() {
        //Given
        GetTypesByNameTest test = new GetTypesByNameTest(database, authenticationAPI, null, false, READ_TYPES_IN_PROGRESS_ROLES, types, instances);

        //When
        test.execute(()->{

            //Then
            Map<String, Result<NormalizedJsonLd>> map = test.assureValidPayload(test.response);
            test.assureValidPayload(map.get(TestDataFactory.TEST_TYPE));
        });
    }

    @Test
    public void getTypesByNameForbidden() {
        //Given
        GetTypesByNameTest test = new GetTypesByNameTest(database, authenticationAPI, null, false, NON_READ_TYPES_IN_PROGRESS_ROLES, types, instances);

        //When
        test.execute(()->{
            //Then
            Result.Error error = test.response.getData().get(TestDataFactory.TEST_TYPE).getError();
            assertNotNull(error);
            assertEquals(403, error.getCode());
        });
    }

}

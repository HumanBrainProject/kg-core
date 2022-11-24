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

package eu.ebrains.kg.core.controller;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.Tuple;
import eu.ebrains.kg.commons.api.GraphDBSpaces;
import eu.ebrains.kg.commons.api.PrimaryStoreEvents;
import eu.ebrains.kg.commons.model.external.spaces.SpaceInformation;
import eu.ebrains.kg.commons.model.internal.spaces.Space;
import eu.ebrains.kg.test.assertions.FunctionalityAssertions;
import eu.ebrains.kg.test.factory.SpaceFactory;
import eu.ebrains.kg.test.factory.UserFactory;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CoreSpaceControllerTest {

    private final Stream<Tuple<AuthContext, Space>> adminRightCombinations =  Stream.of(
            //Any user should have access to their myspace
            new Tuple<>(UserFactory.noPermissionUser(), SpaceFactory.myspace()),

            //Admins should have access to their myspace
            new Tuple<>(UserFactory.globalAdmin(), SpaceFactory.myspace()),

            //Admins should have access to any space available
            new Tuple<>(UserFactory.globalAdmin(), SpaceFactory.foobar())
    );

    private final Stream<Tuple<AuthContext, Space>> noRightsCombinations =  Stream.of(
            //Users with no permissions shouldn't have access to "foobar"
            new Tuple<>(UserFactory.noPermissionUser(), SpaceFactory.foobar())
    );




    @TestFactory
    Stream<DynamicTest> usersHavingAllRightsForSpace(){
        return adminRightCombinations.map(t ->
            DynamicTest.dynamicTest(String.format("All rights for user %s in space %s", t.getA().getUserWithRoles().getUser().getDisplayName(), t.getB().getName().getName()), () -> {
                //given
                CoreSpaceController controller = new CoreSpaceController(Mockito.mock(GraphDBSpaces.Client.class), Mockito.mock( PrimaryStoreEvents.Client.class), t.getA());
                Space space = t.getB();

                //when
                final SpaceInformation spaceInformation = controller.translateSpaceToSpaceInformation(space, true);

                //then
                FunctionalityAssertions.assertHasAdminFunctionalities(spaceInformation.getPermissions());
            })
        );
    }


    @TestFactory
    Stream<DynamicTest> usersHavingNoRightsForSpace(){
        return noRightsCombinations.map(t ->
                DynamicTest.dynamicTest(String.format("No rights for user %s in space %s", t.getA().getUserWithRoles().getUser().getDisplayName(), t.getB().getName().getName()), () -> {
                    //given
                    CoreSpaceController controller = new CoreSpaceController(Mockito.mock(GraphDBSpaces.Client.class), Mockito.mock( PrimaryStoreEvents.Client.class), t.getA());
                    Space space = t.getB();

                    //when
                    final SpaceInformation spaceInformation = controller.translateSpaceToSpaceInformation(space, true);

                    //then
                    assertTrue(spaceInformation.getPermissions().isEmpty());
                })
        );
    }

}
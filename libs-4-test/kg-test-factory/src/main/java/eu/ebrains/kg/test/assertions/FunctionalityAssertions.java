/*
 * Copyright 2022 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.test.assertions;

import eu.ebrains.kg.commons.permission.Functionality;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.List;

public class FunctionalityAssertions {

    private static final List<Functionality> EXPECTED_ADMIN_FUNCTIONALITIES = Arrays.asList(Functionality.READ, Functionality.CREATE, Functionality.WRITE, Functionality.RELEASE, Functionality.UNRELEASE);

    public static void assertHasAdminFunctionalities(List<Functionality> functionalities){
        List<Functionality> expectedFunctionalities = Arrays.asList(Functionality.READ, Functionality.CREATE, Functionality.WRITE, Functionality.RELEASE, Functionality.UNRELEASE);
        Assertions.assertTrue(functionalities.containsAll(expectedFunctionalities));

    }



}
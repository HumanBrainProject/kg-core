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

package eu.ebrains.kg.graphdb.commons.controller;


import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PermissionsControllerTest  {



    @Test
    public void testRemoveSpacesWithoutReadAccessReduce() {
        Permissions permissions = Mockito.mock(Permissions.class);
        PermissionsController permissionsController = new PermissionsController(permissions);
        Set<SpaceName> readableSpaces = Collections.singleton(new SpaceName("canRead"));
        Mockito.when(permissions.getSpacesForPermission(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(readableSpaces);

        Set<SpaceName> spaceNames = permissionsController.removeSpacesWithoutReadAccess(new HashSet<>(Arrays.asList(new SpaceName("anotherSpace"), new SpaceName("canRead"))), null, DataStage.IN_PROGRESS);

        Assert.assertEquals(readableSpaces, spaceNames);
    }

    @Test
    public void testRemoveSpacesWithoutReadAccessNone() {
        Permissions permissions = Mockito.mock(Permissions.class);
        PermissionsController permissionsController = new PermissionsController(permissions);
        Set<SpaceName> readableSpaces = Collections.singleton(new SpaceName("canRead"));
        Mockito.when(permissions.getSpacesForPermission(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(readableSpaces);

        Set<SpaceName> spaceNames = permissionsController.removeSpacesWithoutReadAccess(new HashSet<>(Arrays.asList(new SpaceName("anotherSpace"), new SpaceName("evenAnotherSpace"))), null, DataStage.IN_PROGRESS);

        Assert.assertEquals(Collections.emptySet(), spaceNames);
    }
}
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

package eu.ebrains.kg.test.factory;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import org.mockito.Mockito;

import java.util.ArrayList;

public class UserFactory {

    public static AuthContext noPermissionUser() {
        final AuthContext authContext = Mockito.mock(AuthContext.class);
        final ArrayList<String> userRoles = new ArrayList<>();
        //Every user is the admin of its private space.
        userRoles.add("private-nopermission:admin");
        UserWithRoles userWithRoles = new UserWithRoles(new User("jackCantDoAThing", "jackCantDoAThing", "jackCantDoAThing@kg.ebrains.eu", "Jack", "Can't Do A Thing", "nopermission"), userRoles, null, null);
        Mockito.doReturn(userWithRoles).when(authContext).getUserWithRoles();
        return authContext;
    }


    public static AuthContext globalAdmin() {
        final AuthContext authContext = Mockito.mock(AuthContext.class);
        final ArrayList<String> userRoles = new ArrayList<>();
        //Every user is the admin of its private space.
        userRoles.add(":admin");
        UserWithRoles userWithRoles = new UserWithRoles(new User("johnCanDoEverything", "johnCanDoEverything", "johnCanDoEverything@kg.ebrains.eu", "John", "Can Do Everything", "admin"), userRoles, null, null);
        Mockito.doReturn(userWithRoles).when(authContext).getUserWithRoles();
        return authContext;
    }
}

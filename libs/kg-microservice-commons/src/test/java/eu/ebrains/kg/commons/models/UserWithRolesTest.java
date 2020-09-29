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

package eu.ebrains.kg.commons.models;

import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.permission.roles.ClientRole;
import eu.ebrains.kg.commons.permission.roles.UserRole;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;


public class UserWithRolesTest {

    List<String> clientAdminRoles = Collections.singletonList(ClientRole.ADMIN.toRole().getName());
    SpaceName space = new SpaceName("test");
    User user = new User("testUser", "Test", "test@test.xy", "Test", "User", null);

    private List<String> getUserRoles(UserRole role, SpaceName space){
        return Collections.singletonList(role.toRole(space).getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailWhenNotAClient(){
        //Given
        UserWithRoles userWithRoles = new UserWithRoles(user, getUserRoles(UserRole.ADMIN, null), clientAdminRoles, "testClient");

        //when
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();
    }

    @Test
    public void testEvaluatePermissionsFullUserAccess(){
        //Given
        UserWithRoles userWithRoles = new UserWithRoles(user, getUserRoles(UserRole.ADMIN, null), clientAdminRoles, "testClient");

        //when
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();

        //Then
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.READ, null, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.WRITE, null, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.RELEASE, null, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.CREATE, null, null)));
        assertFalse("Client permissions such as \"create client\" shouldn't be accessible even by an admin user", permissions.contains(new FunctionalityInstance(Functionality.CREATE_CLIENT, null, null)));
    }

    @Test
    public void testEvaluatePermissionsFullServiceAccountAccess(){
        //Given
        UserWithRoles userWithRoles = new UserWithRoles(user, clientAdminRoles, clientAdminRoles, "testClient");

        //when
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();

        //Then
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.READ, null, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.WRITE, null, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.RELEASE, null, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.CREATE, null, null)));
        assertTrue("Client permissions such as \"create client\" should be accessible by admin clients", permissions.contains(new FunctionalityInstance(Functionality.CREATE_CLIENT, null, null)));
    }

    @Test
    public void testEvaluatePermissionsSpaceUserAccess(){
        //Given
        UserWithRoles userWithRoles = new UserWithRoles(user, getUserRoles(UserRole.ADMIN, space), clientAdminRoles, "testClient");

        //when
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();

        //Then
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.READ, space, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.WRITE, space, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.RELEASE, space, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.CREATE, space, null)));

        //Ensure there is no global permissions...
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.READ, null, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.WRITE, null, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.RELEASE, null, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.CREATE, null, null)));
    }

    @Test
    public void testEvaluatePermissionsSpaceReviewUserAccess(){
        //Given
        UserWithRoles userWithRoles = new UserWithRoles(user, getUserRoles(UserRole.REVIEWER, space), clientAdminRoles, "testClient");

        //when
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();

        //Then
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.READ, space, null)));

        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.WRITE, space, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.RELEASE, space, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.CREATE, space, null)));

        //Ensure there is no global permissions either...
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.READ, null, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.WRITE, null, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.RELEASE, null, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.CREATE, null, null)));
    }

}
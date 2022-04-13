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

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.commons.SetupLogic;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.roles.Role;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.core.api.instances.TestContext;
import org.junit.Assert;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.util.Assert.*;

public abstract class AbstractTest {

    protected static final int smallPayload = 5;
    protected static final int averagePayload = 25;
    protected static final int bigPayload = 100;

    protected ExtendedResponseConfiguration defaultResponseConfiguration = new ExtendedResponseConfiguration();
    protected PaginationParam defaultPaginationParam = new PaginationParam().setFrom(0).setSize(20l);

    public final static List<String> ADMIN_ROLE = Collections.singletonList(RoleMapping.ADMIN.toRole(null).getName());
    public final static List<String> ADMIN_CLIENT_ROLE = Collections.singletonList(RoleMapping.ADMIN.toRole(null).getName());

    private List<Role> currentRoles;

    protected final TestContext testContext;

    protected final static UUID USER_ID=UUID.randomUUID();


    public AbstractTest(TestContext testContext) {
        this.testContext = testContext;
        this.defaultResponseConfiguration.setReturnEmbedded(true).setReturnPermissions(false).setReturnAlternatives(false).setReturnPayload(true);
    }

    protected void beAdmin() {
        User user = new User("bobEverythingGoes", "Admin", "fakeAdmin@ebrains.eu", "Bob Everything", "Goes", "admin");
        final List<UUID> invitationRoles = testContext.getAuthenticationRepository().getInvitationRoles(USER_ID.toString());
        UserWithRoles userWithRoles = new UserWithRoles(user, ADMIN_ROLE, ADMIN_CLIENT_ROLE, invitationRoles,"testClient");
        Mockito.doAnswer(a -> user).when(testContext.getAuthentication()).getMyUserInfo();
        Mockito.doAnswer(a -> userWithRoles).when(testContext.getAuthentication()).getRoles(Mockito.anyBoolean());
    }

    protected void beInCurrentRole() {
        if (currentRoles == null || currentRoles.isEmpty()) {
            beUnauthorized();
        } else if (currentRoles.size()==1 && currentRoles.get(0).equals(RoleMapping.ADMIN.toRole(null))) {
            beAdmin();
        } else {
            User user = new User("alice", "Alice", "fakeAlice@ebrains.eu", "Alice", "User", USER_ID.toString());
            final List<UUID> invitationRoles = testContext.getAuthenticationRepository().getInvitationRoles(USER_ID.toString());
            UserWithRoles userWithRoles = new UserWithRoles(user, currentRoles.stream().filter(Objects::nonNull).map(Role::getName).collect(Collectors.toList()), ADMIN_CLIENT_ROLE, invitationRoles,"testClient");
            Mockito.doAnswer(a -> user).when(testContext.getAuthentication()).getMyUserInfo();
            Mockito.doAnswer(a -> userWithRoles).when(testContext.getAuthentication()).getRoles(Mockito.anyBoolean());
        }
    }

    protected void beUnauthorized() {
        User user = new User("joeCantDoAThing", "Joe Cant Do A Thing", "fakeUnauthorized@ebrains.eu", "Joe Cant Do A", "Thing",  USER_ID.toString());
        //It's the user not having any righexts - the client is still the same with full rights
        final List<UUID> invitationRoles = testContext.getAuthenticationRepository().getInvitationRoles(USER_ID.toString());
        UserWithRoles userWithRoles = new UserWithRoles(user, Collections.emptyList(), ADMIN_CLIENT_ROLE, invitationRoles,"testClient");
        Mockito.doAnswer(a -> user).when(testContext.getAuthentication()).getMyUserInfo();
        Mockito.doAnswer(a -> userWithRoles).when(testContext.getAuthentication()).getRoles(Mockito.anyBoolean());
    }

    protected abstract void setup();

    protected abstract void run();

    public void execute(Class<? extends Exception> expectedException){
        execute(null, expectedException);
    }

    public void execute(Assertion assertion){
        execute(assertion, null);
    }

    private void doExecute(Assertion assertion, Class<? extends Exception> expectedException){
        clearDatabase();
        testContext.getSetupLogics().forEach(SetupLogic::setup);
        beAdmin();
        setup();
        beInCurrentRole();
        try {
            run();
            if (assertion != null) {
                assertion.then();
            }
            if(expectedException!=null){
                Assert.fail(String.format("Was expecting %s exception but it wasn't triggered", expectedException.getName()));
            }
        } catch (Exception e) {
            if(!e.getClass().equals(expectedException)){
                throw new RuntimeException(String.format("Failing test with role %s", currentRoles.stream().map(Role::getName).collect(Collectors.joining(", "))), e);
            }
        }
    }

    private void execute(Assertion assertion, Class<? extends Exception> expectedException) {
        final Collection<List<Role>> roleCollections = testContext.getRoleCollections();
        if(roleCollections.isEmpty()){
            //We have no roles - which means, we run it "unauthorized"
            doExecute(assertion, expectedException);
        }
        else {
            for (List<Role> roles : testContext.getRoleCollections()) {
                currentRoles = roles;
                doExecute(assertion, expectedException);
            }
        }
    }


    public <T> T assureValidPayload(Result<T> result) {
        notNull(result, "The response body shouldn't be null");
        T data = result.getData();
        notNull(result, "The data section of the body shouldn't be null");
        return data;
    }

    public <T> T assureValidPayload(ResponseEntity<Result<T>> response) {
        notNull(response, "Response shouldn't be null");
        return assureValidPayload(response.getBody());
    }

    public NormalizedJsonLd assureValidPayloadIncludingId(ResponseEntity<Result<NormalizedJsonLd>> response) {
        NormalizedJsonLd data = assureValidPayload(response);
        JsonLdId id = data.id();
        notNull(id, "The id shouldn't be null when creating an instance");
        return data;
    }

    private void clearDatabase() {
        testContext.getDatabaseProxies().forEach(ArangoDatabaseProxy::removeDatabase);
        if(testContext.getCacheManager()!=null){
            //Also remove the caches to ensure we are in sync with the database (both empty)
            testContext.getCacheManager().getCacheNames().forEach(c -> testContext.getCacheManager().getCache(c).clear());
        }
    }
}

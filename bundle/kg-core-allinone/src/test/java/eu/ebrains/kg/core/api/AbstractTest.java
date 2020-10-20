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

package eu.ebrains.kg.core.api;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.roles.Role;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
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

    protected IngestConfiguration defaultIngestConfiguration = new IngestConfiguration().setNormalizePayload(false).setDeferInference(false);
    protected ResponseConfiguration defaultResponseConfiguration = new ResponseConfiguration().setReturnEmbedded(true).setReturnPermissions(false).setReturnAlternatives(false).setReturnPayload(true);
    protected PaginationParam defaultPaginationParam = new PaginationParam().setFrom(0).setSize(20l);

    private final static List<String> ADMIN_ROLE = Collections.singletonList(RoleMapping.ADMIN.toRole(null).getName());
    private final static List<String> ADMIN_CLIENT_ROLE = Arrays.asList(RoleMapping.ADMIN.toRole(null).getName(), RoleMapping.IS_CLIENT.toRole(null).getName());

    private List<Role> currentRoles;
    private final ToAuthentication authenticationSvc;
    private final Collection<List<Role>> roleCollections;
    private final ArangoDB.Builder database;

    public AbstractTest(ArangoDB.Builder database, ToAuthentication authenticationSvc, RoleMapping[] roleMappings) {
        this(database, authenticationSvc, Arrays.stream(roleMappings).map(r -> Collections.singletonList(r.toRole(null))).collect(Collectors.toSet()));
    }

    public AbstractTest(ArangoDB.Builder database, ToAuthentication authenticationSvc, Collection<List<Role>> roleCollections) {
        this.authenticationSvc = authenticationSvc;
        this.database = database;
        this.roleCollections = roleCollections;
    }

    protected void beAdmin() {
        User user = new User("bobEverythingGoes", "Bob Everything Goes", "fakeAdmin@ebrains.eu", "Bob Everything", "Goes", "admin");
        UserWithRoles userWithRoles = new UserWithRoles(user, ADMIN_ROLE, ADMIN_CLIENT_ROLE, "testClient");
        Mockito.doAnswer(a -> userWithRoles).when(authenticationSvc).getUserWithRoles();
    }

    protected void beInCurrentRole() {
        if (currentRoles == null || currentRoles.isEmpty()) {
            beUnauthorized();
        } else if (currentRoles.size()==1 && currentRoles.get(0).equals(RoleMapping.ADMIN.toRole(null))) {
            beAdmin();
        } else {
            User user = new User("alice", "Alice ", "fakeAlice@ebrains.eu", "Alice", "User", "alice");
            UserWithRoles userWithRoles = new UserWithRoles(user, currentRoles.stream().filter(Objects::nonNull).map(Role::getName).collect(Collectors.toList()), ADMIN_CLIENT_ROLE, "testClient");
            Mockito.doAnswer(a -> userWithRoles).when(authenticationSvc).getUserWithRoles();
        }
    }

    protected void beUnauthorized() {
        User user = new User("joeCantDoAThing", "Joe Cant Do A Thing", "fakeUnauthorized@ebrains.eu", "Joe Cant Do A", "Thing", "unauthorized");
        //It's the user not having any righexts - the client is still the same with full rights
        UserWithRoles userWithRoles = new UserWithRoles(user, Collections.emptyList(), ADMIN_CLIENT_ROLE, "testClient");
        Mockito.doAnswer(a -> userWithRoles).when(authenticationSvc).getUserWithRoles();
    }

    protected abstract void setup();

    protected abstract void run();

    public void execute(Class<? extends Exception> expectedException){
        execute(null, expectedException);
    }

    public void execute(Assertion assertion){
        execute(assertion, null);
    }

    private void execute(Assertion assertion, Class<? extends Exception> expectedException) {
        for (List<Role> roles : roleCollections) {
            currentRoles = roles;
            clearDatabase();
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
                    throw new RuntimeException(String.format("Failing test with role %s", currentRoles), e);
                }
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
        ArangoDB arango = database.build();
        arango.getDatabases().stream().filter(db -> db.startsWith("kg")).forEach(db -> {
            System.out.println(String.format("Removing database %s", db));
            arango.db(db).drop();
        });
    }
}
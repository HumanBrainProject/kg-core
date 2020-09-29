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

package eu.ebrains.kg.testutils;

import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.UserAuthToken;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.springframework.util.Assert.*;

public abstract class AbstractFunctionalityTest extends AbstractSystemTest {

    @MockBean
    protected ToAuthentication authenticationSvc;

    private AuthTokens authTokens;

    protected abstract void authenticate();

    protected <T> T assureValidPayload(Result<T> result){
        notNull(result, "The response body shouldn't be null");
        T data = result.getData();
        notNull(result, "The data section of the body shouldn't be null");
        return data;
    }

    protected <T> T assureValidPayload(ResponseEntity<Result<T>> response){
        notNull(response, "Response shouldn't be null");
        return assureValidPayload(response.getBody());
    }

    protected NormalizedJsonLd assureValidPayloadIncludingId(ResponseEntity<Result<NormalizedJsonLd>> response) {
        NormalizedJsonLd data = assureValidPayload(response);
        JsonLdId id = data.id();
        notNull(id, "The id shouldn't be null when creating an instance");
        return data;
    }

    @Before
    public void setup() {
        super.setup();
        this.authTokens=new AuthTokens();
        this.authTokens.setUserAuthToken(new UserAuthToken("userToken"));
        this.authTokens.setClientAuthToken(new ClientAuthToken("clientToken"));
        Mockito.doAnswer(a -> authTokens).when(authContext).getAuthTokens();
        Mockito.doCallRealMethod().when(authContext).getUserId();
        Mockito.doCallRealMethod().when(authContext).getClientSpace();
        Mockito.doAnswer(a -> authenticationSvc.getUserWithRoles()).when(authContext).getUserWithRoles();
        authenticate();
    }

    private final static List<String> ADMIN_ROLE = Collections.singletonList(RoleMapping.ADMIN.toRole(null).getName());

    public void beAdmin(){
        User user = new User("bobEverythingGoes", "Bob Everything Goes", "fakeAdmin@ebrains.eu", "Bob Everything", "Goes", "admin");
        UserWithRoles userWithRoles = new UserWithRoles(user, ADMIN_ROLE, ADMIN_ROLE, "testClient");
        Mockito.doAnswer(a -> userWithRoles).when(authenticationSvc).getUserWithRoles();
    }

    public void beUnauthorized(){
        User user = new User("joeCantDoAThing", "Joe Cant Do A Thing", "fakeUnauthorized@ebrains.eu", "Joe Cant Do A", "Thing", "unauthorized");
        //It's the user not having any rights - the client is still the same with full rights
        UserWithRoles userWithRoles = new UserWithRoles(user, Collections.emptyList(), ADMIN_ROLE, "testClient");
        Mockito.doAnswer(a -> userWithRoles).when(authenticationSvc).getUserWithRoles();
    }

}

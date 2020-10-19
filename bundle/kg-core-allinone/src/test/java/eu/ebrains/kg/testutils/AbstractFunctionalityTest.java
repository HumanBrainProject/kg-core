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
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.UserAuthToken;
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

public abstract class AbstractFunctionalityTest extends AbstractSystemTest {

    @MockBean
    protected ToAuthentication authenticationSvc;

    private AuthTokens authTokens;

    @Before
    public void setup() {
        this.authTokens=new AuthTokens();
        this.authTokens.setUserAuthToken(new UserAuthToken("userToken"));
        this.authTokens.setClientAuthToken(new ClientAuthToken("clientToken"));
        Mockito.doAnswer(a -> authTokens).when(authContext).getAuthTokens();
        Mockito.doCallRealMethod().when(authContext).getUserId();
        Mockito.doCallRealMethod().when(authContext).getClientSpace();
        Mockito.doAnswer(a -> authenticationSvc.getUserWithRoles()).when(authContext).getUserWithRoles();
    }

}

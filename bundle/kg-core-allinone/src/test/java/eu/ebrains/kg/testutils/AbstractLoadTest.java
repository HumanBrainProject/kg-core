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

package eu.ebrains.kg.testutils;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.UserAuthToken;
import eu.ebrains.kg.core.api.AbstractTest;
import eu.ebrains.kg.metrics.MethodExecution;
import eu.ebrains.kg.metrics.PerformanceTestUtils;
import eu.ebrains.kg.metrics.TestInformation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.util.*;

public abstract class AbstractLoadTest extends AbstractSystemTest {

    protected final static int smallBatchInsertion = 10;
    protected final static int batchInsertion = 100;
    protected final static int bigBatchInsertion = 10000;

    @MockBean
    protected TestInformation testInformation;

    protected String testRunId;

    public static PerformanceTestUtils utils;

    private Map<UUID, List<MethodExecution>> metrics;


    @BeforeClass
    public static void loadResources() {
        utils = new PerformanceTestUtils();
    }

    @AfterClass
    public static void concludeReporting() throws IOException {
        utils.commitReport();
    }

    @Before
    public void setup() {
        clearDatabase();
        //We execute the load tests as an admin...
        User user = new User("bobEverythingGoes", "Bob Everything Goes", "fakeAdmin@ebrains.eu", "Bob Everything", "Goes", "admin");
        UserWithRoles userWithRoles = new UserWithRoles(user, AbstractTest.ADMIN_ROLE, AbstractTest.ADMIN_CLIENT_ROLE, "testClient");
        Mockito.doReturn(user).when(authenticationAPI).getMyUserInfo();
        Mockito.doReturn(userWithRoles).when(authenticationAPI).getRoles(false);
        AuthTokens authTokens=new AuthTokens();
        authTokens.setUserAuthToken(new UserAuthToken("userToken"));
        authTokens.setClientAuthToken(new ClientAuthToken("clientToken"));
        Mockito.doReturn(authTokens).when(authTokenContext).getAuthTokens();
        testRunId = UUID.randomUUID().toString();
        Mockito.doReturn(testRunId).when(testInformation).getRunId();
        metrics = Collections.synchronizedMap(new HashMap<>());
        Mockito.doReturn(metrics).when(testInformation).getMethodExecutions();
    }

    @After
    public void tearDown(){
        if(!metrics.isEmpty()) {
            utils.plotMetrics(metrics);
        }
    }


    private void clearDatabase() {
        arangoDatabaseProxyList.forEach(ArangoDatabaseProxy::removeDatabase);
    }

}

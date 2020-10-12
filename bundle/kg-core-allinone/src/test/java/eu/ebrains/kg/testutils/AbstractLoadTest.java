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
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
import eu.ebrains.kg.metrics.MethodExecution;
import eu.ebrains.kg.metrics.PerformanceTestUtils;
import eu.ebrains.kg.metrics.TestInformation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
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

    public static ThreadLocal<AuthTokens> authTokens = new ThreadLocal<>(){
        @Override
        protected AuthTokens initialValue()
        {
            return null;
        }
    };

    @Autowired
    protected ToAuthentication authenticationSvc;

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
        Mockito.doReturn(authenticationSvc.getUserWithRoles()).when(authContext).getUserWithRoles();
        Mockito.doAnswer(a -> authTokens.get()).when(authContext).getAuthTokens();
        Mockito.doAnswer(a -> {
            Object argument = a.getArgument(0);
            if(argument instanceof AuthTokens){
                authTokens.set((AuthTokens)argument);
            }
            return null;
        }).when(authContext).setAuthTokens(Mockito.any());
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


}

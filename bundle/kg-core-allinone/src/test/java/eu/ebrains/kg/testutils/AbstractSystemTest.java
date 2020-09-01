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

import com.arangodb.ArangoDB;
import eu.ebrains.kg.KgCoreAllInOne;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.IngestConfiguration;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.ResponseConfiguration;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
import eu.ebrains.kg.core.api.Extra;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.metrics.MethodExecution;
import eu.ebrains.kg.metrics.PerformanceTestUtils;
import eu.ebrains.kg.metrics.TestInformation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = KgCoreAllInOne.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd=changeMe", "eu.ebrains.kg.arango.port=9111", "eu.ebrains.kg.develop=true", "opentracing.jaeger.enabled=false", "arangodb.connections.max=1"})
public abstract class AbstractSystemTest {

    protected final static int smallBatchInsertion = 10;
    protected final static int batchInsertion = 100;
    protected final static int bigBatchInsertion = 10000;

    @MockBean
    protected AuthContext authContext;

    @MockBean
    protected TestInformation testInformation;

    @Autowired
    protected Instances instances;

    @Autowired
    protected Extra extra;

    @Autowired
    protected ToAuthentication authenticationSvc;

    @Autowired
    @Qualifier("arangoBuilderForGraphDB")
    protected ArangoDB.Builder arangoBuilder;

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

    @BeforeClass
    public static void loadResources() {
        utils = new PerformanceTestUtils();
    }

    @AfterClass
    public static void concludeReporting() throws IOException {
        utils.commitReport();
    }

    private void clearDatabase() {
        ArangoDB arango = arangoBuilder.build();
        arango.getDatabases().stream().filter(db -> db.startsWith("kg")).forEach(db -> {
            System.out.println(String.format("Removing database %s", db));
            arango.db(db).drop();
        });
    }

    @Before
    public void setup() {
        clearDatabase();
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

    protected PaginationParam EMPTY_PAGINATION = new PaginationParam();
    protected ResponseConfiguration DEFAULT_RESPONSE_CONFIG = new ResponseConfiguration();
    protected IngestConfiguration DEFAULT_INGEST_CONFIG = new IngestConfiguration().setNormalizePayload(false);

    protected String type = "https://core.kg.ebrains.eu/TestPayload";

    protected List<NormalizedJsonLd> getAllInstancesFromInProgress(ExposedStage stage) {
        return this.instances.getInstances(stage, type, null, DEFAULT_RESPONSE_CONFIG, EMPTY_PAGINATION).getData();
    }


    // INSERTION
    protected void testInsert(int numberOfFields, int numberOfIterations, boolean parallelize, boolean deferInference, boolean normalize, PerformanceTestUtils.Link link) throws IOException {
        StringBuilder title = new StringBuilder();
        switch(numberOfFields){
            case smallBatchInsertion:
                title.append("Small");
                break;
            case averagePayload:
                title.append("Average");
                break;
            case bigPayload:
                title.append("Big");
                break;
            default:
                title.append(String.format("%d fields", numberOfFields));
                break;
        }
        title.append(", ");
        title.append(link == null ? "no link" : link == PerformanceTestUtils.Link.PREVIOUS ? "immediate link" : "deferred link");
        title.append(", ");
        title.append(deferInference ? "deferred / async" : "sync");
        title.append(", ");
        title.append(normalize ? "normalization" : "no normalization");
        utils.addSection(title.toString());

        // When
        List<ResponseEntity<Result<NormalizedJsonLd>>> results = utils.executeMany(numberOfFields, normalize, numberOfIterations, parallelize, link, p -> instances.createNewInstance(p, "test", DEFAULT_RESPONSE_CONFIG, new IngestConfiguration().setNormalizePayload(normalize).setDeferInference(deferInference), null), authTokens);

        //Then
        for (int i = 0; i < results.size(); i++) {
            System.out.printf("Result %d: %d ms%n", i, Objects.requireNonNull(results.get(i).getBody()).getDurationInMs());
        }

        if (deferInference) {
            triggerDeferredInference();
        }
    }

    protected static final int smallPayload = 5;
    protected static final int averagePayload = 25;
    protected static final int bigPayload = 100;

    protected void triggerDeferredInference() {
        Instant start = Instant.now();
        System.out.println("Trigger inference");
        extra.triggerDeferredInference(true, "test");
        Instant end = Instant.now();
        System.out.printf("Inference handled in %d ms%n", Duration.between(start, end).toMillis());
    }

}

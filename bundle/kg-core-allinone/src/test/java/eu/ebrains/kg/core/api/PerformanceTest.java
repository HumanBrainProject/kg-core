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
import eu.ebrains.kg.KgCoreAllInOne;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.metrics.TestInformation;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = KgCoreAllInOne.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd=changeMe", "eu.ebrains.kg.arango.port=9111", "eu.ebrains.kg.develop=true", "opentracing.jaeger.enabled=false", "eu.ebrains.kg.metrics=true", "arangodb.connections.max=1"})
public class PerformanceTest {

    private static int smallBatchInsertion = 10;
    private static int batchInsertion = 100;
    private static int bigBatchInsertion = 10000;

    @MockBean
    AuthContext authContext;

    @MockBean
    TestInformation testInformation;

    @Autowired
    private Instances instances;

    @Autowired
    private Extra extra;

    @Autowired
    private Releases releases;

    @Autowired
    private IdUtils idUtils;

    @Autowired
    private ToAuthentication authenticationSvc;

    @Autowired
    @Qualifier("arangoBuilderForGraphDB")
    ArangoDB.Builder arangoBuilder;

    String testRunId;

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
    public static void loadResources() throws URISyntaxException {
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

    PaginationParam EMPTY_PAGINATION = new PaginationParam();
    ResponseConfiguration DEFAULT_RESPONSE_CONFIG = new ResponseConfiguration();
    IngestConfiguration DEFAULT_INGEST_CONFIG = new IngestConfiguration().setNormalizePayload(false);

    String type = "https://core.kg.ebrains.eu/TestPayload";

    private List<NormalizedJsonLd> getAllInstancesFromInProgress(ExposedStage stage) {
        return this.instances.getInstances(stage, type, null, DEFAULT_RESPONSE_CONFIG, EMPTY_PAGINATION).getData();
    }


    // INSERTION
    private void testInsert(int numberOfFields, int numberOfIterations, boolean parallelize, boolean deferInference, boolean normalize, PerformanceTestUtils.Link link) throws IOException {

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

    private static final int smallPayload = 5;
    private static final int averagePayload = 25;
    private static final int bigPayload = 100;


    @Test
    public void testInsertSingleHugePayload() throws IOException {
        utils.addSection("Single huge");
        testInsert(500000, 1, false, false, false, null);
    }

    @Test
    public void testInsertSmallNoLinkSingle() throws IOException {
        utils.addSection("Small, no link, sync, no normalization");
        testInsert(smallPayload, batchInsertion, false, false, false, null);

    }

    @Test
    public void testInsertSmallNoLink() throws IOException {
        utils.addSection("Small, no link, sync, no normalization");
        testInsert(smallPayload, batchInsertion, true, false, false, null);
        System.out.println(metrics);

    }

    @Test
    public void testInsertSmallImmediateLink() throws IOException {
        utils.addSection("Small, immediate link, sync, no normalization");
        testInsert(smallPayload, batchInsertion, true, false, false, PerformanceTestUtils.Link.PREVIOUS);
    }

    @Test
    public void testInsertSmallDeferredLink() throws IOException {
        utils.addSection("Small, deferred link, sync, no normalization");
        testInsert(smallPayload, batchInsertion, true, false, false, PerformanceTestUtils.Link.NEXT);
    }

    @Test
    public void testInsertNoLinkDeferred() throws IOException {
        utils.addSection("Small, no link, async / deferred, no normalization");
        testInsert(smallPayload, batchInsertion, true, true, false, null);
    }

    @Test
    public void testInsertSmallNoLinkNormalize() throws IOException {
        utils.addSection("Small, no link, sync, normalization");
        testInsert(smallPayload, batchInsertion, true, false, true, null);
    }

    @Test
    public void testInsertSmallDeferredLinkNormalize() throws IOException {
        utils.addSection("Small, deferred link, sync, normalization");
        testInsert(smallPayload, batchInsertion, true, false, true, PerformanceTestUtils.Link.NEXT);
    }

    @Test
    public void testInsertSmallNoLinkDeferredNormalize() throws IOException {
        utils.addSection("Small, no link, async / deferred, normalization");
        testInsert(smallPayload, batchInsertion, true, true, true, null);
    }

    @Test
    public void testInsertAverageNoLink() throws IOException {
        testInsert(averagePayload, batchInsertion, true, false, false, null);
    }

    @Test
    public void testInsertAverageNoLinkDeferred() throws IOException {
        utils.addSection("Average, no link, async / deferred, no normalization");
        testInsert(averagePayload, batchInsertion, true, true, false, null);
    }

    @Test
    public void testInsertAverageNoLinkNormalize() throws IOException {
        testInsert(averagePayload, batchInsertion, true, false, true, null);
    }

    @Test
    public void testInsertAverageNoLinkDeferredNormalize() throws IOException {
        testInsert(averagePayload, batchInsertion, true, true, true, null);
    }

    @Test
    public void testInsertBigNoLinkInference() throws IOException {
        utils.addSection("Big, no link, async / deferred, no normalization");
        testInsert(bigPayload, batchInsertion, true, false, false, null);
    }

    @Test
    public void testInsertBigNoLinkInferenceDeferred() throws IOException {
        testInsert(bigPayload, batchInsertion, true, true, false, null);
    }

    @Test
    public void testInsertBigNoLinkInferenceNormalize() throws IOException {
        testInsert(bigPayload, batchInsertion, true, false, true, null);
    }

    @Test
    public void testInsertBigNoLinkInferenceDeferredNormalize() throws IOException {
        testInsert(bigPayload, batchInsertion, true, true, true, null);
    }

    @Test
    public void triggerDeferredInference() {
        Instant start = Instant.now();
        System.out.println("Trigger inference");
        extra.triggerDeferredInference(true, "test");
        Instant end = Instant.now();
        System.out.printf("Inference handled in %d ms%n", Duration.between(start, end).toMillis());
    }


    // DELETION
    @Test
    public void testDeleteSingleAverageNoLink() throws IOException {
        //Given
        testInsert(averagePayload, 1, false, false, false, null);
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);
        //When
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            ResponseEntity<Result<Void>> resultResponseEntity = instances.deleteInstance(idUtils.getUUID(allInstancesFromInProgress.get(i).getId()), null);
            System.out.printf("Result %d: %d ms%n", i, resultResponseEntity.getBody().getDurationInMs());
        }
    }

    @Test
    public void testDeleteSingleAverageNoLinkFullParallelism() throws IOException, InterruptedException {
        //Given
        testInsert(averagePayload, 1, false, false, false, null);
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);

        //When
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            int finalI = i;
            executorService.execute(() -> {
                ResponseEntity<Result<Void>> resultResponseEntity = instances.deleteInstance(idUtils.getUUID(allInstancesFromInProgress.get(finalI).getId()), null);
                System.out.printf("Result %d: %d ms%n", finalI, resultResponseEntity.getBody().getDurationInMs());
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.HOURS);
    }

    // RELEASING
    @Test
    public void testReleaseSingleAverageNoLink() throws IOException {
        //Given
        testInsert(averagePayload, 1, false, false, false, null);
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);
        //When
        long startTime = new Date().getTime();
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            IndexedJsonLdDoc from = IndexedJsonLdDoc.from(allInstancesFromInProgress.get(i));
            ResponseEntity<Result<Void>> resultResponseEntity = releases.releaseInstance(idUtils.getUUID(allInstancesFromInProgress.get(i).getId()), from.getRevision());
            System.out.printf("Result %d: %d ms%n", i, resultResponseEntity.getBody().getDurationInMs());
        }
        System.out.printf("Total time for %d releases: %d ms%n", batchInsertion, new Date().getTime() - startTime);
    }

    @Test
    public void testReleaseSingleAverageNoLinkfullParallelism() throws IOException, InterruptedException {
        //Given
        testInsert(averagePayload, 1, false, false, false, null);
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);

        //When
        long startTime = new Date().getTime();
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            int finalI = i;
            executorService.execute(() -> {
                IndexedJsonLdDoc from = IndexedJsonLdDoc.from(allInstancesFromInProgress.get(finalI));
                ResponseEntity<Result<Void>> resultResponseEntity = releases.releaseInstance(idUtils.getUUID(allInstancesFromInProgress.get(finalI).getId()), from.getRevision());
                System.out.printf("Result %d: %d ms%n", finalI, resultResponseEntity.getBody().getDurationInMs());
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.HOURS);
        System.out.printf("Total time for %d releases: %d ms%n", batchInsertion, new Date().getTime() - startTime);
    }


    @Test
    public void testReleaseSingleBigNoLink() {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(bigPayload, true, 0, null);
        List<ResponseEntity<Result<NormalizedJsonLd>>> l = new ArrayList<>();
        for (int i = 0; i < smallBatchInsertion; i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);
            l.add(instance);
        }

        //When
        long startTime = new Date().getTime();
        for (int i = 0; i < l.size(); i++) {
            JsonLdId id = l.get(i).getBody().getData().getId();
            IndexedJsonLdDoc from = IndexedJsonLdDoc.from(l.get(i).getBody().getData());
            ResponseEntity<Result<Void>> resultResponseEntity = releases.releaseInstance(idUtils.getUUID(id), from.getRevision());
            System.out.println(String.format("Result %d: %d ms", i, resultResponseEntity.getBody().getDurationInMs()));
        }
        System.out.println(String.format("Total time for %d releases: %d ms", batchInsertion, new Date().getTime() - startTime));
    }

    @Test
    public void testUnReleaseSingleAverageNoLink() throws IOException {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(averagePayload, true, 0, null);
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);
        //When
        long startTime = new Date().getTime();
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            ResponseEntity<Result<Void>> resultResponseEntity = releases.unreleaseInstance(idUtils.getUUID(allInstancesFromInProgress.get(i).getId()));
            System.out.println(String.format("Result %d: %d ms", i, resultResponseEntity.getBody().getDurationInMs()));
        }
        System.out.println(String.format("Total time for %d unreleases: %d ms", batchInsertion, new Date().getTime() - startTime));
    }

    @Test
    public void testRealeaseAndUnreleaseAndReReleaseInstance() throws IOException {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(smallPayload, true, 0, null);
        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);
        JsonLdId id = instance.getBody().getData().getId();
        IndexedJsonLdDoc from = IndexedJsonLdDoc.from(instance.getBody().getData());

        //When
        releases.releaseInstance(idUtils.getUUID(id), from.getRevision());
        ResponseEntity<Result<ReleaseStatus>> releaseStatus = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        //Then
        assertEquals(ReleaseStatus.RELEASED.getReleaseStatus(), releaseStatus.getBody().getData().getReleaseStatus());

        releases.unreleaseInstance(idUtils.getUUID(id));
        ResponseEntity<Result<ReleaseStatus>> releaseStatusAfterUnrelease = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        assertEquals(ReleaseStatus.UNRELEASED.getReleaseStatus(), releaseStatusAfterUnrelease.getBody().getData().getReleaseStatus());

        releases.releaseInstance(idUtils.getUUID(id), from.getRevision());
        ResponseEntity<Result<ReleaseStatus>> releaseStatusRerelease = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);
        assertEquals(ReleaseStatus.RELEASED.getReleaseStatus(), releaseStatusRerelease.getBody().getData().getReleaseStatus());
    }

    @Test
    public void testInsertAndDeleteInstance() throws IOException {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(smallPayload, true, 0, null);
        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);
        JsonLdId id = instance.getBody().getData().getId();

        //When
        ResponseEntity<Result<Void>> resultResponseEntity = instances.deleteInstance(idUtils.getUUID(id), null);

        //Then
        assertEquals(HttpStatus.OK, resultResponseEntity.getStatusCode());

        ResponseEntity<Result<NormalizedJsonLd>> instanceById = instances.getInstanceById(idUtils.getUUID(id), ExposedStage.IN_PROGRESS, DEFAULT_RESPONSE_CONFIG);

        assertEquals(HttpStatus.NOT_FOUND, instanceById.getStatusCode());

    }

    @Test
    public void testInsertAndUpdateInstance() throws IOException {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(smallPayload, true, 0, null);

        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);
        JsonLdId id = instance.getBody().getData().getId();

        //When
        JsonLdDoc doc = new JsonLdDoc();
        doc.addProperty("https://core.kg.ebrains.eu/fooE", "fooEUpdated");
        ResponseEntity<Result<NormalizedJsonLd>> resultResponseEntity = instances.contributeToInstancePartialReplacement(doc, idUtils.getUUID(id), false, DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);

        //Then
        assertEquals("fooEUpdated", resultResponseEntity.getBody().getData().getAs("https://core.kg.ebrains.eu/fooE", String.class));
    }

    @Ignore("Failing")
    @Test
    public void testFullCycle() throws IOException {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(smallPayload, true, 0, null);
        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);
        JsonLdId id = instance.getBody().getData().getId();
        IndexedJsonLdDoc from = IndexedJsonLdDoc.from(instance.getBody().getData());

        //When
        //Update
        JsonLdDoc doc = new JsonLdDoc();
        doc.addProperty("https://core.kg.ebrains.eu/fooE", "fooEUpdated");
        ResponseEntity<Result<NormalizedJsonLd>> resultResponseEntity = instances.contributeToInstancePartialReplacement(doc, idUtils.getUUID(id), false, DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);

        //Then
        assertEquals("fooEUpdated", resultResponseEntity.getBody().getData().getAs("https://core.kg.ebrains.eu/fooE", String.class));

        //When
        //Release
        releases.releaseInstance(idUtils.getUUID(id), from.getRevision());
        ResponseEntity<Result<ReleaseStatus>> releaseStatus = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        //Then
        assertEquals(ReleaseStatus.RELEASED.getReleaseStatus(), releaseStatus.getBody().getData().getReleaseStatus());

        //When
        //Unrelease
        releases.unreleaseInstance(idUtils.getUUID(id));
        ResponseEntity<Result<ReleaseStatus>> releaseStatusAfterUnrelease = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        //Then
        assertEquals(ReleaseStatus.UNRELEASED.getReleaseStatus(), releaseStatusAfterUnrelease.getBody().getData().getReleaseStatus());

        //When
        //Delete
        ResponseEntity<Result<Void>> resultResponseEntityDeleted = instances.deleteInstance(idUtils.getUUID(id), null);
        //Then
        assertEquals(HttpStatus.OK, resultResponseEntityDeleted.getStatusCode());

        ResponseEntity<Result<NormalizedJsonLd>> instanceById = instances.getInstanceById(idUtils.getUUID(id), ExposedStage.IN_PROGRESS, DEFAULT_RESPONSE_CONFIG);

        assertEquals(HttpStatus.NOT_FOUND, instanceById.getStatusCode());
    }

}

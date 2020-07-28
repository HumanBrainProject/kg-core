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

import com.google.gson.Gson;
import eu.ebrains.kg.KgCoreAllInOne;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = KgCoreAllInOne.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd=changeMe", "eu.ebrains.kg.arango.port=9111", "eu.ebrains.kg.develop=true"})
public class PerformanceTest {

    private static Path averageNoLink;
    private static Path averageNoLinkUnnormalized;
    private static Path smallNoLink;
    private static Path bigNoLink;

    private static int batchInsertion = 100;

    @MockBean
    AuthContext authContext;

    @Autowired
    private Instances instances;

    @Autowired
    private ToAuthentication authenticationSvc;

    @Before
    public void setup() {
        Mockito.doReturn(authenticationSvc.getUserWithRoles()).when(authContext).getUserWithRoles();
    }

    @BeforeClass
    public static void loadResources() throws URISyntaxException {
        averageNoLink = Paths.get(PerformanceTest.class.getClassLoader().getResource("average_size_payload_no_link.json").toURI());
        averageNoLinkUnnormalized = Paths.get(PerformanceTest.class.getClassLoader().getResource("average_size_payload_no_link_unnormalized.json").toURI());
        smallNoLink = Paths.get(PerformanceTest.class.getClassLoader().getResource("small_size_payload_no_link.json").toURI());
        bigNoLink = Paths.get(PerformanceTest.class.getClassLoader().getResource("big_size_payload_no_link.json").toURI());
    }

    private JsonLdDoc getDoc(Path path) throws IOException {
        return new JsonLdDoc(new Gson().fromJson(Files.lines(path).collect(Collectors.joining("\n")), LinkedHashMap.class));
    }

    // INSERTION

    @Test
    public void testInsertSingleAverageNoLink() throws IOException {

        //Given
        JsonLdDoc payload = getDoc(averageNoLink);

        //When
        List<ResponseEntity<Result<NormalizedJsonLd>>> results = new ArrayList<>();
        for (int i = 0; i < batchInsertion; i++) {
            results.add(instances.createNewInstance(payload, "test", true, false, false, false, false, null));
        }

        //Then
        for (int i = 0; i < results.size(); i++) {
            System.out.println(String.format("Result %d: %d ms", i, results.get(i).getBody().getDurationInMs()));
        }

    }


    @Test
    public void testInsertSingleSmallNoLink() throws IOException {

        //Given
        JsonLdDoc payload = getDoc(smallNoLink);

        //When
        List<ResponseEntity<Result<NormalizedJsonLd>>> results = new ArrayList<>();
        for (int i = 0; i < batchInsertion; i++) {
            results.add(instances.createNewInstance(payload, "test", true, false, false, false, false, null));
        }

        //Then
        for (int i = 0; i < results.size(); i++) {
            System.out.println(String.format("Result %d: %d ms", i, results.get(i).getBody().getDurationInMs()));
        }

    }

    @Test
    public void testInsertSingleAverageNoLinkUnnormalized() throws IOException {

        //Given
        JsonLdDoc payload = getDoc(averageNoLinkUnnormalized);

        //When
        List<ResponseEntity<Result<NormalizedJsonLd>>> results = new ArrayList<>();
        for (int i = 0; i < batchInsertion; i++) {
            results.add(instances.createNewInstance(payload, "test", true, false, false, false, false, null));
        }

        //Then
        for (int i = 0; i < results.size(); i++) {
            System.out.println(String.format("Result %d: %d ms", i, results.get(i).getBody().getDurationInMs()));
        }

    }

    @Test
    public void testInsertBigNoLink() throws IOException {

        //Given
        JsonLdDoc payload = getDoc(bigNoLink);

        //When
        List<ResponseEntity<Result<NormalizedJsonLd>>> results = new ArrayList<>();
        for (int i = 0; i < batchInsertion; i++) {
            results.add(instances.createNewInstance(payload, "test", true, false, false, false, false, null));
        }

        //Then
        for (int i = 0; i < results.size(); i++) {
            System.out.println(String.format("Result %d: %d ms", i, results.get(i).getBody().getDurationInMs()));
        }

    }

    @Test
    public void testInsertBigNoLinkFullParallelism() throws IOException, InterruptedException {

        //Given
        JsonLdDoc payload = getDoc(bigNoLink);

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        //When
        for (int i = 0; i < batchInsertion; i++) {
            executorService.execute(() -> {
                ResponseEntity<Result<NormalizedJsonLd>> result = instances.createNewInstance(payload, "test", true, false, false, false, false, null);
                System.out.println(String.format("Result: %d ms", result.getBody().getDurationInMs()));
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.HOURS);
    }

    @Test
    public void testInsertSingleSmallNoLinkFullParallelism() throws IOException, InterruptedException {

        //Given
        JsonLdDoc payload = getDoc(smallNoLink);

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        //When
        for (int i = 0; i < batchInsertion; i++) {
            executorService.execute(() -> {
                ResponseEntity<Result<NormalizedJsonLd>> result = instances.createNewInstance(payload, "test", true, false, false, false, false, null);
                System.out.println(String.format("Result: %d ms", result.getBody().getDurationInMs()));
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.HOURS);
    }
}

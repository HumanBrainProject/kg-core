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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.inference;

import com.netflix.discovery.EurekaClient;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.docker.SpringDockerComposeRunner;
import eu.ebrains.kg.inference.api.InferenceAPI;
import eu.ebrains.kg.test.GraphDB4Test;
import eu.ebrains.kg.test.TestObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class InferenceTest {

    @Autowired
    EurekaClient discoveryClient;

    @Before
    public void setup() throws IOException {
        new SpringDockerComposeRunner(discoveryClient, true, false, Arrays.asList("arango"), "kg-graphdb-sync", "kg-primarystore", "kg-jsonld").start();
    }

    @Autowired
    GraphDB4Test graphDBSvc;

    @Autowired
    InferenceAPI inference;

    @Autowired
    IdUtils idUtils;

    private final SpaceName space = TestObjectFactory.SIMPSONS;

    @Test
    public void singleObjectWithEmbeddedInsertionInference() throws IOException, URISyntaxException {
        //Given
        UUID homerId = UUID.randomUUID();
        NormalizedJsonLd homer = TestObjectFactory.createJsonLd( "simpsons/homer.json", idUtils.buildAbsoluteUrl(homerId));
        graphDBSvc.upsert(homer, DataStage.NATIVE, homerId, space);

        //When
        List<Event> events = inference.infer(space.getName(), homerId);

        //Then
        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());
        Event event = events.get(0);
        Assert.assertEquals(Event.Type.INSERT, event.getType());
    }

    @Test
    public void twoInstancesInsertInference() throws IOException, URISyntaxException {
        //Given
        UUID bartId = UUID.randomUUID();
        NormalizedJsonLd bart = TestObjectFactory.createJsonLd( "simpsons/bart.json", idUtils.buildAbsoluteUrl(bartId));
        graphDBSvc.upsert(bart, DataStage.NATIVE, bartId, space);


        UUID bartId2 = UUID.randomUUID();
        NormalizedJsonLd bart2 =TestObjectFactory.createJsonLd( "simpsons/bart2.json", idUtils.buildAbsoluteUrl(bartId2));
        graphDBSvc.upsert(bart2, DataStage.NATIVE, bartId2, space);

        NormalizedJsonLd bartUpdate = TestObjectFactory.createJsonLd( "simpsons/bartUpdate.json", idUtils.buildAbsoluteUrl(bartId));
        graphDBSvc.upsert(bartUpdate, DataStage.NATIVE, bartId, space);


        //When
        List<Event> events = inference.infer(space.getName(), bartId);


        //Then
        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());
        Event event = events.get(0);
        Assert.assertEquals(Event.Type.INSERT, event.getType());

        //We assume there are information from both instances in the inferred element now.
        Assert.assertNotNull(event.getData().get("http://schema.org/age"));
        Assert.assertNotNull(event.getData().get("http://schema.org/familyName"));

        //And we ensure, that the updated value (the most recently inserted one) wins.
        Assert.assertEquals("Bartholomew", event.getData().get("http://schema.org/givenName"));

    }


}

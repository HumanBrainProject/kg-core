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

package eu.ebrains.kg;

import com.netflix.discovery.EurekaClient;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.docker.SpringDockerComposeRunner;
import eu.ebrains.kg.indexing.api.Indexing;
import eu.ebrains.kg.test.TestObjectFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class IndexingTest {

    @Autowired
    EurekaClient discoveryClient;

    @Before
    public void setup() throws IOException {
        new SpringDockerComposeRunner(discoveryClient, true, false, Collections.emptyList(),"kg-inference", "kg-graphdb-sync", "kg-primarystore", "kg-jsonld").start();
    }

    @Autowired
    Indexing indexing;

    @Autowired
    IdUtils idUtils;

    private final Space space = TestObjectFactory.SIMPSONS;

    @Test
    public void synchronouslyIndexSingleInstance() {
        //Given

        NormalizedJsonLd homer = TestObjectFactory.createJsonLd(space, "homer.json");

        Event event = new Event(space, UUID.randomUUID(), homer, Event.Type.INSERT, new Date());

        //When
        indexing.indexEvent(new PersistedEvent(event, DataStage.NATIVE, null));

        //Then

    }
//
//    @Test
//    public void synchronouslyIndexTwoInstancesWithInference() {
//        //Given
//
//        NormalizedJsonLd bart = TestObjectFactory.createJsonLd(space, "bart.json");
//        Event event = new Event(bart, Event.Type.INSERT, "user", new Date());
//        indexing.indexEvent(new PersistedEvent(event, DataStage.NATIVE));
//
//        NormalizedJsonLd bart2 = TestObjectFactory.createJsonLd(space, "bart2.json");
//        Event event2 = new Event(bart2, Event.Type.INSERT, "user", new Date());
//        //When
//        indexing.indexEvent(new PersistedEvent(event2, DataStage.NATIVE));
//
//        //Then
//
//    }

}

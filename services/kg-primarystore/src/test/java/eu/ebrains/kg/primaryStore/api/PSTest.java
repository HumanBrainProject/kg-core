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

package eu.ebrains.kg.primaryStore.api;

import com.netflix.discovery.EurekaClient;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.docker.SpringDockerComposeRunner;
import eu.ebrains.kg.test.TestObjectFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd = changeMe"})
public class PSTest {

    @Autowired
    EurekaClient discoveryClient;

    @Before
    public void setup() throws IOException {
        new SpringDockerComposeRunner(discoveryClient, Arrays.asList("arango"), "kg-inference", "kg-graphdb-sync", "kg-indexing", "kg-jsonld", "kg-permissions", "kg-primarystore", "kg-ids").start();
    }

    @Autowired
    PrimaryStoreEventsAPI primaryStore;

    private final SpaceName space = TestObjectFactory.SIMPSONS;

    @Test
    public void postInsertionEvent() {
        //Given
        NormalizedJsonLd carl = TestObjectFactory.createJsonLd(space, "carl_external.json");
        Event event = new Event(space, UUID.randomUUID(), carl, Event.Type.INSERT, new Date());

        //When
        primaryStore.postEvent(event, false);

        //Then
    }

    private void createMilhouseInfra() {
        NormalizedJsonLd milhouse1 = TestObjectFactory.createJsonLd(space, "milhouse1.json");
        Event milhouse1Event = new Event(space, UUID.randomUUID(), milhouse1, Event.Type.INSERT, new Date());
        primaryStore.postEvent(milhouse1Event, false);

        NormalizedJsonLd milhouse2 = TestObjectFactory.createJsonLd(space, "milhouse2.json");

        Event milhouse2Event = new Event(space, UUID.randomUUID(), milhouse2, Event.Type.INSERT, new Date());
        primaryStore.postEvent(milhouse2Event, false);
    }

    @Test
    public void triggerMergeUpdate() {
        //Given
        createMilhouseInfra();

        //When
        NormalizedJsonLd milhouseMerge = TestObjectFactory.createJsonLd(space, "milhouseMergeUpdate.json");

        Event milhouseMergeEvent = new Event(space, UUID.randomUUID(), milhouseMerge, Event.Type.UPDATE, new Date());
        primaryStore.postEvent(milhouseMergeEvent, false);

        //Then
        System.out.println("Hello world");

        //TODO assert only one instance is left in inProgress
    }

    @Test
    public void triggerMergeInsert() {
        //Given
        createMilhouseInfra();

        //When
        NormalizedJsonLd milhouseMerge = TestObjectFactory.createJsonLd(space, "milhouseMergeInsert.json");
        Event milhouseMergeEvent = new Event(space, UUID.randomUUID(), milhouseMerge, Event.Type.INSERT, new Date());
        primaryStore.postEvent(milhouseMergeEvent, false);

        //Then
        System.out.println("Hello world");

        //TODO assert only one instance is left in inProgress
    }
}

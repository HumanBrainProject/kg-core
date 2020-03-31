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

package eu.ebrains.kg.primaryStore.api;

import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.test.TestObjectFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PrimaryStoreTest {

    @Autowired
    Events primaryStore;

    @Test
    @Ignore("This is an integration test - make sure you wrap it with the docker-compose context it needs.")
    public void testRandomEvent(){
        NormalizedJsonLd data = new NormalizedJsonLd();
        data.addProperty("name", "test");
        data.setId(new JsonLdId("https://kg.ebrains.eu/api/instances/foo/bar"));
        Event e = new Event(TestObjectFactory.SIMPSONS, UUID.randomUUID(), data, Event.Type.INSERT, new Date());
        primaryStore.postEvent(e, false);
    }


}
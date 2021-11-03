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

package eu.ebrains.kg;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.model.internal.spaces.Space;
import eu.ebrains.kg.indexing.api.IndexingAPI;
import eu.ebrains.kg.test.TestObjectFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class IndexingTest {

    @Autowired
    IndexingAPI indexing;

    @Autowired
    IdUtils idUtils;

    private final SpaceName spaceName = TestObjectFactory.SIMPSONS;

    @Test
    public void synchronouslyIndexSingleInstance() {
        //Given

        NormalizedJsonLd homer = TestObjectFactory.createJsonLd(spaceName, "homer.json");

        Event event = new Event(spaceName, UUID.randomUUID(), homer, Event.Type.INSERT, new Date());

        //When
        indexing.indexEvent(new PersistedEvent(event, DataStage.NATIVE, null, new Space(spaceName, false, false, false)));

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

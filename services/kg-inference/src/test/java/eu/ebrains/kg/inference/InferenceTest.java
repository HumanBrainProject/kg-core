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

package eu.ebrains.kg.inference;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.inference.api.InferenceAPI;
import eu.ebrains.kg.test.Simpsons;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

//FIXME - We need to transfer this to a system test (in the bundle)

@SpringBootTest
@Disabled
public class InferenceTest {

    @Autowired
    JsonAdapter jsonAdapter;

    @Autowired
    InferenceAPI inference;

    @Autowired
    IdUtils idUtils;

    private final SpaceName space = Simpsons.SPACE_NAME;

    @Test
    public void singleObjectWithEmbeddedInsertionInference()  {
        //Given
        UUID homerId = UUID.randomUUID();
        NormalizedJsonLd homer = jsonAdapter.fromJson(Simpsons.Characters.HOMER, NormalizedJsonLd.class);
        homer.setId(idUtils.buildAbsoluteUrl(homerId));
        //graphDBSvc.upsert(homer, DataStage.NATIVE, homerId, space);

        //When
        List<Event> events = inference.infer(space.getName(), homerId);

        //Then
        assertNotNull(events);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEquals(Event.Type.INSERT, event.getType());
    }

    @Test
    public void twoInstancesInsertInference() throws IOException, URISyntaxException {
        //Given
        UUID bartId = UUID.randomUUID();
        NormalizedJsonLd bart = jsonAdapter.fromJson(Simpsons.Characters.BART, NormalizedJsonLd.class);
        bart.setId(idUtils.buildAbsoluteUrl(bartId));
//        graphDBSvc.upsert(bart, DataStage.NATIVE, bartId, space);


        UUID bartId2 = UUID.randomUUID();
        NormalizedJsonLd bart2 =  jsonAdapter.fromJson(Simpsons.Characters.BART_2, NormalizedJsonLd.class);
        bart2.setId(idUtils.buildAbsoluteUrl(bartId2));
//        graphDBSvc.upsert(bart2, DataStage.NATIVE, bartId2, space);

        NormalizedJsonLd bartUpdate = jsonAdapter.fromJson(Simpsons.Characters.BART_UPDATE, NormalizedJsonLd.class);
        bartUpdate.setId(idUtils.buildAbsoluteUrl(bartId));
//        graphDBSvc.upsert(bartUpdate, DataStage.NATIVE, bartId, space);


        //When
        List<Event> events = inference.infer(space.getName(), bartId);


        //Then
        assertNotNull(events);
        assertEquals(1, events.size());
        Event event = events.get(0);
        assertEquals(Event.Type.INSERT, event.getType());

        //We assume there are information from both instances in the inferred element now.
        assertNotNull(event.getData().get("http://schema.org/age"));
        assertNotNull(event.getData().get("http://schema.org/familyName"));

        //And we ensure, that the updated value (the most recently inserted one) wins.
        assertEquals("Bartholomew", event.getData().get("http://schema.org/givenName"));

    }


}

/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.internal.spaces.Space;
import eu.ebrains.kg.indexing.api.IndexingAPI;
import eu.ebrains.kg.test.Simpsons;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.UUID;

@SpringBootTest
@Disabled //TODO fix test
public class IndexingTest {

    @Autowired
    IndexingAPI indexing;

    @Autowired
    IdUtils idUtils;

    @Autowired
    JsonAdapter jsonAdapter;

    private final SpaceName spaceName = Simpsons.SPACE_NAME;

    @Test
    public void synchronouslyIndexSingleInstance() {
        //Given
        final NormalizedJsonLd homer = jsonAdapter.fromJson(Simpsons.Characters.HOMER, NormalizedJsonLd.class);

        Event event = new Event(spaceName, UUID.randomUUID(), homer, Event.Type.INSERT, new Date());

        //When
        indexing.indexEvent(new PersistedEvent(event, DataStage.NATIVE, null, new Space(spaceName, false, false, false)));

        //Then

    }
}

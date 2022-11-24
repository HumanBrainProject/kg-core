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

package eu.ebrains.kg.ids.api;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.test.TestCategories;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@Disabled //TODO fix test
@SpringBootTest
@Tag(TestCategories.API)
public class IdsTest {

    @Autowired
    IdsAPI ids;

    @Autowired
    IdUtils idUtils;

    @Test
    public void resolveId() {
        //given
        IdWithAlternatives id = new IdWithAlternatives();
        id.setId(UUID.randomUUID());
        id.setSpace("simpsons");
        id.setAlternatives(Collections.singleton("http://simpsons.com/homer"));
        ids.createOrUpdateId(id, DataStage.NATIVE);

        //when
        IdWithAlternatives lookupId = new IdWithAlternatives();
        lookupId.setAlternatives(Collections.singleton("http://simpsons.com/homer"));
        Map<UUID, InstanceId> result = ids.resolveId(Collections.singletonList(lookupId), DataStage.NATIVE);

        //then
        assertEquals(1, result.size());
        assertNotNull(result.get(lookupId.getId()));
        assertEquals(new SpaceName("simpsons"), result.get(lookupId.getId()).getSpace());
        assertEquals(idUtils.buildAbsoluteUrl(id.getId()).getId(), result.get(lookupId.getId()).serialize());
    }

}
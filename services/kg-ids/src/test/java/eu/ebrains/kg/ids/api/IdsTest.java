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

package eu.ebrains.kg.ids.api;

import com.netflix.discovery.EurekaClient;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.docker.SpringDockerComposeRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;


@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd = changeMe"})
public class IdsTest {

    @Autowired
    EurekaClient discoveryClient;


    @Autowired
    Ids ids;

    @Autowired
    IdUtils idUtils;


    @Before
    public void setup() throws IOException {
        new SpringDockerComposeRunner(discoveryClient, true, false, Collections.singletonList("arango")).start();
    }

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
        List<JsonLdIdMapping> mappings = ids.resolveId(Collections.singletonList(lookupId), DataStage.NATIVE);

        //then
        assertEquals(1, mappings.size());
        assertEquals(new Space("simpsons"), mappings.get(0).getSpace());
        assertEquals(id.getId(), mappings.get(0).getRequestedId());
        assertEquals(1, mappings.get(0).getResolvedIds().size());
        assertEquals(idUtils.buildAbsoluteUrl(id.getId()).getId(), mappings.get(0).getResolvedIds().iterator().next().getId());

    }
}
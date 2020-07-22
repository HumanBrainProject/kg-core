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

package eu.ebrains.kg.graphdb.types.controller;

import com.netflix.discovery.EurekaClient;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.docker.SpringDockerComposeRunner;
import eu.ebrains.kg.graphdb.ingestion.controller.TodoListProcessor;
import eu.ebrains.kg.test.TestObjectFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd=changeMe", "eu.ebrains.kg.arango.port=9111"})
public class ArangoRepositoryTypesTest {

    @Autowired
    EurekaClient discoveryClient;

    @Autowired
    ArangoRepositoryTypes arangoRepositoryTypes;

    @Autowired
    TodoListProcessor todoListProcessor;


    private static final DataStage STAGE = DataStage.IN_PROGRESS;
    private static final String CLIENT = "kgeditor";

    @Before
    public void setup() {
        new SpringDockerComposeRunner(discoveryClient, Arrays.asList("arango"), "kg-ids").start();
    }

    private final ArangoCollectionReference simpsons = ArangoCollectionReference.fromSpace(TestObjectFactory.SIMPSONS);


    @Test
    public void getAllTypes() {
        //Given
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd( "simpsons/homer.json"), STAGE, null);

        //When
        Paginated<NormalizedJsonLd> allTypes = arangoRepositoryTypes.getAllTypes(CLIENT, STAGE, true, null);

        //Then
        assertEquals(5, allTypes.getSize());
    }


    @Test
    public void getTypesForSpace() {
        //Given
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd( "simpsons/homer.json"), STAGE, null);

        //When
        Paginated<NormalizedJsonLd> allTypesForSimpsonsSpace = arangoRepositoryTypes.getTypesForSpace(CLIENT, STAGE, TestObjectFactory.SIMPSONS, true,null);
        Paginated<NormalizedJsonLd> allTypesForFooBarSpace = arangoRepositoryTypes.getTypesForSpace(CLIENT, STAGE, new Space("foobar"), true,null);

        //Then
        assertEquals(5, allTypesForSimpsonsSpace.getSize());
        assertEquals(0, allTypesForFooBarSpace.getSize());
    }
}
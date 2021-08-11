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

package eu.ebrains.kg.graphdb.instances.controller;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.graphdb.ingestion.controller.TodoListProcessor;
import eu.ebrains.kg.test.TestObjectFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd=changeMe", "eu.ebrains.kg.arango.port=9111"})
public class ArangoRepositoryInstancesTest {

    @Autowired
    ArangoRepositoryInstances arangoRepository;

    @Autowired
    TodoListProcessor todoListProcessor;

    private final SpaceName space = TestObjectFactory.SIMPSONS;
    private final DataStage stage = DataStage.NATIVE;

    private final ArangoCollectionReference simpsons = ArangoCollectionReference.fromSpace(TestObjectFactory.SIMPSONS);


    @Test
    public void getDocumentsByType() {
        //Given
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd( "simpsons/homer.json"), stage, null);
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd( "simpsons/maggie.json"), stage, null);

        //When
        Paginated<NormalizedJsonLd> kids = arangoRepository.getDocumentsByTypes(stage, new Type("http://schema.org/Kid"), null, null, null, null, null, false, false,  null);
        Paginated<NormalizedJsonLd> familyMembers = arangoRepository.getDocumentsByTypes(stage, new Type("https://thesimpsons.com/FamilyMember"), null, null, null, null, null, false, false, null);


        //Then
        assertEquals(1, kids.getTotalResults());
        assertEquals(0, kids.getFrom());
        assertEquals(1, kids.getSize());
        assertEquals(1, kids.getData().size());
        assertEquals("Maggie", kids.getData().get(0).get("http://schema.org/givenName"));

        assertEquals(2, familyMembers.getTotalResults());
        assertEquals(0, familyMembers.getFrom());
        assertEquals(2, familyMembers.getSize());
        assertEquals(2, familyMembers.getData().size());
    }

    @Test
    public void getDocumentsByTypePaginated() {
        //Given
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd( "simpsons/homer.json"), stage, null);
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd( "simpsons/maggie.json"), stage, null);
        PaginationParam pagination = new PaginationParam();
        pagination.setSize(1L);
        pagination.setFrom(1L);

        //When
        Paginated<NormalizedJsonLd> familyMembers = arangoRepository.getDocumentsByTypes(stage, new Type("https://thesimpsons.com/FamilyMember"), null, null, null, pagination, null, false, false,  null);

        //Then
        assertEquals(1, familyMembers.getSize());
        assertEquals(1, familyMembers.getFrom());
        assertEquals(2, familyMembers.getTotalResults());
        assertEquals("Although the pagination is set to 1, there are more elements", 1, familyMembers.getData().size());
    }
}
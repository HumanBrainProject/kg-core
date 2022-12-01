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

package eu.ebrains.kg.graphdb.instances.controller;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.Type;
import eu.ebrains.kg.graphdb.AbstractGraphTest;
import eu.ebrains.kg.graphdb.ingestion.controller.TodoListProcessor;
import eu.ebrains.kg.test.Simpsons;
import eu.ebrains.kg.test.TestCategories;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@Tag(TestCategories.API)
@Disabled("Fix me")
class ArangoRepositoryInstancesTest extends AbstractGraphTest {

    @Autowired
    ArangoRepositoryInstances arangoRepository;

    @Autowired
    TodoListProcessor todoListProcessor;
    private final DataStage stage = DataStage.IN_PROGRESS;

    private final ArangoCollectionReference simpsons = ArangoCollectionReference.fromSpace(Simpsons.SPACE_NAME);


    @Test
    void getDocumentsByType() {
        //Given

        upsert(Simpsons.SPACE_NAME, jsonAdapter.fromJson(Simpsons.Characters.HOMER, NormalizedJsonLd.class), stage);
        upsert(Simpsons.SPACE_NAME, jsonAdapter.fromJson(Simpsons.Characters.MAGGIE, NormalizedJsonLd.class), stage);

        //When
        Paginated<NormalizedJsonLd> kids = arangoRepository.getDocumentsByTypes(stage, new Type("http://schema.org/Kid"), null, null, null, null, null, false, false,  null);
        Paginated<NormalizedJsonLd> familyMembers = arangoRepository.getDocumentsByTypes(stage, new Type("https://thesimpsons.com/FamilyMember"), null, null, null, null, null, false, false, null);


        //Then
        assertEquals(Long.valueOf(1), kids.getTotalResults());
        assertEquals(0, kids.getFrom());
        assertEquals(1, kids.getSize());
        assertEquals(1, kids.getData().size());
        assertEquals("Maggie", kids.getData().get(0).get("http://schema.org/givenName"));

        assertEquals(Long.valueOf(2), familyMembers.getTotalResults());
        assertEquals(0, familyMembers.getFrom());
        assertEquals(2, familyMembers.getSize());
        assertEquals(2, familyMembers.getData().size());
    }

    @Test
    void getDocumentsByTypePaginated() {
        //Given
        upsert(Simpsons.SPACE_NAME, jsonAdapter.fromJson(Simpsons.Characters.HOMER, NormalizedJsonLd.class), stage);
        upsert(Simpsons.SPACE_NAME, jsonAdapter.fromJson(Simpsons.Characters.MAGGIE, NormalizedJsonLd.class), stage);
        PaginationParam pagination = new PaginationParam();
        pagination.setSize(1L);
        pagination.setFrom(1L);

        //When
        Paginated<NormalizedJsonLd> familyMembers = arangoRepository.getDocumentsByTypes(stage, new Type("https://thesimpsons.com/FamilyMember"), null, null, null, pagination, null, false, false,  null);

        //Then
        assertEquals(1, familyMembers.getSize());
        assertEquals(1, familyMembers.getFrom());
        assertEquals(Long.valueOf(2), familyMembers.getTotalResults());
        assertEquals(1, familyMembers.getData().size(), "Although the pagination is set to 1, there are more elements");
    }
}
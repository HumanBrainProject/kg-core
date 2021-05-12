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

package eu.ebrains.kg.core.api;


import com.netflix.discovery.EurekaClient;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.docker.SpringDockerComposeRunner;
import eu.ebrains.kg.test.GraphDB4Test;
import eu.ebrains.kg.test.TestObjectFactory;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd=changeMe", "eu.ebrains.kg.arango.port=9111"})
public class TypeTest {

    @Autowired
    EurekaClient discoveryClient;

    @Autowired
    IdUtils idUtils;

    @Autowired
    Types types;

    @Autowired
    GraphDB4Test graphDBSyncSvcForTest;

    private final SpaceName space = TestObjectFactory.SIMPSONS;

    @Before
    public void setup() {
        new SpringDockerComposeRunner(discoveryClient, Arrays.asList("arango"), "kg-inference", "kg-indexing", "kg-graphdb-sync", "kg-primarystore", "kg-jsonld", "kg-ids", "kg-permissions").start();
    }



//    @Test
//    public void getTypeStructureByTypeListTest() {
//
//        //Given
//        Space testSpace = new Space("testSpace");
//        DocumentId documentId = new DocumentId(testSpace, UUID.randomUUID());
//        NormalizedJsonLd doc = TestObjectFactory.createJsonLd(testSpace, "metaFieldTest.json");
//        doc.setId(idUtils.buildAbsoluteUrl(documentId));
//        String type = "http://schema.org/Person";
//        graphDBSyncSvcForTest.upsert(doc, DataStage.IN_PROGRESS);
//        List<String> typeRequest = Collections.singletonList(type);
//        //When
//        PaginatedResult<Map<String, Object>> results = types.getTypesByName(testSpace.getName(), true, typeRequest, new PaginationParam());
//        //Then
//        List<Map<String, Object>> fields = new ArrayList<>();
//
//
//        Map<String, Object> fieldFamilyName = new HashMap<>();
//        fieldFamilyName.put(SchemaOrgVocabulary.IDENTIFIER, "http://schema.org/familyName");
//        fieldFamilyName.put(HBPVocabulary.NUMBER_OF_OCCURRENCES, 0);
//        fieldFamilyName.put(HBPVocabulary.LINKED_TYPES, Collections.EMPTY_LIST);
//        fieldFamilyName.put(HBPVocabulary.TYPES, Collections.singletonList(StructureOfField.Type.STRING));
//        fieldFamilyName.put(SchemaOrgVocabulary.NAME, "familyName");
//
//        Map<String, Object> fieldGiveName = new HashMap<>();
//        fieldGiveName.put(SchemaOrgVocabulary.IDENTIFIER, "http://schema.org/givenName");
//        fieldGiveName.put(HBPVocabulary.NUMBER_OF_OCCURRENCES, 0);
//        fieldGiveName.put(HBPVocabulary.LINKED_TYPES, Collections.EMPTY_LIST);
//        fieldGiveName.put(HBPVocabulary.TYPES, Collections.singletonList(StructureOfField.Type.STRING));
//        fieldGiveName.put(SchemaOrgVocabulary.NAME, "givenName");
//
//        Map<String, Object> fieldAge = new HashMap<>();
//        fieldAge.put(SchemaOrgVocabulary.IDENTIFIER, "http://schema.org/age");
//        fieldAge.put(HBPVocabulary.NUMBER_OF_OCCURRENCES, 0);
//        fieldAge.put(HBPVocabulary.LINKED_TYPES, Collections.EMPTY_LIST);
//        fieldAge.put(HBPVocabulary.TYPES, Collections.singletonList(StructureOfField.Type.NUMBER));
//        fieldAge.put(SchemaOrgVocabulary.NAME, "age");
//
//        Map<String, Object> fieldSpouse = new HashMap<>();
//        fieldSpouse.put(SchemaOrgVocabulary.IDENTIFIER, "http://schema.org/spouse");
//        fieldSpouse.put(HBPVocabulary.NUMBER_OF_OCCURRENCES, 0);
//        fieldSpouse.put(HBPVocabulary.LINKED_TYPES, Collections.EMPTY_LIST);
//        fieldSpouse.put(HBPVocabulary.TYPES, Collections.singletonList(StructureOfField.Type.LINK));
//        fieldSpouse.put(SchemaOrgVocabulary.NAME, "spouse");
//
//        Map<String, Object> fieldChildren = new HashMap<>();
//        fieldChildren.put(SchemaOrgVocabulary.IDENTIFIER, "http://schema.org/children");
//        fieldChildren.put(HBPVocabulary.NUMBER_OF_OCCURRENCES, 0);
//        fieldChildren.put(HBPVocabulary.LINKED_TYPES, Collections.EMPTY_LIST);
//        fieldChildren.put(HBPVocabulary.TYPES, Collections.singletonList(StructureOfField.Type.LINK));
//        fieldChildren.put(SchemaOrgVocabulary.NAME, "children");
//
//        Map<String, Object> fieldBirthdate = new HashMap<>();
//        fieldBirthdate.put(SchemaOrgVocabulary.IDENTIFIER, "http://schema.hbp.eu/test/birthdate");
//        fieldBirthdate.put(HBPVocabulary.NUMBER_OF_OCCURRENCES, 0);
//        fieldBirthdate.put(HBPVocabulary.LINKED_TYPES, Collections.EMPTY_LIST);
//        fieldBirthdate.put(HBPVocabulary.TYPES, Collections.singletonList(StructureOfField.Type.DATE));
//        fieldBirthdate.put(SchemaOrgVocabulary.NAME, "birthdate");
//
//        fields.add(fieldFamilyName);
//        fields.add(fieldGiveName);
//        fields.add(fieldAge);
//        fields.add(fieldSpouse);
//        fields.add(fieldChildren);
//        fields.add(fieldBirthdate);
//
//        Map<String, Object> expectedData = new HashMap<>();
//        expectedData.put(SchemaOrgVocabulary.IDENTIFIER, "http://schema.org/Person");
//        expectedData.put(SchemaOrgVocabulary.NAME, "Person");
//        expectedData.put(HBPVocabulary.TYPE_FIELDS, fields);
//
//
//        assertEquals(results.getData().get(0), expectedData);
//
//    }


}

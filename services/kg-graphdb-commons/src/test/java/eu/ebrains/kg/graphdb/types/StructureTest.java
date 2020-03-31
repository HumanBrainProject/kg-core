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

package eu.ebrains.kg.graphdb.types;


import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd=changeMe", "eu.ebrains.kg.arango.port=9111"})
public class StructureTest {

//    @Autowired
//    EurekaClient discoveryClient;
//
//    @Autowired
//    GraphDBSyncSvcForTest graphDBSyncSvcForTest;
//
//    @Autowired
//    IdUtils idUtils;
//
//    @Autowired
//    StructureController structure;
//
//    @Autowired
//    ClientController clientController;
//
//    @Before
//    public void setup() {
//        new SpringDockerComposeRunner(discoveryClient, Arrays.asList("arango"), "kg-inference", "kg-indexing", "kg-graphdb-sync", "kg-primarystore", "kg-jsonld", "kg-ids", "kg-permissions").start();
//    }
//
//    @Test
//    public void getTypesOnEmptyDBTest() {
//        Space testSpace = new Space("testSpace");
//        List<StructureOfType> types = structure.getStructureOfTypesInSpace(DataStage.LIVE, testSpace, null, false);
//        assertEquals(types, Collections.EMPTY_LIST);
//    }
//
//    @Test
//    public void getTypesTest() {
//        Space testSpace = new Space("testSpace");
//        DocumentId documentId = new DocumentId(testSpace, UUID.randomUUID());
//        //Given
//        NormalizedJsonLd doc = TestObjectFactory.createJsonLd(testSpace, "metaFieldTest.json");
//        doc.setId(idUtils.buildAbsoluteUrl(documentId));
//        //When
//        graphDBSyncSvcForTest.upsert(doc, DataStage.LIVE);
//        List<StructureOfType> types = structure.getStructureOfTypesInSpace(DataStage.LIVE, testSpace, null, false);
//
//        //Then
//        assertThat(types, containsInAnyOrder(
//                new StructureOfType("http://schema.org/Person", "Person", null, null),
//                new StructureOfType("https://thesimpsons.com/FamilyMember", "FamilyMember", null, null)
//        ));
//    }
//
//    @Test
//    public void getTypeTest() {
//        //Given
//        Space testSpace = new Space("testSpace");
//        DocumentId documentId = new DocumentId(testSpace, UUID.randomUUID());
//        NormalizedJsonLd doc = TestObjectFactory.createJsonLd(testSpace, "metaFieldTest.json");
//        doc.setId(idUtils.buildAbsoluteUrl(documentId));
//        String type = "http://schema.org/Person";
//        graphDBSyncSvcForTest.upsert(doc, DataStage.LIVE);
//        //When
//        StructureOfType structureType = structure.getStructureOfType(DataStage.LIVE, testSpace, type, null, false);
//        //Then
//        assertEquals(
//                new StructureOfType("http://schema.org/Person", "Person", null, null), structureType
//        );
//    }
//
//    @Test
//    public void addClientTypesTest() {
//        //Given
//        Client client = new Client("editor");
//        clientController.addClientToMeta(client);
//        Space testSpace = new Space("testSpace");
//        DocumentId documentId = new DocumentId(testSpace, UUID.randomUUID());
//        String typeName = "http://schema.org/Person";
//        NormalizedJsonLd doc = TestObjectFactory.createJsonLd(testSpace, "metaFieldTest.json");
//        doc.setId(idUtils.buildAbsoluteUrl(documentId));
//        graphDBSyncSvcForTest.upsert(doc, DataStage.LIVE);
//        ClientStructureOfType clientTypeInfo = new ClientStructureOfType();
//        clientTypeInfo.put("color", "#999");
//        clientTypeInfo.put("widget", "DropdownSelect");
//        StructureOfType updateTypeInfo = new StructureOfType("http://schema.org/Person", "Person", null, clientTypeInfo);
//
//        //When
//        structure.addClientTypeInfo(DataStage.LIVE, testSpace, typeName, clientTypeInfo, client);
//        //Then
//        List<StructureOfType> types = structure.getStructureOfTypesInSpace(DataStage.LIVE, testSpace, client, false);
//        assertThat(types, containsInAnyOrder(
//                updateTypeInfo,
//                new StructureOfType("https://thesimpsons.com/FamilyMember", "FamilyMember", null, null)
//        ));
//    }
//
//
//    @Test
//    public void fieldsByTypeTest() {
//        Space testSpace = new Space("testSpace");
//        DocumentId documentId = new DocumentId(testSpace, UUID.randomUUID());
//        //Given
//        NormalizedJsonLd doc = TestObjectFactory.createJsonLd(testSpace, "metaFieldTest.json");
//        doc.setId(idUtils.buildAbsoluteUrl(documentId));
//        //When
//        graphDBSyncSvcForTest.upsert(doc, DataStage.LIVE);
//
//        List<StructureOfField> structureOfFieldsPerson = structure.fetchFieldsFromType(DataStage.LIVE, testSpace, "http://schema.org/Person", null);
//        List<StructureOfField> structureOfFieldsFamilyMember = structure.fetchFieldsFromType(DataStage.LIVE, testSpace, "https://thesimpsons.com/FamilyMember", null);
//        //Then
//
//        assertThat(structureOfFieldsPerson, containsInAnyOrder(
//                new StructureOfField("http://schema.org/identifier", "identifier", Collections.singletonList(StructureOfField.Type.STRING)),
//                new StructureOfField("@id", "id", Collections.singletonList(StructureOfField.Type.STRING)),
//                new StructureOfField("@type", "type", Collections.singletonList(StructureOfField.Type.STRING_ARRAY)),
//                new StructureOfField("http://schema.org/familyName", "familyName", Collections.singletonList(StructureOfField.Type.STRING)),
//                new StructureOfField("http://schema.org/givenName", "givenName", Collections.singletonList(StructureOfField.Type.STRING)),
//                new StructureOfField("http://schema.org/age", "age", Collections.singletonList(StructureOfField.Type.NUMBER)),
//                new StructureOfField("http://schema.org/spouse", "spouse", Collections.singletonList(StructureOfField.Type.LINK)),
//                new StructureOfField("http://schema.org/children", "children", Collections.singletonList(StructureOfField.Type.LINK)),
//                new StructureOfField("http://schema.hbp.eu/test/birthdate", "birthdate", Collections.singletonList(StructureOfField.Type.DATE))
//        ));
//        assertThat(structureOfFieldsFamilyMember, containsInAnyOrder(
//                new StructureOfField("http://schema.org/identifier", "identifier", Collections.singletonList(StructureOfField.Type.STRING)),
//                new StructureOfField("@id", "id", Collections.singletonList(StructureOfField.Type.STRING)),
//                new StructureOfField("@type", "type", Collections.singletonList(StructureOfField.Type.STRING_ARRAY)),
//                new StructureOfField("http://schema.org/familyName", "familyName", Collections.singletonList(StructureOfField.Type.STRING)),
//                new StructureOfField("http://schema.org/givenName", "givenName", Collections.singletonList(StructureOfField.Type.STRING)),
//                new StructureOfField("http://schema.org/age", "age", Collections.singletonList(StructureOfField.Type.NUMBER)),
//                new StructureOfField("http://schema.org/spouse", "spouse", Collections.singletonList(StructureOfField.Type.LINK)),
//                new StructureOfField("http://schema.org/children", "children", Collections.singletonList(StructureOfField.Type.LINK)),
//                new StructureOfField("http://schema.hbp.eu/test/birthdate", "birthdate", Collections.singletonList(StructureOfField.Type.DATE))
//        ));
//    }
//
//
//    @Test
//    public void typeFieldsUpdateTest() {
//        //Given
//        Space testSpace = new Space("testSpace");
//        String type = "http://schema.org/Person";
//        String fieldName = "http://schema.org/spouse";
//        DocumentId documentId = new DocumentId(testSpace, UUID.randomUUID());
//        NormalizedJsonLd doc = TestObjectFactory.createJsonLd(testSpace, "metaFieldTest.json");
//        doc.setId(idUtils.buildAbsoluteUrl(documentId));
//        graphDBSyncSvcForTest.upsert(doc, DataStage.LIVE);
//        StructureOfField updateFieldInfo = new StructureOfField("http://schema.org/spouse", "the spouse", Collections.singletonList(StructureOfField.Type.STRING), Collections.emptyList(), null);
//        //When
//        structure.addTypeFieldInfo(DataStage.LIVE, testSpace, type, fieldName, updateFieldInfo);
//        //Then
//        List<StructureOfField> structureOfFieldsPerson = structure.fetchFieldsFromType(DataStage.LIVE, testSpace, type, null);
//        assertThat(structureOfFieldsPerson, containsInAnyOrder(
//                new StructureOfField("http://schema.org/identifier", "identifier", Collections.singletonList(StructureOfField.Type.STRING)),
//                new StructureOfField("@id", "id", Collections.singletonList(StructureOfField.Type.STRING)),
//                new StructureOfField("@type", "type", Collections.singletonList(StructureOfField.Type.STRING_ARRAY)),
//                new StructureOfField("http://schema.org/familyName", "familyName", Collections.singletonList(StructureOfField.Type.STRING)),
//                new StructureOfField("http://schema.org/givenName", "givenName", Collections.singletonList(StructureOfField.Type.STRING)),
//                new StructureOfField("http://schema.org/age", "age", Collections.singletonList(StructureOfField.Type.NUMBER)),
//                updateFieldInfo,
//                new StructureOfField("http://schema.org/children", "children", Collections.singletonList(StructureOfField.Type.LINK)),
//                new StructureOfField("http://schema.hbp.eu/test/birthdate", "birthdate", Collections.singletonList(StructureOfField.Type.DATE))
//        ));
//    }
//
//
//    @Test
//    public void clientTypeFieldUpdateTest() {
//        //Given
//        Client client = new Client("editor");
//        clientController.addClientToMeta(client);
//        Space testSpace = new Space("testSpace");
//        String type = "http://schema.org/Person";
//        String fieldName = "http://schema.org/spouse";
//        DocumentId documentId = new DocumentId(testSpace, UUID.randomUUID());
//        NormalizedJsonLd doc = TestObjectFactory.createJsonLd(testSpace, "metaFieldTest.json");
//        doc.setId(idUtils.buildAbsoluteUrl(documentId));
//        graphDBSyncSvcForTest.upsert(doc, DataStage.LIVE);
//        ClientStructureOfField updateFieldInfo = new ClientStructureOfField();
//        updateFieldInfo.put("color", "#999");
//        updateFieldInfo.put("widget", "DropdownSelect");
//        //When
//        structure.addClientTypeFieldInfo(DataStage.LIVE, testSpace, type, fieldName, updateFieldInfo, client);
//        //Then
//        List<StructureOfField> structureOfFieldsPerson = structure.fetchFieldsFromType(DataStage.LIVE, testSpace, type, client);
//        assertThat(structureOfFieldsPerson, containsInAnyOrder(
//                new StructureOfField("http://schema.org/identifier", "identifier", Collections.singletonList(StructureOfField.Type.STRING)),
//                new StructureOfField("@id", "id", Collections.singletonList(StructureOfField.Type.STRING)),
//                new StructureOfField("@type", "type", Collections.singletonList(StructureOfField.Type.STRING_ARRAY)),
//                new StructureOfField("http://schema.org/familyName", "familyName", Collections.singletonList(StructureOfField.Type.STRING), Collections.emptyList(), null),
//                new StructureOfField("http://schema.org/givenName", "givenName", Collections.singletonList(StructureOfField.Type.STRING), Collections.emptyList(), null),
//                new StructureOfField("http://schema.org/age", "age", Collections.singletonList(StructureOfField.Type.NUMBER), Collections.emptyList(), null),
//                new StructureOfField("http://schema.org/spouse", "spouse", Collections.singletonList(StructureOfField.Type.LINK), Collections.emptyList(), updateFieldInfo),
//                new StructureOfField("http://schema.org/children", "children", Collections.singletonList(StructureOfField.Type.LINK), Collections.emptyList(), null),
//                new StructureOfField("http://schema.hbp.eu/test/birthdate", "birthdate", Collections.singletonList(StructureOfField.Type.DATE), Collections.emptyList(), null)
//        ));
//
//    }
//
////    @Test
////    public void addVituralFieldTest() {
////        //Given
////        Client client = new Client("editor");
////        clientController.addClientToMeta(client);
////        Space testSpace = new Space("testSpace");
////        String type = "http://schema.org/Person";
////        String fieldName = "http://schema.org/spouse";
////        DocumentId documentId = new DocumentId(testSpace, UUID.randomUUID());
////        NormalizedJsonLd doc = TestObjectFactory.createJsonLd(testSpace, "metaFieldTest.json");
////        doc.setId(idUtils.buildAbsoluteUrl(documentId));
////        graphDBSyncSvcForTest.upsert(doc, DataStage.LIVE);
////        ClientStructureOfField updateFieldInfo = new ClientStructureOfField();
////        StructureOfField newField = new StructureOfField("http://hbp.eu/Dataset", "Dataset", updateFieldInfo);
////        updateFieldInfo.put("reverse", true);
////
////        //When
////        structure.addClientVirtualFieldEdge(DataStage.LIVE, testSpace, type, newField, client);
////
////        //Then
////        List<StructureOfField> structureOfFieldsPerson = structure.fetchFieldsFromType(DataStage.LIVE, testSpace, type, client);
////        assertThat(structureOfFieldsPerson, containsInAnyOrder(
////                new StructureOfField("http://schema.org/identifier", "identifier", Collections.singletonList(StructureOfField.Type.STRING)),
////                new StructureOfField("@id", "id", Collections.singletonList(StructureOfField.Type.STRING)),
////                new StructureOfField("@type", "type", Collections.singletonList(StructureOfField.Type.STRING_ARRAY)),
////                new StructureOfField("http://schema.org/familyName", "familyName", Collections.singletonList(StructureOfField.Type.STRING), Collections.emptyList(), null),
////                new StructureOfField("http://schema.org/givenName", "givenName", Collections.singletonList(StructureOfField.Type.STRING), Collections.emptyList(), null),
////                new StructureOfField("http://schema.org/age", "age", Collections.singletonList(StructureOfField.Type.NUMBER), Collections.emptyList(), null),
////                new StructureOfField("http://schema.org/spouse", "spouse", Collections.singletonList(StructureOfField.Type.LINK), Collections.emptyList(), null),
////                new StructureOfField("http://schema.org/children", "children", Collections.singletonList(StructureOfField.Type.LINK), Collections.emptyList(), null),
////                new StructureOfField("http://schema.hbp.eu/test/birthdate", "birthdate", Collections.singletonList(StructureOfField.Type.DATE), Collections.emptyList(), null),
////                new StructureOfField("http://hbp.eu/Dataset", "Dataset", Collections.singletonList(StructureOfField.Type.LINK), Collections.emptyList(), updateFieldInfo)
////        ));
////
////        List<StructureOfField> structureOfFieldsPersonWithDifferentClient = structure.fetchFieldsFromType(DataStage.LIVE, testSpace, type, new Client("cool_client"));
////        assertThat(structureOfFieldsPersonWithDifferentClient, containsInAnyOrder(
////                new StructureOfField("http://schema.org/identifier", "identifier", Collections.singletonList(StructureOfField.Type.STRING)),
////                new StructureOfField("@id", "id", Collections.singletonList(StructureOfField.Type.STRING)),
////                new StructureOfField("@type", "type", Collections.singletonList(StructureOfField.Type.STRING_ARRAY)),
////                new StructureOfField("http://schema.org/familyName", "familyName", Collections.singletonList(StructureOfField.Type.STRING), Collections.emptyList(), null),
////                new StructureOfField("http://schema.org/givenName", "givenName", Collections.singletonList(StructureOfField.Type.STRING), Collections.emptyList(), null),
////                new StructureOfField("http://schema.org/age", "age", Collections.singletonList(StructureOfField.Type.NUMBER), Collections.emptyList(), null),
////                new StructureOfField("http://schema.org/spouse", "spouse", Collections.singletonList(StructureOfField.Type.LINK), Collections.emptyList(), null),
////                new StructureOfField("http://schema.org/children", "children", Collections.singletonList(StructureOfField.Type.LINK), Collections.emptyList(), null),
////                new StructureOfField("http://schema.hbp.eu/test/birthdate", "birthdate", Collections.singletonList(StructureOfField.Type.DATE), Collections.emptyList(), null)
////        ));
////        List<StructureOfField> structureOfFieldsPersonWithoutClient = structure.fetchFieldsFromType(DataStage.LIVE, testSpace, type, null);
////        assertThat(structureOfFieldsPersonWithoutClient, containsInAnyOrder(
////                new StructureOfField("http://schema.org/identifier", "identifier", Collections.singletonList(StructureOfField.Type.STRING)),
////                new StructureOfField("@id", "id", Collections.singletonList(StructureOfField.Type.STRING)),
////                new StructureOfField("@type", "type", Collections.singletonList(StructureOfField.Type.STRING_ARRAY)),
////                new StructureOfField("http://schema.org/familyName", "familyName", Collections.singletonList(StructureOfField.Type.STRING), Collections.emptyList(), null),
////                new StructureOfField("http://schema.org/givenName", "givenName", Collections.singletonList(StructureOfField.Type.STRING), Collections.emptyList(), null),
////                new StructureOfField("http://schema.org/age", "age", Collections.singletonList(StructureOfField.Type.NUMBER), Collections.emptyList(), null),
////                new StructureOfField("http://schema.org/spouse", "spouse", Collections.singletonList(StructureOfField.Type.LINK), Collections.emptyList(), null),
////                new StructureOfField("http://schema.org/children", "children", Collections.singletonList(StructureOfField.Type.LINK), Collections.emptyList(), null),
////                new StructureOfField("http://schema.hbp.eu/test/birthdate", "birthdate", Collections.singletonList(StructureOfField.Type.DATE), Collections.emptyList(), null)
////        ));
////    }

}

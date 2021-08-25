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

package eu.ebrains.kg.graphdb.ingestion.controller;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.Ids;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.test.TestObjectFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd=changeMe", "eu.ebrains.kg.arango.port=9111"})
public class TodoListProcessorTest {

    @Autowired
    TodoListProcessor todoListProcessor;

    @Autowired
    ArangoRepositoryCommons repository;

    @Autowired
    IdUtils idUtils;

    @Autowired
    ArangoDatabases arangoDatabases;

    @Autowired
    Ids.Client ids;

    private final SpaceName space = TestObjectFactory.SIMPSONS;
    private final ArangoCollectionReference simpsons = ArangoCollectionReference.fromSpace(TestObjectFactory.SIMPSONS);
    private final ArangoCollectionReference admin = ArangoCollectionReference.fromSpace(new SpaceName("admin"));
    private final ArangoCollectionReference kgeditor = ArangoCollectionReference.fromSpace(new SpaceName("kgeditor"));


    @Test
    public void insertDocument() {
        //Given
        NormalizedJsonLd homer = TestObjectFactory.createJsonLd("simpsons/homer.json");

        //When
        ArangoDocumentReference homerId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), homer, DataStage.NATIVE, null);

        //Then
        Assert.assertNotNull(homerId);
        ArangoDocument homerDoc = repository.getDocument(DataStage.NATIVE, homerId);
        Assert.assertNotNull(homerDoc);
        Assert.assertEquals("Simpson", homerDoc.getDoc().get("http://schema.org/familyName"));
        Assert.assertEquals("Homer", homerDoc.getDoc().get("http://schema.org/givenName"));
    }


    @Test
    public void insertDocumentWithResolution() {
        //Given
        NormalizedJsonLd homer = TestObjectFactory.createJsonLd("simpsons/homer.json");
        ArangoDocumentReference homerId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), homer, DataStage.NATIVE, null);
        NormalizedJsonLd marge = TestObjectFactory.createJsonLd("simpsons/marge.json");

        //When
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), marge, DataStage.NATIVE, null);

        //Then
        //TODO assertions
    }


    @Test
    public void updateDocument() {
        //Given

        NormalizedJsonLd bart = TestObjectFactory.createJsonLd("simpsons/bart.json");
        ArangoDocumentReference bartId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), bart, DataStage.NATIVE, null);
        NormalizedJsonLd bartUpdate = TestObjectFactory.createJsonLd("simpsons/bartUpdate.json");

        //When
        todoListProcessor.upsertDocument(bartId, bartUpdate, DataStage.NATIVE, null);

        //Then
        Assert.assertNotNull(bartId);
        ArangoDocument bartDoc = repository.getDocument(DataStage.NATIVE, bartId);
        Assert.assertNotNull(bartDoc);
        Assert.assertEquals("Simpson", bartDoc.getDoc().get("http://schema.org/familyName"));
    }

    @Test
    public void deleteDocument() {
        //Given
        NormalizedJsonLd marge = TestObjectFactory.createJsonLd("simpsons/marge.json");
        ArangoDocumentReference margeId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), marge, DataStage.NATIVE, null);
        Assert.assertNotNull(margeId);
        ArangoDocument margeDoc = repository.getDocument(DataStage.NATIVE, margeId);
        Assert.assertNotNull(margeDoc);

        //When
        todoListProcessor.deleteDocument(DataStage.NATIVE, margeId);

        //Then
        ArangoDocument margeDocAfterDeletion = repository.getDocument(DataStage.NATIVE, margeId);
        Assert.assertNull(margeDocAfterDeletion);
    }


    @Test
    public void deleteComplexDocument() {
        //Given
        NormalizedJsonLd homer = TestObjectFactory.createJsonLd("simpsons/homer.json");
        ArangoDocumentReference homerId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), homer, DataStage.NATIVE, null);
        Assert.assertNotNull(homerId);
        ArangoDocument homerDoc = repository.getDocument(DataStage.NATIVE, homerId);
        Assert.assertNotNull(homerDoc);

        //When
        todoListProcessor.deleteDocument(DataStage.NATIVE, homerId);

        //Then
        ArangoDocument margeDocAfterDeletion = repository.getDocument(DataStage.NATIVE, homerId);
        Assert.assertNull(margeDocAfterDeletion);
    }


    @Test
    public void updateDocumentWithMerge() {
        //Given
        NormalizedJsonLd milhouse1 = TestObjectFactory.createJsonLd("simpsons/milhouse1.json");
        ArangoDocumentReference milhouse1Id = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), milhouse1, DataStage.IN_PROGRESS, null);
        Assert.assertNotNull(milhouse1Id);

        NormalizedJsonLd milhouse2 = TestObjectFactory.createJsonLd("simpsons/milhouse2.json");
        ArangoDocumentReference milhouse2Id = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), milhouse2, DataStage.IN_PROGRESS, null);
        Assert.assertNotNull(milhouse2Id);

        NormalizedJsonLd milhouseMerge = TestObjectFactory.createJsonLd("simpsons/milhouseMerge.json");

        //When
        ArangoDocumentReference milhouseMergeId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), milhouseMerge, DataStage.IN_PROGRESS, Collections.singletonList(milhouse2Id.getDocumentId()));

        //Then
        ArangoDocument milhouseMerged = repository.getDocument(DataStage.IN_PROGRESS, milhouseMergeId);
        System.out.println(milhouseMerged);

    }


    @Test
    public void upsertTypeDefinitionForClient() {
        //Given
        NormalizedJsonLd bart = TestObjectFactory.createJsonLd("simpsons/bart.json");
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), bart, DataStage.IN_PROGRESS, null);
        NormalizedJsonLd editorClient = TestObjectFactory.createJsonLd("admin/kgeditorClient.json");
        todoListProcessor.upsertDocument(admin.doc(UUID.randomUUID()), editorClient, DataStage.IN_PROGRESS, null);
        NormalizedJsonLd typeDefinition = TestObjectFactory.createJsonLd("kgeditor/personTypeDefinition.json");

        //When
        ArangoDocumentReference typeDocId = todoListProcessor.upsertDocument(kgeditor.doc(UUID.randomUUID()), typeDefinition, DataStage.NATIVE, null);

        //Then
        ArangoDocument typeDoc = repository.getDocument(DataStage.IN_PROGRESS, typeDocId);

    }


    @Test
    public void upsertPropertyDefinitionForClient() {
        //Given
        NormalizedJsonLd bart = TestObjectFactory.createJsonLd("simpsons/bart.json");
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), bart, DataStage.NATIVE, null);
        NormalizedJsonLd editorClient = TestObjectFactory.createJsonLd("admin/kgeditorClient.json");
        ArangoDocumentReference clientId = todoListProcessor.upsertDocument(admin.doc(UUID.randomUUID()), editorClient, DataStage.NATIVE, null);
        NormalizedJsonLd typeDefinition = TestObjectFactory.createJsonLd("kgeditor/givenNamePropertyDefinition.json");

        //When
        ArangoDocumentReference typeDocId = todoListProcessor.upsertDocument(kgeditor.doc(UUID.randomUUID()), typeDefinition, DataStage.IN_PROGRESS, null);

        //Then
        ArangoDocument typeDoc = repository.getDocument(DataStage.IN_PROGRESS, typeDocId);
    }

    @Test
    public void upsertPropertyDefinitionInTypeForClient() {
        //Given
        NormalizedJsonLd bart = TestObjectFactory.createJsonLd("simpsons/bart.json");
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), bart, DataStage.NATIVE, null);
        NormalizedJsonLd editorClient = TestObjectFactory.createJsonLd("admin/kgeditorClient.json");
        ArangoDocumentReference clientId = todoListProcessor.upsertDocument(admin.doc(UUID.randomUUID()), editorClient, DataStage.IN_PROGRESS, null);
        NormalizedJsonLd typeDefinition = TestObjectFactory.createJsonLd("kgeditor/givenNameInPersonPropertyDefinition.json");

        //When
        ArangoDocumentReference typeDocId = todoListProcessor.upsertDocument(kgeditor.doc(UUID.randomUUID()), typeDefinition, DataStage.IN_PROGRESS, null);

        //Then
        ArangoDocument typeDoc = repository.getDocument(DataStage.IN_PROGRESS, typeDocId);
    }


    @Test
    public void testUpdateTypeList() {
        //Given
        DataStage stage = DataStage.IN_PROGRESS;

        NormalizedJsonLd bart = TestObjectFactory.createJsonLd("simpsons/bart.json");
        ArangoDocumentReference bartId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), bart, stage, null);
        ids.createOrUpdateId(new IdWithAlternatives().setId(bartId.getDocumentId()).setSpace(TestObjectFactory.SIMPSONS.getName()).setAlternatives(bart.identifiers()), stage);

        NormalizedJsonLd homer = TestObjectFactory.createJsonLd("simpsons/homer.json");
        ArangoDocumentReference homerId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), homer, stage, null);
        ids.createOrUpdateId(new IdWithAlternatives().setId(homerId.getDocumentId()).setSpace(TestObjectFactory.SIMPSONS.getName()).setAlternatives(homer.identifiers()), stage);

        //When
        NormalizedJsonLd bartUpdate = TestObjectFactory.createJsonLd("simpsons/bartUpdate.json");
        todoListProcessor.upsertDocument(bartId, bartUpdate, stage, null);

        //Then

    }

    @Test
    public void testUpdateTypeListInverse() {
        //Given
        DataStage stage = DataStage.IN_PROGRESS;

        NormalizedJsonLd bart = TestObjectFactory.createJsonLd("simpsons/bartUpdate.json");
        ArangoDocumentReference bartId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), bart, stage, null);
        ids.createOrUpdateId(new IdWithAlternatives().setId(bartId.getDocumentId()).setSpace(TestObjectFactory.SIMPSONS.getName()).setAlternatives(bart.identifiers()), stage);

        NormalizedJsonLd homer = TestObjectFactory.createJsonLd("simpsons/homer.json");
        ArangoDocumentReference homerId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), homer, stage, null);
        ids.createOrUpdateId(new IdWithAlternatives().setId(homerId.getDocumentId()).setSpace(TestObjectFactory.SIMPSONS.getName()).setAlternatives(homer.identifiers()), stage);

        //When
        NormalizedJsonLd bartUpdate = TestObjectFactory.createJsonLd("simpsons/bart.json");
        todoListProcessor.upsertDocument(bartId, bartUpdate, stage, null);

        //Then

    }

    @Test
    public void testUpdateStructureLazyResolve() {
        //Given
        DataStage stage = DataStage.IN_PROGRESS;

        NormalizedJsonLd homer = TestObjectFactory.createJsonLd("simpsons/homer.json");
        ArangoDocumentReference homerId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), homer, stage, null);
        ids.createOrUpdateId(new IdWithAlternatives().setId(homerId.getDocumentId()).setSpace(TestObjectFactory.SIMPSONS.getName()).setAlternatives(homer.identifiers()), stage);

        //When
        NormalizedJsonLd bart = TestObjectFactory.createJsonLd("simpsons/bart.json");
        ArangoDocumentReference bartId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), bart, stage, null);
        ids.createOrUpdateId(new IdWithAlternatives().setId(bartId.getDocumentId()).setSpace(TestObjectFactory.SIMPSONS.getName()).setAlternatives(bart.identifiers()), stage);

        //Then

    }


    private ArangoDocumentReference uploadToDatabase(DataStage stage, SpaceName space, String json){
        NormalizedJsonLd payload = TestObjectFactory.createJsonLd(json);
        UUID uuid = UUID.randomUUID();
        payload.setId(idUtils.buildAbsoluteUrl(uuid));
        ArangoDocumentReference id = todoListProcessor.upsertDocument(ArangoCollectionReference.fromSpace(space).doc(uuid), payload, stage, null);
        ids.createOrUpdateId(new IdWithAlternatives().setId(id.getDocumentId()).setSpace(space.getName()).setAlternatives(payload.identifiers()), stage);
        return id;
    }

    @Test
    public void upsertAllStructureChanges() {
        //Given
        DataStage stage = DataStage.IN_PROGRESS;

        ArangoDocumentReference bartId = uploadToDatabase(stage, TestObjectFactory.SIMPSONS, "simpsons/bart.json");
        ArangoDocumentReference homerId = uploadToDatabase(stage, TestObjectFactory.SIMPSONS, "simpsons/homer.json");
        ArangoDocumentReference lisaId = uploadToDatabase(stage, TestObjectFactory.SIMPSONS, "simpsons/lisa.json");
        ArangoDocumentReference maggieId = uploadToDatabase(stage, TestObjectFactory.SIMPSONS, "simpsons/maggie.json");
        ArangoDocumentReference margeId = uploadToDatabase(stage, TestObjectFactory.SIMPSONS, "simpsons/marge.json");

        NormalizedJsonLd editorClient = TestObjectFactory.createJsonLd("admin/kgeditorClient.json");
        ArangoDocumentReference clientId = todoListProcessor.upsertDocument(admin.doc(UUID.randomUUID()), editorClient, stage, null);

        //When
        todoListProcessor.upsertDocument(admin.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd("admin/personTypeDefinition.json"), stage, null);
        todoListProcessor.upsertDocument(admin.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd("admin/givenNamePropertyDefinition.json"), stage, null);
        todoListProcessor.upsertDocument(admin.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd("admin/familyNamePropertyDefinition.json"), stage, null);
        todoListProcessor.upsertDocument(admin.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd("admin/schemaOrgNamePropertyDefinition.json"), stage, null);
        todoListProcessor.upsertDocument(admin.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd("admin/givenNameInPersonPropertyDefinition.json"), stage, null);
        todoListProcessor.upsertDocument(admin.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd("admin/childrenPropertyDefinition.json"), stage, null);

        todoListProcessor.upsertDocument(kgeditor.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd("kg-editor/personTypeDefinition.json"), stage, null);
        todoListProcessor.upsertDocument(kgeditor.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd("kg-editor/givenNamePropertyDefinition.json"), stage, null);
        todoListProcessor.upsertDocument(kgeditor.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd("kg-editor/givenNameInPersonPropertyDefinition.json"), stage, null);

        //Then

    }

    private void uploadToDatabase(DataStage stage) {
        NormalizedJsonLd bart = TestObjectFactory.createJsonLd("simpsons/bart.json");
        ArangoDocumentReference bartId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), bart, stage, null);
        ids.createOrUpdateId(new IdWithAlternatives().setId(bartId.getDocumentId()).setSpace(TestObjectFactory.SIMPSONS.getName()).setAlternatives(bart.identifiers()), stage);
    }


}

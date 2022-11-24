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

package eu.ebrains.kg.graphdb.ingestion.controller;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.graphdb.AbstractGraphTest;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.test.GlobalSpecifications;
import eu.ebrains.kg.test.KGEditor;
import eu.ebrains.kg.test.Simpsons;
import eu.ebrains.kg.test.TestCategories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag(TestCategories.API)
class TodoListProcessorTest extends AbstractGraphTest {

    @Autowired
    ArangoRepositoryCommons repository;

    @Autowired
    IdUtils idUtils;

    private final SpaceName admin = new SpaceName("admin");
    private final SpaceName kgeditor = new SpaceName("kgeditor");


    @Test
    void insertDocument() {
        //Given
        NormalizedJsonLd homer = jsonAdapter.fromJson(Simpsons.Characters.HOMER, NormalizedJsonLd.class);

        //When
        ArangoDocumentReference homerId = upsert(Simpsons.SPACE_NAME, homer, DataStage.NATIVE);

        //Then
        assertNotNull(homerId);
        ArangoDocument homerDoc = repository.getDocument(DataStage.NATIVE, homerId);
        assertNotNull(homerDoc);
        assertEquals("Simpson", homerDoc.getDoc().get("http://schema.org/familyName"));
        assertEquals("Homer", homerDoc.getDoc().get("http://schema.org/givenName"));
    }


    @Test
    void insertDocumentWithResolution() {
        //Given
        NormalizedJsonLd homer = jsonAdapter.fromJson(Simpsons.Characters.HOMER, NormalizedJsonLd.class);
        ArangoDocumentReference homerId = upsert(Simpsons.SPACE_NAME, homer, DataStage.NATIVE);
        NormalizedJsonLd marge =  jsonAdapter.fromJson(Simpsons.Characters.MARGE, NormalizedJsonLd.class);

        //When
        upsert(Simpsons.SPACE_NAME, marge, DataStage.NATIVE);

        //Then
        //TODO assertions
    }


    @Test
    void updateDocument() {
        //Given

        NormalizedJsonLd bart = jsonAdapter.fromJson(Simpsons.Characters.BART, NormalizedJsonLd.class);
        ArangoDocumentReference bartId = upsert(Simpsons.SPACE_NAME, bart, DataStage.NATIVE);
        NormalizedJsonLd bartUpdate = jsonAdapter.fromJson(Simpsons.Characters.BART_UPDATE, NormalizedJsonLd.class);

        //When
        upsert(Simpsons.SPACE_NAME, bartId.getDocumentId(), bartUpdate, DataStage.NATIVE);

        //Then
        assertNotNull(bartId);
        ArangoDocument bartDoc = repository.getDocument(DataStage.NATIVE, bartId);
        assertNotNull(bartDoc);
        assertEquals("Simpson", bartDoc.getDoc().get("http://schema.org/familyName"));
    }

    @Test
    void deleteDocument() {
        //Given
        NormalizedJsonLd marge = jsonAdapter.fromJson(Simpsons.Characters.MARGE, NormalizedJsonLd.class);
        ArangoDocumentReference margeId = upsert(Simpsons.SPACE_NAME, marge, DataStage.NATIVE);
        assertNotNull(margeId);
        ArangoDocument margeDoc = repository.getDocument(DataStage.NATIVE, margeId);
        assertNotNull(margeDoc);

        //When
        todoListProcessor.deleteDocument(DataStage.NATIVE, margeId);

        //Then
        ArangoDocument margeDocAfterDeletion = repository.getDocument(DataStage.NATIVE, margeId);
        assertNull(margeDocAfterDeletion);
    }


    @Test
    void deleteComplexDocument() {
        //Given
        NormalizedJsonLd homer = jsonAdapter.fromJson(Simpsons.Characters.HOMER, NormalizedJsonLd.class);
        ArangoDocumentReference homerId = upsert(Simpsons.SPACE_NAME, homer, DataStage.NATIVE);
        assertNotNull(homerId);
        ArangoDocument homerDoc = repository.getDocument(DataStage.NATIVE, homerId);
        assertNotNull(homerDoc);

        //When
        todoListProcessor.deleteDocument(DataStage.NATIVE, homerId);

        //Then
        ArangoDocument margeDocAfterDeletion = repository.getDocument(DataStage.NATIVE, homerId);
        assertNull(margeDocAfterDeletion);
    }


    @Test
    void upsertTypeDefinitionForClient() {
        //Given
        NormalizedJsonLd bart = jsonAdapter.fromJson(Simpsons.Characters.BART, NormalizedJsonLd.class);
        upsert(Simpsons.SPACE_NAME, bart, DataStage.NATIVE);
        NormalizedJsonLd editorClient = jsonAdapter.fromJson(GlobalSpecifications.KG_EDITOR_CLIENT, NormalizedJsonLd.class);
        upsert(admin, editorClient, DataStage.NATIVE);
        NormalizedJsonLd typeDefinition =  jsonAdapter.fromJson(KGEditor.PERSON_TYPE_DEFINITION, NormalizedJsonLd.class);

        //When
        ArangoDocumentReference typeDocId = upsert(kgeditor, typeDefinition, DataStage.NATIVE);

        //Then
        ArangoDocument typeDoc = repository.getDocument(DataStage.IN_PROGRESS, typeDocId);

    }


    @Test
    void upsertPropertyDefinitionForClient() {
        //Given
        NormalizedJsonLd bart = jsonAdapter.fromJson(Simpsons.Characters.BART, NormalizedJsonLd.class);
        upsert(Simpsons.SPACE_NAME, bart, DataStage.NATIVE);
        NormalizedJsonLd editorClient = jsonAdapter.fromJson(GlobalSpecifications.KG_EDITOR_CLIENT, NormalizedJsonLd.class);
        ArangoDocumentReference clientId = upsert(admin, editorClient, DataStage.IN_PROGRESS);
        NormalizedJsonLd typeDefinition = jsonAdapter.fromJson(KGEditor.GIVEN_NAME_PROPERTY_DEFINITION, NormalizedJsonLd.class);

        //When
        ArangoDocumentReference typeDocId = upsert(kgeditor, typeDefinition, DataStage.IN_PROGRESS);

        //Then
        ArangoDocument typeDoc = repository.getDocument(DataStage.IN_PROGRESS, typeDocId);
    }

    @Test
    void upsertPropertyDefinitionInTypeForClient() {
        //Given
        NormalizedJsonLd bart = jsonAdapter.fromJson(Simpsons.Characters.BART, NormalizedJsonLd.class);
        upsert(Simpsons.SPACE_NAME, bart, DataStage.NATIVE);
        NormalizedJsonLd editorClient = jsonAdapter.fromJson(GlobalSpecifications.KG_EDITOR_CLIENT, NormalizedJsonLd.class);
        ArangoDocumentReference clientId = upsert(admin, editorClient, DataStage.IN_PROGRESS);
        NormalizedJsonLd typeDefinition = jsonAdapter.fromJson(KGEditor.GIVEN_NAME_IN_PERSON_PROPERY_DEFINITION, NormalizedJsonLd.class);

        //When
        ArangoDocumentReference typeDocId = upsert(kgeditor, typeDefinition, DataStage.IN_PROGRESS);

        //Then
        ArangoDocument typeDoc = repository.getDocument(DataStage.IN_PROGRESS, typeDocId);
    }


    @Test
    void testUpdateTypeList() {
        //Given
        DataStage stage = DataStage.IN_PROGRESS;

        NormalizedJsonLd bart = jsonAdapter.fromJson(Simpsons.Characters.BART, NormalizedJsonLd.class);
        ArangoDocumentReference bartId = upsert(Simpsons.SPACE_NAME, bart, stage);
        ids.createOrUpdateId(new IdWithAlternatives().setId(bartId.getDocumentId()).setSpace(Simpsons.SPACE_NAME.getName()).setAlternatives(bart.identifiers()), stage);

        NormalizedJsonLd homer = jsonAdapter.fromJson(Simpsons.Characters.HOMER, NormalizedJsonLd.class);
        ArangoDocumentReference homerId = upsert(Simpsons.SPACE_NAME, homer, stage);
        ids.createOrUpdateId(new IdWithAlternatives().setId(homerId.getDocumentId()).setSpace(Simpsons.SPACE_NAME.getName()).setAlternatives(homer.identifiers()), stage);

        //When
        NormalizedJsonLd bartUpdate = jsonAdapter.fromJson(Simpsons.Characters.BART_UPDATE, NormalizedJsonLd.class);
        upsert(Simpsons.SPACE_NAME, bartId.getDocumentId(), bartUpdate, stage);

        //Then

    }

    @Test
    void testUpdateTypeListInverse() {
        //Given
        DataStage stage = DataStage.IN_PROGRESS;

        NormalizedJsonLd bart = jsonAdapter.fromJson(Simpsons.Characters.BART_UPDATE, NormalizedJsonLd.class);
        ArangoDocumentReference bartId = upsert(Simpsons.SPACE_NAME, bart, stage);
        ids.createOrUpdateId(new IdWithAlternatives().setId(bartId.getDocumentId()).setSpace(Simpsons.SPACE_NAME.getName()).setAlternatives(bart.identifiers()), stage);

        NormalizedJsonLd homer = jsonAdapter.fromJson(Simpsons.Characters.HOMER, NormalizedJsonLd.class);
        ArangoDocumentReference homerId = upsert(Simpsons.SPACE_NAME, homer, stage);
        ids.createOrUpdateId(new IdWithAlternatives().setId(homerId.getDocumentId()).setSpace(Simpsons.SPACE_NAME.getName()).setAlternatives(homer.identifiers()), stage);

        //When
        NormalizedJsonLd bartUpdate = jsonAdapter.fromJson(Simpsons.Characters.BART, NormalizedJsonLd.class);
        upsert(Simpsons.SPACE_NAME, bartId.getDocumentId(), bartUpdate, stage);

        //Then

    }

    @Test
    void testUpdateStructureLazyResolve() {
        //Given
        DataStage stage = DataStage.IN_PROGRESS;

        NormalizedJsonLd homer = jsonAdapter.fromJson(Simpsons.Characters.HOMER, NormalizedJsonLd.class);
        ArangoDocumentReference homerId = upsert(Simpsons.SPACE_NAME, homer, stage);
        ids.createOrUpdateId(new IdWithAlternatives().setId(homerId.getDocumentId()).setSpace(Simpsons.SPACE_NAME.getName()).setAlternatives(homer.identifiers()), stage);

        //When
        NormalizedJsonLd bart = jsonAdapter.fromJson(Simpsons.Characters.BART, NormalizedJsonLd.class);
        ArangoDocumentReference bartId = upsert(Simpsons.SPACE_NAME, bart, stage);
        ids.createOrUpdateId(new IdWithAlternatives().setId(bartId.getDocumentId()).setSpace(Simpsons.SPACE_NAME.getName()).setAlternatives(bart.identifiers()), stage);

        //Then

    }

    private ArangoDocumentReference uploadToDatabase(DataStage stage, SpaceName space, String json){
        NormalizedJsonLd payload = jsonAdapter.fromJson(json, NormalizedJsonLd.class);
        UUID uuid = UUID.randomUUID();
        payload.setId(idUtils.buildAbsoluteUrl(uuid));
        ArangoDocumentReference id = upsert(space, uuid, payload, stage);
        ids.createOrUpdateId(new IdWithAlternatives().setId(id.getDocumentId()).setSpace(space.getName()).setAlternatives(payload.identifiers()), stage);
        return id;
    }

    @Test
    void upsertAllStructureChanges() {
        //Given
        DataStage stage = DataStage.IN_PROGRESS;

        ArangoDocumentReference bartId = uploadToDatabase(stage, Simpsons.SPACE_NAME, Simpsons.Characters.BART);
        ArangoDocumentReference homerId = uploadToDatabase(stage, Simpsons.SPACE_NAME, Simpsons.Characters.HOMER);
        ArangoDocumentReference lisaId = uploadToDatabase(stage, Simpsons.SPACE_NAME, Simpsons.Characters.LISA);
        ArangoDocumentReference maggieId = uploadToDatabase(stage, Simpsons.SPACE_NAME, Simpsons.Characters.MAGGIE);
        ArangoDocumentReference margeId = uploadToDatabase(stage, Simpsons.SPACE_NAME, Simpsons.Characters.MARGE);

        NormalizedJsonLd editorClient = jsonAdapter.fromJson(GlobalSpecifications.KG_EDITOR_CLIENT, NormalizedJsonLd.class);
        ArangoDocumentReference clientId = upsert(admin, editorClient, stage);



        //When
        upsert(admin, jsonAdapter.fromJson(GlobalSpecifications.PERSON_TYPE_DEFINITION, NormalizedJsonLd.class), stage);
        upsert(admin, jsonAdapter.fromJson(GlobalSpecifications.GIVEN_NAME_PROPERTY_DEFINITION, NormalizedJsonLd.class), stage);
        upsert(admin, jsonAdapter.fromJson(GlobalSpecifications.FAMILY_NAME_PROPERTY_DEFINITION, NormalizedJsonLd.class), stage);
        upsert(admin, jsonAdapter.fromJson(GlobalSpecifications.SCHEMA_ORG_NAME_PROPERTY_DEFINITION, NormalizedJsonLd.class), stage);
        upsert(admin, jsonAdapter.fromJson(GlobalSpecifications.GIVEN_NAME_IN_PERSON_PROPERTY_DEFINITION, NormalizedJsonLd.class), stage);
        upsert(admin, jsonAdapter.fromJson(GlobalSpecifications.CHILDREN_PROPERTY_DEFINITION, NormalizedJsonLd.class), stage);

        upsert(kgeditor, jsonAdapter.fromJson(KGEditor.PERSON_TYPE_DEFINITION, NormalizedJsonLd.class), stage);
        upsert(kgeditor, jsonAdapter.fromJson(KGEditor.GIVEN_NAME_PROPERTY_DEFINITION, NormalizedJsonLd.class), stage);
        upsert(kgeditor, jsonAdapter.fromJson(KGEditor.GIVEN_NAME_IN_PERSON_PROPERY_DEFINITION, NormalizedJsonLd.class), stage);

        //Then

    }


}

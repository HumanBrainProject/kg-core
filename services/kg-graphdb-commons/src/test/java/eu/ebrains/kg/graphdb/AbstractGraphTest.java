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

package eu.ebrains.kg.graphdb;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.api.Ids;
import eu.ebrains.kg.commons.api.PrimaryStoreUsers;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.ingestion.controller.TodoListProcessor;
import eu.ebrains.kg.test.factory.UserFactory;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import java.util.UUID;

@SpringBootTest
@TestPropertySource(properties = {"KEYCLOAK_ISSUER_URI = http://invalid/", ""})
public class AbstractGraphTest {
    @MockBean
    protected Authentication.Client authClient;

    @Autowired
    protected TodoListProcessor todoListProcessor;

    @Autowired
    private ArangoDatabases arangoDatabases;

    @Autowired
    protected JsonAdapter jsonAdapter;

    @BeforeEach
    public void setup(){
        Mockito.doAnswer(a -> UserFactory.globalAdmin().getUserWithRoles()).when(authClient).getRoles(Mockito.anyBoolean());
        arangoDatabases.clearAll();
    }

    @MockBean
    protected Ids.Client ids;

    @MockBean
    protected PrimaryStoreUsers.Client primaryStoreUsers;

    protected ArangoDocumentReference upsert(SpaceName spaceName, NormalizedJsonLd payload, DataStage stage){
        return upsert(spaceName, UUID.randomUUID(), payload, stage);
    }

    protected ArangoDocumentReference upsert(SpaceName spaceName, UUID uuid, @NotNull NormalizedJsonLd payload, DataStage stage){
        return todoListProcessor.upsertDocument(ArangoCollectionReference.fromSpace(spaceName).doc(uuid),payload, stage, spaceName);
    }
}

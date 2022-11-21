/*
 * Copyright 2022 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.graphdb;

import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.api.Ids;
import eu.ebrains.kg.commons.api.PrimaryStoreUsers;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.test.TestCategories;
import eu.ebrains.kg.test.factory.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"KEYCLOAK_ISSUER_URI = http://invalid/", ""})
public class AbstractGraphTest {
    @MockBean
    protected Authentication.Client authClient;

    @Autowired
    ArangoDatabases arangoDatabases;

    @BeforeEach
    public void setup(){
        Mockito.doAnswer(a -> UserFactory.globalAdmin().getUserWithRoles()).when(authClient).getRoles(Mockito.anyBoolean());
        arangoDatabases.clearAll();
    }

    @MockBean
    protected Ids.Client ids;

    @MockBean
    protected PrimaryStoreUsers.Client primaryStoreUsers;
}

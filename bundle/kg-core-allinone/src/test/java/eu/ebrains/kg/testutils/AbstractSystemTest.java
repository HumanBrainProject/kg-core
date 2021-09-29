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

package eu.ebrains.kg.testutils;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.KgCoreAllInOne;
import eu.ebrains.kg.authentication.api.AuthenticationAPI;
import eu.ebrains.kg.authentication.keycloak.KeycloakController;
import eu.ebrains.kg.commons.AuthTokenContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.model.ExtendedResponseConfiguration;
import eu.ebrains.kg.commons.model.IngestConfiguration;
import eu.ebrains.kg.commons.model.PaginationParam;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
@RunWith(SpringRunner.class)
@SpringBootTest(classes = KgCoreAllInOne.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {"eu.ebrains.kg.core.metadata.synchronous=true", "eu.ebrains.kg.test=true", "arangodb.connections.max=1"})
public abstract class AbstractSystemTest {

    protected PaginationParam EMPTY_PAGINATION = new PaginationParam();
    protected ExtendedResponseConfiguration DEFAULT_RESPONSE_CONFIG = new ExtendedResponseConfiguration();
    protected boolean DEFAULT_DEFER_INFERENCE = false;


    protected static final int smallPayload = 5;
    protected static final int averagePayload = 25;
    protected static final int bigPayload = 100;

    protected String type = "https://core.kg.ebrains.eu/TestPayload";

    @MockBean
    protected AuthTokenContext authTokenContext;

    @MockBean
    protected AuthenticationAPI authenticationAPI;

    @MockBean
    protected KeycloakController keycloakController;

    @Autowired
    @Qualifier("arangoBuilderForGraphDB")
    protected ArangoDB.Builder database;

    @Autowired
    protected IdUtils idUtils;

}

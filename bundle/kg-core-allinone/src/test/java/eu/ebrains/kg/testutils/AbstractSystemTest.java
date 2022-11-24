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

package eu.ebrains.kg.testutils;

import eu.ebrains.kg.KgCoreAllInOne;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.authentication.api.AuthenticationAPI;
import eu.ebrains.kg.authentication.controller.AuthenticationRepository;
import eu.ebrains.kg.authentication.keycloak.KeycloakClient;
import eu.ebrains.kg.authentication.keycloak.KeycloakController;
import eu.ebrains.kg.commons.AuthTokenContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.SetupLogic;
import eu.ebrains.kg.commons.model.ExtendedResponseConfiguration;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.permission.roles.Role;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.core.api.instances.TestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

@SpringBootTest(classes = KgCoreAllInOne.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"KEYCLOAK_ISSUER_URI = http://invalid/", ""})
public abstract class AbstractSystemTest {

    protected PaginationParam EMPTY_PAGINATION = new PaginationParam();
    protected ExtendedResponseConfiguration DEFAULT_RESPONSE_CONFIG = new ExtendedResponseConfiguration();

    protected static final int smallPayload = 5;
    protected static final int averagePayload = 25;
    protected static final int bigPayload = 100;

    protected String type = "https://core.kg.ebrains.eu/TestPayload";

    @MockBean
    protected AuthTokenContext authTokenContext;

    @MockBean
    protected AuthenticationAPI authenticationAPI;


    //We mock the keycloak controller bean to prevent it to initialize
    @MockBean
    protected KeycloakController keycloakController;


    //We mock the keycloak client bean to prevent it to initialize
    @MockBean
    protected KeycloakClient keycloakClient;

    @Autowired
    protected IdUtils idUtils;

    @Autowired
    protected List<ArangoDatabaseProxy> arangoDatabaseProxyList;

    @Autowired
    protected List<SetupLogic> setupLogics;

    @Autowired
    protected CacheManager cacheManager;

    @Autowired
    private AuthenticationRepository authenticationRepository;


    protected TestContext ctx(RoleMapping... roles){
        return new TestContext(idUtils, arangoDatabaseProxyList, authenticationAPI, roles, setupLogics, authenticationRepository, cacheManager);
    }

    protected TestContext ctx(List<List<Role>> roleCollections){
        return new TestContext(idUtils, arangoDatabaseProxyList, authenticationAPI, roleCollections, setupLogics, authenticationRepository, cacheManager);
    }

}

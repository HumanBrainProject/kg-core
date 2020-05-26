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

package eu.ebrains.kg.admin.api;

import com.google.gson.Gson;
import com.netflix.discovery.EurekaClient;
import eu.ebrains.kg.admin.controller.AdminArangoRepository;
import eu.ebrains.kg.admin.controller.AdminUserController;
import eu.ebrains.kg.admin.serviceCall.AdminToAuthentication;
import eu.ebrains.kg.admin.serviceCall.AdminToGraphDB;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.docker.SpringDockerComposeRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UsersTest {

    @Autowired
    EurekaClient discoveryClient;

    AdminUsersAPI users;

    SpringDockerComposeRunner dockerComposeRunner;

    @Autowired
    AdminArangoRepository repository;

    @Autowired
    IdUtils idUtils;

    @Autowired
    Gson gson;

    @Autowired
    AdminToGraphDB graphDBSvc;


    @Autowired
    AdminUserController userController;

    String userId = "test";

    @Before
    public void setup() {
        this.dockerComposeRunner = new SpringDockerComposeRunner(discoveryClient, true, false, Arrays.asList("arango"), "kg-primarystore", "kg-query", "kg-graphdb-sync", "kg-jsonld", "kg-indexing", "kg-inference", "kg-permissions");
        dockerComposeRunner.start();

        AdminToAuthentication authenticationSvcMock = Mockito.mock(AdminToAuthentication.class);

        JsonLdDoc responseFromAuthentication = new JsonLdDoc();
        responseFromAuthentication.addProperty("sub", userId);

        Mockito.doReturn(responseFromAuthentication).when(authenticationSvcMock).getUser();

        users = new AdminUsersAPI(authenticationSvcMock, userController, gson);

    }

    @Test
    public void testUserInfo() {
        //Given


        //When
        ResponseEntity<User> user = users.getUser();


        //Then
        Assert.assertNotNull(user);
        Assert.assertEquals(userId, user.getBody().getNativeId());
    }
}
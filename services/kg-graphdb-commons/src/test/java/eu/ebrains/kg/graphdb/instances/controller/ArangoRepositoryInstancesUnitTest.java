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

package eu.ebrains.kg.graphdb.instances.controller;

import com.google.gson.Gson;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.permissions.controller.PermissionSvc;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.controller.ArangoUtils;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class ArangoRepositoryInstancesUnitTest {

    @Test
    public void mergeEmbeddedDocuments() {
        //Given
        ArangoRepositoryInstances repository =
                new ArangoRepositoryInstances(Mockito.mock(ArangoRepositoryCommons.class), Mockito.mock(PermissionsController.class), Mockito.mock(PermissionSvc.class), Mockito.mock(AuthContext.class), Mockito.mock(ArangoUtils.class), Mockito.mock(ArangoDatabases.class), Mockito.mock(IdUtils.class));
        Gson gson = new Gson();
        String originalDoc = "{\"helloWorld\": {\"@id\": \"http://foobar\"}, \"@id\": \"http://foo\"}";
        NormalizedJsonLd original = gson.fromJson(originalDoc, NormalizedJsonLd.class);
        String embeddedDoc = "{\"name\": \"foobar\"}";
        Map<String, NormalizedJsonLd> embedded = new HashMap<>();
        embedded.put("http://foobar", gson.fromJson(embeddedDoc, NormalizedJsonLd.class));

        //When
        repository.mergeEmbeddedDocuments(original, embedded);

        //Then
        Assert.assertEquals("{\"helloWorld\":{\"name\":\"foobar\"},\"@id\":\"http://foo\"}", gson.toJson(original));
    }

}
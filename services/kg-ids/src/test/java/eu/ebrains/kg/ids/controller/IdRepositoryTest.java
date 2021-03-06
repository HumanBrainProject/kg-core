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

package eu.ebrains.kg.ids.controller;

import com.arangodb.ArangoCollection;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.ids.model.PersistedId;
import eu.ebrains.kg.test.JsonAdapter4Test;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class IdRepositoryTest {

    @Test
    public void upsertWithMerge() {
        //Given
        String namespace = "http://test/";
        SpaceName foo = new SpaceName("foo");
        IdUtils idUtils = new IdUtils(namespace);

        IdRepository idRepository = Mockito.spy(new IdRepository(Mockito.mock(ArangoDatabaseProxy.class), new JsonAdapter4Test(), idUtils));
        ArangoCollection collection = Mockito.mock(ArangoCollection.class);
        Mockito.doReturn(collection).when(idRepository).getOrCreateCollection(Mockito.any());

        //We start with two instances in the "database": the first doc with one alternative...
        PersistedId existingPersistedId = new PersistedId();
        existingPersistedId.setUUID(UUID.randomUUID());
        existingPersistedId.setSpace(new SpaceName("foo"));
        existingPersistedId.setAlternativeIds(new HashSet<>(Arrays.asList("http://bar/foo")));
        Mockito.doReturn(existingPersistedId).when(collection).getDocument(Mockito.any(), Mockito.any());

        //And a second mimicked one
        PersistedId secondId = new PersistedId();
        secondId.setUUID(UUID.randomUUID());
        String secondAbsoluteId = idUtils.buildAbsoluteUrl(secondId.getUUID()).getId();
        Mockito.doReturn(true).when(collection).documentExists(Mockito.eq(secondId.getUUID().toString()));

        //We now update the first one: We introduce a link between the two instances and don't provide the previous alternative identifier anymore
        PersistedId newPersistedId = new PersistedId();
        newPersistedId.setUUID(existingPersistedId.getUUID());
        newPersistedId.setAlternativeIds(new HashSet<>(Arrays.asList(secondAbsoluteId)));

        //When
        List<JsonLdId> mergedIds = idRepository.upsert(DataStage.IN_PROGRESS, newPersistedId);

        //Then
        assertEquals(1, mergedIds.size());
        //We assume the second doc id to be registered as a "mergedId"...
        assertEquals(secondAbsoluteId, mergedIds.get(0).getId());
        //... it also should have been removed from the id database
        Mockito.verify(collection, Mockito.times(1)).deleteDocument(Mockito.eq(secondId.getUUID().toString()));
        //... and the update should have been added to the database
        Mockito.verify(collection, Mockito.times(1)).insertDocument(Mockito.eq(new JsonAdapter4Test().toJson(newPersistedId)), Mockito.any());
        //Also, the new persisted id should now contain some alternativeIds
        Assert.assertEquals(2, newPersistedId.getAlternativeIds().size());
        //... the old identifier should be kept although it's not there anymore...
        Assert.assertTrue(newPersistedId.getAlternativeIds().contains("http://bar/foo"));
        //... and obviously, the secondDocId is now an alternative id as well.
        Assert.assertTrue(newPersistedId.getAlternativeIds().contains(secondAbsoluteId));
    }
}
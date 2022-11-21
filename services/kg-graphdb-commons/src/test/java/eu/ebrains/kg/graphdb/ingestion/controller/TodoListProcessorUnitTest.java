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

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.model.internal.spaces.Space;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.test.factory.UserFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TodoListProcessorUnitTest {


    private final TodoListProcessor processor = Mockito.spy(new TodoListProcessor(Mockito.mock(ArangoRepositoryCommons.class), Mockito.mock(StructureSplitter.class), Mockito.mock(MainEventTracker.class), Mockito.mock(IdUtils.class), Mockito.mock(DataController.class), Mockito.mock(ReleasingController.class)));

    @Test
    public void doProcessTodoList() {

        //Given
        UUID id1 = UUID.randomUUID();
        SpaceName spaceName = new SpaceName("foo");
        Space space = new Space(spaceName, false, false, false);
        User user = UserFactory.globalAdmin().getUserWithRoles().getUser();

        List<TodoItem> todoItems = Arrays.asList(
                TodoItem.fromEvent(new PersistedEvent(Event.createDeleteEvent(spaceName, id1, new JsonLdId("http://foobar/"+id1)), DataStage.NATIVE, user, space)),
                TodoItem.fromEvent(new PersistedEvent(Event.createUpsertEvent(spaceName, UUID.randomUUID(), Event.Type.INSERT, Mockito.mock(NormalizedJsonLd.class)), DataStage.NATIVE, user, space)),
                TodoItem.fromEvent(new PersistedEvent(Event.createUpsertEvent(spaceName, UUID.randomUUID(), Event.Type.UPDATE, Mockito.mock(NormalizedJsonLd.class)), DataStage.NATIVE, user, space))
        );

        //When
        processor.doProcessTodoList(todoItems, DataStage.NATIVE);

        //Then
        Mockito.verify(processor, Mockito.times(1)).deleteDocument(Mockito.any(), Mockito.any());
        Mockito.verify(processor, Mockito.times(2)).upsertDocument(Mockito.any(), Mockito.any(), Mockito.any());

    }
}
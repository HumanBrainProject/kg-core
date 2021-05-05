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

package eu.ebrains.kg.graphdb.structure.controller;

import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.model.TodoItem;
import eu.ebrains.kg.commons.models.EventProcessor;
import eu.ebrains.kg.graphdb.commons.serviceCall.IndexingSvc;
import eu.ebrains.kg.graphdb.ingestion.controller.TodoListProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component(value= "main-event-processor")
class MainEventProcessor extends EventProcessor<PersistedEvent> {

    private final IndexingSvc indexingSvc;

    private final TodoListProcessor todoListProcessor;

    public MainEventProcessor(IndexingSvc indexingSvc, TodoListProcessor todoListProcessor, ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        super(threadPoolTaskExecutor);
        this.indexingSvc = indexingSvc;
        this.todoListProcessor = todoListProcessor;
        super.threadPoolTaskExecutor.setCorePoolSize(1);
        super.threadPoolTaskExecutor.setMaxPoolSize(1);
    }

    protected void handleEvent(PersistedEvent persistedEvent) {
        super.logger.debug(String.format("Now processing the event %s", persistedEvent.getEventId()));
        List<TodoItem> todoItemsNative = indexingSvc.getTodoItems(persistedEvent, DataStage.NATIVE);
        todoListProcessor.doProcessTodoList(todoItemsNative, DataStage.NATIVE);

        List<TodoItem> todoItemsInferred = indexingSvc.getTodoItems(persistedEvent, DataStage.LIVE);
        todoListProcessor.doProcessTodoList(todoItemsNative, DataStage.LIVE);

        //TODO released
    }
}

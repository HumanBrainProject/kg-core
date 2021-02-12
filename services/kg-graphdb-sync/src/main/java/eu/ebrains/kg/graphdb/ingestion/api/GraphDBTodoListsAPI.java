/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.graphdb.ingestion.api;

import eu.ebrains.kg.commons.api.GraphDBTodoLists;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.TodoItem;
import eu.ebrains.kg.graphdb.ingestion.controller.TodoListProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GraphDBTodoListsAPI implements GraphDBTodoLists.Client {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TodoListProcessor todoListProcessor;

    public GraphDBTodoListsAPI(TodoListProcessor todoListProcessor) {
        this.todoListProcessor = todoListProcessor;
    }

    @Override
    public void processTodoList(List<TodoItem> todoList, DataStage stage) {
        logger.debug(String.format("Received request to process todolist for stage %s", stage));
        todoListProcessor.doProcessTodoList(todoList, stage);
    }

}

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

package eu.ebrains.kg.graphdb.ingestion.api;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.model.TodoItem;
import eu.ebrains.kg.graphdb.ingestion.controller.TodoListProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/{stage}")
public class TodoLists {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TodoListProcessor todoListProcessor;

    public TodoLists(TodoListProcessor todoListProcessor) {
        this.todoListProcessor = todoListProcessor;
    }

    @PostMapping("/todoLists")
    public void processTodoList(@RequestBody List<TodoItem> todoList, @PathVariable("stage") DataStage stage) {
        logger.debug(String.format("Received request to process todolist for stage %s", stage));
        todoListProcessor.doProcessTodoList(todoList, stage);
    }


    @DeleteMapping("instances-extra/meta/{space}/{id}")
    public void silentRemovalOfInstanceFromMetaDatabase(@PathVariable("stage") DataStage stage, @PathVariable("space") String space, @PathVariable("id") UUID id){
        todoListProcessor.removeDocumentFromMetaDatabase(stage, ArangoCollectionReference.fromSpace(new Space(space)).doc(id));
    }
}

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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/graphdb/{stage}")
public class GraphDBTodoListsAPIRest implements GraphDBTodoLists {

    private final GraphDBTodoListsAPI graphDBTodoListsAPI;

    public GraphDBTodoListsAPIRest(GraphDBTodoListsAPI graphDBTodoListsAPI) {
        this.graphDBTodoListsAPI = graphDBTodoListsAPI;
    }

    @Override
    @PostMapping("/todoLists")
    public void processTodoList(@RequestBody List<TodoItem> todoList, @PathVariable("stage") DataStage stage) {
       graphDBTodoListsAPI.processTodoList(todoList, stage);
    }

}

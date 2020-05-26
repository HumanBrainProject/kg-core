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

package eu.ebrains.kg.indexing.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.TodoItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GraphDBSyncSvc {

    @Autowired
    ServiceCall serviceCall;

    @Autowired
    AuthContext authContext;

    public void handleTodoList(List<TodoItem> todoItems, DataStage stage) {
        serviceCall.post(String.format("http://kg-graphdb-sync/internal/graphdb/%s/todoLists", stage.name()), todoItems, authContext.getAuthTokens(), Void.class);
    }
}

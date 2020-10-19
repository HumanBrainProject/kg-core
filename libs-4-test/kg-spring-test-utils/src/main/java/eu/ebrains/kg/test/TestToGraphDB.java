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

package eu.ebrains.kg.test;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.TodoItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class TestToGraphDB {

    @Autowired
    ServiceCall serviceCall;


    @Autowired
    AuthContext authContext;

    private final static String SERVICE_URL = "http://kg-graphdb-sync/internal/graphdb";

    public void upsert(NormalizedJsonLd jsonLd, DataStage stage, UUID documentId, SpaceName space){
        handleTodoList(Collections.singletonList(new TodoItem(null, documentId, space, Event.Type.INSERT, jsonLd, null, null)), stage);
    }

    public void handleTodoList(List<TodoItem> todoItems, DataStage stage){
        serviceCall.post(String.format("%s/%s/todoLists", SERVICE_URL, stage.name()), todoItems, authContext.getAuthTokens(), Void.class);
    }

    public IndexedJsonLdDoc get(DataStage stage, SpaceName space, UUID id){
        return IndexedJsonLdDoc.from(serviceCall.get(String.format("%s/%s/instances/%s/%s", SERVICE_URL, stage.name(), space.getName(), id), authContext.getAuthTokens(), NormalizedJsonLd.class));
    }

}

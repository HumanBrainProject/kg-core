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

package eu.ebrains.kg.test;

import eu.ebrains.kg.commons.api.GraphDBInstances;
import eu.ebrains.kg.commons.api.GraphDBTodoLists;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.TodoItem;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

@Component
public class GraphDB4Test {

    private final GraphDBTodoLists.Client graphDBTodoLists;
    private final GraphDBInstances.Client graphDBInstances;


    public GraphDB4Test(GraphDBTodoLists.Client graphDBTodoLists, GraphDBInstances.Client graphDBInstances) {
        this.graphDBTodoLists = graphDBTodoLists;
        this.graphDBInstances = graphDBInstances;
    }

    public void upsert(NormalizedJsonLd jsonLd, DataStage stage, UUID documentId, SpaceName space) {
        graphDBTodoLists.processTodoList(Collections.singletonList(new TodoItem(null, documentId, space, Event.Type.INSERT, jsonLd, null, null)), stage);
    }

    public IndexedJsonLdDoc get(DataStage stage, SpaceName space, UUID id) {
        return IndexedJsonLdDoc.from(graphDBInstances.getInstanceById(space.getName(), id, stage, false, false, false, true));
    }

}

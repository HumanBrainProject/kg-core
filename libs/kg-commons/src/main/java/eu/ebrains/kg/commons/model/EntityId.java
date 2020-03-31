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

package eu.ebrains.kg.commons.model;

import java.util.ArrayList;

public class EntityId {

    private String id;

    public EntityId() {
    }

    public EntityId(String id) {
        this.id = id;
    }

    public static class List extends ArrayList<EntityId>{}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}

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

import java.util.List;
import java.util.UUID;

public class ScopeElement {

    private UUID id;
    private String label;
    private transient String internalId;
    private List<String> types;
    private List<ScopeElement> children;

    public ScopeElement() {
    }

    public ScopeElement(UUID id, List<String> types, List<ScopeElement> children, String internalId) {
        this.id = id;
        this.children = children;
        this.types = types;
        this.internalId = internalId;
    }

    public String getInternalId() {
        return internalId;
    }

    public List<String> getTypes() {
        return types;
    }

    public UUID getId() {
        return id;
    }

    public List<ScopeElement> getChildren() {
        return children;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}


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

package eu.ebrains.kg.graphdb.queries.model.spec;

import eu.ebrains.kg.commons.model.Type;
import eu.ebrains.kg.graphdb.queries.model.fieldFilter.PropertyFilter;

import java.util.Collections;
import java.util.List;

public class Specification {
    private final List<SpecProperty> properties;
    private final PropertyFilter documentFilter;
    private final Type rootType;


    public List<SpecProperty> getProperties() {
        return properties;
    }

    public Specification(List<SpecProperty> properties, PropertyFilter documentFilter, Type rootType) {
        this.properties = properties ==null ? Collections.emptyList() : Collections.unmodifiableList(properties);
        this.documentFilter = documentFilter;
        this.rootType = rootType;
    }

    public PropertyFilter getDocumentFilter() {
        return documentFilter;
    }

    public Type getRootType() {
        return rootType;
    }
}

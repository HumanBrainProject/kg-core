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

package eu.ebrains.kg.graphdb.commons.model;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;

import java.util.Collections;
import java.util.Set;

public class AccessRights {

    private final Set<ArangoCollectionReference> accessibleCollections;
    /**
     * Additionally accessible instances can e.g. be defined if a user has been invited to review an instance.
     */
    private final Set<ArangoDocumentReference> additionallyAccessibleInstances;

    public AccessRights(Set<ArangoCollectionReference> accessibleCollections, Set<ArangoDocumentReference> additionallyAccessibleInstances) {
        this.accessibleCollections = Collections.unmodifiableSet(accessibleCollections);
        this.additionallyAccessibleInstances = Collections.unmodifiableSet(additionallyAccessibleInstances);
    }

    public Set<ArangoCollectionReference> getAccessibleCollections() {
        return accessibleCollections;
    }

    public Set<ArangoDocumentReference> getAdditionallyAccessibleInstances() {
        return additionallyAccessibleInstances;
    }
}

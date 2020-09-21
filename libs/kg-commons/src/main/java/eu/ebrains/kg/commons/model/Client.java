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

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;

public class Client {

    @JsonProperty(SchemaOrgVocabulary.NAME)
    private String name;

    @JsonProperty(SchemaOrgVocabulary.IDENTIFIER)
    private String identifier;
    private String serviceAccountId;
    private boolean canExecuteSynchronousQueries;

    public Client() {}

    public Client(String name) {
        this.name = name;
        this.identifier = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getServiceAccountId() {
        return serviceAccountId;
    }

    public void setServiceAccountId(String serviceAccountId) {
        this.serviceAccountId = serviceAccountId;
    }

    public Space getSpace() {
        return new Space(getIdentifier(), true, true);
    }
}

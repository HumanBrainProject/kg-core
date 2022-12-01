/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.commons.jsonld;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class JsonLdId {

     public JsonLdId(String id) {
        if(id==null){
            this.id = null;
        }
        else {
            try {
                new URL(id);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(String.format("A JSON-LD id has to be a proper URI. You have passed \"%s\" instead", id));
            }
            this.id = id.trim();
        }
    }

    public JsonLdId() {
    }

    @JsonProperty(JsonLdConsts.ID)
    private String id;

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonLdId jsonLdId = (JsonLdId) o;
        return Objects.equals(id, jsonLdId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}


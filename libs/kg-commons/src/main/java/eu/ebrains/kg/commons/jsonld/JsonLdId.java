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

package eu.ebrains.kg.commons.jsonld;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class JsonLdId {

    public static JsonLdId cast(String id, JsonLdId fallback){
        try{
            return new JsonLdId(id);
        }
        catch(IllegalArgumentException e){
            return fallback;
        }
    }

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


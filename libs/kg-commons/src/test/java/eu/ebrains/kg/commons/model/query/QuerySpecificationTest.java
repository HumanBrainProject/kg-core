/*
 * Copyright 2022 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.commons.model.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ebrains.kg.commons.JsonAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class QuerySpecificationTest {

    private final JsonAdapter jsonAdapter = new JsonAdapter(new ObjectMapper());


    @Test
    void deserializePayloadWithSimplePath(){
        //given
        String payload = "{\"structure\": [{\"propertyName\": \"id\", \"path\": \"@id\"}]}";

        //when
        QuerySpecification spec = jsonAdapter.fromJson(payload, QuerySpecification.class);

        //then
        final QuerySpecification.StructureItem structureItem = spec.getStructure().get(0);
        assertNotNull(structureItem.getPath());
        assertEquals("@id", structureItem.getPath().get(0).getId());
        assertFalse(structureItem.getPath().get(0).isReverse());
    }

    @Test
    void deserializePayloadWithReversePath(){
        //given
        String payload = "{\"structure\": [{\"propertyName\": \"id\", \"path\": { \"@id\": \"http://aReverseProperty\", \"reverse\": true}}]}";

        //when
        QuerySpecification spec = jsonAdapter.fromJson(payload, QuerySpecification.class);

        //then
        final QuerySpecification.StructureItem structureItem = spec.getStructure().get(0);
        assertNotNull(structureItem.getPath());
        assertEquals("http://aReverseProperty", structureItem.getPath().get(0).getId());
        assertTrue(structureItem.getPath().get(0).isReverse());
    }

    @Test
    void deserializePayloadWithFlattenedPath(){
        //given
        String payload = "{\"structure\": [{\"propertyName\": \"id\", \"path\": [ {\"@id\": \"id\"}, { \"@id\": \"http://aReverseProperty\", \"reverse\": true}]}]}";

        //when
        QuerySpecification spec = jsonAdapter.fromJson(payload, QuerySpecification.class);

        //then
        final QuerySpecification.StructureItem structureItem = spec.getStructure().get(0);
        assertNotNull(structureItem.getPath());
        assertEquals("id", structureItem.getPath().get(0).getId());
        assertEquals("http://aReverseProperty", structureItem.getPath().get(1).getId());
        assertFalse(structureItem.getPath().get(0).isReverse());
        assertTrue(structureItem.getPath().get(1).isReverse());
    }


}
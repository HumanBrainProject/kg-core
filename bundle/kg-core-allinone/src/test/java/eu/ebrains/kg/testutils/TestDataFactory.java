/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
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

package eu.ebrains.kg.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TestDataFactory {

    public static JsonLdDoc createTestData(int numberOfFields, int iteration, boolean normalized) {
        return createTestData(numberOfFields, normalized, String.valueOf(iteration), null);
    }

    public static JsonLdDoc createTestData(int numberOfFields, String salt, boolean normalized) {
        return createTestData(numberOfFields, normalized, salt, null);
    }

    public static JsonLdDoc createTestData(int numberOfFields, boolean normalized, int iteration, Integer linkedInstance) {
        return createTestData(numberOfFields, normalized, String.valueOf(iteration), linkedInstance);
    }

    public static String DYNAMIC_FIELD_PREFIX = "https://schema.hbp.eu/test";
    public static String TEST_TYPE = "https://core.kg.ebrains.eu/TestPayload";


    public static JsonLdDoc createTestData(int numberOfFields, boolean normalized, String salt, Integer linkedInstance) {
        Map<String, Object> testData = new HashMap<>();
        testData.put(JsonLdConsts.TYPE, TEST_TYPE);
        testData.put(JsonLdConsts.ID, String.format("https://core.kg.ebrains.eu/test/%s", salt));
        testData.put(SchemaOrgVocabulary.NAME, salt);
        if (linkedInstance != null) {
            testData.put(normalized ? "https://schema.hbp.eu/linked" : "hbp:linked",
                    //To ensure that the map can be properly normalized, we are not allowed to have a jsonld object in but rather should provide it as a map
                    new ObjectMapper().convertValue(new JsonLdId(String.format("https://core.kg.ebrains.eu/test/%d", linkedInstance)), Map.class));
        }
        if (!normalized) {
            Map<String, String> context = new HashMap<>();
            context.put("hbp", "https://schema.hbp.eu/");
            testData.put("@context", context);
        }
        String randomValue = UUID.randomUUID().toString();
        for (int i = 0; i < numberOfFields; i++) {
            String key = normalized ? DYNAMIC_FIELD_PREFIX + "%d" : "hbp:test%d";
            testData.put(String.format(key, i), String.format("value-%s-%d", randomValue, i));
        }

        return new JsonLdDoc(testData);
    }


}

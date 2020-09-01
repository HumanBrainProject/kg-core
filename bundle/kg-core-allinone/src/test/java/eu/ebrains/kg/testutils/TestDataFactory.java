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

package eu.ebrains.kg.testutils;

import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TestDataFactory {


    public static JsonLdDoc createTestData(int numberOfFields, boolean normalized, int iteration, Integer linkedInstance){
        Map<String, Object> testData = new HashMap<>();
        testData.put("@type", "https://core.kg.ebrains.eu/TestPayload");
        testData.put("@id", String.format("https://core.kg.ebrains.eu/test/%d", iteration));
        if(linkedInstance!=null){
            testData.put(normalized ? "https://schema.hbp.eu/linked" : "hbp:linked", new JsonLdId(String.format("https://core.kg.ebrains.eu/test/%d", linkedInstance)));
        }
        if(!normalized){
            Map<String, String> context = new HashMap<>();
            context.put("hbp", "https://schema.hbp.eu/");
            testData.put("@context", context);
        }
        String randomValue = UUID.randomUUID().toString();
        for (int i = 0; i < numberOfFields; i++) {
            String key = normalized ? "https://schema.hbp.eu/test%d" : "hbp:test%d";
            testData.put(String.format(key, i), String.format("value-%s-%d", randomValue, i));
        }

        return new JsonLdDoc(testData);
    }


}

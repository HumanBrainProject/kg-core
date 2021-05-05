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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.systemTest.controller;

import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.SpaceName;
import org.springframework.stereotype.Component;

@Component
public class TestObjectFactory {

    public static String TEST_TYPE = "http://kg.test/Test";

    public SpaceName getSpaceA(){
        return new SpaceName("testA");
    }

    public SpaceName getSpaceB(){
        return new SpaceName("testB");
    }

    public NormalizedJsonLd createSimplePayload(String id){
        NormalizedJsonLd instance = new NormalizedJsonLd();
        instance.setId(new JsonLdId("http://kg.test/"+id));
        instance.addTypes(TEST_TYPE);
        instance.addProperty("http://foo", "bar"+id);
        return instance;
    }


}

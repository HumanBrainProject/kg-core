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

package eu.ebrains.kg.test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ebrains.kg.commons.JsonAdapter;

public class JsonAdapter4Test extends JsonAdapter {

    public JsonAdapter4Test() {
        super(createObjectMapper());
    }

    private static ObjectMapper createObjectMapper() {
        // In test-cases without spring context, we need the object mapper outside of the spring context -> so we need to duplicate the logic. Please ensure, that this configuration is consistent with the one defined in (@see eu.ebrains.kg.commons.config.JsonConfig)
        return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE).setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }
}

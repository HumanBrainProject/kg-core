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

package eu.ebrains.kg.commons.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.TypeUtils;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonConfig {

    @Bean
    public TypeUtils produceTypeUtils(JsonAdapter jsonAdapter) {
        return new TypeUtils(jsonAdapter);
    }

    @Bean
    public JsonAdapter produceJsonAdapter(ObjectMapper objectMapper) {
        return new JsonAdapter(objectMapper);
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizeObjectMapper() {
        return builder -> builder.visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE).visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }
}

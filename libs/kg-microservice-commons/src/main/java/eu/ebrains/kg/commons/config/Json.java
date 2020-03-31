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

import com.google.gson.*;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Type;
import java.util.List;

/**
 * We're opting for GSON instead of Jackson to be our JSON processing engine - to make sure, this works well with the SpringFox (Swagger) plugin, we need to override some beans..
 */
@Configuration
public class Json {

    @Bean
    public GsonBuilder gsonBuilder(List<GsonBuilderCustomizer> customizers) {
        GsonBuilder builder = new GsonBuilder();
        customizers.forEach((c) -> c.customize(builder));
        return builder.registerTypeAdapter(springfox.documentation.spring.web.json.Json.class, new SpringfoxJsonToGsonAdapter());
    }

    public class SpringfoxJsonToGsonAdapter implements JsonSerializer<springfox.documentation.spring.web.json.Json> {
        @Override
        public JsonElement serialize(springfox.documentation.spring.web.json.Json json, Type type, JsonSerializationContext context) {
            final JsonParser parser = new JsonParser();
            return parser.parse(json.value());
        }
    }
}

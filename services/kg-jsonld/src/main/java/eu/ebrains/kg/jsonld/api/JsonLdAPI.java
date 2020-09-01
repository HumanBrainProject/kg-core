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

package eu.ebrains.kg.jsonld.api;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.api.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import com.google.gson.Gson;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
@RestController
@RequestMapping("/internal/jsonld")
public class JsonLdAPI {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static String NULL_PLACEHOLDER = EBRAINSVocabulary.NAMESPACE + "jsonld/nullvalue";

    private void addNullValuesPlaceholder(Object o) {
        if (o instanceof Map) {
            Map map = (Map) o;
            map.keySet().forEach(k -> {
                Object value = map.get(k);
                if (value == null) {
                    map.put(k, NULL_PLACEHOLDER);
                } else {
                    addNullValuesPlaceholder(value);
                }
            });
        } else if (o instanceof Collection) {
            ((Collection<?>) o).forEach(this::addNullValuesPlaceholder);
        }
    }

    private void removeNullValuesPlaceholder(Object o) {
        if (o instanceof Map) {
            Map map = (Map) o;
            map.keySet().forEach(k -> {
                Object value = map.get(k);
                if (value instanceof String && value.equals(NULL_PLACEHOLDER)) {
                    map.put(k, null);
                } else {
                    removeNullValuesPlaceholder(value);
                }
            });
        } else if (o instanceof Collection) {
            ((Collection<?>) o).forEach(this::removeNullValuesPlaceholder);
        }
    }

    private final static Gson GSON = new Gson();
    private final static JsonDocument EMPTY_DOCUMENT = JsonDocument.of(Json.createObjectBuilder().build());


    @PostMapping
    public NormalizedJsonLd normalize(@RequestBody JsonLdDoc payload, @RequestParam(value = "keepNullValues", required = false, defaultValue = "true") boolean keepNullValues) {
        logger.debug("Received payload to be normalized");
        if (keepNullValues) {
            //The JSON-LD library we're using removes null values - we therefore have to do a little hack and set a placeholder for all null values which we replace with null later on again.
            addNullValuesPlaceholder(payload);
        }
        JsonObject build = Json.createObjectBuilder(payload).build();
        try {
            JsonArray expanded = JsonLd.expand(JsonDocument.of(build)).get();
            JsonObject compacted = JsonLd.compact(JsonDocument.of(expanded), EMPTY_DOCUMENT).get();
            //TODO Optimize - can't we transform the compacted structure directly to a map without taking the way via the string serialization?
            NormalizedJsonLd normalized = new NormalizedJsonLd(GSON.fromJson(compacted.toString(), LinkedHashMap.class));
            if (keepNullValues) {
                removeNullValuesPlaceholder(normalized);
            }
            return flattenLists(normalized, null, null);
        } catch (JsonLdError ex) {
            logger.error("Was not able to handle payload", ex);
            throw new RuntimeException(ex);
        }
    }


    private <T> T flattenLists(T input, Map parent, String parentKey) {
        if (input instanceof List) {
            ((List) input).forEach(i -> flattenLists(i, parent, parentKey));
        } else if (input instanceof Map) {
            if (((Map) input).containsKey(JsonLdConsts.LIST)) {
                Object list = ((Map) input).get(JsonLdConsts.LIST);
                parent.put(parentKey, list);
            } else {
                for (Object o : ((Map) input).keySet()) {
                    flattenLists(((Map) input).get(o), (Map) input, (String) o);
                }
            }
        }
        return input;
    }

}

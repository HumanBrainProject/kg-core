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

package eu.ebrains.kg.jsonld.api;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.exception.InvalidRequestException;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "rawtypes"})
@Component
public class JsonLdAPI implements eu.ebrains.kg.commons.api.JsonLd.Client {

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

    private final JsonAdapter jsonAdapter;
    private final static JsonDocument EMPTY_DOCUMENT = JsonDocument.of(Json.createObjectBuilder().build());

    public JsonLdAPI(JsonAdapter jsonAdapter) {
        this.jsonAdapter = jsonAdapter;
    }


    @Override
    public NormalizedJsonLd normalize(JsonLdDoc payload, boolean keepNullValues) {
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
            NormalizedJsonLd normalized = new NormalizedJsonLd(jsonAdapter.fromJson(compacted.toString(), LinkedHashMap.class));
            if (keepNullValues) {
                removeNullValuesPlaceholder(normalized);
            }
            normalized.normalizeTypes();
            return flattenLists(normalized, null, null);
        } catch (JsonLdError ex) {
            logger.error("Was not able to handle payload", ex);
            throw new InvalidRequestException(ex.getMessage());
        }
    }

    private <T> T applyLists(T input) {
        if (input instanceof List) {
            ((List) input).forEach(this::applyLists);
        } else if (input instanceof Map) {
            Set<Object> keysWithLists = new HashSet<>();
            for (Object key : ((Map) input).keySet()) {
                Object el = ((Map) input).get(key);
                if(el instanceof List){
                    keysWithLists.add(key);
                }
                applyLists(el);
            }
            keysWithLists.forEach(k -> {
                Map<String, Object> listWrapper = new LinkedHashMap();
                listWrapper.put(JsonLdConsts.LIST, ((Map)input).get(k));
                ((Map)input).put(k, listWrapper);
            });
        }
        return input;
    }


    private <T> T flattenLists(T input, Map parent, String parentKey) {
        if(parent==null){
            return input;
        }
        if (input instanceof List) {
            ((List) input).forEach(i -> flattenLists(i, parent, parentKey));
        } else if (input instanceof Map) {
            if (((Map) input).containsKey(JsonLdConsts.LIST)) {
                Object list = ((Map) input).get(JsonLdConsts.LIST);
                if(list instanceof List){
                    parent.put(parentKey, ((List)list).stream().map(l -> flattenLists(l, null, null)).collect(Collectors.toList()));
                }
                else {
                    parent.put(parentKey, list);
                }
            } else {
                for (Object o : ((Map) input).keySet()) {
                    flattenLists(((Map) input).get(o), (Map) input, (String) o);
                }
            }
        }
        return input;
    }

}

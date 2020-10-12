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

import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * A  JSON-LD in compacted format with applied context (the original context is kept for reference). It is the JSON-LD which comes the closest to basic JSON.
 */
public class NormalizedJsonLd extends JsonLdDoc {

    public NormalizedJsonLd() {
    }

    public NormalizedJsonLd(Map<? extends String, ?> m) {
        super(m);
    }

    public Set<String> allIdentifiersIncludingId() {
        Set<String> identifiers = new HashSet<>(identifiers());
        if (id() != null) {
            identifiers.add(id().getId());
        }
        return identifiers;
    }

    public NormalizedJsonLd removeAllFieldsFromNamespace(String namespace){
        this.keySet().removeIf(k -> k.startsWith(namespace));
        return this;
    }

    public NormalizedJsonLd keepPropertiesOnly(Collection<String> whiteList){
        this.keySet().removeIf(k -> !whiteList.contains(k));
        return this;
    }

    public NormalizedJsonLd removeAllInternalProperties() {
        this.keySet().removeIf(NormalizedJsonLd::isInternalKey);
        return this;
    }

    public void normalizeTypes() {
        Object type = get(JsonLdConsts.TYPE);
        if (type != null && !(type instanceof Collection)) {
            put(JsonLdConsts.TYPE, Collections.singletonList(type));
        }
    }


    public void visitPublicKeys(BiConsumer<String, Object> consumer) {
        for (Object key : keySet()) {
            if (key instanceof String && !isInternalKey((String) key)) {
                consumer.accept((String) key, get(key));
            }
        }
    }

    public static boolean isInternalKey(String key) {
        return key.startsWith("_");
    }

    public void defineFieldUpdateTimes(Map<String, ZonedDateTime> fieldUpdateTimes) {
        put(EBRAINSVocabulary.META_PROPERTYUPDATES, serializeUpdateTimes(fieldUpdateTimes));
    }

    public Map<String, ZonedDateTime> fieldUpdateTimes() {
        Object o = get(EBRAINSVocabulary.META_PROPERTYUPDATES);
        if (o instanceof Map) {
            return deserializeUpdateTimes((Map) o);
        }
        return null;
    }

    private Map<String, String> serializeUpdateTimes(Map<String, ZonedDateTime> map) {
        Map<String, String> newMap = new HashMap<>();
        for (String key : map.keySet()) {
            newMap.put(key, map.get(key).format(DateTimeFormatter.ISO_INSTANT));
        }
        return newMap;
    }

    private Map<String, ZonedDateTime> deserializeUpdateTimes(Map<String, String> map) {
        Map<String, ZonedDateTime> newMap = new HashMap<>();
        for (String key : map.keySet()) {
            newMap.put(key, ZonedDateTime.parse(map.get(key)));
        }
        return newMap;
    }

}

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

package eu.ebrains.kg.commons.jsonld;

import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A  JSON-LD in compacted format with applied context. It is the JSON-LD which comes the closest to basic JSON.
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


    public void resolvePrivateSpace(SpaceName privateSpace){
        final String s = getAs(EBRAINSVocabulary.META_SPACE, String.class);
        if(privateSpace.getName().equals(s)){
            put(EBRAINSVocabulary.META_SPACE, SpaceName.PRIVATE_SPACE);
        }
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

    public void applyVocab(String vocab){
        JsonLdDoc context = new JsonLdDoc();
        context.addProperty(JsonLdConsts.VOCAB, vocab);
        addProperty(JsonLdConsts.CONTEXT, context);
        visitKeys((map, key) ->{
            if(key.startsWith(vocab)){
                map.put(key.substring(vocab.length()), map.get(key));
                map.remove(key);
            }
        });
    }

}

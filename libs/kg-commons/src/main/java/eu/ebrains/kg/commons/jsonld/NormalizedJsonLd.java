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


    public NormalizedJsonLd(Map<String, ?> m) {
        super(m);
    }

    public Set<String> allIdentifiersIncludingId() {
        Set<String> identifiers = new HashSet<>(identifiers());
        if (id() != null) {
            identifiers.add(id().getId());
        }
        return identifiers;
    }


    public void defineFieldUpdateTimes(Map<String, ZonedDateTime> fieldUpdateTimes) {
        put(EBRAINSVocabulary.META_PROPERTYUPDATES, serializeUpdateTimes(fieldUpdateTimes));
    }

    private transient Map<String, ZonedDateTime> fieldUpdateTimes;

    public Map<String, ZonedDateTime> fieldUpdateTimes() {
        if(fieldUpdateTimes == null) {
            Object o = get(EBRAINSVocabulary.META_PROPERTYUPDATES);
            if (o instanceof Map) {
                fieldUpdateTimes = deserializeUpdateTimes((Map) o);
            }
        }
        return fieldUpdateTimes;
    }

    private Map<String, String> serializeUpdateTimes(Map<String, ZonedDateTime> map) {
        Map<String, String> newMap = new HashMap<>();
        for (Map.Entry<String,ZonedDateTime> entry : map.entrySet()) {
            newMap.put(entry.getKey(), entry.getValue().format(DateTimeFormatter.ISO_INSTANT));
        }
        return newMap;
    }

    private Map<String, ZonedDateTime> deserializeUpdateTimes(Map<String, String> map) {
        Map<String, ZonedDateTime> newMap = new HashMap<>();
        for (Map.Entry<String,String> entry : map.entrySet()) {
            newMap.put(entry.getKey(),ZonedDateTime.parse(entry.getValue()));
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

    public NormalizedJsonLd renameSpace(SpaceName privateSpace, boolean invitation){
        if(privateSpace!=null || invitation) {
            visitKeys((map, key) -> {
                if(key.equals(EBRAINSVocabulary.META_SPACE)) {
                    if (privateSpace != null && privateSpace.getName().equals(map.get(key))){
                        map.put(key, SpaceName.PRIVATE_SPACE);
                    }
                    else if(invitation){
                        map.put(key, SpaceName.REVIEW_SPACE);
                    }
                }
            });
        }
        return this;
    }

    public void removeAllPropertiesWhenNoPayload() {
        this.keySet().removeIf(NormalizedJsonLd::isNotNecessaryKey);
    }

    public void removeSpace() {
        this.keySet().removeIf(NormalizedJsonLd::isSpaceKey);
    }

    public static boolean isNotNecessaryKey(String key) {
        return (!key.equals(EBRAINSVocabulary.META_SPACE) && !key.equals(EBRAINSVocabulary.META_INCOMING_LINKS) && !key.equals(JsonLdConsts.ID));
    }

    public static boolean isSpaceKey(String key) {
        return key.equals(EBRAINSVocabulary.META_SPACE);
    }

}

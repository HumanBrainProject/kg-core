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


import eu.ebrains.kg.commons.semantics.vocabularies.HBPVocabulary;

import java.util.Set;
import java.util.TreeMap;

public class Alternative extends TreeMap<String, Object> {

    public Alternative(Object value, Set<String> userIds, Boolean isSelected) {
        this.put(HBPVocabulary.INFERENCE_ALTERNATIVES_VALUE, value);
        this.put(HBPVocabulary.INFERENCE_ALTERNATIVES_USERIDS, userIds);
        this.put(HBPVocabulary.INFERENCE_ALTERNATIVES_SELECTED, isSelected);
    }

    public Object getValue() {
        return this.get(HBPVocabulary.INFERENCE_ALTERNATIVES_VALUE);
    }
    public Boolean getIsSelected() {
        return (Boolean) this.get(HBPVocabulary.INFERENCE_ALTERNATIVES_SELECTED);
    }

    public Set<String> getUserIds() {
        return (Set<String>)this.get(HBPVocabulary.INFERENCE_ALTERNATIVES_USERIDS);
    }
}

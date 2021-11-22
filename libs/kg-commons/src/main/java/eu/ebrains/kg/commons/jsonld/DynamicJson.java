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

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * A JSON in any format (with / without context) / @graph annotations, etc.
 */
public class DynamicJson extends LinkedHashMap<String, Object> {

    public DynamicJson() {
    }

    public DynamicJson(Map<? extends String, ?> m) {
        super(m);
    }


    public <T> List<T> getAsListOf(String key, Class<T> clazz) {
        return getAsListOf(key, clazz, false, Collections.emptyList());
    }

    public <T> List<T> getAsListOf(String key, Class<T> clazz, List<T> fallback) {
        return getAsListOf(key, clazz, false, fallback);
    }

    public <T> List<T> getAsListOf(String key, Class<T> clazz, boolean skipWrongTypes){
        return getAsListOf(key, clazz, skipWrongTypes, Collections.emptyList());
    }

    public <T> List<T> getAsListOf(String key, Class<T> clazz, boolean skipWrongTypes, List<T> fallback){
        Object o = get(key);
        if(o == null){
            return fallback;
        }
        if(o instanceof Collection){
            return ((Collection<?>) o).stream().map(i -> castType(clazz, i, !skipWrongTypes)).filter(Objects::nonNull).collect(Collectors.toList());
        }
        else{
            T singleInstance = castType(clazz, o, !skipWrongTypes);
            if(singleInstance!=null){
                return Collections.singletonList(singleInstance);
            }
            else{
                return fallback;
            }
        }
    }

    public <T> T getAs(String key, Class<T> clazz, T fallback){
        T value = castType(clazz, get(key), false);
        return value != null ? value : fallback;
    }

    public <T> T getAs(String key, Class<T> clazz) {
        return castType(clazz, get(key), true);
    }

    public void removeAllFieldsFromNamespace(String namespace){
        this.keySet().removeIf(k -> k.startsWith(namespace));
    }

    public void keepPropertiesOnly(Collection<String> whiteList){
        this.keySet().removeIf(k -> !whiteList.contains(k));
    }

    public void removeAllInternalProperties() {
        this.keySet().removeIf(DynamicJson::isInternalKey);
    }

    public void visitPublicKeys(BiConsumer<String, Object> consumer) {
        for (Object key : keySet()) {
            if (key instanceof String && !isInternalKey((String) key)) {
                consumer.accept((String) key, get(key));
            }
        }
    }

    protected void visitKeys(BiConsumer<Map<String, Object>, String> consumer){
        doVisitKey(consumer, this);
    }

    private void doVisitKey(BiConsumer<Map<String, Object>, String> consumer, Map<String, Object> map){
        final Set<String> keys = new HashSet<>(map.keySet());
        for (String key : keys) {
            final Object value = map.get(key);
            if(value instanceof Collection<?>){
                for (Object o : ((Collection<?>) value)) {
                    if(o instanceof Map){
                        doVisitKey(consumer, (Map<String, Object>)o);
                    }
                }
            }
            else if(value instanceof Map){
                doVisitKey(consumer, (Map<String, Object>)value);
            }
            consumer.accept(map, key);
        }
    }


    public static boolean isInternalKey(String key) {
        return key.startsWith("_");
    }

    private <T> T castType(Class<T> clazz, Object o, boolean throwException) {
        if (o == null) {
            return null;
        }
        if (clazz.isInstance(o)) {
            return (T) o;
        }
        if(o instanceof Map){
            if(clazz == Map.class){
                return (T)o;
            }
            if(clazz == JsonLdId.class){
                Object id = ((Map) o).get(JsonLdConsts.ID);
                try {
                    return id instanceof String ? (T) new JsonLdId((String) id) : null;
                }
                catch (IllegalArgumentException e){
                    if(throwException){
                        throw e;
                    }
                    return null;
                }
            }
            if(clazz == DynamicJson.class){
                return (T)new DynamicJson((Map)o);
            }
            else if(clazz == NormalizedJsonLd.class){
                return (T) new NormalizedJsonLd((Map)o);
            }
        }
        if(o instanceof Collection){
            if(((Collection<?>)o).isEmpty()){
                return null;
            }
            else if(((Collection<?>)o).size()==1) {
                return castType(clazz, ((Collection<?>) o).iterator().next(), throwException);
            }
        }
        if(clazz == UUID.class && o instanceof String){
            return (T)UUID.fromString((String)o);
        }
        if(clazz == SpaceName.class && o instanceof String){
            return (T)new SpaceName((String)o);
        }
        if(clazz.isEnum() && o instanceof String){
            try {
                return (T) Enum.valueOf((Class<? extends Enum>) clazz, (String) o);
            }
            catch (IllegalArgumentException e){
                if(throwException){
                    throw e;
                }
                return null;
            }
        }
        if(throwException) {
            throw new IllegalArgumentException(String.format("Wrong type casting - was expecting %s but got %s", clazz.getName(), o.getClass().getName()));
        }
        else{
            return null;
        }
    }
}

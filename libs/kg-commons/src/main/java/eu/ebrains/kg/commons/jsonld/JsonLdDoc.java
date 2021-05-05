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
import eu.ebrains.kg.commons.semantics.vocabularies.HBPVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A JSON-LD in any format (with / without context) / @graph annotations, etc.
 */
public class JsonLdDoc extends TreeMap<String, Object> {

    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_LD_JSON = "application/ld+json";
    protected static final List<String> IDENTIFIER_FIELDS = Collections.singletonList(SchemaOrgVocabulary.IDENTIFIER);

    public JsonLdDoc() {
    }

    public JsonLdDoc(Map<? extends String, ?> m) {
        super(m);
    }


    public List<String> types() {
        Object type = get(JsonLdConsts.TYPE);
        if (type instanceof Collection) {
            return ((Collection<?>)type).stream().map(Object::toString).collect(Collectors.toList());
        } else if (type instanceof String) {
            return Collections.singletonList((String) type);
        }
        return Collections.emptyList();
    }

    public void addTypes(String... types) {
        List<String> allTypes = new ArrayList<>(types());
        for (String type : types) {
            if (!allTypes.contains(type)) {
                allTypes.add(type);
            }
        }
        put(JsonLdConsts.TYPE, allTypes);
    }

    public Set<String> identifiers() {
        return getAsListOf(SchemaOrgVocabulary.IDENTIFIER, String.class).stream().filter(this::isValidIdentifier).collect(Collectors.toSet());
    }

    private boolean isValidIdentifier(String identifier){
        return !identifier.trim().isBlank();
    }

    public void addIdentifiers(String... identifiers) {
        Set<String> allIdentifiers = new HashSet<>(identifiers());
        allIdentifiers.addAll(Arrays.asList(identifiers));
        put(SchemaOrgVocabulary.IDENTIFIER, allIdentifiers);
    }

    public void removeIdentifiers(String... identifiers) {
        for (String identifier : identifiers) {
            identifiers().remove(identifier);
        }
    }

    public void setId(JsonLdId id) {
        put(JsonLdConsts.ID, id!=null ? id.getId() : null);
    }

    public JsonLdId id() {
        String id = getAs(JsonLdConsts.ID, String.class);
        return id!=null ? new JsonLdId(id) : null;
    }

    public <T> List<T> getAsListOf(String key, Class<T> clazz) {
        return getAsListOf(key, clazz, false);
    }

    public <T> List<T> getAsListOf(String key, Class<T> clazz, boolean skipWrongTypes){
        Object o = get(key);
        if(o instanceof Collection){
            return ((Collection<?>)o).stream().map(i -> castType(clazz, i, !skipWrongTypes)).filter(Objects::nonNull).collect(Collectors.toList());
        }
        else{
            T singleInstance = castType(clazz, o, !skipWrongTypes);
            if(singleInstance!=null){
                return Collections.singletonList(singleInstance);
            }
            else{
                return Collections.emptyList();
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

    private <T> T castType(Class<T> clazz, Object o, boolean throwException) {
        if (o == null) {
            return null;
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
            if(clazz == JsonLdDoc.class){
                return (T)new JsonLdDoc((Map)o);
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
        if (clazz.isInstance(o)) {
            return (T) o;
        } else {
            if(throwException) {
                throw new IllegalArgumentException(String.format("Wrong type casting - was expecting %s but got %s", clazz.getName(), o.getClass().getName()));
            }
            else{
                return null;
            }
        }
    }

    public void addProperty(Object key, Object value) {
        put(key.toString(), value);
    }

    public void addReference(String propertyName, String url){
        Map<String, String> reference = new HashMap<>();
        reference.put(JsonLdConsts.ID, url);
        addToProperty(propertyName, reference);
    }

    public JsonLdDoc addToProperty(String propertyName, Object value){
        addToProperty(propertyName, value, this);
        return this;
    }

    public boolean isOfType(String lookupType){
        Object type = get(JsonLdConsts.TYPE);
        if(type!=null && lookupType!=null){
            if(type instanceof String){
                return type.equals(lookupType);
            }
            else if(type instanceof Collection){
                return ((Collection)type).contains(lookupType);
            }
        }
        return false;
    }

    public String primaryIdentifier(){
        if(this.containsKey(SchemaOrgVocabulary.IDENTIFIER)){
            Object identifier = get(SchemaOrgVocabulary.IDENTIFIER);
            if(identifier instanceof List && !((List)identifier).isEmpty()){
                for (Object o : ((List) identifier)) {
                    if(o instanceof String){
                        return (String)o;
                    }
                }
            }
            else if(identifier instanceof String){
                return (String) identifier;
            }
        }
        return null;
    }


    public void addAlternative(String propertyName, Alternative value){
        Map<String, Object> alternatives = (Map<String, Object>)get(HBPVocabulary.INFERENCE_ALTERNATIVES);
        if(alternatives==null){
            alternatives = new TreeMap<>();
            put(HBPVocabulary.INFERENCE_ALTERNATIVES, alternatives);
        }
        Object v;
        if(alternatives.isEmpty()){
            v = new ArrayList<Alternative>();
            ((List) v).add(value);
        }else{
            v = value;
        }
        if(!value.getUserIds().isEmpty() && !value.getUserIds().stream().allMatch(Objects::isNull)){
            addToProperty(propertyName, v, alternatives);
        }
    }

    private static void addToProperty(String propertyName, Object value, Map map){
        Object o = map.get(propertyName);
        if(o==null){
            map.put(propertyName, value);
        }
        else if(o instanceof Collection){
            if(!((Collection)o).contains(value)) {
                ((Collection) o).add(value);
            }
        }
        else if(!o.equals(value)){
            List<Object> list = new ArrayList<>();
            list.add(o);
            list.add(value);
            map.put(propertyName, list);
        }
    }

    public void processLinks(Consumer<Map> referenceConsumer){
        processLinks(referenceConsumer, this, true);
    }

    private void processLinks(Consumer<Map> referenceConsumer, Map currentMap, boolean root){
        //Skip root-id
        if(!root && currentMap.containsKey(JsonLdConsts.ID)){
            Object id = currentMap.get(JsonLdConsts.ID);
            if(id!=null){
                referenceConsumer.accept(currentMap);
            }
        }
        else {
            for (Object key : currentMap.keySet()) {
                Object value = currentMap.get(key);
                if(value instanceof Map){
                    processLinks(referenceConsumer, (Map)value, false);
                }
            }
        }
    }

    public void replaceNamespace(String oldNamespace, String newNamespace){
        replaceNamespace(oldNamespace, newNamespace, this);
    }

    private void replaceNamespace(String oldNamespace, String newNamespace, Map currentMap){
        HashSet keyList = new HashSet<>(currentMap.keySet());
        for (Object key : keyList) {
            if(key instanceof String){
                if(((String)key).startsWith(oldNamespace)){
                    Object value = currentMap.remove(key);
                    if(value instanceof Map){
                        replaceNamespace(oldNamespace, newNamespace, (Map)value);
                    }
                    currentMap.put(newNamespace+((String)key).substring(oldNamespace.length()), value);
                }
            }
        }
    }
}

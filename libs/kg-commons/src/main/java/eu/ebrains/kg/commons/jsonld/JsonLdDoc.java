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

import eu.ebrains.kg.commons.exception.InvalidRequestException;
import eu.ebrains.kg.commons.semantics.vocabularies.HBPVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A JSON-LD in any format (with / without context) / @graph annotations, etc.
 */
public class JsonLdDoc extends DynamicJson {

    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_LD_JSON = "application/ld+json";
    protected static final List<String> IDENTIFIER_FIELDS = Collections.singletonList(SchemaOrgVocabulary.IDENTIFIER);

    public JsonLdDoc() {
    }

    public JsonLdDoc(Map<? extends String, ?> m) {
        super(m);
    }


    public List<String> types() {
        return getAsListOf(JsonLdConsts.TYPE, String.class);
    }

    public void normalizeTypes() {
        visitKeys((map, key) -> {
            if (JsonLdConsts.TYPE.equals(key)) {
                final Object type = map.get(key);
                if (type != null && !(type instanceof Collection)) {
                    map.put(JsonLdConsts.TYPE, Collections.singletonList(type));
                }
            }
        });
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

    private boolean isValidIdentifier(String identifier) {
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
        put(JsonLdConsts.ID, id != null ? id.getId() : null);
    }

    public JsonLdId id() {
        String id = getAs(JsonLdConsts.ID, String.class);
        return id != null ? new JsonLdId(id) : null;
    }

    public void addProperty(Object key, Object value) {
        put(key.toString(), value);
    }

    public void addReference(String propertyName, String url) {
        Map<String, String> reference = new HashMap<>();
        reference.put(JsonLdConsts.ID, url);
        addToProperty(propertyName, reference);
    }

    public JsonLdDoc addToProperty(String propertyName, Object value) {
        addToProperty(propertyName, value, this);
        return this;
    }

    public boolean isOfType(String lookupType) {
        Object type = get(JsonLdConsts.TYPE);
        if (type != null && lookupType != null) {
            if (type instanceof String) {
                return type.equals(lookupType);
            } else if (type instanceof Collection) {
                return ((Collection) type).contains(lookupType);
            }
        }
        return false;
    }

    public boolean hasType() {
        return hasType(this);
    }

    private boolean hasType(Map<?,?> object) {
        return object.containsKey(JsonLdConsts.TYPE);
    }

    private boolean isValidType(Object type) {
        if (type instanceof List && !((List<?>) type).isEmpty()) {
            for (Object o : ((List<?>) type)) {
                if (!isValidIRI(o)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void validateEmbeddedInstances(String key, Object value){
        if(value instanceof Collection){
            for (Object o : ((Collection<?>) value)) {
                doValidateEmbeddedInstance(key, o);
            }
        }
        else{
            doValidateEmbeddedInstance(key, value);
        }
    }

    private void doValidateEmbeddedInstance(String key, Object o){
        if(o instanceof Map && !((Map<?, ?>) o).containsKey(JsonLdConsts.ID)){
            //It's an embedded instance
            final Map<?,?> embeddedInstance = (Map<?,?>) o;
            if(!embeddedInstance.containsKey(JsonLdConsts.TYPE)){
                throw new InvalidRequestException(String.format("The embedded structure of %s needs to provide an @type", key));
            }
        }
    }

    public void validate(boolean requiresTypeAtRootLevel){
        visitKeys((map, key)->{
            boolean rootLevel = map == this;
            final Object value = map.get(key);
            if(requiresTypeAtRootLevel && rootLevel){
                final Object type = map.get(JsonLdConsts.TYPE);
                if(type==null){
                    throw new InvalidRequestException("A payload needs to provide the @type argument");
                }
            }
            switch (key){
                case JsonLdConsts.ID:
                    if(!isValidIRI(value)){
                        throw new InvalidRequestException(String.format("Payload contains at least one invalid @id: %s", value));
                    }
                    if(!rootLevel && map.keySet().size()>1){
                        //If we're not at the root level, the "@id" should be the only element in a map
                        throw new InvalidRequestException(String.format("The reference to %s contained additional fields -> this is not allowed", value));
                    }
                    break;
                case JsonLdConsts.TYPE:
                    if(!isValidType(value)){
                        throw new InvalidRequestException("@type should contain a list of valid urls");
                    }
                    break;
                default:
                    if(!isInternalKey(key)) {
                        if (!isValidIRI(key)) {
                            throw new InvalidRequestException(String.format("The property %s is not fully qualified", key));
                        }
                        validateEmbeddedInstances(key, value);
                    }
                    break;
            }
        });

    }

    private boolean isValidIRI(Object value) {
        return value instanceof String && ((String) value).matches("http(s?)://.*");
    }

    public String primaryIdentifier() {
        if (this.containsKey(SchemaOrgVocabulary.IDENTIFIER)) {
            Object identifier = get(SchemaOrgVocabulary.IDENTIFIER);
            if (identifier instanceof List && !((List) identifier).isEmpty()) {
                for (Object o : ((List) identifier)) {
                    if (o instanceof String) {
                        return (String) o;
                    }
                }
            } else if (identifier instanceof String) {
                return (String) identifier;
            }
        }
        return null;
    }


    public void addAlternative(String propertyName, Alternative value) {
        Map<String, Object> alternatives = (Map<String, Object>) get(HBPVocabulary.INFERENCE_ALTERNATIVES);
        if (alternatives == null) {
            alternatives = new TreeMap<>();
            put(HBPVocabulary.INFERENCE_ALTERNATIVES, alternatives);
        }
        Object v;
        if (alternatives.isEmpty()) {
            v = new ArrayList<Alternative>();
            ((List) v).add(value);
        } else {
            v = value;
        }
        if (!value.getUserIds().isEmpty() && !value.getUserIds().stream().allMatch(Objects::isNull)) {
            addToProperty(propertyName, v, alternatives);
        }
    }

    private static void addToProperty(String propertyName, Object value, Map map) {
        Object o = map.get(propertyName);
        if (o == null) {
            map.put(propertyName, value);
        } else if (o instanceof Collection) {
            if (!((Collection) o).contains(value)) {
                ((Collection) o).add(value);
            }
        } else if (!o.equals(value)) {
            List<Object> list = new ArrayList<>();
            list.add(o);
            list.add(value);
            map.put(propertyName, list);
        }
    }

    public void processLinks(Consumer<Map> referenceConsumer) {
        processLinks(referenceConsumer, this, true);
    }

    private void processLinks(Consumer<Map> referenceConsumer, Map currentMap, boolean root) {
        //Skip root-id
        if (!root && currentMap.containsKey(JsonLdConsts.ID)) {
            Object id = currentMap.get(JsonLdConsts.ID);
            if (id != null) {
                referenceConsumer.accept(currentMap);
            }
        } else {
            for (Object key : currentMap.keySet()) {
                Object value = currentMap.get(key);
                if (value instanceof Map) {
                    processLinks(referenceConsumer, (Map) value, false);
                }
            }
        }
    }

    public void replaceNamespace(String oldNamespace, String newNamespace) {
        replaceNamespace(oldNamespace, newNamespace, this);
    }

    private void replaceNamespace(String oldNamespace, String newNamespace, Map currentMap) {
        HashSet keyList = new HashSet<>(currentMap.keySet());
        for (Object key : keyList) {
            if (key instanceof String) {
                if (((String) key).startsWith(oldNamespace)) {
                    Object value = currentMap.remove(key);
                    if (value instanceof Map) {
                        replaceNamespace(oldNamespace, newNamespace, (Map) value);
                    }
                    currentMap.put(newNamespace + ((String) key).substring(oldNamespace.length()), value);
                }
            }
        }
    }
}

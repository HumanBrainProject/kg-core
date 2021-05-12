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

package eu.ebrains.kg.graphdb.queries.controller;

import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Type;
import eu.ebrains.kg.graphdb.queries.model.fieldFilter.Op;
import eu.ebrains.kg.graphdb.queries.model.fieldFilter.Parameter;
import eu.ebrains.kg.graphdb.queries.model.fieldFilter.PropertyFilter;
import eu.ebrains.kg.graphdb.queries.model.fieldFilter.Value;
import eu.ebrains.kg.graphdb.queries.model.spec.GraphQueryKeys;
import eu.ebrains.kg.graphdb.queries.model.spec.SpecProperty;
import eu.ebrains.kg.graphdb.queries.model.spec.SpecTraverse;
import eu.ebrains.kg.graphdb.queries.model.spec.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SpecificationInterpreter {

    protected Logger logger = LoggerFactory.getLogger(SpecificationInterpreter.class);

    public Specification readSpecification(NormalizedJsonLd jsonObject, Map<String, String> allParameters) {
        List<SpecProperty> specFields = null;
        Type rootType = null;
        String responseVocab = null;
        if (jsonObject.containsKey(GraphQueryKeys.GRAPH_QUERY_META.getFieldName())) {
            NormalizedJsonLd meta = jsonObject.getAs(GraphQueryKeys.GRAPH_QUERY_META.getFieldName(), NormalizedJsonLd.class);
            if (meta != null) {
                if (meta.containsKey(GraphQueryKeys.GRAPH_QUERY_TYPE.getFieldName())) {
                    String type = meta.getAs(GraphQueryKeys.GRAPH_QUERY_TYPE.getFieldName(), String.class);
                    if (type != null) {
                        rootType = new Type(type);
                    }
                }
                if(meta.containsKey(GraphQueryKeys.GRAPH_QUERY_RESPONSE_VOCAB.getFieldName())){
                    responseVocab = meta.getAs(GraphQueryKeys.GRAPH_QUERY_RESPONSE_VOCAB.getFieldName(), String.class);
                }
            }
        }

        if (jsonObject.containsKey(GraphQueryKeys.GRAPH_QUERY_STRUCTURE.getFieldName())) {
            specFields = createSpecFields(jsonObject.get(GraphQueryKeys.GRAPH_QUERY_STRUCTURE.getFieldName()), allParameters);
        }
        PropertyFilter fieldFilter = null;
        if (jsonObject.containsKey(GraphQueryKeys.GRAPH_QUERY_FILTER.getFieldName())) {
            fieldFilter = createFieldFilter((Map) jsonObject.get(GraphQueryKeys.GRAPH_QUERY_FILTER.getFieldName()));
        }
        return new Specification(specFields, fieldFilter, rootType, responseVocab);
    }


    private List<SpecProperty> createSpecFields(Object origin, Map<String, String> allParameters) {
        List<SpecProperty> result = new ArrayList<>();
        if (origin instanceof List) {
            List originArray = (List) origin;
            for (int i = 0; i < originArray.size(); i++) {
                result.addAll(createSpecFields(originArray.get(i), allParameters));
            }
        } else if (origin instanceof Map) {
            Map originObj = (Map) origin;
            List<Object> allRelativePaths = null;
            if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_MERGE.getFieldName())) {
                allRelativePaths = getAllRelativePaths(originObj.get(GraphQueryKeys.GRAPH_QUERY_MERGE.getFieldName()));
            } else if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_PATH.getFieldName())) {
                allRelativePaths = Collections.singletonList(originObj.get(GraphQueryKeys.GRAPH_QUERY_PATH.getFieldName()));
            }
            if (allRelativePaths != null && !allRelativePaths.isEmpty()) {
                List<SpecProperty> fieldsPerRelativePath = new ArrayList<>();
                for (Object relativePath : allRelativePaths) {
                    if (relativePath != null) {
                        List<SpecTraverse> traversalPath = createTraversalPath(relativePath);
                        if (traversalPath != null && !traversalPath.isEmpty()) {
                            String fieldName = null;
                            List<SpecProperty> specFields = null;
                            boolean required = false;
                            boolean sortAlphabetically = false;
                            boolean groupBy = false;
                            boolean ensureOrder = false;
                            PropertyFilter fieldFilter = null;
                            SpecProperty.SingleItemStrategy singleItemStrategy = null;
                            String groupedInstances = GraphQueryKeys.GRAPH_QUERY_GROUPED_INSTANCES_DEFAULT.getFieldName();
                            if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_PROPERTY_NAME.getFieldName())) {
                                fieldName = (String) ((Map) originObj.get(GraphQueryKeys.GRAPH_QUERY_PROPERTY_NAME.getFieldName())).get(JsonLdConsts.ID);
                            }
                            if (fieldName == null) {
                                //Fall back to the name of the last traversal item if the fieldname is not defined.
                                fieldName = traversalPath.get(traversalPath.size() - 1).pathName;
                            }
                            if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_STRUCTURE.getFieldName())) {
                                specFields = createSpecFields(originObj.get(GraphQueryKeys.GRAPH_QUERY_STRUCTURE.getFieldName()), allParameters);
                            }
                            if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_REQUIRED.getFieldName())) {
                                required = (Boolean) originObj.get(GraphQueryKeys.GRAPH_QUERY_REQUIRED.getFieldName());
                            }
                            if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_SORT.getFieldName())) {
                                sortAlphabetically = (Boolean) originObj.get(GraphQueryKeys.GRAPH_QUERY_SORT.getFieldName());
                            }
                            if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_ENSURE_ORDER.getFieldName())) {
                                ensureOrder = (Boolean) originObj.get(GraphQueryKeys.GRAPH_QUERY_ENSURE_ORDER.getFieldName());
                            }
                            if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_GROUPED_INSTANCES.getFieldName())) {
                                groupedInstances = (String) ((Map) originObj.get(GraphQueryKeys.GRAPH_QUERY_GROUPED_INSTANCES.getFieldName())).get(JsonLdConsts.ID);
                            }
                            if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_GROUP_BY.getFieldName())) {
                                groupBy = (Boolean) originObj.get(GraphQueryKeys.GRAPH_QUERY_GROUP_BY.getFieldName());
                            }
                            if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_FILTER.getFieldName())) {
                                fieldFilter = createFieldFilter((Map) originObj.get(GraphQueryKeys.GRAPH_QUERY_FILTER.getFieldName()));
                            }
                            if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_SINGLE_VALUE.getFieldName())) {
                                singleItemStrategy = SpecProperty.SingleItemStrategy.valueOf((String) originObj.get(GraphQueryKeys.GRAPH_QUERY_SINGLE_VALUE.getFieldName()));
                            }

                            Map<String, Object> customDirectives = new TreeMap<>();
                            for (Object key : originObj.keySet()) {
                                if (key instanceof String && !GraphQueryKeys.isKey((String) key)) {
                                    customDirectives.put((String) key, originObj.get((String) key));
                                }
                            }
                            fieldsPerRelativePath.add(new SpecProperty(fieldName, specFields, traversalPath, groupedInstances, required, sortAlphabetically, groupBy, ensureOrder, fieldFilter, singleItemStrategy, customDirectives));
                        }
                    }
                }
                if (fieldsPerRelativePath.size() > 1) {
                    SpecProperty rootField = null;
                    for (int i = 0; i < fieldsPerRelativePath.size(); i++) {
                        SpecProperty specField = fieldsPerRelativePath.get(i);
                        if (rootField == null) {
                            rootField = new SpecProperty(specField.propertyName, fieldsPerRelativePath, Collections.emptyList(), specField.groupedInstances, specField.required, specField.sortAlphabetically, specField.groupBy, specField.ensureOrder, specField.propertyFilter, specField.singleItem);
                        }
                        specField.sortAlphabetically = false;
                        specField.groupBy = false;
                        specField.required = false;
                        specField.propertyName = String.format("%s_%d", specField.propertyName, i);
                    }
                    return Collections.singletonList(rootField);
                } else if (!fieldsPerRelativePath.isEmpty()) {
                    return Collections.singletonList(fieldsPerRelativePath.get(0));
                }

            }
        }
        return result;
    }

    private Object removeAtId(Object object) {
        if (object instanceof Map && ((Map) object).containsKey(JsonLdConsts.ID)) {
            return ((Map) object).get(JsonLdConsts.ID);
        }
        return object;

    }

    private boolean hasMultipleRelativePaths(List relativePath) {
        for (int i = 0; i < relativePath.size(); i++) {
            if (relativePath.get(i) instanceof List) {
                return true;
            }
        }
        return false;
    }


    private List<Object> getAllRelativePaths(Object merge) {
        if (merge instanceof List) {
            List mergeArray = (List) merge;
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < mergeArray.size(); i++) {
                if (mergeArray.get(i) instanceof Map) {
                    Map jsonObject = (Map) mergeArray.get(i);
                    if (jsonObject.containsKey(GraphQueryKeys.GRAPH_QUERY_PATH.getFieldName())) {
                        result.add(jsonObject.get(GraphQueryKeys.GRAPH_QUERY_PATH.getFieldName()));
                    }
                }
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }


    private List<SpecTraverse> createTraversalPath(Object relativePath) {
        List<SpecTraverse> result = new ArrayList<>();
        if (relativePath instanceof List) {
            List relativePathArray = (List) relativePath;
            for (int i = 0; i < relativePathArray.size(); i++) {
                Object relativePathElement = relativePathArray.get(i);
                if (relativePathElement != null) {
                    result.add(createSpecTraverse(relativePathElement));
                }
            }
        } else {
            result.add(createSpecTraverse(relativePath));
        }
        return result;
    }

    private SpecTraverse createSpecTraverse(Object relativePathElement) {
        String path = null;
        boolean reverse = false;
        List<Type> types = null;
        if (relativePathElement instanceof Map && ((Map) relativePathElement).containsKey(JsonLdConsts.ID)) {
            path = (String) ((Map) relativePathElement).get(JsonLdConsts.ID);
            if (((Map) relativePathElement).containsKey(GraphQueryKeys.GRAPH_QUERY_REVERSE.getFieldName())) {
                reverse = (Boolean) ((Map) relativePathElement).get(GraphQueryKeys.GRAPH_QUERY_REVERSE.getFieldName());
            }
            if (((Map) relativePathElement).containsKey(GraphQueryKeys.GRAPH_QUERY_TYPE_FILTER.getFieldName())) {
                Object typeFilter = ((Map) relativePathElement).get(GraphQueryKeys.GRAPH_QUERY_TYPE_FILTER.getFieldName());
                if (typeFilter instanceof Collection) {
                    types = ((Collection<?>) typeFilter).stream().filter(t -> t instanceof Map).map(t -> ((Map<?, ?>) t).get(JsonLdConsts.ID)).filter(id -> id instanceof String).map(id -> new Type((String) id)).collect(Collectors.toList());
                } else if (typeFilter instanceof Map) {
                    Object id = ((Map) typeFilter).get(JsonLdConsts.ID);
                    if (id instanceof String) {
                        types = Collections.singletonList(new Type((String) id));
                    }
                }
            }
        } else {
            path = relativePathElement.toString();
        }
        return new SpecTraverse(path, reverse, types);
    }


    public static PropertyFilter createFieldFilter(Map fieldFilter) {
        if (fieldFilter.containsKey(GraphQueryKeys.GRAPH_QUERY_FILTER_OP.getFieldName())) {
            String stringOp = (String) fieldFilter.get(GraphQueryKeys.GRAPH_QUERY_FILTER_OP.getFieldName());
            if (stringOp != null) {
                Op op = Op.valueOf(stringOp.toUpperCase());
                Value value = null;
                Parameter parameter = null;
                if (fieldFilter.containsKey(GraphQueryKeys.GRAPH_QUERY_FILTER_VALUE.getFieldName())) {
                    String stringValue = (String) fieldFilter.get(GraphQueryKeys.GRAPH_QUERY_FILTER_VALUE.getFieldName());
                    if (stringValue != null) {
                        value = new Value(stringValue);
                    }
                }
                if (fieldFilter.containsKey(GraphQueryKeys.GRAPH_QUERY_FILTER_PARAM.getFieldName())) {
                    String stringParameter = (String) fieldFilter.get(GraphQueryKeys.GRAPH_QUERY_FILTER_PARAM.getFieldName());
                    if (stringParameter != null) {
                        parameter = new Parameter(stringParameter);
                    }
                }
                return new PropertyFilter(op, value, parameter);
            }
        }
        return null;

    }
}

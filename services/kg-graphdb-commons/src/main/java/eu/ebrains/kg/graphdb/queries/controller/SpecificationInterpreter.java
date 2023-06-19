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

package eu.ebrains.kg.graphdb.queries.controller;

import eu.ebrains.kg.commons.exception.InvalidRequestException;
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

    public Specification readSpecification(NormalizedJsonLd jsonObject) {
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
                if (meta.containsKey(GraphQueryKeys.GRAPH_QUERY_RESPONSE_VOCAB.getFieldName())) {
                    responseVocab = meta.getAs(GraphQueryKeys.GRAPH_QUERY_RESPONSE_VOCAB.getFieldName(), String.class);
                }
            }
        }

        if (jsonObject.containsKey(GraphQueryKeys.GRAPH_QUERY_STRUCTURE.getFieldName())) {
            specFields = createSpecFields(jsonObject.get(GraphQueryKeys.GRAPH_QUERY_STRUCTURE.getFieldName()), true);
        }
        PropertyFilter fieldFilter = null;
        if (jsonObject.containsKey(GraphQueryKeys.GRAPH_QUERY_FILTER.getFieldName())) {
            fieldFilter = createFieldFilter((Map<?,?>) jsonObject.get(GraphQueryKeys.GRAPH_QUERY_FILTER.getFieldName()));
        }
        return new Specification(specFields, fieldFilter, rootType, responseVocab);
    }


    private List<SpecProperty> createSpecFields(Object origin, boolean rootLevel) {
        List<SpecProperty> result = new ArrayList<>();
        if (origin instanceof List) {
            List<?> originArray = (List<?>) origin;
            for (Object o : originArray) {
                result.addAll(createSpecFields(o, rootLevel));
            }
        } else if (origin instanceof Map) {
            Map<?,?> originObj = (Map<?,?>) origin;
            Object relativePath = null;
            if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_PATH.getFieldName())) {
                relativePath = originObj.get(GraphQueryKeys.GRAPH_QUERY_PATH.getFieldName());
            }
            if (relativePath != null) {
                SpecProperty fieldForRelativePath = null;
                List<SpecTraverse> traversalPath = createTraversalPath(relativePath);
                if (!traversalPath.isEmpty()) {
                    String fieldName = null;
                    List<SpecProperty> specFields = null;
                    boolean required = false;
                    boolean sort = false;
                    boolean groupBy = false;
                    boolean ensureOrder = false;
                    PropertyFilter fieldFilter = null;
                    SpecProperty.SingleItemStrategy singleItemStrategy = null;
                    String groupedInstances = GraphQueryKeys.GRAPH_QUERY_GROUPED_INSTANCES_DEFAULT.getFieldName();
                    if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_PROPERTY_NAME.getFieldName())) {
                        fieldName = (String) ((Map<?,?>) originObj.get(GraphQueryKeys.GRAPH_QUERY_PROPERTY_NAME.getFieldName())).get(JsonLdConsts.ID);
                    }
                    if (fieldName == null) {
                        //Fall back to the name of the last traversal item if the fieldname is not defined.
                        fieldName = traversalPath.get(traversalPath.size() - 1).pathName;
                    }
                    if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_STRUCTURE.getFieldName())) {
                        specFields = createSpecFields(originObj.get(GraphQueryKeys.GRAPH_QUERY_STRUCTURE.getFieldName()), false);
                    }
                    if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_REQUIRED.getFieldName())) {
                        required = (Boolean) originObj.get(GraphQueryKeys.GRAPH_QUERY_REQUIRED.getFieldName());
                    }
                    // Sorting is only allowed on the root level
                    if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_SORT.getFieldName())) {
                        if(!rootLevel){
                            throw new InvalidRequestException("Sorting is only allowed on the root level of a query. If you want to sort by a nested value, please flatten it.");
                        }
                        sort = (Boolean) originObj.get(GraphQueryKeys.GRAPH_QUERY_SORT.getFieldName());
                    }
                    if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_ENSURE_ORDER.getFieldName())) {
                        ensureOrder = (Boolean) originObj.get(GraphQueryKeys.GRAPH_QUERY_ENSURE_ORDER.getFieldName());
                    }
                    if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_GROUPED_INSTANCES.getFieldName())) {
                        groupedInstances = (String) ((Map<?,?>) originObj.get(GraphQueryKeys.GRAPH_QUERY_GROUPED_INSTANCES.getFieldName())).get(JsonLdConsts.ID);
                    }
                    if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_GROUP_BY.getFieldName())) {
                        groupBy = (Boolean) originObj.get(GraphQueryKeys.GRAPH_QUERY_GROUP_BY.getFieldName());
                    }
                    if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_FILTER.getFieldName())) {
                        fieldFilter = createFieldFilter((Map<?,?>) originObj.get(GraphQueryKeys.GRAPH_QUERY_FILTER.getFieldName()));
                    }
                    if (originObj.containsKey(GraphQueryKeys.GRAPH_QUERY_SINGLE_VALUE.getFieldName())) {
                        singleItemStrategy = SpecProperty.SingleItemStrategy.valueOf((String) originObj.get(GraphQueryKeys.GRAPH_QUERY_SINGLE_VALUE.getFieldName()));
                    }

                    Map<String, Object> customDirectives = new TreeMap<>();
                    for (Object key : originObj.keySet()) {
                        if (key instanceof String && !GraphQueryKeys.isKey((String) key)) {
                            customDirectives.put((String) key, originObj.get(key));
                        }
                    }
                    fieldForRelativePath = new SpecProperty(fieldName, specFields, traversalPath, groupedInstances, required, sort, groupBy, ensureOrder, fieldFilter, singleItemStrategy, customDirectives);
                }
                if (fieldForRelativePath != null) {
                    return Collections.singletonList(fieldForRelativePath);
                }
            }
        }
        return result;
    }

    private List<SpecTraverse> createTraversalPath(Object relativePath) {
        List<SpecTraverse> result = new ArrayList<>();
        if (relativePath instanceof List) {
            List<?> relativePathArray = (List<?>) relativePath;
            for (Object relativePathElement : relativePathArray) {
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
        String path;
        boolean reverse = false;
        List<Type> types = null;
        if (relativePathElement instanceof Map && ((Map<?,?>) relativePathElement).containsKey(JsonLdConsts.ID)) {
            path = (String) ((Map<?,?>) relativePathElement).get(JsonLdConsts.ID);
            if (((Map<?,?>) relativePathElement).containsKey(GraphQueryKeys.GRAPH_QUERY_REVERSE.getFieldName())) {
                reverse = (Boolean) ((Map<?,?>) relativePathElement).get(GraphQueryKeys.GRAPH_QUERY_REVERSE.getFieldName());
            }
            if (((Map<?,?>) relativePathElement).containsKey(GraphQueryKeys.GRAPH_QUERY_TYPE_FILTER.getFieldName())) {
                Object typeFilter = ((Map<?,?>) relativePathElement).get(GraphQueryKeys.GRAPH_QUERY_TYPE_FILTER.getFieldName());
                if (typeFilter instanceof Collection) {
                    types = ((Collection<?>) typeFilter).stream().filter(t -> t instanceof Map).map(t -> ((Map<?, ?>) t).get(JsonLdConsts.ID)).filter(id -> id instanceof String).map(id -> new Type((String) id)).collect(Collectors.toList());
                } else if (typeFilter instanceof Map) {
                    Object id = ((Map<?,?>) typeFilter).get(JsonLdConsts.ID);
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


    public static PropertyFilter createFieldFilter(Map<?,?> fieldFilter) {
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

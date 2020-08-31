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

package eu.ebrains.kg.graphdb.queries.model.spec;

import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;

public enum GraphQueryKeys {

    GRAPH_QUERY_META("meta"),
    GRAPH_QUERY_TYPE("type"),
    GRAPH_QUERY_NAME("name"),
    GRAPH_QUERY_TYPE_FILTER("typeFilter"),
    GRAPH_QUERY_SPECIFICATION ("specification"),
    GRAPH_QUERY_PROPERTY_NAME("propertyName"),
    GRAPH_QUERY_PATH("path"),
    GRAPH_QUERY_STRUCTURE("structure"),
    GRAPH_QUERY_REQUIRED("required"),
    GRAPH_QUERY_REVERSE("reverse"),
    GRAPH_QUERY_MERGE("merge"),
    GRAPH_QUERY_SORT("sort"),
    GRAPH_QUERY_GROUP_BY("group_by"),
    GRAPH_QUERY_ENSURE_ORDER("ensure_order"),
    GRAPH_QUERY_GROUPED_INSTANCES("grouped_instances"),
    GRAPH_QUERY_GROUPED_INSTANCES_DEFAULT("instances"),
    GRAPH_QUERY_ARANGO_REV("_rev"),
    GRAPH_QUERY_ARANGO_ID(ArangoVocabulary.ID),
    GRAPH_QUERY_ARANGO_KEY(ArangoVocabulary.KEY),
    GRAPH_QUERY_FILTER("filter"),
    GRAPH_QUERY_SINGLE_VALUE("singleValue"),
    GRAPH_QUERY_FILTER_OP("op"),
    GRAPH_QUERY_FILTER_VALUE("value"),
    GRAPH_QUERY_FILTER_PARAM("parameter");

    private final String fieldName;
    public static final String GRAPH_QUERY = EBRAINSVocabulary.NAMESPACE + "query/";

    GraphQueryKeys(String fieldName){
        this.fieldName = GRAPH_QUERY+fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public static boolean isKey(String key){
        for (GraphQueryKeys graphQueryKey : values()) {
            if(graphQueryKey.fieldName.equals(key)){
                return true;
            }
        }
        return false;

    }

}

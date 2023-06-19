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

package eu.ebrains.kg.graphdb.queries.model.spec;

import eu.ebrains.kg.arango.commons.aqlbuilder.ArangoVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;

public enum GraphQueryKeys {

    GRAPH_QUERY_META("meta"),
    GRAPH_QUERY_TYPE("type"),
    GRAPH_QUERY_RESPONSE_VOCAB("responseVocab"),
    GRAPH_QUERY_NAME("name"),
    GRAPH_QUERY_LABEL("label"),
    GRAPH_QUERY_DESCRIPTION("description"),
    GRAPH_QUERY_TYPE_FILTER("typeFilter"),
    GRAPH_QUERY_PROPERTY_NAME("propertyName"),
    GRAPH_QUERY_PATH("path"),
    GRAPH_QUERY_STRUCTURE("structure"),
    GRAPH_QUERY_REQUIRED("required"),
    GRAPH_QUERY_REVERSE("reverse"),
    GRAPH_QUERY_SORT("sort"),
    GRAPH_QUERY_GROUP_BY("groupBy"),
    GRAPH_QUERY_ENSURE_ORDER("ensureOrder"),
    GRAPH_QUERY_GROUPED_INSTANCES("groupedInstances"),
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

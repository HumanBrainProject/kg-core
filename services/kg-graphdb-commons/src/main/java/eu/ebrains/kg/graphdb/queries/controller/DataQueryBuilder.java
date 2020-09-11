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

package eu.ebrains.kg.graphdb.queries.controller;

import eu.ebrains.kg.arango.commons.aqlBuilder.*;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.graphdb.queries.model.fieldFilter.PropertyFilter;
import eu.ebrains.kg.graphdb.queries.model.spec.SpecProperty;
import eu.ebrains.kg.graphdb.queries.model.spec.SpecTraverse;
import eu.ebrains.kg.graphdb.queries.model.spec.Specification;

import java.util.*;
import java.util.stream.Collectors;

import static eu.ebrains.kg.arango.commons.aqlBuilder.AQL.*;

public class DataQueryBuilder {

    private final Specification specification;
    private final PaginationParam pagination;
    private final AQL q;
    private final Map<String, String> filterValues;
    private final Map<String, Object> bindVars = new HashMap<>();

    private final List<ArangoCollectionReference> existingCollections;
    private final Map<String, Object> whiteListFilter;

    public static ArangoAlias fromSpecField(SpecProperty specField) {
        return new ArangoAlias(specField.propertyName);
    }

    private static ArangoCollectionReference fromSpecTraversal(SpecTraverse traverse) {
        return new ArangoCollectionReference(new ArangoKey(traverse.pathName).getValue(), true);
    }

    public AQLQuery build() {
        //Define the global parameters
        ArangoAlias rootAlias = new ArangoAlias("root");
        q.setParameter("rootFieldName", rootAlias.getArangoName());

        //Setup the root instance
        defineRootInstance();

        if(whiteListFilter!=null) {
            q.addDocumentFilterWithWhitelistFilter(rootAlias.getArangoDocName());
        }

        q.addLine(trust("FILTER TO_ARRAY(@idRestriction) == [] OR ${rootFieldName}_doc._key IN TO_ARRAY(@idRestriction)"));

        q.add(new MergeBuilder(rootAlias, specification.getProperties()).getMergedFields());

        //Define the complex fields (the ones with traversals)
        q.add(new TraverseBuilder(rootAlias, specification.getProperties()).getTraversedProperty());

        //Define filters
        q.add(new FilterBuilder(rootAlias, specification.getDocumentFilter(), specification.getProperties()).getFilter());

        //Define sorting
        q.addLine(new SortBuilder(rootAlias, specification.getProperties()).getSort());

        //Pagination
        q.addPagination(pagination);

        //Define return value
        q.add(new ReturnBuilder(rootAlias, null, specification.getProperties()).getReturnStructure());

        return new AQLQuery(q, bindVars);
    }

    public DataQueryBuilder(Specification specification, PaginationParam pagination, Map<String, Object> whitelistFilter, Map<String, String> filterValues, List<ArangoCollectionReference> existingCollections) {
        this.q = new AQL();
        this.specification = specification;
        this.pagination = pagination;
        this.filterValues = filterValues == null ? Collections.emptyMap() : new HashMap<>(filterValues);
        this.existingCollections = existingCollections;
        this.whiteListFilter = whitelistFilter;
    }

    public void defineRootInstance() {
        if(whiteListFilter!=null) {
            this.q.specifyWhitelist();
            this.bindVars.putAll(whiteListFilter);
        }
        this.q.addLine(trust("FOR ${rootFieldName}_doc IN 1..1 OUTBOUND DOCUMENT(@@typeCollection, @typeId) @@typeRelation"));
        ArangoCollectionReference collectionReference = ArangoCollectionReference.fromSpace(InternalSpace.TYPE_SPACE);
        this.bindVars.put("@typeCollection", collectionReference.getCollectionName());
        this.bindVars.put("@typeRelation", InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName());
        this.bindVars.put("typeId", collectionReference.docWithStableId(this.specification.getRootType().getName()).getDocumentId().toString());
        this.q.addLine(trust(""));
    }

    static TrustedAqlValue getRepresentationOfField(ArangoAlias alias, SpecProperty field) {
        AQL representation = new AQL();
        if (field.isDirectChild()) {
            return representation.add(trust("${parentAlias}.`${originalKey}`")).setTrustedParameter("parentAlias", alias.getArangoDocName()).setParameter("originalKey", field.getLeafPath().pathName).build();
        } else if (field.hasGrouping()) {
            return representation.add(preventAqlInjection(fromSpecField(field).getArangoName() + "_grp")).build();
        } else {
            return representation.add(preventAqlInjection(fromSpecField(field).getArangoName())).build();
        }
    }


    private class MergeBuilder {

        private final List<SpecProperty> fields;
        private final ArangoAlias parentAlias;

        public MergeBuilder(ArangoAlias parentAlias, List<SpecProperty> fields) {
            this.fields = fields;
            this.parentAlias = parentAlias;
        }

        private List<SpecProperty> fieldsWithMerge() {
            return fields.stream().filter(SpecProperty::isMerge).collect(Collectors.toList());
        }


        List<SpecProperty> getSortFieldsForMerge(SpecProperty mergeField) {
            List<SpecProperty> specFields = new ArrayList<>();
            if (mergeField.property != null && !mergeField.property.isEmpty()) {
                for (SpecProperty field : mergeField.property) {
                    if (field.isSortAlphabetically()) {
                        specFields.add(field);
                    }
                    specFields.addAll(field.getSubPropertiesWithSort());
                }
            }
            return specFields;
        }


        TrustedAqlValue getMergedFields() {
            List<SpecProperty> mergeFields = fieldsWithMerge();
            if (!mergeFields.isEmpty()) {
                AQL aql = new AQL();
                for (SpecProperty mergeField : mergeFields) {
                    aql.add(new TraverseBuilder(parentAlias, mergeField.property).getTraversedProperty());
                    AQL merge = new AQL();
                    merge.add(trust("LET ${alias} = "));

                    List<SpecProperty> sortFieldsForMerge = getSortFieldsForMerge(mergeField);
                    if (mergeField.property.size() == 1) {
                        merge.add(trust("${alias}_0"));
                    } else {
                        if (!sortFieldsForMerge.isEmpty()) {
                            merge.add(trust("(FOR ${alias}_sort IN "));
                        }

                        for (int i = 0; i < mergeField.property.size(); i++) {
                            AQL append = new AQL();
                            if (i < mergeField.property.size() - 1) {
                                append.add(trust("(APPEND(${field},"));
                            } else {
                                append.add(trust("${field}"));
                            }
                            append.setParameter("field", fromSpecField(mergeField).getArangoName() + "_" + i);
                            merge.add(append.build());
                        }
                        for (SpecProperty field : mergeField.property) {
                            if (field != mergeField.property.get(mergeField.property.size() - 1)) {
                                merge.add(trust(", true))"));
                            }
                        }
                        if (!sortFieldsForMerge.isEmpty()) {
                            merge.addLine(trust(""));
                            Set<String> duplicateTracker = new HashSet<>();
                            for (SpecProperty specField : sortFieldsForMerge) {
                                if (!duplicateTracker.contains(specField.propertyName)) {
                                    duplicateTracker.add(specField.propertyName);
                                    AQL sort = new AQL();
                                    sort.addLine(trust("SORT ${alias}_sort.`${targetField}` ASC"));
                                    sort.setParameter("alias", fromSpecField(mergeField).getArangoName());
                                    sort.setParameter("targetField", specField.propertyName);
                                    merge.add(sort.build());
                                }
                            }
                            merge.addLine(trust("RETURN ${alias}_sort"));
                            merge.add(trust(")"));
                        }
                    }
                    merge.setParameter("alias", fromSpecField(mergeField).getArangoName());
                    aql.addLine(merge.build());
                }
                return aql.build();
            }

            return null;
        }


    }


    private class TraverseBuilder {

        private final List<SpecProperty> fields;
        private final ArangoAlias parentAlias;


        public TraverseBuilder(ArangoAlias parentAlias, List<SpecProperty> fields) {
            this.fields = fields;
            this.parentAlias = parentAlias;
        }

        private List<SpecProperty> propertiesWithTraversal() {
            return fields.stream().filter(SpecProperty::needsTraversal).filter(f -> !f.isMerge()).collect(Collectors.toList());
        }

        TrustedAqlValue handleTraverse(boolean hasSort, SpecProperty.SingleItemStrategy singleItemStrategy, SpecTraverse traverse, ArangoAlias alias, Stack<ArangoAlias> aliasStack, boolean ensureOrder) {
            AQL aql = new AQL();
            aql.add(trust("LET ${alias} = "));
            if(singleItemStrategy!=null){
                switch (singleItemStrategy){
                    case FIRST:
                        aql.add(trust("FIRST("));
                        break;
                    case CONCAT:
                        aql.add(trust("CONCAT_SEPARATOR(\", \", "));
                        break;
                }
            }
            if (aliasStack.size() == 1 && hasSort) {
                aql.add(trust("(FOR ${alias}_sort IN "));
            }
            aql.add(trust(ensureOrder ? "(" : "UNIQUE("));
            aql.add(trust("FLATTEN("));
            aql.indent().add(trust("FOR ${aliasDoc} "));
            if (ensureOrder) {
                aql.add(trust(", ${aliasDoc}_e"));
            }
            if(traverseExists(traverse)) {
                aql.addLine(trust(" IN 1..1 ${direction} ${parentAliasDoc} `${edgeCollection}`"));
            }
            else{
                //TODO if the collection doesn't exist, the query could be simplified a lot - so there is quite some potential for optimization.
                aql.addLine(trust(" IN [] "));
            }
            if(whiteListFilter!=null) {
                aql.indent().addDocumentFilterWithWhitelistFilter(alias.getArangoDocName());
            }
            if(traverse.typeRestrictions!=null){
                aql.addLine(trust(" FILTER "));
                for (int i = 0; i < traverse.typeRestrictions.size(); i++) {
                    aql.add(trust(" \"${aliasDoc_typefilter_"+i+"}\" IN ${aliasDoc}.`@type`"));
                    if(i<traverse.typeRestrictions.size()-1){
                        aql.add(trust(" OR "));
                    }
                    aql.setParameter("aliasDoc_typefilter_"+i, traverse.typeRestrictions.get(i).getName());
                }
            }
            if (ensureOrder) {
                aql.addLine(trust("SORT ${aliasDoc}_e." + ArangoVocabulary.ORDER_NUMBER + " ASC"));
            }
            aql.setTrustedParameter("alias", alias);
            aql.setTrustedParameter("aliasDoc", alias.getArangoDocName());
            aql.setParameter("direction", traverse.reverse ? "INBOUND" : "OUTBOUND");
            aql.setTrustedParameter("parentAliasDoc", aliasStack.peek().getArangoDocName());
            aql.setParameter("edgeCollection", fromSpecTraversal(traverse).getCollectionName());
            aql.setParameter("fieldPath", traverse.pathName);
            return aql.build();
        }


        TrustedAqlValue finalizeTraversalWithSubfields(SpecProperty field, SpecTraverse lastTraverse, Stack<ArangoAlias> aliasStack) {
            AQL aql = new AQL();
            aql.addLine(new TraverseBuilder(aliasStack.peek(), field.property).getTraversedProperty());
            return aql.build();
        }

        boolean traverseExists(SpecTraverse traverse) {
            return existingCollections.contains(fromSpecTraversal(traverse));
        }


        TrustedAqlValue getTraversedProperty() {
            List<SpecProperty> traversalProperties = propertiesWithTraversal();
            if (!traversalProperties.isEmpty()) {
                AQL properties = new AQL();
                for (SpecProperty traversalProperty : traversalProperties) {
                    ArangoAlias traversalFieldAlias = fromSpecField(traversalProperty);
                    ArangoAlias alias = traversalFieldAlias;
                    Stack<ArangoAlias> aliasStack = new Stack<>();
                    aliasStack.push(parentAlias);
                    List<SpecProperty> subFieldsWithSort = traversalProperty.getSubPropertiesWithSort();
                    SpecProperty.SingleItemStrategy singleItemStrategy = traversalProperty.singleItem;
                    for (SpecTraverse traverse : traversalProperty.path) {
                        boolean lastTraversal;
                        if (!traversalProperty.hasSubProperties()) {
                            lastTraversal = traversalProperty.path.size() < 2 || traverse == traversalProperty.path.get(traversalProperty.path.size() - 2);
                        } else {
                            lastTraversal = traverse == traversalProperty.path.get(traversalProperty.path.size() - 1);
                        }
                        properties.addLine(handleTraverse(traversalProperty.isSortAlphabetically() || !subFieldsWithSort.isEmpty(), singleItemStrategy, traverse, alias, aliasStack, traversalProperty.ensureOrder));
                        singleItemStrategy = null;
                        aliasStack.push(alias);
                        if (lastTraversal) {
                            if (traversalProperty.hasSubProperties()) {
                                properties.add(finalizeTraversalWithSubfields(traversalProperty, traverse, aliasStack));
                            }
                        }
                        if (lastTraversal) {
                            break;
                        }
                        alias = alias.increment();
                    }

                    properties.add(new FilterBuilder(alias, traversalProperty.propertyFilter, traversalProperty.property).getFilter());
                    //fields.add(new SortBuilder(alias, traversalField.fields).getSort());
                    properties.addLine(new ReturnBuilder(alias, traversalProperty, traversalProperty.property).getReturnStructure());
                    while (aliasStack.size() > 1) {
                        ArangoAlias a = aliasStack.pop();
                        properties.addLine(trust("))"));

                        if (aliasStack.size() > 1) {
                            AQL returnStructure = new AQL();
                            returnStructure.addLine(trust("RETURN DISTINCT ${traverseField}"));
                            returnStructure.setParameter("traverseField", a.getArangoName());
                            properties.addLine(returnStructure.build());
                        } else if (aliasStack.size() == 1) {
                            if (traversalProperty.isSortAlphabetically() || !subFieldsWithSort.isEmpty()) {
                                AQL sortStructure = new AQL();
                                if (traversalProperty.isSortAlphabetically()) {
                                    sortStructure.indent().addLine(trust("SORT ${traverseField}_sort ASC"));
                                }
                                for (SpecProperty specField : subFieldsWithSort) {
                                    if (specField.isSortAlphabetically()) {
                                        AQL subFieldSort = new AQL();
                                        subFieldSort.addLine(trust("SORT ${traverseField}_sort.`${fieldName}` ASC"));
                                        subFieldSort.setParameter("fieldName", specField.propertyName);
                                        sortStructure.addLine(subFieldSort.build());
                                    }
                                }
                                sortStructure.addLine(trust("RETURN ${traverseField}_sort")).outdent();
                                sortStructure.addLine(trust(")"));

                                sortStructure.setParameter("traverseField", a.getArangoName());
                                properties.addLine(sortStructure.build());
                            }
                            if(traversalProperty.singleItem != null){
                                properties.addLine(trust(")"));
                            }
                        }
                    }
                    if (traversalProperty.hasGrouping()) {
                        handleGrouping(properties, traversalProperty, traversalFieldAlias);
                    }

                }
                return properties.build();
            } else {
                //return new ReturnBuilder(parentAlias, fields).getReturnStructure();
                return null;
            }
        }

        private void handleGrouping(AQL fields, SpecProperty traversalField, ArangoAlias traversalFieldAlias) {
            AQL group = new AQL();
            group.addLine(trust("LET ${traverseField}_grp = (FOR ${traverseField}_grp_inst IN ${traverseField}"));
            group.indent().add(trust("COLLECT "));

            List<SpecProperty> groupByFields = traversalField.property.stream().filter(SpecProperty::isGroupBy).collect(Collectors.toList());
            for (SpecProperty groupByField : groupByFields) {
                AQL groupField = new AQL();
                groupField.add(trust("`${field}` = ${traverseField}_grp_inst.`${field}`"));
                if (groupByField != groupByFields.get(groupByFields.size() - 1)) {
                    groupField.addComma();
                }
                groupField.setParameter("field", groupByField.propertyName);
                group.addLine(groupField.build());
            }
            group.addLine(trust("INTO ${traverseField}_group"));
            group.addLine(trust("LET ${traverseField}_instances = ( FOR ${traverseField}_group_el IN ${traverseField}_group"));
            List<SpecProperty> notGroupedByFields = traversalField.property.stream().filter(f -> !f.isGroupBy()).collect(Collectors.toList());

            sortNotGroupedFields(group, notGroupedByFields);

            group.addLine(trust("RETURN {"));

            for (SpecProperty notGroupedByField : notGroupedByFields) {
                AQL notGroupedField = new AQL();
                notGroupedField.add(trust("\"${field}\": ${traverseField}_group_el.${traverseField}_grp_inst.`${field}`"));
                if (notGroupedByField != notGroupedByFields.get(notGroupedByFields.size() - 1)) {
                    notGroupedField.addComma();
                }
                notGroupedField.setParameter("field", notGroupedByField.propertyName);
                group.addLine(notGroupedField.build());
            }

            group.addLine(trust("})"));

            sortGroupedFields(group, groupByFields);

            group.addLine(trust("RETURN {"));

            for (SpecProperty groupByField : groupByFields) {
                AQL groupField = new AQL();
                groupField.add(trust("\"${field}\": `${field}`"));
                groupField.addComma();
                groupField.setParameter("field", groupByField.propertyName);
                group.addLine(groupField.build());
            }
            group.addLine(trust("\"${collectField}\": ${traverseField}_instances"));
            group.addLine(trust("})"));
            group.setParameter("traverseField", traversalFieldAlias.getArangoName());
            group.setParameter("collectField", traversalField.groupedInstances);
            fields.addLine(group.build());
        }

        private void sortGroupedFields(AQL group, List<SpecProperty> groupByFields) {
            List<SpecProperty> groupedSortFields = groupByFields.stream().filter(f -> f.isSortAlphabetically()).collect(Collectors.toList());
            if (!groupedSortFields.isEmpty()) {
                group.add(trust("SORT "));
                for (SpecProperty specField : groupedSortFields) {
                    AQL groupSort = new AQL();
                    groupSort.add(trust("`${field}`"));
                    if (specField != groupedSortFields.get(groupedSortFields.size() - 1)) {
                        groupSort.addComma();
                    }
                    groupSort.setParameter("field", specField.propertyName);
                    group.add(groupSort.build());
                }
                group.add(trust(" ASC"));
            }
        }

        private void sortNotGroupedFields(AQL group, List<SpecProperty> notGroupedByFields) {
            List<SpecProperty> notGroupedSortFields = notGroupedByFields.stream().filter(SpecProperty::isSortAlphabetically).collect(Collectors.toList());
            if (!notGroupedSortFields.isEmpty()) {
                group.add(trust("SORT "));
                for (SpecProperty notGroupedSortField : notGroupedSortFields) {
                    AQL notGroupedSort = new AQL();
                    notGroupedSort.add(trust("${traverseField}_group_el.${traverseField}_grp_inst.`${field}`"));
                    if (notGroupedSortField != notGroupedSortFields.get(notGroupedSortFields.size() - 1)) {
                        notGroupedSort.addComma();
                    }
                    notGroupedSort.setParameter("field", notGroupedSortField.propertyName);
                    group.add(notGroupedSort.build());
                }
                group.addLine(trust(" ASC"));
            }
        }

    }


    private static class ReturnBuilder {

        private final List<SpecProperty> properties;
        private final ArangoAlias parentAlias;
        private final SpecProperty parentProperty;

        public ReturnBuilder(ArangoAlias parentAlias, SpecProperty parentProperty, List<SpecProperty> properties) {
            this.properties = properties;
            this.parentAlias = parentAlias;
            this.parentProperty = parentProperty;
        }

        TrustedAqlValue getReturnStructure() {
            AQL aql = new AQL();

            if (this.properties == null || this.properties.isEmpty()) {
                if (this.parentProperty != null) {
                    aql.addLine(trust("FILTER ${parentAliasDoc}.`${field}` != NULL"));
                }
            } else if (this.parentProperty != null) {
                aql.add(trust("FILTER "));
                for (SpecProperty field : properties) {
                    AQL fieldResult = new AQL();
                    fieldResult.add(trust("(${fieldRepresentation} != NULL AND ${fieldRepresentation} != [])"));
                    if (field != properties.get(properties.size() - 1)) {
                        fieldResult.add(trust(" OR "));
                    }
                    fieldResult.setTrustedParameter("fieldRepresentation", getRepresentationOfField(parentAlias, field));
                    aql.addLine(fieldResult.build());
                }
            }

            aql.addLine(trust("RETURN "));
            if (parentProperty != null) {
                aql.add(trust("DISTINCT "));
            }

            if (this.properties == null || this.properties.isEmpty()) {
                if (this.parentProperty != null) {
                    aql.add(trust("${parentAliasDoc}.`${field}`"));
                    aql.setTrustedParameter("parentAliasDoc", parentAlias.getArangoDocName());
                    aql.setParameter("field", parentProperty.getLeafPath().pathName);
                }
            } else {
                aql.indent();
                aql.add(trust("{"));
                for (SpecProperty field : properties) {
                    AQL fieldResult = new AQL();
                    fieldResult.add(new TrustedAqlValue("\"${fieldName}\": ${fieldRepresentation}"));
                    fieldResult.setParameter("fieldName", field.propertyName);
                    fieldResult.setTrustedParameter("fieldRepresentation", getRepresentationOfField(parentAlias, field));
                    if (field != properties.get(properties.size() - 1)) {
                        fieldResult.addComma();
                    }
                    aql.addLine(fieldResult.build());
                }
                aql.outdent();
                aql.addLine(trust("}"));
            }
            return aql.build();
        }


    }


    private static class SortBuilder {

        private final List<SpecProperty> fields;
        private final ArangoAlias parentAlias;

        public SortBuilder(ArangoAlias parentAlias, List<SpecProperty> fields) {
            this.fields = fields;
            this.parentAlias = parentAlias;
        }


        private List<SpecProperty> fieldsWithSort() {
            return fields.stream().filter(SpecProperty::isSortAlphabetically).collect(Collectors.toList());
        }

        TrustedAqlValue getSort() {
            List<SpecProperty> sortFields = fieldsWithSort();
            if (!sortFields.isEmpty()) {
                AQL aql = new AQL();
                aql.add(trust("SORT "));
                for (SpecProperty sortField : sortFields) {
                    AQL sort = new AQL();
                    if (sortField != sortFields.get(0)) {
                        sort.addComma();
                    }
                    sort.add(trust("${field}"));
                    sort.setTrustedParameter("field", getRepresentationOfField(parentAlias, sortField));
                    aql.add(sort.build());
                }
                aql.addLine(trust(" ASC"));
                return aql.build();
            }
            return null;
        }


    }


    private class FilterBuilder {

        private final List<SpecProperty> fields;
        private final ArangoAlias alias;
        private final PropertyFilter parentFilter;

        public FilterBuilder(ArangoAlias alias, PropertyFilter parentFilter, List<SpecProperty> fields) {
            this.fields = fields;
            this.alias = alias;
            this.parentFilter = parentFilter;
        }


        private List<SpecProperty> fieldsWithFilter() {
            return fields.stream().filter(f -> f.isRequired() || (f.propertyFilter != null && f.propertyFilter.getOp() != null && !f.propertyFilter.getOp().isInstanceFilter())).collect(Collectors.toList());
        }

        TrustedAqlValue getFilter() {
            AQL filter = new AQL();
            filter.addDocumentFilter(alias.getArangoDocName());
            if (parentFilter != null && parentFilter.getOp() != null && parentFilter.getOp().isInstanceFilter()) {
                filter.addLine(createInstanceFilter(parentFilter));
            }
            List<SpecProperty> fieldsWithFilter = fieldsWithFilter();
            if (!fieldsWithFilter.isEmpty()) {
                for (SpecProperty specField : fieldsWithFilter) {
                    filter.addLine(createFilter(specField));
                }
            }
            return filter.build();
        }

        private TrustedAqlValue createInstanceFilter(PropertyFilter filter) {
            AQL aql = new AQL();
            if (filter != null && filter.getOp().isInstanceFilter()) {
                TrustedAqlValue fieldFilter = createFieldFilter(filter);
                if (fieldFilter != null) {
                    aql.addLine(trust("AND ${fieldFilter}"));
                    aql.setTrustedParameter("fieldFilter", fieldFilter);
                    aql.setTrustedParameter("document", alias.getArangoDocName());
                }
            }
            return aql.build();
        }


        private TrustedAqlValue createFilter(SpecProperty field) {
            AQL aql = new AQL();
            if (field.isRequired()) {
                aql.addLine(trust("AND ${field} !=null"));
                aql.addLine(trust("AND ${field} !=\"\""));
                aql.addLine(trust("AND ${field} !=[]"));
            }
            if (field.propertyFilter != null && field.propertyFilter.getOp() != null && !field.propertyFilter.getOp().isInstanceFilter()) {
                TrustedAqlValue fieldFilter = createFieldFilter(field.propertyFilter);
                if (fieldFilter != null) {
                    aql.addLine(trust("AND LOWER(${field})${fieldFilter}"));
                    aql.setTrustedParameter("fieldFilter", fieldFilter);
                }
            }
            aql.setTrustedParameter("field", getRepresentationOfField(alias, field));
            return aql.build();
        }


        private TrustedAqlValue createAqlForFilter(PropertyFilter fieldFilter, boolean prefixWildcard, boolean postfixWildcard) {
            String value = null;
            String key = null;
            if (fieldFilter.getParameter() != null) {
                key = fieldFilter.getParameter().getName();
            } else {
                key = "staticFilter" + DataQueryBuilder.this.bindVars.size();
            }
            if (DataQueryBuilder.this.filterValues.containsKey(key)) {
                Object fromMap = DataQueryBuilder.this.filterValues.get(key);
                value = fromMap != null ? fromMap.toString() : null;
            }
            if (value == null && fieldFilter.getValue() != null) {
                value = fieldFilter.getValue().getValue();
            }
            if (value != null && key != null) {
                if (prefixWildcard && !value.startsWith("%")) {
                    value = "%" + value;
                }
                if (postfixWildcard && !value.endsWith("%")) {
                    value = value + "%";
                }
                DataQueryBuilder.this.bindVars.put(key, value);
                AQL aql = new AQL();
                if (fieldFilter.getOp().isInstanceFilter()) {
                    aql.add(trust("@${field}"));
                } else {
                    aql.add(trust("LOWER(@${field})"));
                }
                aql.setParameter("field", key);
                return aql.build();
            }
            return null;
        }


        private TrustedAqlValue createFieldFilter(PropertyFilter fieldFilter) {
            AQL aql = new AQL();
            TrustedAqlValue value;
            switch (fieldFilter.getOp()) {
                case REGEX:
                case EQUALS:
                case TYPE:
                case MBB:
                case ID:
                    value = createAqlForFilter(fieldFilter, false, false);
                    break;
                case STARTS_WITH:
                    value = createAqlForFilter(fieldFilter, false, true);
                    break;
                case ENDS_WITH:
                    value = createAqlForFilter(fieldFilter, true, false);
                    break;
                case CONTAINS:
                    value = createAqlForFilter(fieldFilter, true, true);
                    break;
                default:
                    value = null;
            }
            if (value != null) {
                switch (fieldFilter.getOp()) {
                    case EQUALS:
                        aql.add(trust(" == " + value.getValue()));
                        break;
                    case STARTS_WITH:
                    case ENDS_WITH:
                    case CONTAINS:
                        aql.add(trust(" LIKE " + value.getValue()));
                        break;
                    case REGEX:
                        aql.add(trust(" =~ " + value.getValue()));
                        break;
                    case MBB:
                    case ID:
                        aql.add(trust("${document}._id IN " + value.getValue() + " "));
                        break;
                    case TYPE:
                        aql.add(trust(value.getValue() + " IN ${document}._type"));
                }
                return aql.build();
            }
            return null;
        }
    }

}

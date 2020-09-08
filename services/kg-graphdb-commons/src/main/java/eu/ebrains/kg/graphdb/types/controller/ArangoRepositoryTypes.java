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

package eu.ebrains.kg.graphdb.types.controller;

import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.InferredJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.controller.ArangoUtils;
import eu.ebrains.kg.graphdb.ingestion.controller.structure.StaticStructureController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ArangoRepositoryTypes {

    private final ArangoDatabases databases;
    private final ArangoRepositoryCommons arangoRepositoryCommons;
    private final ArangoUtils arangoUtils;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ArangoRepositoryTypes(ArangoDatabases databases, ArangoRepositoryCommons arangoRepositoryCommons, ArangoUtils arangoUtils) {
        this.databases = databases;
        this.arangoRepositoryCommons = arangoRepositoryCommons;
        this.arangoUtils = arangoUtils;
    }


    public static List<Type> extractExtendedTypeInformationFromPayload(Collection<NormalizedJsonLd> payload) {
        return payload.stream().map(t -> {
            //TODO this can probably be solved in a more optimized way - we don't need all properties but only the labels...
            Type targetType = new Type(t.getPrimaryIdentifier());
            targetType.setLabelProperty(t.getAs(EBRAINSVocabulary.META_TYPE_LABEL_PROPERTY, String.class));
            return targetType;
        }).collect(Collectors.toList());
    }


    public Paginated<NormalizedJsonLd> getTargetTypesForProperty(String client, DataStage stage, TargetsForProperties targetTypesForProperty, boolean withProperties, boolean withCount, PaginationParam pagination) {
        ArangoDatabase db = databases.getMetaByStage(stage);
        AQLQuery typeStructureQuery = createTypeStructureQuery(db, client, null, targetTypesForProperty, null, withProperties, withCount, pagination);
        return arangoRepositoryCommons.queryDocuments(db, typeStructureQuery);
    }

    public Paginated<NormalizedJsonLd> getAllTypes(String client, DataStage stage, boolean withProperties, boolean withCount, PaginationParam pagination) {
        ArangoDatabase db = databases.getMetaByStage(stage);
        AQLQuery typeStructureQuery = createTypeStructureQuery(db, client, null, null, null, withProperties, withCount, pagination);
        return arangoRepositoryCommons.queryDocuments(db, typeStructureQuery);
    }

    public Paginated<NormalizedJsonLd> getTypesForSpace(String client, DataStage stage, Space space, boolean withProperties, boolean withCount, PaginationParam pagination) {
        ArangoDatabase db = databases.getMetaByStage(stage);
        AQLQuery typeStructureQuery = createTypeStructureQuery(db, client, null, null, space, withProperties, withCount, pagination);
        return arangoRepositoryCommons.queryDocuments(db, typeStructureQuery);
    }

    public List<Type> getTypeInformation(String client, DataStage stage, Collection<Type> types){
        return getTypes(client, stage, types, false, false).stream().map(Type::fromPayload).collect(Collectors.toList());
    }

    public List<NormalizedJsonLd> getTypes(String client, DataStage stage, Collection<Type> types, boolean withProperties, boolean withCount) {
        ArangoDatabase db = databases.getMetaByStage(stage);
        AQLQuery typeStructureQuery = createTypeStructureQuery(db, client, types, null, null, withProperties, withCount, null);
        return db.query(typeStructureQuery.getAql().build().getValue(), typeStructureQuery.getBindVars(), new AqlQueryOptions(), NormalizedJsonLd.class).asListRemaining();

    }

    public List<NormalizedJsonLd> getTypesForSpace(String client, DataStage stage, Space space, List<Type> types, boolean withProperties, boolean withCount) {
        ArangoDatabase db = databases.getMetaByStage(stage);
        AQLQuery typeStructureQuery = createTypeStructureQuery(db, client, types, null, space, withProperties, withCount, null);
        return db.query(typeStructureQuery.getAql().build().getValue(), typeStructureQuery.getBindVars(), new AqlQueryOptions(), NormalizedJsonLd.class).asListRemaining();
    }

    private void ensureTypeStructureCollections(ArangoDatabase db, boolean withProperties) {
        List<ArangoCollectionReference> collections = new ArrayList<>(Arrays.asList(ArangoCollectionReference.fromSpace(InternalSpace.GLOBAL_SPEC),
                InternalSpace.SPACE_TO_TYPE_EDGE_COLLECTION, ArangoCollectionReference.fromSpace(InternalSpace.TYPES_SPACE),
                ArangoCollectionReference.fromSpace(InternalSpace.SPACES_SPACE),
                ArangoCollectionReference.fromSpace(new Space(EBRAINSVocabulary.META_TYPE), true),
                ArangoCollectionReference.fromSpace(new Space(EBRAINSVocabulary.META_PROPERTY), true),
                InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION,
                InternalSpace.TYPE_TO_PROPERTY_EDGE_COLLECTION, InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION,
                InternalSpace.PROPERTY_TO_PROPERTY_VALUE_TYPE_EDGE_COLLECTION,
                InternalSpace.CLIENT_TYPE_PROPERTY_EDGE_COLLECTION));
        if (withProperties) {
            collections.add(InternalSpace.GLOBAL_TYPE_TO_PROPERTY_EDGE_COLLECTION);
        }
        collections.forEach(c -> {
            arangoUtils.getOrCreateArangoCollection(db, c);
        });
    }

    public static class TargetsForProperties {
        private final String propertyName;
        private final List<String> originalTypes;

        public TargetsForProperties(String propertyName, List<String> originalTypes) {
            this.propertyName = propertyName;
            this.originalTypes = originalTypes;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public List<String> getOriginalTypes() {
            return originalTypes;
        }
    }

    private AQLQuery createTypeStructureQuery(ArangoDatabase db, String client, Collection<Type> types, TargetsForProperties targetTypesForProperty, Space space, boolean withProperties, boolean withCount, PaginationParam paginationParam) {
        ensureTypeStructureCollections(db, withProperties);
        Map<String, Object> bindVars = new HashMap<>();
        AQL aql = new AQL();
        aql.addComment("We first define the black list for properties we don't want to be part of the result ");
        aql.addLine(AQL.trust("LET propertiesToRemove = @propertiesToRemove"));
        aql.addLine(AQL.trust("LET propertiesToRemoveForOverrides = UNION(propertiesToRemove, @propertiesToRemoveForOverrides)"));

        List<String> propertiesToRemove = new ArrayList<>(IndexedJsonLdDoc.INTERNAL_FIELDS);
        propertiesToRemove.add(JsonLdConsts.ID);
        propertiesToRemove.add(ArangoVocabulary.ID);
        propertiesToRemove.add(ArangoVocabulary.KEY);
        propertiesToRemove.add(EBRAINSVocabulary.META_TYPE);
        propertiesToRemove.add(EBRAINSVocabulary.META_PROPERTY);
        propertiesToRemove.add(EBRAINSVocabulary.META_SPACE);
        propertiesToRemove.add(EBRAINSVocabulary.META_REVISION);
        propertiesToRemove.add(EBRAINSVocabulary.META_USER);
        propertiesToRemove.add(EBRAINSVocabulary.META_ALTERNATIVE);
        propertiesToRemove.add(EBRAINSVocabulary.META_PROPERTYUPDATES);
        propertiesToRemove.add(IndexedJsonLdDoc.IDENTIFIERS);
        propertiesToRemove.add(InferredJsonLdDoc.INFERENCE_OF);
        bindVars.put("propertiesToRemove", propertiesToRemove);
        bindVars.put("propertiesToRemoveForOverrides", Arrays.asList(JsonLdConsts.TYPE, SchemaOrgVocabulary.IDENTIFIER));
        bindVars.put("clientName", ArangoCollectionReference.fromSpace(new Client(client).getSpace()).getCollectionName());
        bindVars.put("globalSpace", ArangoCollectionReference.fromSpace(InternalSpace.GLOBAL_SPEC).getCollectionName());
        if (targetTypesForProperty != null && !targetTypesForProperty.getPropertyName().isBlank()) {
            aql.addComment("We're interested in the target types of a property. We query the list of types matching.");
            aql.addLine(AQL.trust("LET rootProperty = DOCUMENT(@propertyId)"));
            ArangoDocumentReference propRef = StaticStructureController.createDocumentRefForMetaRepresentation(targetTypesForProperty.getPropertyName(), ArangoCollectionReference.fromSpace(InternalSpace.PROPERTIES_SPACE));
            bindVars.put("propertyId", propRef.getId());
            aql.addLine(AQL.trust("FOR originalType, originalTypeToRootProperty IN 1..1 INBOUND rootProperty " + InternalSpace.TYPE_TO_PROPERTY_EDGE_COLLECTION.getCollectionName()));
            aql.indent().addLine(AQL.trust("FILTER DOCUMENT(originalType._to).`" + SchemaOrgVocabulary.IDENTIFIER + "` IN @originalTypeFilter"));
            bindVars.put("originalTypeFilter", targetTypesForProperty.getOriginalTypes());
            aql.addLine(AQL.trust("FOR targetTypeForRootProperty IN 1..1 OUTBOUND originalTypeToRootProperty " + InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION.getCollectionName()));
            aql.indent().addLine(AQL.trust("LET type = DOCUMENT(targetTypeForRootProperty._to)"));
        } else if (types == null || types.isEmpty()) {
            if (space != null) {
                bindVars.put("space", space.getName());
                aql.addComment("We're fetching the space based on the restriction and lookup the types which are existing for this space.");
                aql.addLine(AQL.trust("LET sp = FIRST(FOR s IN @@spaces FILTER s.`" + SchemaOrgVocabulary.IDENTIFIER + "`== @space RETURN s)"));
                aql.addLine(AQL.trust("FOR type IN (FOR t IN 1..1 OUTBOUND sp @@spaceToType RETURN t)"));
                bindVars.put("@spaceToType", InternalSpace.SPACE_TO_TYPE_EDGE_COLLECTION.getCollectionName());
                bindVars.put("@spaces", ArangoCollectionReference.fromSpace(InternalSpace.SPACES_SPACE).getCollectionName());
            } else {
                aql.addComment("We're fetching all available types.");
                aql.addLine(AQL.trust("FOR type IN (FOR t IN @@types RETURN t)"));
                bindVars.put("@types", ArangoCollectionReference.fromSpace(InternalSpace.TYPES_SPACE).getCollectionName());
            }
        } else {
            aql.addComment("We're fetching all available types which are matching our filter criteria.");
            aql.addLine(AQL.trust("FOR type IN @@typesSpace"));
            aql.addLine(AQL.trust("FILTER TO_ARRAY(type.`" + SchemaOrgVocabulary.IDENTIFIER + "`) ANY IN @types"));
            bindVars.put("types", types.stream().map(Type::getName).collect(Collectors.toList()));
            bindVars.put("@typesSpace", ArangoCollectionReference.fromSpace(InternalSpace.TYPES_SPACE).getCollectionName());
        }

        aql.addNewline();
        aql.addComment("First, we fetch the specification for the type - this can either be defined by client or globally. The results will be appended to the result document at the very end.");
        aql.addLine(AQL.trust("LET clientSpecific = ("));
        aql.addLine(AQL.trust("FOR g IN 1..1 INBOUND type @@typeDefinition"));
        aql.indent().addLine(AQL.trust("FILTER IS_SAME_COLLECTION(@clientName, g._id)"));
        aql.addLine(AQL.trust("RETURN UNSET(g, propertiesToRemoveForOverrides)"));
        aql.outdent().addLine(AQL.trust(")"));
        bindVars.put("@typeDefinition", ArangoCollectionReference.fromSpace(new Space(EBRAINSVocabulary.META_TYPE)).getCollectionName());
        aql.addNewline();
        aql.addLine(AQL.trust("LET globalTypeDef = ("));
        aql.addLine(AQL.trust("FOR g IN 1..1 INBOUND type @@typeDefinition"));
        aql.indent().addLine(AQL.trust("FILTER IS_SAME_COLLECTION(@globalSpace, g._id)"));
        aql.addLine(AQL.trust("RETURN UNSET(g, propertiesToRemoveForOverrides)"));
        aql.outdent().addLine(AQL.trust(")"));

        if (withProperties) {
            aql.addComment("Now, we're investigating the edge which contextualizes the type in a specific space (since we keep the information on this granularity). This means, we first query the numbers for every space-type combination and aggregate them later for the global view.");
            aql.addLine(AQL.trust("LET spaces = [{\"" + EBRAINSVocabulary.META_SPACES + "\": (FOR space, space2type IN 1..1 INBOUND type @@spaceToType"));
            bindVars.put("@spaceToType", InternalSpace.SPACE_TO_TYPE_EDGE_COLLECTION.getCollectionName());
            bindVars.put("space", space == null ? null : space.getName());
            aql.addLine(AQL.trust("FILTER @space == null OR space.`" + SchemaOrgVocabulary.IDENTIFIER + "`==@space"));
            aql.addNewline();
            aql.indent();
            aql.addComment("We want to know about the properties existing for this type in this space...");
            aql.addLine(AQL.trust("LET properties = (FOR spaceType2property, spaceType2propertyEdge IN 1..1 OUTBOUND space2type @@typeToProperty"));
            bindVars.put("@typeToProperty", InternalSpace.TYPE_TO_PROPERTY_EDGE_COLLECTION.getCollectionName());
            if (withCount) {
                aql.addComment("To be able to count the occurrences of the property, we need to find the contributing documents");
                aql.addLine(AQL.trust("LET docsContributingToSpace2Property = FIRST(FOR docContributingToSpace2Property IN 1..1 INBOUND spaceType2propertyEdge @@documentRelation"));
                aql.addLine(AQL.trust("COLLECT WITH COUNT INTO numberOfContributingDocs"));
                aql.addLine(AQL.trust("RETURN numberOfContributingDocs)"));

                aql.addNewline();
                aql.indent();
                aql.addComment("We're also interested in the target types of the property in this space-type combination...");
                aql.addLine(AQL.trust("LET targets = (FOR target, targetEdge IN 1..1 OUTBOUND spaceType2propertyEdge @@property2type"));
                bindVars.put("@property2type", InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION.getCollectionName());
                aql.addNewline();
                aql.indent();
                aql.addComment("And of course, we also count those target occurrences...");
                aql.addLine(AQL.trust(" LET countTargetOccurrences = FIRST(FOR targetRel IN 1..1 INBOUND targetEdge @@documentRelation "));
                aql.addLine(AQL.trust(" COLLECT WITH COUNT INTO occurrenceOfTarget"));
                aql.addLine(AQL.trust(" RETURN occurrenceOfTarget)"));
                aql.addLine(AQL.trust(" LET targetSpace = DOCUMENT(target._from)"));
                aql.addLine(AQL.trust(" LET targetType = DOCUMENT(target._to)"));
                aql.addLine(AQL.trust(" RETURN {"));
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_TYPE + "\": targetType.`" + SchemaOrgVocabulary.IDENTIFIER + "`,"));
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_SPACE + "\": targetSpace.`" + SchemaOrgVocabulary.IDENTIFIER + "`,"));
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": countTargetOccurrences"));
                aql.addLine(AQL.trust("  })"));
                aql.outdent();
                aql.addNewline();
                aql.outdent();

                aql.addComment("Now we collect the found targets and return them");
                aql.addLine(AQL.trust("LET targetsByType = (FOR targetT IN targets"));
                aql.addLine(AQL.trust(" COLLECT targetType = targetT.`" + EBRAINSVocabulary.META_TYPE + "` INTO tByType"));
                aql.addLine(AQL.trust("  RETURN {"));
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_TYPE + "\": targetType,"));
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_SPACES + "\": tByType[*].targetT"));
                aql.addLine(AQL.trust("  })"));
                aql.addNewline();
                aql.outdent();
            }
            bindVars.put("@metaProperty", ArangoCollectionReference.fromSpace(new Space(EBRAINSVocabulary.META_PROPERTY)).getCollectionName());
            bindVars.put("@clientTypeProperty", InternalSpace.CLIENT_TYPE_PROPERTY_EDGE_COLLECTION.getCollectionName());

            aql.addComment("Now, we need to figure out the property specification");
            aql.addLine(AQL.trust("LET globalPropertySpec = NOT_NULL(FIRST(")).indent();
            aql.addLine(AQL.trust("FOR g IN 1..1 INBOUND spaceType2property @@metaProperty"));
            aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION(@globalSpace, g._id)"));
            aql.addLine(AQL.trust("RETURN UNSET(UNSET(g, propertiesToRemoveForOverrides), propertiesToRemove)"));
            aql.outdent().addLine(AQL.trust("), {})"));

            if (withCount) {
                aql.addComment("Finally, we count the target occurrences and sum them for a total number of all target occurrences.");
                aql.addLine(AQL.trust("LET targetsByTypeWithCount = (FOR targetByTypeWithCount IN targetsByType"));
                aql.addLine(AQL.trust("LET countOccurrences = SUM(targetByTypeWithCount.`" + EBRAINSVocabulary.META_SPACES + "`[*].`" + EBRAINSVocabulary.META_OCCURRENCES + "`)"));
                aql.addLine(AQL.trust("RETURN MERGE({\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": countOccurrences}, targetByTypeWithCount))"));
                aql.addNewline();
                aql.outdent();
            }

            aql.addComment("Last but not least, we now combine all the found information about the property in the space-type context and return it as an object.");
            aql.addLine(AQL.trust("  RETURN MERGE({"));
            aql.addLine(AQL.trust("\"" + SchemaOrgVocabulary.IDENTIFIER + "\": spaceType2property.`" + SchemaOrgVocabulary.IDENTIFIER + "`"));
            if (withCount) {
                aql.addLine(AQL.trust(", \"" + EBRAINSVocabulary.META_OCCURRENCES + "\": docsContributingToSpace2Property,"));
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES + "\": targetsByTypeWithCount"));
            }
            aql.addLine(AQL.trust("  }, globalPropertySpec)"));
            aql.outdent();
            aql.addLine(AQL.trust(")"));
            aql.addNewline();
        }
        if (withCount) {
            aql.addComment("We now are combining the type in space information (such as occurrences)");
            aql.addLine(AQL.trust("LET occurrenceBySpace = FIRST(FOR rel IN @@documentRelation"));
            aql.addLine(AQL.trust("    FILTER rel._to==space2type._id"));
            aql.addLine(AQL.trust("    COLLECT WITH COUNT INTO occurrenceBySpaceCount"));
            aql.addLine(AQL.trust("    RETURN occurrenceBySpaceCount)"));
            bindVars.put("@documentRelation", InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION.getCollectionName());
        }
        if(withProperties){
            aql.addLine(AQL.trust("RETURN {"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_SPACE + "\": space.`" + SchemaOrgVocabulary.NAME + "`,"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_PROPERTIES + "\": properties"));
            if(withCount) {
                aql.addLine(AQL.trust(", \"" + EBRAINSVocabulary.META_OCCURRENCES + "\": occurrenceBySpace"));
            }
            aql.addLine(AQL.trust("})"));
            aql.addLine(AQL.trust("}]"));
        }
        if (withCount) {
            aql.addComment("And we sum all the space specific information to generate the global occurrence number.");
            aql.addLine(AQL.trust("LET typeOccurrences = SUM(spaces[0].`" + EBRAINSVocabulary.META_SPACES + "`[**].`" + EBRAINSVocabulary.META_OCCURRENCES + "`)"));
            aql.addLine(AQL.trust("FILTER typeOccurrences > 0"));
            aql.addLine(AQL.trust("LET globalOccurrences = [{\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": typeOccurrences}]"));
        }
        if (withProperties) {
            aql.addNewline();
            aql.addComment("Since we also want to have a global view on the properties, we now collect the global information. For this, we first iterate across all already fetched spaces and extract their properties");
            aql.addLine(AQL.trust("LET globalProperties = [{\"" + EBRAINSVocabulary.META_PROPERTIES + "\": (FOR props IN spaces[0].`" + EBRAINSVocabulary.META_SPACES + "`[**].`" + EBRAINSVocabulary.META_PROPERTIES + "`[**]"));
            aql.addLine(AQL.trust("COLLECT globalPropertyName = props.`" + SchemaOrgVocabulary.IDENTIFIER + "` INTO groupedGlobalProperties"));

            aql.addComment("We still need to query some global information for the property (such as specifications), so we have to get a reference to the property object");
            aql.addLine(AQL.trust("LET property = FIRST(FOR p IN @@properties FILTER p.`" + SchemaOrgVocabulary.IDENTIFIER + "` == globalPropertyName RETURN p)"));
            bindVars.put("@properties", ArangoCollectionReference.fromSpace(InternalSpace.PROPERTIES_SPACE).getCollectionName());

            aql.addComment("... and we collect the specifications on the global (space-independent) level which can either be client&type specific, client-only specific, type-only specific or global");
            aql.indent();

            bindVars.put("@globalType", InternalSpace.GLOBAL_TYPE_TO_PROPERTY_EDGE_COLLECTION.getCollectionName());
            aql.addLine(AQL.trust("LET globalTypeProperties = (FOR g, gRel IN 1..1 INBOUND property @@globalType"));
            aql.addLine(AQL.trust("FILTER g.`" + SchemaOrgVocabulary.IDENTIFIER + "`==type.`" + SchemaOrgVocabulary.IDENTIFIER + "`"));
            aql.addLine(AQL.trust("RETURN gRel)"));

            aql.addComment("First, we check for the property specifications which are defined for this client AND the type");
            aql.addLine(AQL.trust("LET clientSpecificTypePropertySpec = NOT_NULL(FIRST(FOR typeProperty IN globalTypeProperties"));
            aql.addLine(AQL.trust("RETURN NOT_NULL(FIRST(FOR clientTypeProperty IN 1..1 INBOUND typeProperty @@clientTypeProperty"));
            aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION(@clientName, clientTypeProperty._id)"));
            aql.addLine(AQL.trust("RETURN UNSET(clientTypeProperty, propertiesToRemoveForOverrides)), {})), {})"));

            aql.addComment("Second, we go for the specification defined for the current client independent of the type");
            aql.addLine(AQL.trust("LET clientSpecificGlobalPropertySpec = NOT_NULL(FIRST(FOR g IN 1..1 INBOUND property @@metaProperty"));
            aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION(@clientName, g._id)"));
            aql.addLine(AQL.trust("RETURN UNSET(g, propertiesToRemoveForOverrides)), {})"));

            aql.addComment("Third, we look up the specification declared for this type - regardless of the requesting client");
            aql.addLine(AQL.trust("LET globalTypePropertySpec = NOT_NULL(FIRST(FOR typeProperty IN globalTypeProperties"));
            aql.addLine(AQL.trust("RETURN NOT_NULL(FIRST(FOR clientTypeProperty IN 1..1 INBOUND typeProperty @@clientTypeProperty"));
            aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION(@globalSpace, clientTypeProperty._id)"));
            aql.addLine(AQL.trust("RETURN UNSET(clientTypeProperty, propertiesToRemoveForOverrides)), {})), {})"));

            aql.addComment("Fourth, we query the global and non-type-specific specifications");
            aql.addLine(AQL.trust("LET globalPropertySpec = NOT_NULL(FIRST(FOR g IN 1..1 INBOUND property @@metaProperty"));
            aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION(@globalSpace, g._id)"));
            aql.addLine(AQL.trust("RETURN UNSET(g, propertiesToRemoveForOverrides)), {})"));

            aql.outdent();
            aql.addNewline();
            aql.addComment("Now we're aggregating the information from the spaces queries above.");
            aql.addLine(AQL.trust("LET targetTypes = (FOR globalPropertyBySpace IN groupedGlobalProperties[*].props[**].`" + EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES + "`[**].`" + EBRAINSVocabulary.META_SPACES + "`[**]"));
            aql.addLine(AQL.trust("COLLECT globalType = globalPropertyBySpace.`" + EBRAINSVocabulary.META_TYPE + "` INTO globalTypeBySpace"));
            if (withCount) {
                aql.addLine(AQL.trust("LET globalTypeCount = SUM(globalTypeBySpace[*].globalPropertyBySpace[**].`" + EBRAINSVocabulary.META_OCCURRENCES + "`[**])"));
            }
            aql.addLine(AQL.trust("RETURN {"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_TYPE + "\": globalType,"));
            if (withCount) {
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": globalTypeCount,"));
            }
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_SPACES + "\": globalTypeBySpace[*].globalPropertyBySpace"));
            aql.addLine(AQL.trust("})"));

            aql.addLine(AQL.trust("RETURN MERGE({"));
            aql.addLine(AQL.trust("\"" + SchemaOrgVocabulary.IDENTIFIER + "\": globalPropertyName,"));
            if (withCount) {
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": SUM(groupedGlobalProperties[*].props[**].`" + EBRAINSVocabulary.META_OCCURRENCES + "`),"));
            }
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES + "\": targetTypes"));
            aql.addLine(AQL.trust("}, globalPropertySpec, globalTypePropertySpec, clientSpecificGlobalPropertySpec, clientSpecificTypePropertySpec)"));
            aql.addLine(AQL.trust(")}]"));
        }

        if (types == null || types.isEmpty()) {
            aql.addPagination(paginationParam);
        }
        aql.addLine(AQL.trust("RETURN DISTINCT MERGE(UNION([UNSET(type, propertiesToRemove)], globalTypeDef, clientSpecific"));
        if (withCount) {
            aql.add(AQL.trust(", globalOccurrences"));
        }
        if (withProperties) {
            aql.add(AQL.trust(", globalProperties, spaces"));
        }
        aql.add(AQL.trust("))"));
        logger.info(aql.buildSimpleDebugQuery(bindVars));

        return new AQLQuery(aql, bindVars);
    }

}

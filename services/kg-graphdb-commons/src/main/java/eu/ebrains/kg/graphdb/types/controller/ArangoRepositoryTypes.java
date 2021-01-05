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
            Type targetType = new Type(t.primaryIdentifier());
            targetType.setLabelProperty(t.getAs(EBRAINSVocabulary.META_TYPE_LABEL_PROPERTY, String.class));
            return targetType;
        }).collect(Collectors.toList());
    }


    public List<NormalizedJsonLd> getTargetTypesForProperty(String client, DataStage stage, List<Type> types, String propertyName) {
        ArangoDatabase db = databases.getMetaByStage(stage);
        AQLQuery typeStructureQuery = createTypeStructureQuery(db, client, types, propertyName, null, true, false, true, null);
        Paginated<NormalizedJsonLd> documents = arangoRepositoryCommons.queryDocuments(db, typeStructureQuery);
        List<Type> targetTypes = documents.getData().stream().map(t -> t.getAsListOf(EBRAINSVocabulary.META_PROPERTIES, NormalizedJsonLd.class)).flatMap(Collection::stream).map(p -> p.getAsListOf(EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES, NormalizedJsonLd.class)).flatMap(Collection::stream).map(targetType -> targetType.getAs(EBRAINSVocabulary.META_TYPE, String.class)).distinct().map(Type::new).collect(Collectors.toList());
        if (targetTypes.isEmpty()) {
            //This is important since an empty target type list would result in an non-existing filter and would return all types in the next step
            return Collections.emptyList();
        }
        return getTypes(client, stage, targetTypes, false, false, false);
    }

    public Paginated<NormalizedJsonLd> getAllTypes(String client, DataStage stage, boolean withProperties, boolean withIncomingLinks, boolean withCount, PaginationParam pagination) {
        ArangoDatabase db = databases.getMetaByStage(stage);
        AQLQuery typeStructureQuery = createTypeStructureQuery(db, client, null, null, null, withProperties, withIncomingLinks,  withCount, pagination);
        return arangoRepositoryCommons.queryDocuments(db, typeStructureQuery);
    }

    public Paginated<NormalizedJsonLd> getTypesForSpace(String client, DataStage stage, SpaceName space, boolean withProperties, boolean withIncomingLinks, boolean withCount, PaginationParam pagination) {
        ArangoDatabase db = databases.getMetaByStage(stage);
        AQLQuery typeStructureQuery = createTypeStructureQuery(db, client, null, null, space, withProperties, withIncomingLinks, withCount, pagination);
        return arangoRepositoryCommons.queryDocuments(db, typeStructureQuery);
    }

    public List<Type> getTypeInformation(String client, DataStage stage, Collection<Type> types) {
        return getTypes(client, stage, types, false, false, false).stream().map(Type::fromPayload).collect(Collectors.toList());
    }

    public List<NormalizedJsonLd> getTypes(String client, DataStage stage, Collection<Type> types, boolean withProperties, boolean withIncomingLinks, boolean withCount) {
        ArangoDatabase db = databases.getMetaByStage(stage);
        AQLQuery typeStructureQuery = createTypeStructureQuery(db, client, types, null, null, withProperties, withIncomingLinks, withCount, null);
        return db.query(typeStructureQuery.getAql().build().getValue(), typeStructureQuery.getBindVars(), new AqlQueryOptions(), NormalizedJsonLd.class).asListRemaining();

    }

    public List<NormalizedJsonLd> getTypesForSpace(String client, DataStage stage, SpaceName space, List<Type> types, boolean withProperties, boolean withIncomingLinks, boolean withCount) {
        ArangoDatabase db = databases.getMetaByStage(stage);
        AQLQuery typeStructureQuery = createTypeStructureQuery(db, client, types, null, space, withProperties, withIncomingLinks, withCount, null);
        return db.query(typeStructureQuery.getAql().build().getValue(), typeStructureQuery.getBindVars(), new AqlQueryOptions(), NormalizedJsonLd.class).asListRemaining();
    }

    private void ensureTypeStructureCollections(ArangoDatabase db, boolean withProperties) {
        List<ArangoCollectionReference> collections = new ArrayList<>(Arrays.asList(ArangoCollectionReference.fromSpace(InternalSpace.GLOBAL_SPEC),
                InternalSpace.SPACE_TO_TYPE_EDGE_COLLECTION, ArangoCollectionReference.fromSpace(InternalSpace.TYPES_SPACE),
                ArangoCollectionReference.fromSpace(InternalSpace.SPACES_SPACE),
                ArangoCollectionReference.fromSpace(new SpaceName(EBRAINSVocabulary.META_TYPE), true),
                ArangoCollectionReference.fromSpace(new SpaceName(EBRAINSVocabulary.META_PROPERTY), true),
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

    private AQLQuery createTypeStructureQuery(ArangoDatabase db, String client, Collection<Type> types, String propertyName, SpaceName space, boolean withProperties, boolean withIncomingLinks, boolean withCount, PaginationParam paginationParam) {
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
        bindVars.put("propertiesToRemoveForOverrides", Arrays.asList(JsonLdConsts.TYPE, SchemaOrgVocabulary.IDENTIFIER, EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES));
        bindVars.put("clientName", ArangoCollectionReference.fromSpace(new Client(client).getSpace().getName()).getCollectionName());
        if (types == null || types.isEmpty()) {
            if (space != null) {
                bindVars.put("space", space.getName());
                aql.addComment("We're fetching the space based on the restriction and lookup the types which are existing for this space.");
                aql.addLine(AQL.trust("LET sp = FIRST(FOR s IN `" + ArangoCollectionReference.fromSpace(InternalSpace.SPACES_SPACE).getCollectionName() + "` FILTER s.`" + SchemaOrgVocabulary.IDENTIFIER + "`== @space RETURN s)"));
                aql.addLine(AQL.trust("FOR type IN (FOR t IN 1..1 OUTBOUND sp `" + InternalSpace.SPACE_TO_TYPE_EDGE_COLLECTION.getCollectionName() + "` RETURN t)"));
            } else {
                aql.addComment("We're fetching all available types.");
                aql.addLine(AQL.trust("FOR type IN (FOR t IN `" + ArangoCollectionReference.fromSpace(InternalSpace.TYPES_SPACE).getCollectionName() + "` RETURN t)"));
            }
        } else {
            aql.addComment("We're fetching all available types which are matching our filter criteria.");
            aql.addLine(AQL.trust("FOR type IN `" + ArangoCollectionReference.fromSpace(InternalSpace.TYPES_SPACE).getCollectionName() + "`"));
            aql.addLine(AQL.trust("FILTER TO_ARRAY(type.`" + SchemaOrgVocabulary.IDENTIFIER + "`) ANY IN @types"));
            bindVars.put("types", types.stream().map(Type::getName).collect(Collectors.toList()));
        }

        if (withIncomingLinks) {
            aql.addComment("For incoming links, we have to handle the following combinations for property2type:");
            aql.addComment("1. _from: properties, _to: types");
            aql.addComment("2. _from: globaltype2property, _to: types");
            aql.addComment("3. _from: type2property, _to: space2type");
            aql.addComment("");
            aql.addComment("First we take care of the \"contract first\" incoming links -> they have by default a count of 0 (otherwise they will appear in the space specific incoming links");
            aql.addLine(AQL.trust("LET globalIncomingLinks = (FOR p IN 1..1 INBOUND type `" + InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION.getCollectionName() + "` RETURN p)"));
            aql.addLine(AQL.trust("LET typeSpecificIncomingLinks = FLATTEN(UNIQUE(FOR p IN globalIncomingLinks"));
            aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION(\"" + InternalSpace.GLOBAL_TYPE_TO_PROPERTY_EDGE_COLLECTION.getCollectionName() + "\", p._id)"));
            aql.addComment("//Let's expand on all spaces the type is registered for");
            aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION(\"" + InternalSpace.GLOBAL_TYPE_TO_PROPERTY_EDGE_COLLECTION.getCollectionName() + "\", p._id)"));
            aql.addLine(AQL.trust("RETURN (FOR space IN 1..1 INBOUND p._from `" + InternalSpace.SPACE_TO_TYPE_EDGE_COLLECTION.getCollectionName() + "` RETURN {\"type\": DOCUMENT(p._from).`" + SchemaOrgVocabulary.IDENTIFIER + "`, \"space\": space.`" + SchemaOrgVocabulary.IDENTIFIER + "`, \"property\": DOCUMENT(p._to).`" + SchemaOrgVocabulary.IDENTIFIER + "`})))\n"));
            aql.addLine(AQL.trust("LET propertySpecificIncomingLinks = (FOR p IN globalIncomingLinks"));
//TODO global properties themselves are meaningless without a type, so we have to investigate which types are providing this property. -> We have to walk through the type2property to figure out all potential types and probably through space2type for finding all potential spaces too
//
//                    FILTER IS_SAME_COLLECTION("properties", p._id)
//
//                    LET globalTypes = FLATTEN(FOR t IN 1..1 INBOUND p `globaltype2property`
//                                        RETURN (FOR s IN 1..1 INBOUND t  `space2type` RETURN {"type": t.`http://schema.org/identifier`, "space": s.`http://schema.org/identifier`, "property": p.`http://schema.org/identifier`}))
//                    LET spaceTypes = FLATTEN(FOR space2type IN 1..1 INBOUND p `type2property` RETURN {"type": DOCUMENT(space2type._to).`http://schema.org/identifier`, "space": DOCUMENT(space2type_from).`http://schema.org/identifier`, "property": p.`http://schema.org/identifier`})
//
//                    RETURN UNION_DISTINCT(spaceTypes, globalTypes)
            aql.addLine(AQL.trust("RETURN []"));
            aql.addLine(AQL.trust(")"));
            aql.addLine(AQL.trust("LET allIncomingLinks = FLATTEN(FOR space, space2type IN 1..1 INBOUND type `" + InternalSpace.SPACE_TO_TYPE_EDGE_COLLECTION.getCollectionName() + "`"));
            aql.addComment("Here's the links created by data and therefore the only relevant one for the count.");
            aql.addLine(AQL.trust("LET incomingLinks = (FOR type2property IN 1..1 INBOUND space2type `" + InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION.getCollectionName() + "`"));
            aql.addLine(AQL.trust("LET originalSpaceType = DOCUMENT(type2property._from)"));
            aql.addLine(AQL.trust("RETURN {\"type\": DOCUMENT(originalSpaceType._to).`" + SchemaOrgVocabulary.IDENTIFIER + "`, \"space\":  DOCUMENT(originalSpaceType._from).`" + SchemaOrgVocabulary.IDENTIFIER + "`, \"property\": DOCUMENT(type2property._to).`" + SchemaOrgVocabulary.IDENTIFIER + "`}\n"));
            aql.addLine(AQL.trust(")"));

            aql.addLine(AQL.trust("LET allLinks = UNION_DISTINCT(incomingLinks, typeSpecificIncomingLinks, propertySpecificIncomingLinks)"));
            aql.addLine(AQL.trust("FILTER allLinks != []"));
            aql.addLine(AQL.trust("RETURN allLinks"));
            aql.addLine(AQL.trust(")"));

            aql.addLine(AQL.trust("LET incomingLinks = [{\""+EBRAINSVocabulary.META_INCOMING_LINKS+"\": ("));
            aql.addLine(AQL.trust("FOR l in allIncomingLinks"));
            aql.addLine(AQL.trust("COLLECT property = l.property INTO linksByProperties"));
            aql.addLine(AQL.trust("LET sourceTypes = (FOR i in linksByProperties"));
            aql.addLine(AQL.trust("COLLECT t = i.l.type INTO spaces"));
            aql.addLine(AQL.trust("RETURN {"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_TYPE + "\": t,"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_SPACES + "\": (FOR s IN spaces RETURN {\"" + EBRAINSVocabulary.META_SPACE + "\": s.i.l.space})"));
            aql.addLine(AQL.trust("})"));
            aql.addLine(AQL.trust("FILTER property!=NULL"));

            aql.addLine(AQL.trust("RETURN {"));
            aql.addLine(AQL.trust("\"" + SchemaOrgVocabulary.IDENTIFIER + "\": property,"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_PROPERTY_SOURCE_TYPES + "\": sourceTypes"));
            aql.addLine(AQL.trust("})}]"));
        }


        aql.addNewline();
        aql.addComment("First, we fetch the specification for the type - this can either be defined by client or globally. The results will be appended to the result document at the very end.");
        aql.addLine(AQL.trust("LET clientSpecific = ("));
        aql.addLine(AQL.trust("FOR g IN 1..1 INBOUND type `" + ArangoCollectionReference.fromSpace(new SpaceName(EBRAINSVocabulary.META_TYPE)).getCollectionName() + "`"));
        aql.indent().addLine(AQL.trust("FILTER IS_SAME_COLLECTION(@clientName, g._id)"));
        aql.addLine(AQL.trust("RETURN UNSET(g, propertiesToRemoveForOverrides)"));
        aql.outdent().addLine(AQL.trust(")"));
        aql.addNewline();
        aql.addLine(AQL.trust("LET globalTypeDef = ("));
        aql.addLine(AQL.trust("FOR g IN 1..1 INBOUND type `" + ArangoCollectionReference.fromSpace(new SpaceName(EBRAINSVocabulary.META_TYPE)).getCollectionName() + "`"));
        aql.indent().addLine(AQL.trust("FILTER IS_SAME_COLLECTION(\"" + ArangoCollectionReference.fromSpace(InternalSpace.GLOBAL_SPEC).getCollectionName() + "\", g._id)"));
        aql.addLine(AQL.trust("RETURN UNSET(g, propertiesToRemoveForOverrides)"));
        aql.outdent().addLine(AQL.trust(")"));
        aql.addNewline();
        if (withProperties) {
            aql.addComment("We're looking for the properties involved - there are some global sources, we can already fetch before knowing the space...");
            aql.addLine(AQL.trust("LET globalPropertyDefinitionsByType = (FOR prop IN 1..1 OUTBOUND type `" + InternalSpace.GLOBAL_TYPE_TO_PROPERTY_EDGE_COLLECTION.getCollectionName() + "`"));
            aql.addLine(AQL.trust("FILTER prop.`" + SchemaOrgVocabulary.IDENTIFIER + "` != null"));
            if (propertyName != null) {
                aql.addLine(AQL.trust("FILTER prop.`" + SchemaOrgVocabulary.IDENTIFIER + "` == @propertyName"));
                bindVars.put("propertyName", propertyName);
            }
            aql.addLine(AQL.trust("RETURN prop)"));
            aql.addNewline();
            aql.addLine(AQL.trust("LET typeSpecificProperties = MERGE(FOR globalProp, globaltype2prop IN 1..1 OUTBOUND type `" + InternalSpace.GLOBAL_TYPE_TO_PROPERTY_EDGE_COLLECTION.getCollectionName() + "`"));
            aql.addLine(AQL.trust("FILTER globalProp.`" + SchemaOrgVocabulary.IDENTIFIER + "` != null"));
            if (propertyName != null) {
                aql.addLine(AQL.trust("FILTER globalProp.`" + SchemaOrgVocabulary.IDENTIFIER + "` == @propertyName"));
            }
            aql.addLine(AQL.trust("LET targetTypes = (FOR targetType IN 1..1 OUTBOUND globaltype2prop `" + InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION.getCollectionName() + "`"));
            aql.addLine(AQL.trust("RETURN DISTINCT targetType.`" + SchemaOrgVocabulary.IDENTIFIER + "`)"));
            aql.addLine(AQL.trust("FILTER targetTypes != []"));
            aql.addLine(AQL.trust("RETURN {[ globalProp.`" + SchemaOrgVocabulary.IDENTIFIER + "` ] : {"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES + "\": targetTypes"));
            aql.addLine(AQL.trust("}"));
            aql.addLine(AQL.trust("})"));
        }

        aql.addComment("Now, we're investigating the edge which contextualizes the type in a specific space (since we keep the information on this granularity). This means, we first query the numbers for every space-type combination and aggregate them later for the global view.");
        aql.addLine(AQL.trust("LET spaces = (FOR space, space2type IN 1..1 INBOUND type `" + InternalSpace.SPACE_TO_TYPE_EDGE_COLLECTION.getCollectionName() + "`"));
        aql.addLine(AQL.trust("FILTER @space == NULL OR space.`http://schema.org/identifier` == @space"));
        bindVars.put("space", space == null ? null : space.getName());
        aql.addNewline();
        if (!withProperties) {
            //If we don't ask for properties, we only want the space information back
            aql.addLine(AQL.trust(" RETURN {"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_SPACE + "\": space.`" + SchemaOrgVocabulary.IDENTIFIER + "`"));
            aql.addLine(AQL.trust("  })"));
        }
        if (withProperties) {
            aql.indent();
            aql.addComment("Let's investigate on where we're getting the property information from. Since they can either be reflected or defined by contract-first, there's multiple souces involved");

            aql.addLine(AQL.trust("LET allProperties = UNION_DISTINCT((FOR p IN 1..1 OUTBOUND space2type `" + InternalSpace.TYPE_TO_PROPERTY_EDGE_COLLECTION.getCollectionName() + "`"));
            if (propertyName != null) {
                aql.addLine(AQL.trust("FILTER p.`" + SchemaOrgVocabulary.IDENTIFIER + "` == @propertyName"));
            } else {
                aql.addLine(AQL.trust("FILTER p.`" + SchemaOrgVocabulary.IDENTIFIER + "` != NULL"));
            }
            aql.addLine(AQL.trust("RETURN p), globalPropertyDefinitionsByType)"));

            aql.addNewline();
            aql.addLine(AQL.trust("LET globalProperties = MERGE(FOR globalProp IN allProperties"));
            aql.addLine(AQL.trust("FILTER globalProp.`" + SchemaOrgVocabulary.IDENTIFIER + "` != null"));
            aql.addLine(AQL.trust("LET targetTypes = (FOR targetType IN 1..1 OUTBOUND globalProp `" + InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION.getCollectionName() + "` RETURN DISTINCT targetType.`" + SchemaOrgVocabulary.IDENTIFIER + "`)"));
            aql.addLine(AQL.trust("FILTER targetTypes != []"));
            aql.addLine(AQL.trust("RETURN {"));
            aql.addLine(AQL.trust("[ globalProp.`" + SchemaOrgVocabulary.IDENTIFIER + "` ] : {"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES + "\": targetTypes"));
            aql.addLine(AQL.trust("}})"));
            aql.addNewline();
            aql.addLine(AQL.trust("LET spaceSpecificProperties = MERGE(FOR spaceType2property, spaceType2propertyEdge IN 1..1 OUTBOUND space2type `" + InternalSpace.TYPE_TO_PROPERTY_EDGE_COLLECTION.getCollectionName() + "`"));
            if (propertyName != null) {
                aql.addLine(AQL.trust("FILTER spaceType2property.`" + SchemaOrgVocabulary.IDENTIFIER + "`==@propertyName"));
            }
            if (withCount) {
                aql.addComment("To be able to count the occurrences of the property, we need to find the contributing documents");
                aql.addLine(AQL.trust("LET docsContributingToSpace2Property = FIRST(FOR docContributingToSpace2Property IN 1..1 INBOUND spaceType2propertyEdge `" + InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION.getCollectionName() + "`"));
                aql.addLine(AQL.trust("COLLECT WITH COUNT INTO numberOfContributingDocs"));
                aql.addLine(AQL.trust("RETURN numberOfContributingDocs)"));
                aql.addNewline();
                aql.indent();
            }
            aql.addComment("We're also interested in the target types of the property in this space-type combination...");
            aql.addLine(AQL.trust("LET targets = (FOR target, targetEdge IN 1..1 OUTBOUND spaceType2propertyEdge `" + InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION.getCollectionName() + "`"));
            aql.addNewline();
            aql.indent();
            aql.addLine(AQL.trust(" LET targetSpace = DOCUMENT(target._from)"));
            aql.addLine(AQL.trust(" LET targetType = DOCUMENT(target._to)"));
            if (withCount) {
                aql.addComment("And of course, we also count those target occurrences...");
                aql.addLine(AQL.trust(" LET countTargetOccurrences = @space == null OR space.`" + SchemaOrgVocabulary.IDENTIFIER + "`==@space ? FIRST(FOR targetRel IN 1..1 INBOUND targetEdge `" + InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION.getCollectionName() + "`"));
                aql.addLine(AQL.trust(" COLLECT WITH COUNT INTO occurrenceOfTarget"));
                aql.addLine(AQL.trust(" RETURN occurrenceOfTarget) : 0"));
            }
            aql.addLine(AQL.trust(" RETURN {"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_TYPE + "\": targetType.`" + SchemaOrgVocabulary.IDENTIFIER + "`,"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_SPACE + "\": targetSpace.`" + SchemaOrgVocabulary.IDENTIFIER + "`"));
            if (withCount) {
                aql.addLine(AQL.trust(", \"" + EBRAINSVocabulary.META_OCCURRENCES + "\": countTargetOccurrences"));
            }
            aql.addLine(AQL.trust("  })"));
            aql.outdent();
            aql.addNewline();
            aql.addComment("Now we collect the found targets and return them");
            aql.addLine(AQL.trust("LET targetsByType = (FOR targetT IN targets"));
            aql.addLine(AQL.trust(" COLLECT targetType = targetT.`" + EBRAINSVocabulary.META_TYPE + "` INTO tByType"));
            aql.addLine(AQL.trust("  RETURN {"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_TYPE + "\": targetType,"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_SPACES + "\": tByType[*].targetT"));
            aql.addLine(AQL.trust("  })"));
            aql.addNewline();
            aql.outdent();
            if (withCount) {
                aql.addComment("Finally, we count the target occurrences and sum them for a total number of all target occurrences.");
                aql.addLine(AQL.trust("LET targetsByTypeWithCount = (FOR targetByTypeWithCount IN targetsByType"));
                aql.addLine(AQL.trust("LET countOccurrences = @space == null OR space.`" + SchemaOrgVocabulary.IDENTIFIER + "`==@space ? SUM(targetByTypeWithCount.`" + EBRAINSVocabulary.META_SPACES + "`[*].`" + EBRAINSVocabulary.META_OCCURRENCES + "`) : 0"));
                aql.addLine(AQL.trust("RETURN MERGE({\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": countOccurrences}, targetByTypeWithCount))"));
            }
            aql.addNewline();
            aql.outdent();
            aql.addComment("We now combine all the found information about the property in the space-type context and return it as an object.");
            aql.addLine(AQL.trust("  RETURN {"));
            aql.addLine(AQL.trust("[ spaceType2property.`" + SchemaOrgVocabulary.IDENTIFIER + "` ] : {"));
            if (withCount) {
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": docsContributingToSpace2Property,"));
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES + "\": targetsByTypeWithCount"));
            } else {
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES + "\": targetsByType"));
            }
            aql.addLine(AQL.trust("  }}"));
            aql.outdent();
            aql.addLine(AQL.trust(")"));
            aql.addNewline();

            aql.addComment("Now that we've defined the specifications in the different levels, we're going to merge them for having a single result.");

            aql.addLine(AQL.trust("LET properties = (FOR p IN allProperties"));

            aql.addLine(AQL.trust("LET spaceSpecific = spaceSpecificProperties[p.`" + SchemaOrgVocabulary.IDENTIFIER + "`]"));
            aql.addLine(AQL.trust("LET typeSpecific = typeSpecificProperties[p.`" + SchemaOrgVocabulary.IDENTIFIER + "`]"));
            aql.addLine(AQL.trust("LET global = globalProperties[p.`" + SchemaOrgVocabulary.IDENTIFIER + "`]"));

            aql.addLine(AQL.trust("LET targetTypesFromSpaceDef = NOT_NULL(spaceSpecific.`" + EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES + "`, [])"));
            aql.addLine(AQL.trust("LET targetTypesFromTypeDef = NOT_NULL(typeSpecific.`" + EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES + "`, [])"));
            aql.addLine(AQL.trust("LET targetTypesFromGlobalDef = NOT_NULL(global.`" + EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES + "`, [])"));

            aql.addLine(AQL.trust("LET spaceDefinedTargetTypes = (FOR t IN targetTypesFromSpaceDef RETURN t.`" + EBRAINSVocabulary.META_TYPE + "`)"));

            aql.addLine(AQL.trust("LET targetTypesWithTypeDef = APPEND(targetTypesFromSpaceDef, (FOR t IN targetTypesFromTypeDef"));
            aql.addLine(AQL.trust("FILTER t NOT IN spaceDefinedTargetTypes"));
            aql.addLine(AQL.trust("RETURN {"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": 0,"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_TYPE + "\": t"));
            aql.addLine(AQL.trust("}"));
            aql.addLine(AQL.trust("))"));

            aql.addLine(AQL.trust("LET spaceAndTypeDefinedTargetTypes = UNION_DISTINCT(spaceDefinedTargetTypes, targetTypesFromTypeDef)"));

            aql.addLine(AQL.trust("LET targetTypes = APPEND(targetTypesWithTypeDef, (FOR t IN targetTypesFromGlobalDef"));
            aql.addLine(AQL.trust("FILTER t NOT IN spaceAndTypeDefinedTargetTypes"));
            aql.addLine(AQL.trust("RETURN {"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": 0,"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_TYPE + "\": t"));
            aql.addLine(AQL.trust(" }))"));

            aql.addLine(AQL.trust(" RETURN {"));
            aql.addLine(AQL.trust("\"_tempSpace\": space.`" + SchemaOrgVocabulary.IDENTIFIER + "`,"));
            aql.addLine(AQL.trust("\"" + SchemaOrgVocabulary.IDENTIFIER + "\": p.`" + SchemaOrgVocabulary.IDENTIFIER + "`,"));
            if (withCount) {
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": NOT_NULL(spaceSpecific.`" + EBRAINSVocabulary.META_OCCURRENCES + "`, 0),"));
            }
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES + "\": targetTypes"));
            aql.addLine(AQL.trust("})"));
            aql.addNewline();
        }
        if (withCount) {
            aql.addComment("We now are combining the type in space information (such as occurrences)");
            aql.addLine(AQL.trust("LET occurrenceBySpace = @space == null OR space.`" + SchemaOrgVocabulary.IDENTIFIER + "`==@space ? FIRST(FOR rel IN `" + InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION.getCollectionName() + "`"));
            aql.addLine(AQL.trust("    FILTER rel._to==space2type._id"));
            aql.addLine(AQL.trust("    COLLECT WITH COUNT INTO occurrenceBySpaceCount"));
            aql.addLine(AQL.trust("    RETURN occurrenceBySpaceCount) : 0"));
        }
        if (withProperties) {
            aql.addLine(AQL.trust("RETURN {"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_SPACE + "\": space.`" + SchemaOrgVocabulary.NAME + "`,"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_PROPERTIES + "\": properties"));
            if (withCount) {
                aql.addLine(AQL.trust(", \"" + EBRAINSVocabulary.META_OCCURRENCES + "\": occurrenceBySpace"));
            }
            aql.addLine(AQL.trust("})"));
        }
        if (withCount) {
            aql.addComment("And we sum all the space specific information to generate the global occurrence number.");
            aql.addLine(AQL.trust("LET typeOccurrences = SUM(spaces[**].`" + EBRAINSVocabulary.META_OCCURRENCES + "`)"));
            //aql.addLine(AQL.trust("FILTER typeOccurrences > 0"));
            aql.addLine(AQL.trust("LET globalOccurrences = [{\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": typeOccurrences}]"));
        }
        if (withProperties) {
            aql.addNewline();
            aql.addComment("Since we also want to have a global view on the properties, we now collect the global information. For this, we first iterate across all already fetched spaces and extract their properties");
            aql.addLine(AQL.trust("LET globalProperties = [{\"" + EBRAINSVocabulary.META_PROPERTIES + "\": (FOR props IN spaces[**].`" + EBRAINSVocabulary.META_PROPERTIES + "`[**]"));
            aql.addLine(AQL.trust("COLLECT globalPropertyName = props.`" + SchemaOrgVocabulary.IDENTIFIER + "` INTO groupedGlobalProperties"));

            aql.addComment("We still need to query some global information for the property (such as specifications), so we have to get a reference to the property object");
            aql.addLine(AQL.trust("LET property = FIRST(FOR p IN `" + ArangoCollectionReference.fromSpace(InternalSpace.PROPERTIES_SPACE).getCollectionName() + "` FILTER p.`" + SchemaOrgVocabulary.IDENTIFIER + "` == globalPropertyName RETURN p)"));

            aql.addComment("... and we collect the specifications on the global (space-independent) level which can either be client&type specific, client-only specific, type-only specific or global");
            aql.indent();

            aql.addLine(AQL.trust("LET globalTypeProperties = (FOR g, gRel IN 1..1 INBOUND property `" + InternalSpace.GLOBAL_TYPE_TO_PROPERTY_EDGE_COLLECTION.getCollectionName() + "`"));
            aql.addLine(AQL.trust("FILTER g.`" + SchemaOrgVocabulary.IDENTIFIER + "`==type.`" + SchemaOrgVocabulary.IDENTIFIER + "`"));
            aql.addLine(AQL.trust("RETURN gRel)"));

            aql.addComment("First, we check for the property specifications which are defined for this client AND the type");
            aql.addLine(AQL.trust("LET clientSpecificTypePropertySpec = NOT_NULL(FIRST(FOR typeProperty IN globalTypeProperties"));
            aql.addLine(AQL.trust("RETURN NOT_NULL(FIRST(FOR clientTypeProperty IN 1..1 INBOUND typeProperty `" + InternalSpace.CLIENT_TYPE_PROPERTY_EDGE_COLLECTION.getCollectionName() + "`"));
            aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION(@clientName, clientTypeProperty._id)"));
            aql.addLine(AQL.trust("RETURN UNSET(clientTypeProperty, propertiesToRemoveForOverrides)), {})), {})"));

            aql.addComment("Second, we go for the specification defined for the current client independent of the type");
            aql.addLine(AQL.trust("LET clientSpecificGlobalPropertySpec = NOT_NULL(FIRST(FOR g IN 1..1 INBOUND property `" + ArangoCollectionReference.fromSpace(new SpaceName(EBRAINSVocabulary.META_PROPERTY)).getCollectionName() + "`"));
            aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION(@clientName, g._id)"));
            aql.addLine(AQL.trust("RETURN UNSET(g, propertiesToRemoveForOverrides)), {})"));

            aql.addComment("Third, we look up the specification declared for this type - regardless of the requesting client");
            aql.addLine(AQL.trust("LET globalTypePropertySpec = NOT_NULL(FIRST(FOR typeProperty IN globalTypeProperties"));
            aql.addLine(AQL.trust("RETURN NOT_NULL(FIRST(FOR clientTypeProperty IN 1..1 INBOUND typeProperty `" + InternalSpace.CLIENT_TYPE_PROPERTY_EDGE_COLLECTION.getCollectionName() + "`"));
            aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION(\"" + ArangoCollectionReference.fromSpace(InternalSpace.GLOBAL_SPEC).getCollectionName() + "\", clientTypeProperty._id)"));
            aql.addLine(AQL.trust("RETURN UNSET(clientTypeProperty, propertiesToRemoveForOverrides)), {})), {})"));

            aql.addComment("Fourth, we query the global and non-type-specific specifications");
            aql.addLine(AQL.trust("LET globalPropertySpec = NOT_NULL(FIRST(FOR g IN 1..1 INBOUND property `" + ArangoCollectionReference.fromSpace(new SpaceName(EBRAINSVocabulary.META_PROPERTY)).getCollectionName() + "`"));
            aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION(\"" + ArangoCollectionReference.fromSpace(InternalSpace.GLOBAL_SPEC).getCollectionName() + "\", g._id)"));
            aql.addLine(AQL.trust("RETURN UNSET(g, propertiesToRemoveForOverrides)), {})"));

            aql.outdent();
            aql.addNewline();
            aql.addComment("Now we're aggregating the information from the spaces queries above.");

            //We need to treat the types with 0-occurrences (usually contract-first) different from the others because they can - by nature - not be part of any space and therefore wouldn't be properly reflected.
            aql.addLine(AQL.trust("LET targetTypesWithZeroOccurrences = (FOR typeWithNoOccurrences IN groupedGlobalProperties[*].props[**].`" + EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES + "`[**]"));
            aql.addLine(AQL.trust("FILTER typeWithNoOccurrences.`" + EBRAINSVocabulary.META_OCCURRENCES + "`==0"));
            aql.addLine(AQL.trust("RETURN {\"" + EBRAINSVocabulary.META_TYPE + "\": typeWithNoOccurrences.`" + EBRAINSVocabulary.META_TYPE + "`"));
            if (withCount) {
                aql.addLine(AQL.trust(", \"" + EBRAINSVocabulary.META_OCCURRENCES + "\": 0"));
            }
            aql.addLine(AQL.trust("})"));
            aql.addLine(AQL.trust("LET targetTypes = APPEND(targetTypesWithZeroOccurrences, (FOR globalPropertyBySpace IN groupedGlobalProperties[*].props[**].`" + EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES + "`[**].`" + EBRAINSVocabulary.META_SPACES + "`[**]"));
            aql.addLine(AQL.trust("FILTER globalPropertyBySpace.`" + EBRAINSVocabulary.META_OCCURRENCES + "`>0"));
            aql.addLine(AQL.trust("COLLECT globalType = globalPropertyBySpace.`" + EBRAINSVocabulary.META_TYPE + "` INTO globalTypeBySpace"));
            if (withCount) {
                aql.addLine(AQL.trust("LET globalTypeCount = SUM((FOR p IN globalTypeBySpace[*].globalPropertyBySpace[**] FILTER @space == null OR  p.`" + EBRAINSVocabulary.META_SPACE + "`==@space RETURN p)[**].`" + EBRAINSVocabulary.META_OCCURRENCES + "`)"));
            }
            aql.addLine(AQL.trust("RETURN {"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_TYPE + "\": globalType,"));
            if (withCount) {
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": globalTypeCount,"));
            }
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_SPACES + "\": globalTypeBySpace[*].globalPropertyBySpace"));
            aql.addLine(AQL.trust("}))"));

            aql.addLine(AQL.trust("LET filteredTargetTypes = (FOR t IN targetTypes"));
            if (withCount) {
                aql.addLine(AQL.trust("LET o=t.`" + EBRAINSVocabulary.META_OCCURRENCES + "`"));
            }
            aql.addLine(AQL.trust("LET s=t.`"+EBRAINSVocabulary.META_SPACES +"`"));
            aql.addLine(AQL.trust("COLLECT filteredType=t.`"+EBRAINSVocabulary.META_TYPE+"` INTO resultFilteredTypes KEEP o, s"));
            aql.addLine(AQL.trust("RETURN {"));
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_TYPE + "\": filteredType,"));
            if (withCount) {
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": SUM(resultFilteredTypes[*].o),"));
            }
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_SPACES + "\": (FOR r IN resultFilteredTypes[*].s FILTER r != null RETURN DISTINCT r)"));
            aql.addLine(AQL.trust("})"));

            aql.addLine(AQL.trust("RETURN MERGE({"));
            aql.addLine(AQL.trust("\"" + SchemaOrgVocabulary.IDENTIFIER + "\": globalPropertyName,"));
            if (withCount) {
                aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": SUM(NOT_NULL((FOR p IN groupedGlobalProperties[*].props[**] FILTER @space == null OR  p.`_tempSpace`==@space RETURN p)[*].`" + EBRAINSVocabulary.META_OCCURRENCES + "`, 0)),"));
            }
            aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES + "\": filteredTargetTypes"));
            aql.addLine(AQL.trust("}, globalPropertySpec, globalTypePropertySpec, clientSpecificGlobalPropertySpec, clientSpecificTypePropertySpec)"));
            aql.addLine(AQL.trust(")}]"));
            aql.addLine(AQL.trust("LET filteredSpaces = (FOR s IN spaces RETURN MERGE(s, {\"" + EBRAINSVocabulary.META_PROPERTIES + "\": (FOR p IN s.`" + EBRAINSVocabulary.META_PROPERTIES + "` RETURN UNSET(p, \"_tempSpace\"))}))"));
        } else {
            aql.addLine(AQL.trust("LET filteredSpaces = spaces"));
        }

        if (types == null || types.isEmpty()) {
            aql.addPagination(paginationParam);
        }
        aql.addLine(AQL.trust("RETURN DISTINCT MERGE(UNION([UNSET(type, propertiesToRemove)], globalTypeDef, clientSpecific, [{\"" + EBRAINSVocabulary.META_SPACES + "\": (FOR s IN filteredSpaces FILTER @space == null OR s.`" + EBRAINSVocabulary.META_SPACE + "`==@space RETURN DISTINCT s)}]"));
        if (withIncomingLinks){
            aql.add(AQL.trust(", incomingLinks"));
        }
        if (withCount) {
            aql.add(AQL.trust(", globalOccurrences"));
        }
        if (withProperties) {
            aql.add(AQL.trust(", globalProperties"));
        }
        aql.add(AQL.trust("))"));
        logger.debug(aql.buildSimpleDebugQuery(bindVars));

        return new AQLQuery(aql, bindVars);
    }

}

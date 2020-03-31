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

package eu.ebrains.kg.graphdb.properties.controller;

public class ArangoRepositoryProperties {
//
//    public static void definePropertiesQuery(AQL aql, Map<String,Object> bindVars, ){
//
//        aql.addLine(AQL.trust("LET properties = [{\"" + EBRAINSVocabulary.META_PROPERTIES + "\": (FOR property, typetoproperty IN 1..1 OUTBOUND type @@typeToProperty"));
//        bindVars.put("@typeToProperty", InternalSpace.TYPE_TO_PROPERTY_EDGE_COLLECTION.getCollectionName());
//
//        aql.addLine(AQL.trust("LET occurrenceByType =  [{"));
//        aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_OCCURRENCES + "\": FIRST (FOR doc, rel IN 1..1 INBOUND typetoproperty @@documentRelation"));
//        aql.addLine(AQL.trust("FILTER type.`" + SchemaOrgVocabulary.IDENTIFIER + "` IN rel._docTypes AND (@space == null OR rel._docCollection==@space)"));
//        aql.addLine(AQL.trust("COLLECT WITH COUNT INTO length"));
//        aql.addLine(AQL.trust("RETURN length)"));
//        aql.addLine(AQL.trust("}]"));
//
//        aql.addLine(AQL.trust("LET clientAndTypeSpecificProperty = ( "));
//        aql.addLine(AQL.trust("FOR g IN 1..1 INBOUND typetoproperty @@clientTypeProperty "));
//        bindVars.put("@clientTypeProperty", InternalSpace.CLIENT_TYPE_PROPERTY_EDGE_COLLECTION.getCollectionName());
//        aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION( @clientName, g._id )"));
//        aql.addLine(AQL.trust("RETURN UNSET (g, propertiesToRemoveForOverrides)"));
//        aql.addLine(AQL.trust(")"));
//
//        aql.addLine(AQL.trust("LET clientSpecificProperty = ("));
//        aql.addLine(AQL.trust("FOR g IN 1..1 INBOUND property @@metaProperty"));
//        bindVars.put("@metaProperty", ArangoCollectionReference.fromSpace(new Space(EBRAINSVocabulary.META_PROPERTY)).getCollectionName());
//        aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION ( @clientName, g._id )"));
//        aql.addLine(AQL.trust("RETURN UNSET (g, propertiesToRemoveForOverrides)"));
//        aql.addLine(AQL.trust(")"));
//
//        aql.addLine(AQL.trust("LET globalTypeSpecificProperty = ("));
//        aql.addLine(AQL.trust("FOR g IN 1..1 INBOUND typetoproperty @@clientTypeProperty"));
//        aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION (@globalSpace, g._id )"));
//        aql.addLine(AQL.trust("RETURN UNSET (g, propertiesToRemoveForOverrides)"));
//        aql.addLine(AQL.trust(")"));
//
//        aql.addLine(AQL.trust("LET globalProperty = ("));
//        aql.addLine(AQL.trust("FOR g IN 1..1 INBOUND property @@metaProperty"));
//        aql.addLine(AQL.trust("FILTER IS_SAME_COLLECTION (@globalSpace, g._id )"));
//        aql.addLine(AQL.trust("RETURN UNSET (g, propertiesToRemoveForOverrides)"));
//        aql.addLine(AQL.trust(")"));
//
//        //FIXME the current query reflects all types connected with this field (doesn't take into account the context of the originating type)
//        aql.addLine(AQL.trust("LET relatedTypes = [{\""+ EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES+"\": (FOR relT, relTE IN 1..1 OUTBOUND property @@property2type"));
//        bindVars.put("@property2type", InternalSpace.PROPERTY_TO_TYPE_EDGE_COLLECTION.getCollectionName());
//        aql.addLine(AQL.trust("LET occurrences = LENGTH(FOR doc, docE IN 1..1 INBOUND relTE @@documentRelation RETURN docE)"));
//        aql.addLine(AQL.trust("SORT occurrences DESC, relT.`"+ SchemaOrgVocabulary.IDENTIFIER+"` ASC"));
//        aql.addLine(AQL.trust("RETURN relT.`"+SchemaOrgVocabulary.IDENTIFIER+"`)}]"));
//
//        aql.addLine(AQL.trust("LET valueTypes = [{\"" + EBRAINSVocabulary.META_VALUE_TYPES + "\":(FOR valueType, propertyvaluetype IN 1..1 OUTBOUND property @@property2propertyValueType"));
//        bindVars.put("@property2propertyValueType", InternalSpace.PROPERTY_TO_PROPERTY_VALUE_TYPE_EDGE_COLLECTION.getCollectionName());
//
//        aql.addLine(AQL.trust("LET occurrencesOfValueTypes = FIRST(FOR doc, rel IN 1..1 INBOUND propertyvaluetype @@documentRelation"));
//        aql.addLine(AQL.trust("FILTER type.`" + SchemaOrgVocabulary.IDENTIFIER + "` IN rel._docTypes AND ( @space ==null OR rel._docCollection == @space)"));
//        aql.addLine(AQL.trust("COLLECT WITH COUNT INTO length"));
//        aql.addLine(AQL.trust("RETURN length)"));
//        aql.addLine(AQL.trust("FILTER occurrencesOfValueTypes>0"));
//        aql.addLine(AQL.trust("RETURN {\"" + SchemaOrgVocabulary.NAME + "\":valueType.`" + SchemaOrgVocabulary.NAME + "`,"));
//        aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_OCCURRENCES + "\":occurrencesOfValueTypes"));
//        aql.addLine(AQL.trust("})}]"));
//        aql.addLine(AQL.trust("RETURN MERGE (UNION([UNSET(property, propertiesToRemove)], globalProperty, globalTypeSpecificProperty, clientSpecificProperty, clientAndTypeSpecificProperty, occurrenceByType, valueTypes, relatedTypes))"));
//        aql.addLine(AQL.trust(")}]"));
//
//
//    }


}

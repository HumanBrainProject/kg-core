/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Specifications {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ArangoDatabases databases;

    public Specifications(ArangoDatabases databases) {
        this.databases = databases;
    }
    public final static List<String> SPEC_PROPERTY_BLACKLIST = Arrays.asList(SchemaOrgVocabulary.IDENTIFIER, EBRAINSVocabulary.META_ALTERNATIVE, EBRAINSVocabulary.META_REVISION, EBRAINSVocabulary.META_USER, EBRAINSVocabulary.META_SPACE, EBRAINSVocabulary.META_TYPE, EBRAINSVocabulary.META_PROPERTY);


    @Cacheable("typesInSpaces")
    public List<String> getTypesInSpace(DataStage stage, SpaceName spaceName){
        //Types in space definitions are only valid on a global level (it doesn't make to assign a type to a space in a client only)
        AQL query = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        query.addLine(AQL.trust("FOR s IN @@collection"));
        bindVars.put("@collection", ArangoCollectionReference.fromSpace(InternalSpace.GLOBAL_SPEC).getCollectionName());
        query.addLine(AQL.trust(String.format("FILTER \"%s\" IN s.`%s`", EBRAINSVocabulary.META_TYPE_IN_SPACE_DEFINITION_TYPE, JsonLdConsts.TYPE)));
        query.addLine(AQL.trust(String.format("FILTER @space IN s.`%s`", EBRAINSVocabulary.META_SPACES)));
        bindVars.put("space", ArangoCollectionReference.fromSpace(spaceName).getCollectionName());
        query.addLine(AQL.trust(String.format("RETURN DISTINCT FIRST(s.`%s`)[\"%s\"]", EBRAINSVocabulary.META_TYPE, JsonLdConsts.ID)));
        return databases.getByStage(stage).query(query.build().getValue(), bindVars, String.class).asListRemaining();
    }

    @Cacheable("clientSpecificSpecs")
    public NormalizedJsonLd getClientSpecifications(DataStage stage, String definitionType, List<String> groupingField, SpaceName clientName){
        if(clientName == null) {
            return null;
        }
        String clientCollection = ArangoCollectionReference.fromSpace(clientName).getCollectionName();
        AQL query = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        query.addLine(AQL.trust("LET space = MERGE(FOR d IN @@spaceCollection"));
        bindVars.put("@spaceCollection", clientCollection);
        query.addLine(AQL.trust(String.format("FILTER \"%s\" IN d.`@type`", definitionType)));
        query.addLine(AQL.trust("LET attributesToKeep = (FOR a IN ATTRIBUTES(d, true) FILTER a NOT LIKE \"@%\" AND a NOT IN @blacklist RETURN a)"));
        bindVars.put("blacklist", SPEC_PROPERTY_BLACKLIST);
        query.addLine(AQL.trust(String.format("RETURN {[ %s ] : KEEP(d, attributesToKeep)})", buildDynamicAttribute(groupingField))));
        query.addLine(AQL.trust("RETURN MERGE_RECURSIVE(global, space)"));

        final ArangoDatabase db = databases.getByStage(stage);
        final List<NormalizedJsonLd> normalizedJsonLds = db.query(query.build().getValue(), bindVars, NormalizedJsonLd.class).asListRemaining();
        return normalizedJsonLds.isEmpty() ? null : normalizedJsonLds.get(0);
    }

    private String buildDynamicAttribute(List<String> groupingField){
        StringBuilder dynamicAttribute = new StringBuilder();
        if(groupingField.size()>1){
            dynamicAttribute.append("CONCAT(");
        }
        for (int i = 0; i < groupingField.size(); i++) {
            String grouping = groupingField.get(i);
            dynamicAttribute.append(String.format("FIRST(d[\"%s\"])[\"@id\"]", grouping));
            if(i<groupingField.size()-1){
                dynamicAttribute.append(", \"@\", ");
            }
        }
        if(groupingField.size()>1){
            dynamicAttribute.append(")");
        }
        return dynamicAttribute.toString();
    }


    @Cacheable("globalSpecs")
    public NormalizedJsonLd getGlobalSpecifications(DataStage stage, String definitionType, List<String> groupingField){
        AQL query = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        query.addLine(AQL.trust("RETURN MERGE(FOR d IN @@globalCollection"));
        bindVars.put("@globalCollection", ArangoCollectionReference.fromSpace(InternalSpace.GLOBAL_SPEC).getCollectionName());
        query.addLine(AQL.trust(String.format("FILTER \"%s\" IN d.`@type`", definitionType)));
        query.addLine(AQL.trust("LET attributesToKeep = (FOR a IN ATTRIBUTES(d, true) FILTER a NOT LIKE \"@%\" AND a NOT IN @blacklist RETURN a)"));
        bindVars.put("blacklist", SPEC_PROPERTY_BLACKLIST);
        query.addLine(AQL.trust(String.format("RETURN {[ %s ] : KEEP(d, attributesToKeep)})", buildDynamicAttribute(groupingField))));
        final ArangoDatabase db = databases.getByStage(stage);
        final List<NormalizedJsonLd> normalizedJsonLds = db.query(query.build().getValue(), bindVars, NormalizedJsonLd.class).asListRemaining();
        return normalizedJsonLds.isEmpty() ? null : normalizedJsonLds.get(0);
    }


}

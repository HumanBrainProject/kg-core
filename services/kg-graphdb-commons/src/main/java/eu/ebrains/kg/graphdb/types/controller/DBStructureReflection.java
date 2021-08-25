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
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.CollectionsReadOptions;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.types.SpaceDefinition;
import eu.ebrains.kg.commons.model.types.TargetTypeWithOccurrence;
import eu.ebrains.kg.commons.model.types.TypeWithOccurrencesAndProperties;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DBStructureReflection {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ArangoDatabases databases;

    public DBStructureReflection(ArangoDatabases databases) {
        this.databases = databases;
    }

    //@Cacheable("targetTypes")
    public List<TargetTypeWithOccurrence> getTargetTypeWithOccurrence(String space, String type, String property, String propertyEdge, DataStage stage, boolean withCounts){
        logger.info(String.format("Reflecting on target types for property %s in type %s in space %s", property, type, space));
        final ArangoDatabase db = databases.getByStage(stage);
        AQL query = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        query.addLine(AQL.trust("LET targetTypes = FLATTEN(FOR i IN @@collection FILTER @type IN i.`@type` AND i.@property != NULL"));
        bindVars.put("@collection", space);
        bindVars.put("type", type);
        bindVars.put("property", property);
        query.addLine(AQL.trust("LET targetType = FLATTEN(FOR target IN 1..1 OUTBOUND i @@propertyEdge RETURN target)"));
        bindVars.put("@propertyEdge", propertyEdge);
        query.addLine(AQL.trust("RETURN targetType)"));
        query.addLine(AQL.trust("FOR t IN targetTypes"));
        query.addLine(AQL.trust(String.format("COLLECT targetType = FIRST(t.`%s`), space = t.`%s`", JsonLdConsts.TYPE, EBRAINSVocabulary.META_SPACE)));
        if(withCounts){
            query.addLine(AQL.trust("WITH COUNT INTO length"));
        }
        query.addLine(AQL.trust("RETURN { \"targetType\" : targetType, \"space\": space"));
        if(withCounts){
            query.addLine(AQL.trust(", \"occurrences\" : length"));
        }
        query.addLine(AQL.trust("}"));

        return db.query(query.build().getValue(), bindVars, TargetTypeWithOccurrence.class).asListRemaining();
    }

    public List<TypeWithOccurrencesAndProperties> getTypesWithProperties(String space, List<String> typeFilter, DataStage stage){
        final ArangoDatabase db = databases.getByStage(stage);
        AQL query = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        query.addLine(AQL.trust("FOR p IN (FOR d IN @@collection"));
        bindVars.put("@collection", space);
        query.addLine(AQL.trust("FILTER d.`@type` != NULL && d.`@type` != []"));
        query.addLine(AQL.trust("RETURN DISTINCT { \"type\": FIRST(d.`@type`), \"properties\": ATTRIBUTES(d, true) })"));
        query.addLine(AQL.trust("RETURN {\"type\": p.type, \"properties\": (FOR prop IN p.properties RETURN {\"property\": prop})}"));
        final List<TypeWithOccurrencesAndProperties> typeWithOccurrencesAndProperties = db.query(query.build().getValue(), bindVars, TypeWithOccurrencesAndProperties.class).asListRemaining();
        final Map<String, List<TypeWithOccurrencesAndProperties>> typeDefsByType = typeWithOccurrencesAndProperties.stream().collect(Collectors.groupingBy(TypeWithOccurrencesAndProperties::getType));
        return typeDefsByType.keySet().stream().map(k -> {
            final List<TypeWithOccurrencesAndProperties> types = typeDefsByType.get(k);
            if (types.size() == 1) {
                return types.get(0);
            } else {
                TypeWithOccurrencesAndProperties type = new TypeWithOccurrencesAndProperties();
                type.setType(k);
                type.setProperties(types.stream().map(TypeWithOccurrencesAndProperties::getProperties).flatMap(Collection::stream).distinct().collect(Collectors.toList()));
                return type;
            }
        }).collect(Collectors.toList());

    }


    //@Cacheable("typesWithProperties")
    public List<TypeWithOccurrencesAndProperties> getTypesWithOccurrencesAndProperties(String space, List<String> typeFilter, DataStage stage){
        logger.info(String.format("Reflecting on type with occurrence for space %s", space));
        final ArangoDatabase db = databases.getByStage(stage);
        AQL query = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        query.addLine(AQL.trust("FOR e IN (FOR d IN @@collection"));
        bindVars.put("@collection", space);
        if(typeFilter!=null){
            query.addLine(AQL.trust("FILTER "));
            for (int i = 0; i < typeFilter.size(); i++) {
                String type = typeFilter.get(i);
                query.addLine(AQL.trust("@type" + i + " IN d.`@type`"));
                bindVars.put("@type"+i, type);
                if (i < typeFilter.size() - 1) {
                    query.addLine(AQL.trust("OR"));
                }
            }
        }
        query.addLine(AQL.trust("FILTER d.`@type` != NULL"));
        query.addLine(AQL.trust("RETURN { \"type\": FIRST(d.`@type`), \"properties\": ATTRIBUTES(d, true) })"));
        query.addLine(AQL.trust("FILTER e[\"type\"] != NULL "));
        query.addLine(AQL.trust("COLLECT type = e[\"type\"] into byType"));
        query.addLine(AQL.trust("RETURN { \"type\" : type, \"occurrences\": LENGTH(byType), \"properties\" : (FOR a IN FLATTEN(byType[*].e.properties[*]) COLLECT att = a WITH COUNT INTO length"));
        query.addLine(AQL.trust("RETURN {\"property\" : att,  \"occurrences\" : length })"));
        query.addLine(AQL.trust("}"));
        return db.query(query.build().getValue(), bindVars, TypeWithOccurrencesAndProperties.class).asListRemaining();
    }

    private static final List<SpaceName> SPACE_BLACKLIST = Arrays.asList(InternalSpace.GLOBAL_SPEC, InternalSpace.DOCUMENT_ID_SPACE, InternalSpace.RELEASE_STATUS_SPACE, InternalSpace.TYPE_SPACE);
    private static final List<String> COLLECTIONS_BLACKLIST = SPACE_BLACKLIST.stream().map(s -> ArangoCollectionReference.fromSpace(s).getCollectionName()).collect(Collectors.toList());

    public List<SpaceDefinition> getAllSpaceDefinitions(DataStage stage, List<String> collections){
        final ArangoDatabase db = databases.getByStage(stage);
        List<String> spaceSpecs = getSpaceSpecification(null, db);
        final Map<String, SpaceName> collectionToSpaceName = spaceSpecs.stream().map(s -> new SpaceName(s)).collect(Collectors.toMap(k -> ArangoCollectionReference.fromSpace(k).getCollectionName(), v -> v));
        return collections.stream().map(c -> {
            SpaceDefinition spaceDefinition = new SpaceDefinition();
            spaceDefinition.setName(collectionToSpaceName.get(c));
            spaceDefinition.setCollectionName(c);
            return spaceDefinition;
        }).collect(Collectors.toList());
        //TODO what about collections specified but not existing as a collection?
    }

    public List<SpaceDefinition> getAllTypeDefinitions(DataStage stage, List<String> collections){
        final ArangoDatabase db = databases.getByStage(stage);
        List<String> spaceSpecs = getSpaceSpecification(null, db);
        final Map<String, SpaceName> collectionToSpaceName = spaceSpecs.stream().map(s -> new SpaceName(s)).collect(Collectors.toMap(k -> ArangoCollectionReference.fromSpace(k).getCollectionName(), v -> v));
        return collections.stream().map(c -> {
            SpaceDefinition spaceDefinition = new SpaceDefinition();
            spaceDefinition.setName(collectionToSpaceName.get(c));
            spaceDefinition.setCollectionName(c);
            return spaceDefinition;
        }).collect(Collectors.toList());
        //TODO what about collections specified but not existing as a collection?
    }




    @Cacheable("spaces")
    public List<String> getAllSpaceCollections(DataStage stage){
        final ArangoDatabase db = databases.getByStage(stage);

        final Collection<CollectionEntity> collections = db.getCollections(new CollectionsReadOptions().excludeSystem(true));
        return collections.stream().filter(c -> c.getType().equals(CollectionType.DOCUMENT) && !COLLECTIONS_BLACKLIST.contains(c.getName())).map(CollectionEntity::getName).collect(Collectors.toList());
    }

    private List<String> getSpaceSpecification(String client, ArangoDatabase db){
        AQL query = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        query.addLine(AQL.trust(String.format("FOR s in @@collection FILTER \"%s\" IN s.`@type` RETURN s.`%s`", EBRAINSVocabulary.META_SPACEDEFINITION_TYPE, SchemaOrgVocabulary.NAME)));
        bindVars.put("@collection", client == null ? ArangoCollectionReference.fromSpace(InternalSpace.GLOBAL_SPEC).getCollectionName() : new ArangoCollectionReference(client, false).getCollectionName());
        return db.query(query.build().getValue(), bindVars, String.class).asListRemaining();
    }


    private final List<String> EDGE_BLACKLIST = Arrays.asList(
            new ArangoCollectionReference(EBRAINSVocabulary.META_ALTERNATIVE, true).getCollectionName(),
            new ArangoCollectionReference(EBRAINSVocabulary.META_USER, true).getCollectionName(),
            InternalSpace.DOCUMENT_ID_EDGE_COLLECTION.getCollectionName(),
            ArangoCollectionReference.fromSpace(InternalSpace.INFERENCE_OF_SPACE).getCollectionName(),
            InternalSpace.RELEASE_STATUS_EDGE_COLLECTION.getCollectionName(),
            InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName(),
            ArangoCollectionReference.fromSpace(InternalSpace.UNRESOLVED_SPACE).getCollectionName());


    @Cacheable("edges")
    public List<String> getAllRelevantEdges(DataStage stage){
        final ArangoDatabase db = databases.getByStage(stage);
        final Collection<CollectionEntity> collections = db.getCollections(new CollectionsReadOptions().excludeSystem(true));
        return collections.stream().filter(c -> c.getType().equals(CollectionType.EDGES)).map(CollectionEntity::getName).filter(c -> !EDGE_BLACKLIST.contains(c)).collect(Collectors.toList());
    }


    public NormalizedJsonLd getPropertiesWithTargetSpaces(DataStage stage){
        AQL query = new AQL();
//        query.addLine(AQL.trust("RETURN MERGE ("));
//        getAllEdges(stage).forEach(e -> {
//            query.addLine(AQL.trust(String.format("{\"%s\": (FOR e IN `%s` RETURN DISTINCT SPLIT (e._to, \"/\")[0])},", e, e)));
//        });
//        query.addLine(AQL.trust("{})"));
        query.addLine(AQL.trust("FOR d IN dataset"));
        query.addLine(AQL.trust("FILTER d.`@type`!= NULL and d.`@type` != []"));
        query.addLine(AQL.trust("LET incoming = MERGE(FLATTEN(FOR i, e in 1..1 INBOUND d"));
        query.addLine(AQL.trust(String.format("`%s`", String.join("`,`", getAllRelevantEdges(stage)))));
        query.addLine(AQL.trust("RETURN { [ e.`_originalLabel` ] : i.`@type`} ))"));
        query.addLine(AQL.trust("RETURN DISTINCT MERGE(incoming, {\"targetType\": FIRST(d.`@type`)})"));
        final List<NormalizedJsonLd> result = databases.getByStage(stage).query(query.build().getValue(), NormalizedJsonLd.class).asListRemaining();
        return result.isEmpty() ? null : result.get(0);
    }

}

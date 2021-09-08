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

package eu.ebrains.kg.graphdb.structure.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.CollectionsReadOptions;
import com.arangodb.model.DocumentCreateOptions;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.exception.AmbiguousException;
import eu.ebrains.kg.commons.jsonld.DynamicJson;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.external.spaces.SpaceSpecification;
import eu.ebrains.kg.commons.model.internal.spaces.Space;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.GraphDBArangoUtils;
import eu.ebrains.kg.graphdb.commons.model.ArangoEdge;
import eu.ebrains.kg.graphdb.structure.model.PropertyOfTypeInSpaceReflection;
import eu.ebrains.kg.graphdb.structure.model.TargetTypeReflection;
import eu.ebrains.kg.graphdb.structure.model.TypeWithInstanceCountReflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class StructureRepository {

    private final ArangoDatabases arangoDatabases;
    private final JsonAdapter jsonAdapter;
    private final GraphDBArangoUtils graphDBArangoUtils;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public StructureRepository(ArangoDatabases arangoDatabases, JsonAdapter jsonAdapter, GraphDBArangoUtils graphDBArangoUtils) {
        this.arangoDatabases = arangoDatabases;
        this.jsonAdapter = jsonAdapter;
        this.graphDBArangoUtils = graphDBArangoUtils;
    }

    private final static ArangoCollectionReference SPACES = new ArangoCollectionReference("spaces", false);
    private final static ArangoCollectionReference TYPES = new ArangoCollectionReference("types", false);
    private final static ArangoCollectionReference PROPERTIES = new ArangoCollectionReference("properties", false);
    private final static ArangoCollectionReference TYPE_IN_SPACE = new ArangoCollectionReference("typeInSpace", true);
    private final static ArangoCollectionReference PROPERTY_IN_TYPE = new ArangoCollectionReference("propertyInType", true);

    public static void setupCollections(ArangoDatabaseProxy database) {
        database.createCollectionIfItDoesntExist(SPACES);
        database.createCollectionIfItDoesntExist(TYPES);
        database.createCollectionIfItDoesntExist(PROPERTIES);
        database.createCollectionIfItDoesntExist(TYPE_IN_SPACE);
        database.createCollectionIfItDoesntExist(PROPERTY_IN_TYPE);
    }

    @Cacheable("reflectedSpaces")
    public List<SpaceName> reflectSpaces(DataStage stage) {
        logger.info("Missing cache hit: Fetching space reflection from database");
        final ArangoDatabase database = arangoDatabases.getByStage(stage);
        final List<CollectionEntity> spaces = database.getCollections(new CollectionsReadOptions().excludeSystem(true)).stream().filter(c -> c.getType() == CollectionType.DOCUMENT).filter(c -> !InternalSpace.INTERNAL_SPACENAMES.contains(c.getName())).distinct().collect(Collectors.toList());
        AQL query = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        query.addLine(AQL.trust("FOR space IN UNION_DISTINCT("));
        for (int i = 0; i < spaces.size(); i++) {
            bindVars.put(String.format("@collection%d", i), spaces.get(i).getName());
            query.addLine(AQL.trust(String.format("(FOR e IN @@collection%d FILTER e.@space != NULL AND e.@space != [] LIMIT 0,1 RETURN e.@space)", i)));
            if (i < spaces.size() - 1) {
                query.add(AQL.trust(", "));
            }
            bindVars.put("space", EBRAINSVocabulary.META_SPACE);
        }
        query.addLine(AQL.trust(") RETURN space"));
        return database.query(query.build().getValue(), bindVars, String.class).asListRemaining().stream().map(SpaceName::fromString).collect(Collectors.toList());
    }

    @CacheEvict("reflectedSpaces")
    public void evictReflectedSpacesCache(DataStage stage) {
        logger.info("Cache evict: clearing cache for reflected spaces");
    }

    @Cacheable("spaceSpecifications")
    public List<Space> getSpaceSpecifications() {
        logger.info("Missing cache hit: Fetching space specifications from database");
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR d IN @@collection"));
        bindVars.put("@collection", SPACES.getCollectionName());
        aql.addLine(AQL.trust(String.format("SORT d.`%s` ASC", SchemaOrgVocabulary.NAME)));
        aql.addLine(AQL.trust("RETURN KEEP(d, ATTRIBUTES(d, true))"));
        return Collections.unmodifiableList(arangoDatabases.getStructureDB().query(aql.build().getValue(), bindVars, Space.class).asListRemaining());
    }

    @CacheEvict("spaceSpecifications")
    public void evictSpaceSpecificationCache() {
        logger.info("Cache evict: clearing cache for space specifications");
    }

    @Cacheable("typesInSpaceBySpec")
    public List<String> getTypesInSpaceBySpecification(SpaceName spaceName) {
        logger.info(String.format("Missing cache hit: Fetching types in space %s specifications from database", spaceName.getName()));
        final UUID spaceUUID = spaceSpecificationRef(spaceName.getName());
        AQL query = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        query.addLine(AQL.trust("FOR t IN @@collection FILTER t._from == @id"));
        bindVars.put("@collection", TYPE_IN_SPACE.getCollectionName());
        bindVars.put("id", String.format("%s/%s", SPACES.getCollectionName(), spaceUUID));
        query.addLine(AQL.trust(String.format("RETURN DOCUMENT(t._to).`%s`", SchemaOrgVocabulary.IDENTIFIER)));
        return arangoDatabases.getStructureDB().query(query.build().getValue(), bindVars, String.class).asListRemaining();
    }

    @CacheEvict("typesInSpaceBySpec")
    public void evictTypesInSpaceBySpecification(SpaceName spaceName) {
        logger.info("Cache evict: clearing cache for type in space specifications");
    }

    @Cacheable("typeSpecification")
    public DynamicJson getTypeSpecification(String typeName) {
        logger.info(String.format("Missing cache hit: Fetching type specification for %s from database", typeName));
        return doGetTypeSpecification(typeName, TYPES);
    }

    @CacheEvict("typeSpecification")
    public void evictTypeSpecification(String typeName) {
        logger.info(String.format("Cache evict: clearing cache for type specification %s", typeName));
    }

    @Cacheable("clientSpecificTypeSpecification")
    public DynamicJson getClientSpecificTypeSpecification(String typeName, SpaceName clientSpaceName) {
        logger.info(String.format("Missing cache hit: Fetching type specification for %s from database (client: %s)", typeName, clientSpaceName.getName()));
        return doGetTypeSpecification(typeName, clientTypesCollection(clientSpaceName.getName()));
    }

    private DynamicJson doGetTypeSpecification(String typeName, ArangoCollectionReference collectionReference) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        if (structureDB.collection(collectionReference.getCollectionName()).exists()) {
            final UUID typeUUID = typeSpecificationRef(typeName);
            AQL query = new AQL();
            Map<String, Object> bindVars = new HashMap<>();
            query.addLine(AQL.trust("LET doc = DOCUMENT(@id)"));
            bindVars.put("id", String.format("%s/%s", TYPES.getCollectionName(), typeUUID));
            query.addLine(AQL.trust("LET result = DOCUMENT(@clientSpace, doc._key)"));
            query.addLine(AQL.trust("FILTER result != NULL"));
            bindVars.put("clientSpace", collectionReference.getCollectionName());
            query.addLine(AQL.trust("RETURN KEEP(result, ATTRIBUTES(result, True))"));
            return getSingleResult(structureDB.query(query.build().getValue(), bindVars, DynamicJson.class).asListRemaining(), typeUUID);
        }
        return null;
    }


    @CacheEvict("clientSpecificTypeSpecification")
    public void evictClientSpecificTypeSpecification(String typeName, SpaceName clientSpaceName) {
        logger.info(String.format("Cache evict: clearing cache for type specification %s (client: %s)", typeName, clientSpaceName.getName()));
    }

    @Cacheable("typesInSpace")
    public List<TypeWithInstanceCountReflection> reflectTypesInSpace(DataStage stage, SpaceName name) {
        logger.info(String.format("Missing cache hit: Reflecting types in space %s (stage %s)", name, stage.name()));
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("LET typeGroups=(FOR i in @@collection"));
        bindVars.put("@collection", ArangoCollectionReference.fromSpace(name).getCollectionName());
        aql.addLine(AQL.trust(String.format("FILTER i.`%s` != NULL", JsonLdConsts.TYPE)));
        aql.addLine(AQL.trust(String.format("COLLECT t=i.`%s` WITH COUNT INTO length", JsonLdConsts.TYPE)));
        aql.addLine(AQL.trust("RETURN {\"name\": t, \"occurrences\": length})"));
        aql.addLine(AQL.trust("LET types = UNIQUE(typeGroups[**].name[**])"));
        aql.addLine(AQL.trust("FOR t IN types"));
        aql.addLine(AQL.trust("LET countsInGroup = SUM(FOR g IN typeGroups FILTER t IN g.name RETURN g.occurrences)"));
        aql.addLine(AQL.trust("RETURN { \"name\": t, \"occurrences\": countsInGroup }"));
        return Collections.unmodifiableList(arangoDatabases.getByStage(stage).query(aql.build().getValue(), bindVars, TypeWithInstanceCountReflection.class).asListRemaining());
    }

    @CacheEvict("typesInSpace")
    public void evictTypesInSpaceCache(DataStage stage, SpaceName name) {
        logger.info(String.format("Cache evict: clearing cache for types in space %s (stage %s)", name, stage.name()));
    }

    @Cacheable("propertySpecification")
    public DynamicJson getPropertyBySpecification(String propertyName) {
        logger.info(String.format("Missing cache hit: Reflecting property specification %s", propertyName));
        return doGetPropertyBySpecification(propertyName, PROPERTIES);
    }

    @CacheEvict("propertySpecification")
    public void evictPropertySpecificationCache(String propertyName) {
        logger.info(String.format("Cache evict: clearing cache for property %s", propertyName));
    }


    @Cacheable("clientSpecificPropertySpecification")
    public DynamicJson getClientSpecificPropertyBySpecification(String propertyName, SpaceName clientSpaceName) {
        logger.info(String.format("Missing cache hit: Reflecting property specification %s (client: %s)", propertyName, clientSpaceName.getName()));
        return doGetPropertyBySpecification(propertyName, clientTypesCollection(clientSpaceName.getName()));
    }

    private DynamicJson getSingleResult(List<DynamicJson> dynamicJsons, UUID id) {
        if (dynamicJsons.isEmpty()) {
            return null;
        }
        if (dynamicJsons.size() > 1) {
            throw new AmbiguousException(String.format("The lookup for %s resulted in too many results", id));
        }
        return dynamicJsons.get(0);
    }

    private DynamicJson doGetPropertyBySpecification(String propertyName, ArangoCollectionReference collectionReference) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        if (structureDB.collection(collectionReference.getCollectionName()).exists()) {
            final UUID propertyUUID = propertySpecificationRef(propertyName);
            AQL query = new AQL();
            Map<String, Object> bindVars = new HashMap<>();
            query.addLine(AQL.trust("LET doc = DOCUMENT(@id)"));
            bindVars.put("id", String.format("%s/%s", collectionReference.getCollectionName(), propertyUUID));
            query.addLine(AQL.trust("RETURN KEEP(doc, ATTRIBUTES(doc, True))"));
            return getSingleResult(structureDB.query(query.build().getValue(), bindVars, DynamicJson.class).asListRemaining(), propertyUUID);
        }
        return null;
    }

    @CacheEvict("clientSpecificPropertySpecification")
    public void evictClientSpecificPropertySpecificationCache(String propertyName, SpaceName clientSpaceName) {
        logger.info(String.format("Cache evict: clearing cache for property %s (client: %s)", propertyName, clientSpaceName.getName()));
    }


    @Cacheable("propertiesInTypeSpecification")
    public List<DynamicJson> getPropertiesOfTypeBySpecification(String type) {
        logger.info(String.format("Missing cache hit: Reflecting properties for type %s", type));
        return doGetPropertiesOfTypeBySpecification(type, PROPERTY_IN_TYPE);
    }

    @CacheEvict("propertiesInTypeSpecification")
    public void evictPropertiesInTypeBySpecificationCache(String type) {
        logger.info(String.format("Cache evict: clearing cache for properties in type %s", type));
    }


    @Cacheable("clientSpecificPropertiesInTypeSpecification")
    public List<DynamicJson> getClientSpecificPropertiesOfTypeBySpecification(String type, SpaceName clientSpaceName) {
        logger.info(String.format("Missing cache hit: Reflecting properties for type %s (client: %s)", type, clientSpaceName.getName()));
        return doGetPropertiesOfTypeBySpecification(type, clientPropertyInTypeCollection(clientSpaceName.getName()));
    }

    private List<DynamicJson> doGetPropertiesOfTypeBySpecification(String type, ArangoCollectionReference collectionReference) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        if (structureDB.collection(collectionReference.getCollectionName()).exists()) {
            final UUID typeUUID = typeSpecificationRef(type);
            AQL query = new AQL();
            Map<String, Object> bindVars = new HashMap<>();
            query.addLine(AQL.trust("FOR t IN @@collection FILTER t._from == @id"));
            bindVars.put("@collection", collectionReference.getCollectionName());
            bindVars.put("id", String.format("%s/%s", TYPES.getCollectionName(), typeUUID));
            query.addLine(AQL.trust("LET doc = DOCUMENT(t._to)"));
            query.addLine(AQL.trust("FILTER doc != NULL"));
            query.addLine(AQL.trust(String.format("RETURN MERGE(KEEP(doc, [\"%s\"]), KEEP(t, ATTRIBUTES(t, True)))", SchemaOrgVocabulary.IDENTIFIER)));
            return structureDB.query(query.build().getValue(), bindVars, DynamicJson.class).asListRemaining();
        }
        return Collections.emptyList();
    }


    @CacheEvict("clientSpecificPropertiesInTypeSpecification")
    public void evictClientSpecificPropertiesInTypeBySpecificationCache(String type, SpaceName clientSpaceName) {
        logger.info(String.format("Cache evict: clearing cache for properties in type %s (client: %s)", type, clientSpaceName.getName()));
    }


    @Cacheable("propertiesOfTypeInSpace")
    public List<PropertyOfTypeInSpaceReflection> reflectPropertiesOfTypeInSpace(DataStage stage, SpaceName spaceName, String type) {
        logger.info(String.format("Missing cache hit: Reflecting properties of type %s in space %s (stage %s)", type, spaceName, stage.name()));
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("LET attGroups = (FOR d IN @@collection"));
        bindVars.put("@collection", ArangoCollectionReference.fromSpace(spaceName).getCollectionName());
        aql.addLine(AQL.trust(String.format("FILTER @type IN d.`%s`", JsonLdConsts.TYPE)));
        bindVars.put("type", type);
        aql.addLine(AQL.trust("COLLECT attGroup =  ATTRIBUTES(d, true) WITH COUNT INTO length"));
        aql.addLine(AQL.trust("RETURN {\"attributes\": attGroup, \"count\": length})"));
        aql.addLine(AQL.trust("LET attributes = UNIQUE(attGroups[**].attributes[**])"));
        aql.addLine(AQL.trust("FOR att IN attributes"));
        aql.addLine(AQL.trust("LET countsInGroup = SUM(FOR g IN attGroups FILTER att IN g.attributes RETURN g.count)"));
        aql.addLine(AQL.trust("RETURN { \"name\": att, \"occurrences\": countsInGroup }"));
        return Collections.unmodifiableList(arangoDatabases.getByStage(stage).query(aql.build().getValue(), bindVars, PropertyOfTypeInSpaceReflection.class).asListRemaining());
    }

    @CacheEvict("propertiesOfTypeInSpace")
    public void evictPropertiesOfTypeInSpaceCache(DataStage stage, SpaceName spaceName, String type) {
        logger.info(String.format("Cache evict: clearing cache for properties of type %s in space %s (stage %s)", type, spaceName, stage.name()));
    }


    @Cacheable("targetTypes")
    public List<TargetTypeReflection> reflectTargetTypes(DataStage stage, SpaceName spaceName, String type, String property) {
        logger.info(String.format("Missing cache hit: Reflecting target type of property %s of type %s in space %s (stage %s)", property, type, spaceName, stage.name()));
        final ArangoCollectionReference edgeCollection = new ArangoCollectionReference(property, true);
        //It's a property which actually does have target types
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust(String.format("FOR i IN @@collection FILTER @type IN i.`%s` AND i.@property != NULL", JsonLdConsts.TYPE)));
        bindVars.put("@collection", ArangoCollectionReference.fromSpace(spaceName).getCollectionName());
        bindVars.put("type", type);
        bindVars.put("property", property);
        aql.addLine(AQL.trust("LET targetType = FLATTEN("));
        aql.addLine(AQL.trust("       FOR target IN 1..1 OUTBOUND i @@propertyEdge"));
        bindVars.put("@propertyEdge", edgeCollection.getCollectionName());
        aql.addLine(AQL.trust("FILTER target != NULL AND target.`@type` != NULL"));
        aql.addLine(AQL.trust(String.format("FOR type IN target.`%s`", JsonLdConsts.TYPE)));
        aql.addLine(AQL.trust(String.format("COLLECT t = type, s=target.`%s` WITH COUNT INTO length", EBRAINSVocabulary.META_SPACE)));
        aql.addLine(AQL.trust("RETURN {"));
        aql.addLine(AQL.trust("    \"type\": t,"));
        aql.addLine(AQL.trust("    \"space\": s,"));
        aql.addLine(AQL.trust("    \"count\": length"));
        aql.addLine(AQL.trust("}"));
        aql.addLine(AQL.trust(" )"));
        aql.addLine(AQL.trust(" FOR t IN targetType"));
        aql.addLine(AQL.trust(" COLLECT type = t.type, space = t.space AGGREGATE count = SUM(t.count)"));
        aql.addLine(AQL.trust(" RETURN {"));
        aql.addLine(AQL.trust("\"name\": type,"));
        aql.addLine(AQL.trust("\"space\": space,"));
        aql.addLine(AQL.trust("\"occurrences\": count"));
        aql.addLine(AQL.trust("}"));
        return Collections.unmodifiableList(arangoDatabases.getByStage(stage).query(aql.build().getValue(), bindVars, TargetTypeReflection.class).asListRemaining());
    }

    @CacheEvict("targetTypes")
    public void evictTargetTypesCache(DataStage stage, SpaceName spaceName, String type, String property) {
        logger.info(String.format("Cache evict: clearing cache for target type of property %s of type %s in space %s (stage %s)", property, type, spaceName, stage.name()));
    }


    private final List<String> EDGE_BLACKLIST = Arrays.asList(
            new ArangoCollectionReference(EBRAINSVocabulary.META_ALTERNATIVE, true).getCollectionName(),
            new ArangoCollectionReference(EBRAINSVocabulary.META_USER, true).getCollectionName(),
            InternalSpace.DOCUMENT_ID_EDGE_COLLECTION.getCollectionName(),
            ArangoCollectionReference.fromSpace(InternalSpace.INFERENCE_OF_SPACE).getCollectionName(),
            InternalSpace.RELEASE_STATUS_EDGE_COLLECTION.getCollectionName(),
            InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName(),
            ArangoCollectionReference.fromSpace(InternalSpace.UNRESOLVED_SPACE).getCollectionName());

    public List<String> getAllRelevantEdges(DataStage stage) {
        final ArangoDatabase db = arangoDatabases.getByStage(stage);
        final Collection<CollectionEntity> collections = db.getCollections(new CollectionsReadOptions().excludeSystem(true));
        return collections.stream().filter(c -> c.getType().equals(CollectionType.EDGES)).map(CollectionEntity::getName).filter(c -> !EDGE_BLACKLIST.contains(c)).collect(Collectors.toList());
    }


    private ArangoCollectionReference createClientCollection(String ref, String client, boolean edge) {
        return new ArangoCollectionReference(String.format("%s_%s", client, ref), edge);
    }

    private ArangoCollectionReference clientTypesCollection(String client) {
        return createClientCollection("types", client, false);
    }

    private ArangoCollectionReference clientPropertiesCollection(String client) {
        return createClientCollection("properties", client, false);
    }

    private ArangoCollectionReference clientPropertyInTypeCollection(String client) {
        return createClientCollection("propertyInType", client, true);
    }

    private UUID spaceSpecificationRef(String spaceName) {
        return UUID.nameUUIDFromBytes((String.format("spaces/%s", spaceName)).getBytes(StandardCharsets.UTF_8));
    }

    private UUID typeSpecificationRef(String typeName) {
        return UUID.nameUUIDFromBytes((String.format("types/%s", typeName)).getBytes(StandardCharsets.UTF_8));
    }

    private UUID propertySpecificationRef(String propertyName) {
        return UUID.nameUUIDFromBytes((String.format("properties/%s", propertyName)).getBytes(StandardCharsets.UTF_8));
    }

    private UUID typeInSpaceSpecificationRef(String spaceName, String typeName) {
        return UUID.nameUUIDFromBytes((String.format("space/%s/types/%s", spaceName, typeName)).getBytes(StandardCharsets.UTF_8));
    }

    private UUID propertyInTypeSpecificationRef(String typeName, String propertyName) {
        return UUID.nameUUIDFromBytes((String.format("types/%s/properties/%s", typeName, propertyName)).getBytes(StandardCharsets.UTF_8));
    }

    public void createOrUpdateSpaceDocument(SpaceSpecification spaceSpecification) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        final ArangoCollection spaces = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, SPACES);
        final DynamicJson arangoDoc = jsonAdapter.fromJson(jsonAdapter.toJson(spaceSpecification), DynamicJson.class);
        arangoDoc.put(ArangoVocabulary.KEY, spaceSpecificationRef(spaceSpecification.getName()));
        spaces.insertDocument(arangoDoc, new DocumentCreateOptions().overwrite(true));
    }

    public void removeSpaceDocument(SpaceName spaceName) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        final ArangoCollection spaces = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, SPACES);
        final String id = spaceSpecificationRef(spaceName.getName()).toString();
        if (spaces.documentExists(id)) {
            spaces.deleteDocument(id);
        } else {
            logger.info(String.format("Was trying to remove document %s but it doesn't exist in collection %s", id, spaces.name()));
        }
    }

    public void addLinkBetweenSpaceAndType(SpaceName spaceName, String type) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        ArangoEdge edge = new ArangoEdge();
        edge.setFrom(new ArangoDocumentReference(SPACES, spaceSpecificationRef(spaceName.getName())));
        edge.setTo(new ArangoDocumentReference(TYPES, typeSpecificationRef(type)));
        edge.redefineId(new ArangoDocumentReference(TYPE_IN_SPACE, typeInSpaceSpecificationRef(spaceName.getName(), type)));
        final ArangoCollection typeInSpace = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, TYPE_IN_SPACE);
        typeInSpace.insertDocument(jsonAdapter.toJson(edge), new DocumentCreateOptions().overwrite(true));
    }

    public void removeLinkBetweenSpaceAndType(SpaceName spaceName, String type) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        final ArangoCollection typeInSpace = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, TYPE_IN_SPACE);
        final String id = typeInSpaceSpecificationRef(spaceName.getName(), type).toString();
        if (typeInSpace.documentExists(id)) {
            typeInSpace.deleteDocument(id);
        } else {
            logger.info(String.format("Was trying to remove document %s but it doesn't exist in collection %s", id, typeInSpace.name()));
        }
    }

    public void createOrUpdateTypeDocument(JsonLdId typeName, NormalizedJsonLd typeSpecification, SpaceName clientSpace) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        ArangoCollectionReference collection = clientSpace == null ? TYPES : clientTypesCollection(clientSpace.getName());
        final ArangoCollection types = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, collection);
        typeSpecification.put(ArangoVocabulary.KEY, typeSpecificationRef(typeName.getId()));
        typeSpecification.put(SchemaOrgVocabulary.IDENTIFIER, typeName.getId());
        types.insertDocument(typeSpecification, new DocumentCreateOptions().overwrite(true));
    }

    public void removeTypeDocument(JsonLdId typeName, SpaceName clientSpace) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        ArangoCollectionReference collection = clientSpace == null ? TYPES : clientTypesCollection(clientSpace.getName());
        final ArangoCollection types = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, collection);
        final String id = typeSpecificationRef(typeName.getId()).toString();
        if (types.documentExists(id)) {
            types.deleteDocument(id);
        } else {
            logger.info(String.format("Was trying to remove document %s but it doesn't exist in collection %s", id, types.name()));
        }
    }


    public void createOrUpdatePropertyDocument(JsonLdId propertyName, NormalizedJsonLd propertySpecification, SpaceName clientSpace) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        ArangoCollectionReference collection = clientSpace == null ? PROPERTIES : clientPropertiesCollection(clientSpace.getName());
        final ArangoCollection properties = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, collection);
        propertySpecification.put(ArangoVocabulary.KEY, propertySpecificationRef(propertyName.getId()));
        propertySpecification.put(SchemaOrgVocabulary.IDENTIFIER, propertyName.getId());
        properties.insertDocument(propertySpecification, new DocumentCreateOptions().overwrite(true));
    }

    public void removePropertyDocument(JsonLdId propertyName, SpaceName clientSpace) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        ArangoCollectionReference collection = clientSpace == null ? PROPERTIES : clientPropertiesCollection(clientSpace.getName());
        final ArangoCollection properties = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, collection);
        final String id = propertySpecificationRef(propertyName.getId()).toString();
        if (properties.documentExists(id)) {
            properties.deleteDocument(id);
        } else {
            logger.info(String.format("Was trying to remove document %s but it doesn't exist in collection %s", id, properties.name()));
        }

    }

    public void addLinkBetweenTypeAndProperty(String type, String property, NormalizedJsonLd payload, SpaceName clientSpace) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        payload.put(ArangoVocabulary.FROM, new ArangoDocumentReference(TYPES, typeSpecificationRef(type)).getId());
        payload.put(ArangoVocabulary.TO, new ArangoDocumentReference(PROPERTIES, propertySpecificationRef(property)).getId());
        payload.put(ArangoVocabulary.KEY, propertyInTypeSpecificationRef(type, property));
        payload.remove(ArangoVocabulary.ID);
        ArangoCollectionReference collection = clientSpace == null ? PROPERTY_IN_TYPE : clientPropertyInTypeCollection(clientSpace.getName());
        final ArangoCollection propertyInType = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, collection);
        propertyInType.insertDocument(payload, new DocumentCreateOptions().overwrite(true));
    }

    public void removeLinkBetweenTypeAndProperty(String type, String property, SpaceName clientSpace) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        ArangoCollectionReference collection = clientSpace == null ? PROPERTY_IN_TYPE : clientPropertyInTypeCollection(clientSpace.getName());
        final ArangoCollection propertyInType = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, collection);
        final String id = propertyInTypeSpecificationRef(type, property).toString();
        if (propertyInType.documentExists(id)) {
            propertyInType.deleteDocument(id);
        } else {
            logger.info(String.format("Was trying to remove document %s but it doesn't exist in collection %s", id, propertyInType.name()));
        }
    }

}

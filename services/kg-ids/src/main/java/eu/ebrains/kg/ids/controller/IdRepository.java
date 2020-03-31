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

package eu.ebrains.kg.ids.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.SkiplistIndexOptions;
import com.google.gson.Gson;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.Tuple;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.ids.model.PersistedId;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class IdRepository {
    private final ArangoDatabaseProxy arangoDatabase;

    private final Gson gson;

    private final IdUtils idUtils;

    public PersistedId getId(UUID uuid, DataStage stage){
        ArangoDatabase database = arangoDatabase.getOrCreate();
        ArangoCollection collection = database.collection(getCollectionName(stage));
        if(collection.exists()){
            String document = collection.getDocument(uuid.toString(), String.class);
            return gson.fromJson(document, PersistedId.class);
        }
        return null;
    }

    public IdRepository(ArangoDatabaseProxy arangoDatabase, Gson gson, IdUtils idUtils) {
        this.arangoDatabase = arangoDatabase;
        this.gson = gson;
        this.idUtils = idUtils;
    }

    /**
     * @return ids which have been merged newly into the persisted id
     */
    public List<JsonLdId> upsert(DataStage stage, PersistedId id) {
        Set<UUID> mergedIds = Collections.emptySet();
        ArangoCollection coll = getOrCreateCollection(stage);
        //TODO make this transactional
        if (stage == DataStage.LIVE) {
            PersistedId document = gson.fromJson(coll.getDocument(id.getId().toString(), String.class), PersistedId.class);
            //It could happen that identifiers disappear during updates. We need to make sure that the old identifiers are not lost though (getting rid of them is called "splitting" and is a separate process).
            if (document != null && document.getAlternativeIds() != null) {
                JsonLdId instanceId = idUtils.buildAbsoluteUrl(document.getId());
                List<String> alternativeIds = new ArrayList<>(id.getAlternativeIds());
                alternativeIds.addAll(document.getAlternativeIds());
                id.setAlternativeIds(alternativeIds.stream().filter(a -> !a.equals(instanceId.getId())).distinct().collect(Collectors.toSet()));
            }
            //It also could be that what has been an id before is now an alternative... we need to clean them up
            mergedIds = id.getAlternativeIds().stream()
                    //Only instance ids are of interest (we skip external ids)
                    .map(i -> idUtils.getUUID(JsonLdId.cast(i, null))).filter(Objects::nonNull)
                    .filter(i -> coll.documentExists(i.toString()))
                    .collect(Collectors.toSet());
            mergedIds.forEach(i -> coll.deleteDocument(i.toString()));
        }
        //Add the id in its fully qualified form as an alternative
        id.setAlternativeIds(new HashSet<>(id.getAlternativeIds() != null ? id.getAlternativeIds() : Collections.emptySet()));
        id.getAlternativeIds().add(idUtils.buildAbsoluteUrl(id.getId()).getId());
        coll.insertDocument(gson.toJson(id), new DocumentCreateOptions().waitForSync(true).overwrite(true));
        return mergedIds.stream().map(idUtils::buildAbsoluteUrl).collect(Collectors.toList());
    }

    public List<JsonLdIdMapping> resolveIds(DataStage stage, List<IdWithAlternatives> ids) {
        Set<Tuple<String, Space>> idsWithAlternatives = ids.stream().filter(Objects::nonNull).filter(id -> id.getAlternatives()!=null).map(id -> id.getAlternatives().stream().map(alternative -> new Tuple<String, Space>().setA(alternative).setB(id.getSpace()!=null ? new Space(id.getSpace()) : null)).collect(Collectors.toSet())).flatMap(Collection::stream).filter(Objects::nonNull).collect(Collectors.toSet());
        idsWithAlternatives.addAll(ids.stream().map(id -> id!=null ? new Tuple<String, Space>().setA(idUtils.buildAbsoluteUrl(id.getId()).getId()).setB(id.getSpace()!=null ? new Space(id.getSpace()) : null) : null).filter(Objects::nonNull).collect(Collectors.toSet()));
        String collectionName = getCollectionName(stage);
        ArangoDatabase database = arangoDatabase.getOrCreate();
        if (!database.collection(collectionName).exists() || idsWithAlternatives.isEmpty()) {
            return Collections.emptyList();
        }
        StringBuilder sb = new StringBuilder("FOR doc IN `").append(collectionName).append("` FILTER \n");
        Map<String, Object> bindVars = new HashMap<>();
        int counter = 0;

        for (Tuple<String, Space> searchKey : idsWithAlternatives) {
            if (counter > 0) {
                sb.append(" OR ");
            }
            if(searchKey.getB()!=null){
                sb.append("(");
            }
            sb.append("@identifier" + counter + " IN doc.alternativeIds");
            bindVars.put("identifier" + counter, searchKey.getA());
            if(searchKey.getB()!=null){
                sb.append(" AND doc._space==@space"+counter+")");
                bindVars.put("space"+counter, searchKey.getB().getName());
            }
            counter++;
        }
        sb.append("    RETURN doc\n");
        List<PersistedId> persistedIds = database.query(sb.toString(), bindVars, new AqlQueryOptions(), String.class).asListRemaining().stream().map(s -> gson.fromJson(s, PersistedId.class)).collect(Collectors.toList());
        Set<String> deprecatedInstances = persistedIds.stream().filter(p -> p.isDeprecated()).map(id -> idUtils.buildAbsoluteUrl(id.getId()).getId()).collect(Collectors.toSet());
        Map<String, Space> resultingSpaceByIdentifier = new HashMap<>();
        Map<String, Set<JsonLdId>> resultingIdsByIdentifier = new HashMap<>();
        persistedIds.forEach(id -> {
            JsonLdId absoluteId = idUtils.buildAbsoluteUrl(id.getId());
            resultingSpaceByIdentifier.put(absoluteId.getId(), id.getSpace());
            //List<JsonLdId> jsonLdIds = resultingIdsByIdentifier.computeIfAbsent(absoluteId.getId(), k -> new ArrayList<>());
            //jsonLdIds.add(absoluteId);
            if (id.getAlternativeIds() != null) {
                id.getAlternativeIds().forEach(alternativeId -> {
                    resultingSpaceByIdentifier.put(alternativeId, id.getSpace());
                    Set<JsonLdId> jsonLdIds2 = resultingIdsByIdentifier.computeIfAbsent(alternativeId, k -> new HashSet<>());
                    jsonLdIds2.add(absoluteId);
                });
            }
        });
        List<JsonLdIdMapping> mappings = new ArrayList<>();
        ids.forEach(id -> {
            JsonLdId absoluteId = idUtils.buildAbsoluteUrl(id.getId());
            if (absoluteId != null) {
                if (resultingIdsByIdentifier.containsKey(absoluteId.getId())) {
                    mappings.add(new JsonLdIdMapping(id.getId(), resultingIdsByIdentifier.get(absoluteId.getId()), resultingSpaceByIdentifier.get(absoluteId.getId()), deprecatedInstances.contains(absoluteId.getId())));
                } else if (id.getAlternatives() != null) {
                    for (String alternative : id.getAlternatives()) {
                        if (resultingIdsByIdentifier.containsKey(alternative)) {
                            Set<JsonLdId> resolvedIds = resultingIdsByIdentifier.get(alternative);
                            mappings.add(new JsonLdIdMapping(id.getId(), resolvedIds, resultingSpaceByIdentifier.get(alternative), resolvedIds.stream().anyMatch(resolvedId -> deprecatedInstances.contains(resolvedId.getId()))));
                            break;
                        }
                    }
                }
            }
        });
        return mappings;
    }


    private String getCollectionName(DataStage stage) {
        return stage.name().toLowerCase() + "_ids";
    }

    ArangoCollection getOrCreateCollection(DataStage stage) {
        ArangoCollection ids = arangoDatabase.getOrCreate().collection(getCollectionName(stage));
        if (!ids.exists()) {
            ids.create();
        }
        ids.ensureSkiplistIndex(Arrays.asList("alternativeIds[*]", JsonLdConsts.ID), new SkiplistIndexOptions());
        ids.ensureSkiplistIndex(Collections.singletonList(JsonLdConsts.ID), new SkiplistIndexOptions());
        return ids;
    }

}

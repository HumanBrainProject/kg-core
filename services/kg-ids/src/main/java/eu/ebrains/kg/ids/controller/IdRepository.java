/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
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

package eu.ebrains.kg.ids.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.SkiplistIndexOptions;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.Tuple;
import eu.ebrains.kg.commons.TypeUtils;
import eu.ebrains.kg.commons.exception.AmbiguousException;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.ids.model.PersistedId;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class IdRepository {
    private final ArangoDatabaseProxy arangoDatabase;

    private final IdsDBUtils idsDBUtils;

    private final JsonAdapter jsonAdapter;

    private final IdUtils idUtils;

    public PersistedId getId(UUID uuid, DataStage stage) {
        ArangoDatabase database = arangoDatabase.getOrCreate();
        ArangoCollection collection = database.collection(getCollectionName(stage));
        if (collection.exists()) {
            String document = collection.getDocument(uuid.toString(), String.class);
            return jsonAdapter.fromJson(document, PersistedId.class);
        }
        return null;
    }

    public IdRepository(@Qualifier("idsDB") ArangoDatabaseProxy arangoDatabase, JsonAdapter jsonAdapter, IdUtils idUtils, IdsDBUtils idsDBUtils) {
        this.arangoDatabase = arangoDatabase;
        this.jsonAdapter = jsonAdapter;
        this.idUtils = idUtils;
        this.idsDBUtils = idsDBUtils;
    }

    public void remove(DataStage stage, PersistedId id) {
        ArangoCollection coll = getOrCreateCollection(stage);
        if (coll.documentExists(id.getKey())) {
            coll.deleteDocument(id.getKey());
        }
    }

    public synchronized void upsert(DataStage stage, PersistedId id) {
        ArangoCollection coll = getOrCreateCollection(stage);
        //TODO make this transactional
        if (stage == DataStage.IN_PROGRESS) {
            PersistedId document = jsonAdapter.fromJson(coll.getDocument(id.getKey(), String.class), PersistedId.class);
            //It could happen that identifiers disappear during updates. We need to make sure that the old identifiers are not lost though (getting rid of them is called "splitting" and is a separate process).
            if (document != null && document.getAlternativeIds() != null) {
                JsonLdId instanceId = idUtils.buildAbsoluteUrl(document.getUUID());
                List<String> alternativeIds = new ArrayList<>(id.getAlternativeIds());
                alternativeIds.addAll(document.getAlternativeIds());
                id.setAlternativeIds(alternativeIds.stream().filter(a -> !a.equals(instanceId.getId())).distinct().collect(Collectors.toSet()));
            }
        }
        //Add the id in its fully qualified form as an alternative
        id.setAlternativeIds(new HashSet<>(id.getAlternativeIds() != null ? id.getAlternativeIds() : Collections.emptySet()));
        id.getAlternativeIds().add(idUtils.buildAbsoluteUrl(id.getUUID()).getId());
        coll.insertDocument(jsonAdapter.toJson(id), new DocumentCreateOptions().waitForSync(true).overwrite(true));
    }

    private List<PersistedId> fetchPersistedIdsByUUID(ArangoDatabase database, List<UUID> uuid, String collectionName){
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR id IN @ids"));
        bindVars.put("ids", uuid);
        aql.addLine(AQL.trust("LET d = DOCUMENT(@@collection, id)"));
        bindVars.put("@collection", collectionName);
        aql.addLine(AQL.trust("FILTER d != NULL"));
        aql.addLine(AQL.trust("RETURN d"));
        return database.query(aql.build().getValue(), bindVars, new AqlQueryOptions(), String.class).asListRemaining().stream().map(s->jsonAdapter.fromJson(s, PersistedId.class)).collect(Collectors.toList());
    }

    private List<PersistedId> fetchPersistedIdsByAlternativeId(ArangoDatabase database, List<Tuple<String, SpaceName>> ids, String collectionName){
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR doc IN @@collection FILTER"));
        bindVars.put("@collection", collectionName);
        int counter = 0;
        for (Tuple<String, SpaceName> searchKey : ids) {
            if (counter > 0) {
                aql.addLine(AQL.trust(" OR "));
            }
            if (searchKey.getB() != null) {
                aql.addLine(AQL.trust("("));
            }
            aql.addLine(AQL.trust("@identifier" + counter + " IN doc.alternativeIds"));
            bindVars.put("identifier" + counter, searchKey.getA());
            if (searchKey.getB() != null) {
                aql.addLine(AQL.trust(" AND doc._space==@space" + counter + ")"));
                bindVars.put("space" + counter, searchKey.getB().getName());
            }
            counter++;
        }
        aql.addLine(AQL.trust("RETURN doc"));
        return database.query(aql.build().getValue(), bindVars, new AqlQueryOptions(), String.class).asListRemaining().stream().map(s -> jsonAdapter.fromJson(s, PersistedId.class)).collect(Collectors.toList());
    }

    public InstanceId findInstanceByIdentifiers(DataStage stage, UUID uuid, List<String> identifiers) throws AmbiguousException{
        if(uuid!=null){
            identifiers.add(idUtils.buildAbsoluteUrl(uuid).getId());
        }
        ArangoDatabase database = arangoDatabase.getOrCreate();
        String collectionName = getCollectionName(stage);
        if (!database.collection(collectionName).exists() || CollectionUtils.isEmpty(identifiers)) {
            return null;
        }
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR i in @@collectionName FILTER"));
        bindVars.put("@collectionName", collectionName);
        for (int i = 0; i < identifiers.size(); i++) {
            aql.addLine(AQL.trust("@identifier"+i+" IN i.alternativeIds"));
            bindVars.put("identifier"+i, identifiers.get(i));
            if(i<identifiers.size()-1){
                aql.addLine(AQL.trust("OR"));
            }
        }
        aql.addLine(AQL.trust("RETURN i"));
        final List<PersistedId> persistedIds = database.query(aql.build().getValue(), bindVars, String.class).asListRemaining().stream().map(s -> jsonAdapter.fromJson(s, PersistedId.class)).collect(Collectors.toList());
        switch(persistedIds.size()) {
            case 0:
                return null;
            case 1:
                final PersistedId persistedId = persistedIds.get(0);
                return new InstanceId(persistedId.getUUID(), persistedId.getSpace());
            default:
                break;
        }
        throw new AmbiguousException(StringUtils.joinWith(", ", persistedIds.stream().map(p -> new InstanceId(p.getUUID(), p.getSpace()).serialize()).collect(Collectors.toList())));
    }


    public Map<UUID, InstanceId> resolveIds(DataStage stage, List<IdWithAlternatives> ids) {
        ArangoDatabase database = arangoDatabase.getOrCreate();
        String collectionName = getCollectionName(stage);
        Map<UUID, InstanceId> result = new HashMap<>();
        if (!database.collection(collectionName).exists() || CollectionUtils.isEmpty(ids)) {
            if(ids!=null){
                ids.forEach(id -> result.put(id.getId(), null));
            }
            return result;
        }
        //We first try to resolve by UUID since this can be done with way better performance...
        List<PersistedId> persistedIdsByUUID = fetchPersistedIdsByUUID(database, ids.stream().filter(Objects::nonNull).map(IdWithAlternatives::getId).filter(Objects::nonNull).collect(Collectors.toList()), collectionName);
        final Set<UUID> handledUUIDs = persistedIdsByUUID.stream().map(id -> {
            result.put(id.getUUID(), new InstanceId(id.getUUID(), id.getSpace()));
            return id.getUUID();
        }).collect(Collectors.toSet());

        //For those ids that weren't resolved via UUID, we go for the "by alternative" approach.
        final Set<IdWithAlternatives> remainingIds = ids.stream().filter(Objects::nonNull).filter(id -> id.getId() != null && !handledUUIDs.contains(id.getId()) && !CollectionUtils.isEmpty(id.getAlternatives())).collect(Collectors.toSet());
        if(remainingIds.isEmpty()){
            //We're done - don't do the extra steps.
            return result;
        }
        //Let's build one entry for each to find all possible combinations of the remaining ids...
        Set<Tuple<String, SpaceName>> idsWithAlternatives = remainingIds.stream().map(id -> id.getAlternatives().stream().map(alternative -> new Tuple<>(alternative, id.getSpace() != null ? new SpaceName(id.getSpace()) : null)).collect(Collectors.toSet())).flatMap(Collection::stream).filter(Objects::nonNull).collect(Collectors.toSet());
        idsWithAlternatives.addAll(remainingIds.stream().map(id -> new Tuple<>(idUtils.buildAbsoluteUrl(id.getId()).getId(), id.getSpace() != null ? new SpaceName(id.getSpace()) : null)).collect(Collectors.toSet()));

        //Depending on the size of the list, this can be overwhelming for the DB. Accordingly, we are going to run this in multiple steps...
        final List<PersistedId> persistedIdsByAlternativeIds = TypeUtils.splitList(new ArrayList<>(idsWithAlternatives), 2000).stream().map(i -> fetchPersistedIdsByAlternativeId(database, i, collectionName)).flatMap(Collection::stream).collect(Collectors.toList());
        Map<String, PersistedId> persistedIdsByAlternative = new HashMap<>();
        persistedIdsByAlternativeIds.forEach(p -> {
            persistedIdsByAlternative.put(idUtils.buildAbsoluteUrl(p.getUUID()).getId(), p);
            p.getAlternativeIds().forEach(a -> persistedIdsByAlternative.put(a, p));
        });
        remainingIds.forEach(id -> {
            PersistedId foundId = persistedIdsByAlternative.get(idUtils.buildAbsoluteUrl(id.getId()).getId());
            if(foundId==null){
                foundId = id.getAlternatives().stream().map(persistedIdsByAlternative::get).filter(Objects::nonNull).findFirst().orElse(null);
            }
            result.put(id.getId(), foundId != null ? new InstanceId(foundId.getUUID(), foundId.getSpace()) : null);
        });
        return result;
    }

    private String getCollectionName(DataStage stage) {
        return stage.name().toLowerCase() + "_ids";
    }

    ArangoCollection getOrCreateCollection(DataStage stage) {
        ArangoCollection ids = idsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(getCollectionName(stage), false));
        ids.ensureSkiplistIndex(Arrays.asList("alternativeIds[*]", JsonLdConsts.ID), new SkiplistIndexOptions());
        ids.ensureSkiplistIndex(Collections.singletonList(JsonLdConsts.ID), new SkiplistIndexOptions());
        return ids;
    }

}

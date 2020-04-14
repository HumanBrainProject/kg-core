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

package eu.ebrains.kg.graphdb.instances.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.CollectionCreateOptions;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.conventions.InternalCollections;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.HBPVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.instances.model.ArangoRelation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ArangoRepositoryInstances {

    private final ArangoRepositoryCommons arangoRepositoryCommons;
    private final PermissionsController permissionsController;
    private final AuthContext authContext;

    private final ArangoDatabases databases;

    private final IdUtils idUtils;

    public ArangoRepositoryInstances(ArangoRepositoryCommons arangoRepositoryCommons, ArangoDatabases databases, IdUtils idUtils, PermissionsController permissionsController, AuthContext authContext) {
        this.arangoRepositoryCommons = arangoRepositoryCommons;
        this.databases = databases;
        this.permissionsController = permissionsController;
        this.authContext = authContext;
        this.idUtils = idUtils;
    }

    public NormalizedJsonLd getInstance(DataStage stage, Space space, UUID id, boolean embedded, boolean removeInternalProperties, boolean alternatives) {
        ArangoDocument document = arangoRepositoryCommons.getDocument(stage, ArangoCollectionReference.fromSpace(space).doc(id));
        if (document == null) {
            return null;
        }
        List<NormalizedJsonLd> singleDoc = Collections.singletonList(document.getDoc());
        handleAlternativesAndEmbedded(singleDoc, stage, alternatives, embedded);
        exposeRevision(singleDoc);
        if (removeInternalProperties) {
            document.getDoc().removeAllInternalProperties();
        }
        return document.getDoc();
    }

    private void exposeRevision(List<NormalizedJsonLd> documents) {
        documents.forEach(doc -> doc.put(EBRAINSVocabulary.META_REVISION, doc.get(ArangoVocabulary.REV)));
    }

    private void handleAlternativesAndEmbedded(List<NormalizedJsonLd> documents, DataStage stage, boolean alternatives, boolean embedded) {
        if (alternatives) {
            List<NormalizedJsonLd[]> embeddedDocuments = getEmbeddedDocuments(documents, stage, true);
            resolveUsersForAlternatives(stage, embeddedDocuments);
            for (NormalizedJsonLd[] embeddedDocument : embeddedDocuments) {
                Arrays.stream(embeddedDocument).forEach(e -> {
                    e.remove(EBRAINSVocabulary.META_REVISION);
                });
            }
            addEmbeddedInstancesToDocument(documents, embeddedDocuments);
        } else {
            documents.forEach(d -> d.remove(EBRAINSVocabulary.META_ALTERNATIVE));
        }
        if (embedded) {
            resolveUsersForDocuments(stage, documents);
            addEmbeddedInstancesToDocument(documents, getEmbeddedDocuments(documents, stage, false));
        }
    }

    private void resolveUsersForDocuments(DataStage stage, List<NormalizedJsonLd> documents) {
        List<ArangoDocumentReference> userIdsToResolve = documents.stream()
                .filter(e -> e.containsKey(EBRAINSVocabulary.META_USER) && e.get(EBRAINSVocabulary.META_USER) != null)
                .map(d -> {
                    Map<String, Object> userObj = d.getAs(EBRAINSVocabulary.META_USER, Map.class);
                    UUID uuid = idUtils.getUUID(new JsonLdId(userObj.get(JsonLdConsts.ID).toString()));
                    return ArangoCollectionReference.fromSpace(InternalSpace.USERS_SPACE).doc(uuid);
                }).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<UUID, Result<NormalizedJsonLd>> usersById = getDocumentsByReferenceList(DataStage.NATIVE, userIdsToResolve, false, false);
        documents.forEach(e -> {
            if (e.containsKey(EBRAINSVocabulary.META_USER)) {
                Map<String, Object> userObj = e.getAs(EBRAINSVocabulary.META_USER, Map.class);
                if (userObj.containsKey(JsonLdConsts.ID)) {
                    UUID uuid = idUtils.getUUID(new JsonLdId(userObj.get(JsonLdConsts.ID).toString()));
                    Result<NormalizedJsonLd> userResult = null;
                    if (uuid != null) {
                        userResult = usersById.get(uuid);
                    }
                    NormalizedJsonLd user = userResult != null && userResult.getData() != null ? userResult.getData() : null;
                    NormalizedJsonLd reducedUser = new NormalizedJsonLd();
                    reducedUser.put(SchemaOrgVocabulary.NAME, user != null ? user.get(SchemaOrgVocabulary.NAME) : "Unknown");
                    reducedUser.put(SchemaOrgVocabulary.ALTERNATE_NAME, user != null ? user.get(SchemaOrgVocabulary.ALTERNATE_NAME) : "unknown");
                    reducedUser.put(SchemaOrgVocabulary.IDENTIFIER, user != null ? user.get(SchemaOrgVocabulary.IDENTIFIER) : Collections.emptyList());
                    e.put(EBRAINSVocabulary.META_USER, reducedUser);
                }
            }
        });
    }

    private void resolveUsersForAlternatives(DataStage stage, List<NormalizedJsonLd[]> alternatives) {
        List<ArangoDocumentReference> userIdsToResolve = alternatives.stream().flatMap(Arrays::stream).map(d ->
                d.keySet().stream().filter(k -> !NormalizedJsonLd.isInternalKey(k) && !JsonLdConsts.isJsonLdConst(k)).map(k -> {
                    List<NormalizedJsonLd> alternative;
                    try {
                        alternative = d.getAsListOf(k, NormalizedJsonLd.class);
                    } catch (IllegalArgumentException e) {
                        alternative = Collections.emptyList();
                    }
                    return alternative != null ? alternative.stream().map(a -> a.getAsListOf(EBRAINSVocabulary.META_USER, String.class)).flatMap(List::stream).collect(Collectors.toList()) : Collections.<String>emptyList();
                }).flatMap(List::stream).collect(Collectors.toList())).flatMap(List::stream).map(id -> ArangoCollectionReference.fromSpace(InternalSpace.USERS_SPACE).doc(idUtils.getUUID(new JsonLdId(id)))).distinct().collect(Collectors.toList());
        //We always resolve users in native space, since users are only maintained there...
        Map<UUID, Result<NormalizedJsonLd>> usersById = getDocumentsByReferenceList(DataStage.NATIVE, userIdsToResolve, false, false);
        alternatives.stream().flatMap(Arrays::stream).forEach(d ->
                d.keySet().stream().filter(k -> !NormalizedJsonLd.isInternalKey(k) && !JsonLdConsts.isJsonLdConst(k)).forEach(k -> {
                    Object obj = d.get(k);
                    if (obj instanceof Collection) {
                        ((Collection<?>) obj).forEach(o -> {
                            if (o instanceof Map) {
                                Map alternative = (Map) o;
                                List<Object> users = new NormalizedJsonLd(alternative).getAsListOf(EBRAINSVocabulary.META_USER, String.class).stream().map(id -> {
                                    UUID uuid = idUtils.getUUID(new JsonLdId(id));
                                    Result<NormalizedJsonLd> userResult = null;
                                    if (uuid != null) {
                                        userResult = usersById.get(uuid);
                                    }
                                    NormalizedJsonLd user = userResult != null && userResult.getData() != null ? userResult.getData() : null;
                                    NormalizedJsonLd reducedUser = new NormalizedJsonLd();
                                    //We only expose the necessary subset of user information.
                                    reducedUser.put(SchemaOrgVocabulary.NAME, user != null ? user.get(SchemaOrgVocabulary.NAME) : "Unknown");
                                    reducedUser.put(SchemaOrgVocabulary.ALTERNATE_NAME, user != null ? user.get(SchemaOrgVocabulary.ALTERNATE_NAME) : "unknown");
                                    reducedUser.put(SchemaOrgVocabulary.IDENTIFIER, user != null ? user.get(SchemaOrgVocabulary.IDENTIFIER) : Collections.emptyList());
                                    return reducedUser;
                                }).collect(Collectors.toList());
                                alternative.put(EBRAINSVocabulary.META_USER, users);
                            }
                        });
                    }
                }));
    }

    public Map<UUID, Result<NormalizedJsonLd>> getDocumentsByReferenceList(DataStage stage, List<ArangoDocumentReference> documentReferences, boolean embedded, boolean alternatives) {
        ArangoDatabase db = databases.getByStage(stage);
        AQL aql = new AQL().addLine(AQL.trust("RETURN {"));
        int counter = 0;
        Map<String, Object> bindVars = new HashMap<>();

        for (ArangoDocumentReference reference : documentReferences) {
            bindVars.put("doc" + counter, reference.getId());
            aql.addLine(AQL.trust("\"" + reference.getDocumentId() + "\": DOCUMENT(@doc" + counter + ")"));
            counter++;
            if (counter < documentReferences.size()) {
                aql.add(AQL.trust(", "));
            }
        }
        aql.addLine(AQL.trust("}"));
        Map<UUID, Result<NormalizedJsonLd>> result = new HashMap<>();
        List<NormalizedJsonLd> results = db.query(aql.build().getValue(), bindVars, ArangoRepositoryCommons.EMPTY_QUERY_OPTIONS, NormalizedJsonLd.class).asListRemaining().stream().filter(Objects::nonNull).collect(Collectors.toList());
        List<NormalizedJsonLd> normalizedJsonLds = new ArrayList<>();
        if (!results.isEmpty()) {
            // The response object is always just a single dictionary
            NormalizedJsonLd singleResult = results.get(0);
            for (String uuid : singleResult.keySet()) {
                NormalizedJsonLd doc = singleResult.getAs(uuid, NormalizedJsonLd.class);
                if (doc != null) {
                    normalizedJsonLds.add(doc);
                    result.put(UUID.fromString(uuid), Result.ok(doc));
                } else {
                    result.put(UUID.fromString(uuid), Result.nok(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase()));
                }
            }
        }
        handleAlternativesAndEmbedded(normalizedJsonLds, stage, alternatives, embedded);
        exposeRevision(normalizedJsonLds);
        normalizedJsonLds.forEach(NormalizedJsonLd::removeAllInternalProperties);
        return result;
    }

    public <T, V> Map<T, Result<V>> mergeRequestedIdWithPayload(List<T> key, List<V> normalizedJsonLds) {
        if (key.size() != normalizedJsonLds.size()) {
            throw new IllegalArgumentException(String.format("Tried to merge two lists of different sizes (keys: %d - %s, docs: %d)", key.size(), key.stream().map(Object::toString).collect(Collectors.joining(", ")), normalizedJsonLds.size()));
        }
        Map<T, Result<V>> result = new TreeMap<>();
        for (int i = 0; i < key.size(); i++) {
            V value = normalizedJsonLds.get(i);
            result.put(key.get(i), value == null ? Result.nok(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase()) : Result.ok(value));
        }
        return result;
    }

    public Map<UUID, Result<NormalizedJsonLd>> getDocumentsByIdList(DataStage stage, List<InstanceId> instanceIds, boolean embedded, boolean alternatives) {
        return getDocumentsByReferenceList(stage, instanceIds.stream().map(ArangoDocumentReference::fromInstanceId).collect(Collectors.toList()), embedded, alternatives);
    }

    public Paginated<SuggestedLink> getSuggestionsByTypes(DataStage stage, List<Type> types, PaginationParam paginationParam, String search, List<UUID> excludeIds) {
        //Suggestions are special in terms of permissions: We even allow instances to show up which are in spaces the user doesn't have read access for. This is only acceptable because we're returning a restricted result with minimal information
        if (types == null || types.isEmpty()) {
            return new Paginated<>();
        }
        Map<String, Object> bindVars = new HashMap<>();
        AQL aql = new AQL();
        iterateThroughTypeList(types, bindVars, aql);
        aql.indent().addLine(AQL.trust("FOR v IN 1..1 OUTBOUND typeDefinition.type @@typeRelationCollection"));
        aql.addLine(AQL.trust("FILTER v." + ArangoVocabulary.KEY + " NOT IN @excludeIds"));
        bindVars.put("excludeIds", excludeIds);
        aql.addLine(AQL.trust("LET attributes = ( FOR name IN typeDefinition.labelProperties"));
        aql.addLine(AQL.trust("RETURN {name: name, value: v[name]} )"));
        addSearchLabelFilter(bindVars, aql, search);
        aql.addLine(AQL.trust("LET attWithMeta = APPEND(attributes, [{name: \"" + JsonLdConsts.ID + "\", value: v.`" + JsonLdConsts.ID + "`}, {name: \"" + EBRAINSVocabulary.LABEL + "\", value: CONCAT_SEPARATOR(\" \", (FOR name IN typeDefinition.labelProperties RETURN v[name]))},  {name: \"" + EBRAINSVocabulary.META_TYPE + "\", value: typeDefinition.typeName}, {name: \"" + EBRAINSVocabulary.META_SPACE + "\", value: v.`" + EBRAINSVocabulary.META_SPACE + "`}])"));
        aql.addPagination(paginationParam);
        aql.addLine(AQL.trust("RETURN ZIP(attWithMeta[*].name, attWithMeta[*].value)"));
        bindVars.put("@typeRelationCollection", InternalCollections.TYPE_EDGE_COLLECTION.getCollectionName());
        Paginated<NormalizedJsonLd> normalizedJsonLdPaginated = arangoRepositoryCommons.queryDocuments(databases.getByStage(stage), new AQLQuery(aql, bindVars));
        List<SuggestedLink> links = normalizedJsonLdPaginated.getData().stream().map(payload -> {
            SuggestedLink link = new SuggestedLink();
            link.setId(idUtils.getUUID(payload.getId()));
            link.setLabel(payload.getAs(EBRAINSVocabulary.LABEL, String.class, null));
            link.setType(payload.getAs(EBRAINSVocabulary.META_TYPE, String.class, null));
            link.setSpace(payload.getAs(EBRAINSVocabulary.META_SPACE, String.class, null));
            return link;
        }).collect(Collectors.toList());
        return new Paginated<>(links, normalizedJsonLdPaginated.getTotalResults(), normalizedJsonLdPaginated.getSize(), normalizedJsonLdPaginated.getFrom());
    }

    private void addSearchLabelFilter(Map<String, Object> bindVars, AQL aql, String search) {
        if (search != null && !search.isBlank()) {
            List<String> searchLabels = Arrays.stream(search.trim().split(" ")).filter(s -> !s.isBlank()).map(s -> s.replaceAll("%", "") + "%").collect(Collectors.toList());
            if (!searchLabels.isEmpty()) {
                aql.addLine(AQL.trust("LET found = (FOR name IN typeDefinition.labelProperties FILTER "));
                for (int i = 0; i < searchLabels.size(); i++) {
                    aql.addLine(AQL.trust("LIKE(v[name], @search" + i + ", true) "));
                    if (i < searchLabels.size() - 1) {
                        aql.add(AQL.trust("OR "));
                    }
                    bindVars.put("search" + i, searchLabels.get(i));
                }
                aql.addLine(AQL.trust("RETURN name) "));
                aql.addLine(AQL.trust("FILTER LENGTH(found)>=" + searchLabels.size()));
            }
        }
    }

    private void iterateThroughTypeList(List<Type> types, Map<String, Object> bindVars, AQL aql) {
        aql.addLine(AQL.trust("FOR typeDefinition IN ["));
        ArangoCollectionReference typeCollection = ArangoCollectionReference.fromSpace(InternalCollections.TYPE_SPACE);
        bindVars.put("@typeCollection", typeCollection.getCollectionName());
        for (int i = 0; i < types.size(); i++) {
            aql.addLine(AQL.trust(" {typeName: @typeName" + i + ", type: DOCUMENT(@@typeCollection, @documentId" + i + "), labelProperties: @labelProperties" + i + "}"));
            bindVars.put("documentId" + i, typeCollection.docWithStableId(types.get(i).getName()).getDocumentId().toString());
            bindVars.put("labelProperties" + i, types.get(i).getLabelProperties());
            bindVars.put("typeName" + i, types.get(i).getName());
            if (i < types.size() - 1) {
                aql.add(AQL.trust(","));
            }
        }
        aql.addLine(AQL.trust("]"));
    }

    public List<String> getDocumentIdsBySpace(Space space) {
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR doc IN @@space"));
        bindVars.put("@space", ArangoCollectionReference.fromSpace(space).getCollectionName());
        aql.addLine(AQL.trust("FILTER doc." + IndexedJsonLdDoc.EMBEDDED + " == NULL"));
        aql.addLine(AQL.trust("RETURN doc.`" + IndexedJsonLdDoc.DOCUMENT_ID + "`"));
        return databases.getByStage(DataStage.NATIVE).query(aql.build().getValue(), bindVars, aql.getQueryOptions(), String.class).asListRemaining();
    }


    public Paginated<NormalizedJsonLd> getDocumentsByTypes(DataStage stage, List<Type> typesWithLabelInfo, PaginationParam paginationParam, String search, boolean embedded, boolean alternatives) {
        //TODO find label field for type (and client) and filter by search if set.
        ArangoDatabase database = databases.getByStage(stage);
        if (database.collection(InternalCollections.TYPE_EDGE_COLLECTION.getCollectionName()).exists()) {
            Map<String, Object> bindVars = new HashMap<>();
            AQL aql = new AQL();
            iterateThroughTypeList(typesWithLabelInfo, bindVars, aql);
            Map<String, Object> whitelistFilter = permissionsController.whitelistFilter(authContext.getUserWithRoles(), stage);
            if (whitelistFilter != null) {
                aql.specifyWhitelist();
                bindVars.putAll(whitelistFilter);
            }
            aql.indent().addLine(AQL.trust("FOR v IN 1..1 OUTBOUND typeDefinition.type @@typeRelationCollection"));
            if (whitelistFilter != null) {
                aql.addDocumentFilterWithWhitelistFilter(AQL.trust("v"));
            }
            addSearchLabelFilter(bindVars, aql, search);
            aql.addPagination(paginationParam);
            aql.addLine(AQL.trust("RETURN v"));
            bindVars.put("@typeRelationCollection", InternalCollections.TYPE_EDGE_COLLECTION.getCollectionName());
            Paginated<NormalizedJsonLd> normalizedJsonLdPaginated = arangoRepositoryCommons.queryDocuments(database, new AQLQuery(aql, bindVars));
            handleAlternativesAndEmbedded(normalizedJsonLdPaginated.getData(), stage, alternatives, embedded);
            exposeRevision(normalizedJsonLdPaginated.getData());
            normalizedJsonLdPaginated.getData().forEach(NormalizedJsonLd::removeAllInternalProperties);
            return normalizedJsonLdPaginated;
        }
        return new Paginated<>(Collections.emptyList(), 0, 0, 0);
    }

    public Paginated<NormalizedJsonLd> getQueriesByRootType(DataStage stage, PaginationParam paginationParam, String search, boolean embedded, boolean alternatives, String typeFilter) {
        ArangoDatabase database = databases.getByStage(stage);
        if (database.collection(InternalCollections.TYPE_EDGE_COLLECTION.getCollectionName()).exists()) {
            Map<String, Object> bindVars = new HashMap<>();
            AQL aql = new AQL();
            iterateThroughTypeList(Collections.singletonList(new Type(KgQuery.getKgQueryType())), bindVars, aql);
            aql.indent().addLine(AQL.trust("FOR v IN 1..1 OUTBOUND typeDefinition.type @@typeRelationCollection"));
            if (typeFilter != null && !typeFilter.isBlank()) {
                aql.addLine(AQL.trust("FILTER v.`" + HBPVocabulary.GRAPH_QUERY_META + "`.`" + HBPVocabulary.GRAPH_QUERY_ROOT_TYPE + "` == @typeFilter"));
                bindVars.put("typeFilter", typeFilter);
            }
            if (search != null && !search.isBlank()) {
                aql.addLine(AQL.trust("FILTER LIKE(v.`" + HBPVocabulary.GRAPH_QUERY_META + "`.`" + HBPVocabulary.GRAPH_QUERY_NAME + "`, @search, true)"));
                bindVars.put("search", "%" + search + "%");
            }
            aql.addPagination(paginationParam);
            aql.addLine(AQL.trust("RETURN v"));
            bindVars.put("@typeRelationCollection", InternalCollections.TYPE_EDGE_COLLECTION.getCollectionName());
            Paginated<NormalizedJsonLd> normalizedJsonLdPaginated = arangoRepositoryCommons.queryDocuments(database, new AQLQuery(aql, bindVars));
            handleAlternativesAndEmbedded(normalizedJsonLdPaginated.getData(), stage, alternatives, embedded);
            exposeRevision(normalizedJsonLdPaginated.getData());
            normalizedJsonLdPaginated.getData().forEach(NormalizedJsonLd::removeAllInternalProperties);
            return normalizedJsonLdPaginated;
        }
        return new Paginated<>(Collections.emptyList(), 0, 0, 0);
    }


    public List<NormalizedJsonLd> getDocumentsByIncomingRelation(DataStage stage, Space space, UUID id, ArangoRelation relation, boolean useOriginalTo, boolean embedded, boolean alternatives) {
        return getDocumentsByRelation(stage, space, id, relation, true, useOriginalTo, embedded, alternatives);
    }

    public List<NormalizedJsonLd> getDocumentsByOutgoingRelation(DataStage stage, Space space, UUID id, ArangoRelation relation, boolean embedded, boolean alternatives) {
        return getDocumentsByRelation(stage, space, id, relation, false, false, embedded, alternatives);
    }

    private List<NormalizedJsonLd> getDocumentsByRelation(DataStage stage, Space space, UUID id, ArangoRelation relation, boolean incoming, boolean useOriginalTo, boolean embedded, boolean alternatives) {
        ArangoDatabase db = databases.getByStage(stage);

        ArangoCollectionReference relationColl;
        if (relation.isInternal()) {
            relationColl = ArangoCollectionReference.fromSpace(new InternalSpace(relation.getRelationField()), true);
        } else {
            relationColl = new ArangoCollectionReference(relation.getRelationField(), true);
        }
        ArangoCollectionReference documentSpace = ArangoCollectionReference.fromSpace(space);
        if (documentSpace != null && db.collection(relationColl.getCollectionName()).exists() && db.collection(documentSpace.getCollectionName()).exists()) {
            String aql = "LET docs = (FOR d IN @@relation\n" +
                    "    FILTER d." + (incoming ? useOriginalTo ? IndexedJsonLdDoc.ORIGINAL_TO : ArangoVocabulary.TO : ArangoVocabulary.FROM) + " == @id \n" +
                    "    LET doc = DOCUMENT(d." + (incoming ? ArangoVocabulary.FROM : ArangoVocabulary.TO) + ") \n" +
                    "    FILTER IS_SAME_COLLECTION(@@space, doc) \n" +
                    "    RETURN doc) \n" +
                    "    FOR doc IN docs" +
                    "       RETURN DISTINCT doc";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("@relation", relationColl.getCollectionName());
            bindVars.put("@space", documentSpace.getCollectionName());
            bindVars.put("id", useOriginalTo ? idUtils.buildAbsoluteUrl(id).getId() : space.getName() + "/" + id);
            List<NormalizedJsonLd> result = db.query(aql, bindVars, ArangoRepositoryCommons.EMPTY_QUERY_OPTIONS, NormalizedJsonLd.class).asListRemaining();
            handleAlternativesAndEmbedded(result, stage, alternatives, embedded);
            exposeRevision(result);
            return result;
        }
        return Collections.emptyList();
    }

    public List<NormalizedJsonLd> getDocumentsBySharedIdentifiers(DataStage stage, Space space, UUID id, boolean embedded, boolean alternatives) {
        ArangoDatabase db = databases.getByStage(stage);
        ArangoCollectionReference collectionReference = ArangoCollectionReference.fromSpace(space);
        if (db.collection(collectionReference.getCollectionName()).exists()) {
            //Because Arango doesn't support indexed filtering of array in array search, we expand the (very limited) list of identifiers of the root document and state them explicitly as individual filter elements. This way, the index applies and we profit from more speed.
            NormalizedJsonLd rootDocument = db.collection(collectionReference.getCollectionName()).getDocument(id.toString(), NormalizedJsonLd.class);
            if (rootDocument != null) {
                List<NormalizedJsonLd> result = getDocumentsByIdentifiers(rootDocument.getAllIdentifiersIncludingId(), stage, space, embedded, alternatives);
                if (result != null) return result;
            }
        }
        return Collections.emptyList();
    }

    public List<NormalizedJsonLd> getDocumentsByIdentifiers(Set<String> allIdentifiersIncludingId, DataStage stage, Space space, boolean embedded, boolean alternatives) {
        ArangoDatabase db = databases.getByStage(stage);
        ArangoCollectionReference collectionReference = ArangoCollectionReference.fromSpace(space);
        if (!db.collection(collectionReference.getCollectionName()).exists()) {
            return Collections.emptyList();
        }
        if (allIdentifiersIncludingId != null && !allIdentifiersIncludingId.isEmpty()) {
            Iterator<String> identifiers = allIdentifiersIncludingId.iterator();
            AQL aql = new AQL();
            HashMap<String, Object> bindVars = new HashMap<>();
            aql.addLine(AQL.trust("FOR doc IN @@space"));
            bindVars.put("@space", collectionReference.getCollectionName());
            aql.addLine(AQL.trust("FILTER @firstIdentifier IN doc." + IndexedJsonLdDoc.IDENTIFIERS));
            bindVars.put("firstIdentifier", identifiers.next());
            int identifierCnt = 0;
            while (identifiers.hasNext()) {
                aql.addLine(AQL.trust("OR @id" + identifierCnt + " IN doc." + IndexedJsonLdDoc.IDENTIFIERS));
                bindVars.put("id" + identifierCnt, identifiers.next());
                identifierCnt++;
            }
            aql.addLine(AQL.trust("RETURN doc"));
            List<NormalizedJsonLd> result = db.query(aql.build().getValue(), bindVars, ArangoRepositoryCommons.EMPTY_QUERY_OPTIONS, NormalizedJsonLd.class).asListRemaining();
            handleAlternativesAndEmbedded(result, stage, alternatives, embedded);
            exposeRevision(result);
            return result;
        }
        return null;
    }

    public ReleaseStatus getReleaseStatus(Space space, UUID id, ReleaseTreeScope treeScope) {

        switch (treeScope) {
            case TOP_INSTANCE_ONLY:
                return getTopInstanceReleaseStatus(space, id);
            case CHILDREN_ONLY:
//                return getChildrenReleaseStatus(space, id);
            default:
                throw new RuntimeException("Release tree scope unknown");
        }
    }

    private ReleaseStatus getTopInstanceReleaseStatus(Space space, UUID id) {
        ArangoDatabase db = databases.getByStage(DataStage.LIVE);
        ArangoCollectionReference collectionReference = ArangoCollectionReference.fromSpace(space);
        ArangoCollectionReference releaseStatusCollection = InternalSpace.RELEASE_STATUS_EDGE_COLLECTION;
        ArangoCollection collection = db.collection(collectionReference.getCollectionName());
        if (!collection.exists() || !collection.documentExists(id.toString())) {
            return null;
        }
        if (!db.collection(releaseStatusCollection.getCollectionName()).exists()) {
            db.createCollection(releaseStatusCollection.getCollectionName(), new CollectionCreateOptions().type(CollectionType.EDGES));
        }
        String aql = "LET doc = DOCUMENT(@@space, @id) " +
                "FOR v  IN 1..1 INBOUND  doc @@releaseStatusCollection " +
                "RETURN v.`" + SchemaOrgVocabulary.NAME + "`";
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("@space", collectionReference.getCollectionName());
        bindVars.put("@releaseStatusCollection", releaseStatusCollection.getCollectionName());
        bindVars.put("id", id);
        List<String> data = db.query(aql, bindVars, ArangoRepositoryCommons.EMPTY_QUERY_OPTIONS, String.class).asListRemaining();
        if (data.isEmpty()) {
            return ReleaseStatus.UNRELEASED;
        } else {
            try {
                return ReleaseStatus.valueOf(data.get(0));
            } catch (IllegalArgumentException e) {
                return ReleaseStatus.UNRELEASED;
            }
        }
    }


    void addEmbeddedInstancesToDocument(List<NormalizedJsonLd> documentsToBeResolved, List<NormalizedJsonLd[]> embeddedDocuments) {
        for (int i = 0; i < documentsToBeResolved.size(); i++) {
            Map<String, NormalizedJsonLd> embeddedDocs = Arrays.stream(embeddedDocuments.get(i)).filter(Objects::nonNull).collect(Collectors.toMap(e -> e.getId().getId(), e -> e));
            embeddedDocs.values().forEach(e -> {
                e.removeAllInternalProperties();
                e.remove(JsonLdConsts.ID);
            });
            NormalizedJsonLd originalDocument = documentsToBeResolved.get(i);
            mergeEmbeddedDocuments(originalDocument, embeddedDocs);
        }
    }

    private List<NormalizedJsonLd[]> getEmbeddedDocuments(List<NormalizedJsonLd> documentsToBeResolved, DataStage stage, boolean alternatives) {
        List<String> ids = documentsToBeResolved.stream().map(doc -> ArangoDocument.from(doc).getId().getId()).collect(Collectors.toList());
        AQL aql = new AQL();
        aql.addLine(AQL.trust("FOR id IN @idlist"));
        aql.addLine(AQL.trust("LET embedded = (FOR docId IN 1..1 INBOUND DOCUMENT(id) @@documentIdRelationCollection"));
        aql.indent().addLine(AQL.trust("FOR partdoc IN 1..1 OUTBOUND docId @@documentIdRelationCollection"));
        aql.indent().addLine(AQL.trust("FILTER partdoc._id != id"));
        aql.addLine(AQL.trust("FILTER !HAS(partdoc, \"" + ArangoVocabulary.FROM + "\")"));
        if (alternatives) {
            aql.addLine(AQL.trust("FILTER HAS(partdoc, \"" + IndexedJsonLdDoc.ALTERNATIVE + "\") AND partdoc." + IndexedJsonLdDoc.ALTERNATIVE));
        } else {
            aql.addLine(AQL.trust("FILTER !HAS(partdoc, \"" + IndexedJsonLdDoc.ALTERNATIVE + "\")"));
        }
        aql.addLine(AQL.trust("FILTER HAS(partdoc, \"" + IndexedJsonLdDoc.EMBEDDED + "\") AND partdoc." + IndexedJsonLdDoc.EMBEDDED));
        aql.addLine(AQL.trust("RETURN partdoc)")).outdent();
        aql.addLine(AQL.trust("RETURN embedded"));
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("@documentIdRelationCollection", InternalSpace.DOCUMENT_ID_EDGE_COLLECTION.getCollectionName());
        bindVars.put("idlist", ids);
        return databases.getByStage(stage).query(aql.build().getValue(), bindVars, ArangoRepositoryCommons.EMPTY_QUERY_OPTIONS, NormalizedJsonLd[].class).asListRemaining();
    }

    void mergeEmbeddedDocuments(NormalizedJsonLd originalDocument, Map<String, NormalizedJsonLd> embeddedById) {
        for (String k : originalDocument.keySet()) {
            Object value = originalDocument.get(k);
            if (value instanceof Collection) {
                ArrayList<Object> temporaryCollection = new ArrayList<>();
                ((Collection<?>) value).forEach(v -> {
                    handleMergeOfPropertyValue(originalDocument, embeddedById, k, v, temporaryCollection);
                });
                originalDocument.put(k, temporaryCollection);
            } else {
                handleMergeOfPropertyValue(originalDocument, embeddedById, k, value, null);
            }
        }
    }

    private void handleMergeOfPropertyValue(NormalizedJsonLd originalDocument, Map<String, NormalizedJsonLd> embeddedById, String k, Object value, ArrayList<Object> temporaryCollection) {
        if (value instanceof Map && ((Map) value).containsKey(JsonLdConsts.ID)) {
            Object reference = ((Map) value).get(JsonLdConsts.ID);
            if (reference instanceof String) {
                String ref = (String) reference;
                if (embeddedById.containsKey(ref)) {
                    NormalizedJsonLd embeddedDoc = embeddedById.get(ref);
                    mergeEmbeddedDocuments(embeddedDoc, embeddedById);
                    if (temporaryCollection != null) {
                        temporaryCollection.add(embeddedDoc);
                    } else {
                        originalDocument.put(k, embeddedDoc);
                    }
                    return;
                }
            }
        }
        if (temporaryCollection != null) {
            temporaryCollection.add(value);
        }
    }

}

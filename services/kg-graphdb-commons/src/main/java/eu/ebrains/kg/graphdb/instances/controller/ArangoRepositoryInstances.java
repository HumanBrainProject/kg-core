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
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.CollectionsReadOptions;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.exception.AmbiguousException;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.markers.*;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permissions.controller.PermissionSvc;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.controller.ArangoUtils;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.instances.model.ArangoRelation;
import eu.ebrains.kg.graphdb.queries.controller.QueryController;
import eu.ebrains.kg.graphdb.queries.model.spec.GraphQueryKeys;
import eu.ebrains.kg.graphdb.types.controller.ArangoRepositoryTypes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ArangoRepositoryInstances {

    private final ArangoRepositoryCommons arangoRepositoryCommons;
    private final PermissionsController permissionsController;
    private final PermissionSvc permissionSvc;
    private final AuthContext authContext;
    private final ArangoUtils arangoUtils;
    private final QueryController queryController;
    private final ArangoRepositoryTypes typesRepo;
    private final ArangoDatabases databases;
    private final IdUtils idUtils;

    public ArangoRepositoryInstances(ArangoRepositoryCommons arangoRepositoryCommons, PermissionsController permissionsController, PermissionSvc permissionSvc, AuthContext authContext, ArangoUtils arangoUtils, QueryController queryController, ArangoRepositoryTypes typesRepo, ArangoDatabases databases, IdUtils idUtils) {
        this.arangoRepositoryCommons = arangoRepositoryCommons;
        this.permissionsController = permissionsController;
        this.permissionSvc = permissionSvc;
        this.authContext = authContext;
        this.arangoUtils = arangoUtils;
        this.queryController = queryController;
        this.typesRepo = typesRepo;
        this.databases = databases;
        this.idUtils = idUtils;
    }


    @ExposesData
    public NormalizedJsonLd getInstance(DataStage stage, SpaceName space, UUID id, boolean embedded, boolean removeInternalProperties, boolean alternatives) {
        if (!permissionSvc.hasPermission(authContext.getUserWithRoles(), Functionality.MINIMAL_READ, space, id)) {
            throw new ForbiddenException(String.format("You don't have read rights on the instance with the id %s", id));
        }
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
        NormalizedJsonLd doc = document.getDoc();
        if (doc != null && !permissionSvc.hasPermission(authContext.getUserWithRoles(), Functionality.READ, space, id)) {
            //The user doesn't have read rights - we need to restrict the information to minimal data
            doc.keepPropertiesOnly(getMinimalFields(stage, doc.types()));
        }
        return doc;
    }

    private Set<String> getMinimalFields(DataStage stage, List<String> types) {
        Set<String> keepProperties = typesRepo.getTypeInformation(authContext.getUserWithRoles().getClientId(), stage, types.stream().map(Type::new).collect(Collectors.toList())).stream().map(Type::getLabelProperty).collect(Collectors.toSet());
        keepProperties.add(JsonLdConsts.ID);
        keepProperties.add(JsonLdConsts.TYPE);
        keepProperties.add(EBRAINSVocabulary.META_SPACE);
        return keepProperties;
    }

    private void exposeRevision(List<NormalizedJsonLd> documents) {
        documents.forEach(doc -> doc.put(EBRAINSVocabulary.META_REVISION, doc.get(ArangoVocabulary.REV)));
    }

    private void handleAlternativesAndEmbedded(List<NormalizedJsonLd> documents, DataStage stage, boolean alternatives, boolean embedded) {
        if (alternatives) {
            List<NormalizedJsonLd[]> embeddedDocuments = getEmbeddedDocuments(documents, stage, true);
            resolveUsersForAlternatives(embeddedDocuments);
            for (NormalizedJsonLd[] embeddedDocument : embeddedDocuments) {
                Arrays.stream(embeddedDocument).forEach(e -> {
                    e.remove(EBRAINSVocabulary.META_REVISION);
                    //We don't need the space field of embedded instances since it's redundant
                    e.remove(EBRAINSVocabulary.META_SPACE);
                });
            }
            addEmbeddedInstancesToDocument(documents, embeddedDocuments);
        } else {
            documents.forEach(d -> d.remove(EBRAINSVocabulary.META_ALTERNATIVE));
        }
        if (embedded) {
            resolveUsersForDocuments(documents);
            addEmbeddedInstancesToDocument(documents, getEmbeddedDocuments(documents, stage, false));
        }
    }

    private void resolveUsersForDocuments(List<NormalizedJsonLd> documents) {
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
                    String absoluteId = userObj.get(JsonLdConsts.ID).toString();
                    UUID uuid = idUtils.getUUID(new JsonLdId(absoluteId));
                    Result<NormalizedJsonLd> userResult = null;
                    if (uuid != null) {
                        userResult = usersById.get(uuid);
                    }
                    NormalizedJsonLd user = userResult != null && userResult.getData() != null ? userResult.getData() : null;
                    NormalizedJsonLd reducedUser = new NormalizedJsonLd();
                    reducedUser.put(SchemaOrgVocabulary.NAME, user != null ? user.get(SchemaOrgVocabulary.NAME) : "Unknown");
                    reducedUser.put(SchemaOrgVocabulary.ALTERNATE_NAME, user != null ? user.get(SchemaOrgVocabulary.ALTERNATE_NAME) : "unknown");
                    reducedUser.put(SchemaOrgVocabulary.IDENTIFIER, user != null ? user.get(SchemaOrgVocabulary.IDENTIFIER) : Collections.emptyList());
                    reducedUser.put(JsonLdConsts.ID, absoluteId);
                    e.put(EBRAINSVocabulary.META_USER, reducedUser);
                }
            }
        });
    }

    private void resolveUsersForAlternatives(List<NormalizedJsonLd[]> alternatives) {
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
                                    reducedUser.put(JsonLdConsts.ID, id);
                                    return reducedUser;
                                }).collect(Collectors.toList());
                                alternative.put(EBRAINSVocabulary.META_USER, users);
                            }
                        });
                    }
                }));
    }

    private Map<UUID, Result<NormalizedJsonLd>> getDocumentsByReferenceList(DataStage stage, List<ArangoDocumentReference> documentReferences, boolean embedded, boolean alternatives) {
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
        List<NormalizedJsonLd> results = db.query(aql.build().getValue(), bindVars, new AqlQueryOptions(), NormalizedJsonLd.class).asListRemaining().stream().filter(Objects::nonNull).collect(Collectors.toList());
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

    @ExposesData
    public Map<UUID, Result<NormalizedJsonLd>> getDocumentsByIdList(DataStage stage, List<InstanceId> instanceIds, boolean embedded, boolean alternatives) {
        UserWithRoles userWithRoles = authContext.getUserWithRoles();

        Set<InstanceId> hasReadPermissions = instanceIds.stream().filter(i -> permissionSvc.hasPermission(userWithRoles, permissionsController.getReadFunctionality(stage), i.getSpace(), i.getUuid())).collect(Collectors.toSet());
        Set<InstanceId> hasOnlyMinimalReadPermissions = instanceIds.stream().filter(i -> !hasReadPermissions.contains(i) && permissionSvc.hasPermission(userWithRoles, permissionsController.getMinimalReadFunctionality(stage), i.getSpace(), i.getUuid())).collect(Collectors.toSet());
        Set<InstanceId> hasNoPermissions = instanceIds.stream().filter(i -> !hasReadPermissions.contains(i) && !hasOnlyMinimalReadPermissions.contains(i)).collect(Collectors.toSet());

        Map<UUID, Result<NormalizedJsonLd>> documentsByReferenceList = getDocumentsByReferenceList(stage, hasReadPermissions.stream().map(ArangoDocumentReference::fromInstanceId).collect(Collectors.toList()), embedded, alternatives);
        Map<UUID, Result<NormalizedJsonLd>> documentsByReferenceListWithMinimalReadAccess = getDocumentsByReferenceList(stage, hasOnlyMinimalReadPermissions.stream().map(ArangoDocumentReference::fromInstanceId).collect(Collectors.toList()), false, false);

        //Reduce the payload to the minimal fields
        documentsByReferenceListWithMinimalReadAccess.values().forEach(d -> d.getData().keepPropertiesOnly(getMinimalFields(stage, d.getData().types())));
        documentsByReferenceList.putAll(documentsByReferenceListWithMinimalReadAccess);

        //Define responses for no-permission instances
        hasNoPermissions.forEach(i -> {
            documentsByReferenceList.put(i.getUuid(), Result.nok(HttpStatus.FORBIDDEN.value(), String.format("You don't have rights to read id %s", i.getUuid())));
        });
        return documentsByReferenceList;
    }

    @ExposesData
    //FIXME: Do we want to return suggested links for RELEASED stage?
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
        addSearchFilter(bindVars, aql, search);
        aql.addLine(AQL.trust("LET attWithMeta = [{name: \"" + JsonLdConsts.ID + "\", value: v.`" + JsonLdConsts.ID + "`}, {name: \"" + EBRAINSVocabulary.LABEL + "\", value: v[typeDefinition.labelProperty]},  {name: \"" + EBRAINSVocabulary.META_TYPE + "\", value: typeDefinition.typeName}, {name: \"" + EBRAINSVocabulary.META_SPACE + "\", value: v.`" + EBRAINSVocabulary.META_SPACE + "`}]"));
        aql.addPagination(paginationParam);
        aql.addLine(AQL.trust("RETURN ZIP(attWithMeta[*].name, attWithMeta[*].value)"));
        bindVars.put("@typeRelationCollection", InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName());
        Paginated<NormalizedJsonLd> normalizedJsonLdPaginated = arangoRepositoryCommons.queryDocuments(databases.getByStage(stage), new AQLQuery(aql, bindVars));
        List<SuggestedLink> links = normalizedJsonLdPaginated.getData().stream().map(payload -> {
            SuggestedLink link = new SuggestedLink();
            link.setId(idUtils.getUUID(payload.id()));
            link.setLabel(payload.getAs(EBRAINSVocabulary.LABEL, String.class, null));
            link.setType(payload.getAs(EBRAINSVocabulary.META_TYPE, String.class, null));
            link.setSpace(payload.getAs(EBRAINSVocabulary.META_SPACE, String.class, null));
            return link;
        }).collect(Collectors.toList());
        return new Paginated<>(links, normalizedJsonLdPaginated.getTotalResults(), normalizedJsonLdPaginated.getSize(), normalizedJsonLdPaginated.getFrom());
    }

    private void addSearchFilter(Map<String, Object> bindVars, AQL aql, String search) {
        if (search != null && !search.isBlank()) {
            List<String> searchTerms = Arrays.stream(search.trim().split(" ")).filter(s -> !s.isBlank()).map(s -> "%" + s.replaceAll("%", "") + "%").collect(Collectors.toList());
            if (!searchTerms.isEmpty()) {
                //TODO Search also for searchable properties - not only the label and id property
                aql.addLine(AQL.trust("LET found = (FOR name IN [typeDefinition.labelProperty, \"" + ArangoVocabulary.KEY + "\"] FILTER "));
                for (int i = 0; i < searchTerms.size(); i++) {
                    aql.addLine(AQL.trust("LIKE(v[name], @search" + i + ", true) "));
                    if (i < searchTerms.size() - 1) {
                        aql.add(AQL.trust("AND "));
                    }
                    bindVars.put("search" + i, searchTerms.get(i));
                }
                aql.addLine(AQL.trust("RETURN name) "));
                aql.addLine(AQL.trust("FILTER LENGTH(found)>=1"));
            }
        }
    }

    private void iterateThroughTypeList(List<Type> types, Map<String, Object> bindVars, AQL aql) {
        aql.addLine(AQL.trust("FOR typeDefinition IN ["));
        ArangoCollectionReference typeCollection = ArangoCollectionReference.fromSpace(InternalSpace.TYPE_SPACE);
        bindVars.put("@typeCollection", typeCollection.getCollectionName());
        for (int i = 0; i < types.size(); i++) {
            aql.addLine(AQL.trust(" {typeName: @typeName" + i + ", type: DOCUMENT(@@typeCollection, @documentId" + i + "), labelProperty: @labelProperty" + i + "}"));
            bindVars.put("documentId" + i, typeCollection.docWithStableId(types.get(i).getName()).getDocumentId().toString());
            bindVars.put("labelProperty" + i, types.get(i).getLabelProperty());
            bindVars.put("typeName" + i, types.get(i).getName());
            if (i < types.size() - 1) {
                aql.add(AQL.trust(","));
            }
        }
        aql.addLine(AQL.trust("]"));
    }

    @ExposesIds
    public List<String> getDocumentIdsBySpace(SpaceName space) {
        //FIXME: Shouldn't those be restricted to the ones we have at least minimal read access rights?
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR doc IN @@space"));
        bindVars.put("@space", ArangoCollectionReference.fromSpace(space).getCollectionName());
        aql.addLine(AQL.trust("FILTER doc." + IndexedJsonLdDoc.EMBEDDED + " == NULL"));
        aql.addLine(AQL.trust("RETURN doc.`" + IndexedJsonLdDoc.DOCUMENT_ID + "`"));
        return databases.getByStage(DataStage.NATIVE).query(aql.build().getValue(), bindVars, aql.getQueryOptions(), String.class).asListRemaining();
    }


    @ExposesData
    public Paginated<NormalizedJsonLd> getDocumentsByTypes(DataStage stage, List<Type> typesWithLabelInfo, SpaceName space, PaginationParam paginationParam, String search, boolean embedded, boolean alternatives) {
        //TODO find label field for type (and client) and filter by search if set.
        ArangoDatabase database = databases.getByStage(stage);
        if (database.collection(InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName()).exists()) {
            Map<String, Object> bindVars = new HashMap<>();
            AQL aql = new AQL();
            iterateThroughTypeList(typesWithLabelInfo, bindVars, aql);
            Map<String, Object> whitelistFilter = permissionsController.whitelistFilterForReadInstances(authContext.getUserWithRoles(), stage);
            if (whitelistFilter != null) {
                aql.specifyWhitelist();
                bindVars.putAll(whitelistFilter);
            }
            aql.indent().addLine(AQL.trust("FOR v IN 1..1 OUTBOUND typeDefinition.type @@typeRelationCollection"));
            if (whitelistFilter != null) {
                aql.addDocumentFilterWithWhitelistFilter(AQL.trust("v"));
            }
            if (space != null) {
                aql.addLine(AQL.trust("FILTER v." + ArangoVocabulary.COLLECTION + " == @spaceFilter"));
                bindVars.put("spaceFilter", ArangoCollectionReference.fromSpace(space).getCollectionName());
            }
            addSearchFilter(bindVars, aql, search);
            aql.addPagination(paginationParam);
            aql.addLine(AQL.trust("RETURN v"));
            bindVars.put("@typeRelationCollection", InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName());
            Paginated<NormalizedJsonLd> normalizedJsonLdPaginated = arangoRepositoryCommons.queryDocuments(database, new AQLQuery(aql, bindVars));
            handleAlternativesAndEmbedded(normalizedJsonLdPaginated.getData(), stage, alternatives, embedded);
            exposeRevision(normalizedJsonLdPaginated.getData());
            normalizedJsonLdPaginated.getData().forEach(NormalizedJsonLd::removeAllInternalProperties);
            return normalizedJsonLdPaginated;
        }
        return new Paginated<>(Collections.emptyList(), 0, 0, 0);
    }

    @ExposesQuery
    public Paginated<NormalizedJsonLd> getQueriesByRootType(DataStage stage, PaginationParam paginationParam, String search, boolean embedded, boolean alternatives, String typeFilter) {
        ArangoDatabase database = databases.getByStage(stage);
        if (database.collection(InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName()).exists()) {
            Map<String, Object> bindVars = new HashMap<>();
            AQL aql = new AQL();
            iterateThroughTypeList(Collections.singletonList(new Type(KgQuery.getKgQueryType())), bindVars, aql);
            aql.indent().addLine(AQL.trust("FOR v IN 1..1 OUTBOUND typeDefinition.type @@typeRelationCollection"));
            if (typeFilter != null && !typeFilter.isBlank()) {
                aql.addLine(AQL.trust("FILTER v.`" + GraphQueryKeys.GRAPH_QUERY_META.getFieldName() + "`.`" + GraphQueryKeys.GRAPH_QUERY_TYPE.getFieldName() + "` == @typeFilter"));
                bindVars.put("typeFilter", typeFilter);
            }
            if (search != null && !search.isBlank()) {
                aql.addLine(AQL.trust("FILTER LIKE(v.`" + GraphQueryKeys.GRAPH_QUERY_META.getFieldName() + "`.`" + GraphQueryKeys.GRAPH_QUERY_NAME.getFieldName() + "`, @search, true)"));
                bindVars.put("search", "%" + search + "%");
            }
            aql.addPagination(paginationParam);
            aql.addLine(AQL.trust("RETURN v"));
            bindVars.put("@typeRelationCollection", InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName());
            Paginated<NormalizedJsonLd> normalizedJsonLdPaginated = arangoRepositoryCommons.queryDocuments(database, new AQLQuery(aql, bindVars));
            handleAlternativesAndEmbedded(normalizedJsonLdPaginated.getData(), stage, alternatives, embedded);
            exposeRevision(normalizedJsonLdPaginated.getData());
            normalizedJsonLdPaginated.getData().forEach(NormalizedJsonLd::removeAllInternalProperties);
            return normalizedJsonLdPaginated;
        }
        return new Paginated<>(Collections.emptyList(), 0, 0, 0);
    }


    @ExposesData
    public List<NormalizedJsonLd> getDocumentsByIncomingRelation(DataStage stage, SpaceName space, UUID id, ArangoRelation relation, boolean useOriginalTo, boolean embedded, boolean alternatives) {
        return getDocumentsByRelation(stage, space, id, relation, true, useOriginalTo, embedded, alternatives);
    }

    @ExposesMinimalData
    public GraphEntity getNeighbors(DataStage stage, SpaceName space, UUID id) {
        if(!permissionSvc.hasPermission(authContext.getUserWithRoles(), permissionsController.getReadFunctionality(stage), space, id)){
            throw new ForbiddenException();
        }
        //FIXME: Do we have to restrict this to the instances with minimal read access?
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        ArangoDatabase db = databases.getByStage(stage);
        Set<String> edgeCollections = db.getCollections(new CollectionsReadOptions().excludeSystem(true)).stream().filter(c ->
                //We're only interested in edges
                c.getType() == CollectionType.EDGES &&
                        //We want to exclude meta properties
                        !c.getName().startsWith(ArangoCollectionReference.fromSpace(new SpaceName(EBRAINSVocabulary.META), true).getCollectionName()) &&
                        //And we want to exclude the internal ones...
                        !InternalSpace.INTERNAL_NON_META_EDGES.contains(new ArangoCollectionReference(c.getName(), true))
        ).map(c -> AQL.preventAqlInjection(c.getName()).getValue()).collect(Collectors.toSet());


        //The edges are injection-safe since they have been checked beforehand - so we can trust these values.
        String edges = String.join(", ", edgeCollections);

        //For now, we're hardcoding the number of investigated levels for simplicity. this could be done differently if we want to make it parametrized
        aql.addLine(AQL.trust("LET doc = DOCUMENT(@id)"));
        bindVars.put("id", String.format("%s/%s", ArangoCollectionReference.fromSpace(space).getCollectionName(), id));

        //TODO use dynamic name label
        if (!edgeCollections.isEmpty()) {
            aql.addLine(AQL.trust("LET inbnd = (FOR inbnd IN 1..1 INBOUND doc " + edges));
            aql.addLine(AQL.trust("    RETURN { \"id\": inbnd._key, \"name\": inbnd.`http://schema.org/name`, \"types\": inbnd.`@type`, \"space\": inbnd.`" + EBRAINSVocabulary.META_SPACE + "`})"));
            aql.addLine(AQL.trust("LET outbnd = (FOR outbnd IN 1..1 OUTBOUND doc " + edges));
            aql.addLine(AQL.trust("    LET outbnd2 = (FOR outbnd2 IN 1..1 OUTBOUND outbnd " + edges));
            aql.addLine(AQL.trust("    RETURN {\"id\": outbnd2._key, \"name\": outbnd2.`http://schema.org/name`, \"types\": outbnd2.`@type`, \"space\": outbnd2.`" + EBRAINSVocabulary.META_SPACE + "`})"));
            aql.addLine(AQL.trust("    RETURN {\"id\": outbnd._key,  \"name\": outbnd.`http://schema.org/name`, \"outbound\": outbnd2, \"types\": outbnd.`@type`, \"space\": outbnd.`" + EBRAINSVocabulary.META_SPACE + "` })"));
        } else {
            aql.addLine(AQL.trust("LET inbnd = []"));
            aql.addLine(AQL.trust("LET outbnd = []"));
        }
        aql.addLine(AQL.trust("RETURN {\"id\": doc._key, \"name\": doc.`http://schema.org/name`, \"inbound\" : inbnd, \"outbound\": outbnd, \"types\": doc.`@type`, \"space\": doc.`" + EBRAINSVocabulary.META_SPACE + "` }"));

        List<GraphEntity> graphEntities = db.query(aql.build().getValue(), bindVars, new AqlQueryOptions(), GraphEntity.class).asListRemaining();
        if (graphEntities.isEmpty()) {
            return null;
        } else if (graphEntities.size() == 1) {
            return graphEntities.get(0);
        } else {
            throw new AmbiguousException(String.format("Did find multiple instances for the id %s", id));
        }

    }

    @ExposesData
    public List<NormalizedJsonLd> getDocumentsByOutgoingRelation(DataStage stage, SpaceName space, UUID id, ArangoRelation relation, boolean embedded, boolean alternatives) {
        return getDocumentsByRelation(stage, space, id, relation, false, false, embedded, alternatives);
    }

    private List<NormalizedJsonLd> getDocumentsByRelation(DataStage stage, SpaceName space, UUID id, ArangoRelation relation, boolean incoming, boolean useOriginalTo, boolean embedded, boolean alternatives) {
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
            List<NormalizedJsonLd> result = db.query(aql, bindVars, new AqlQueryOptions(), NormalizedJsonLd.class).asListRemaining();
            handleAlternativesAndEmbedded(result, stage, alternatives, embedded);
            exposeRevision(result);
            return result;
        }
        return Collections.emptyList();
    }

    @ExposesData
    public List<NormalizedJsonLd> getDocumentsBySharedIdentifiers(DataStage stage, SpaceName space, UUID id, boolean embedded, boolean alternatives) {
        ArangoDatabase db = databases.getByStage(stage);
        ArangoCollectionReference collectionReference = ArangoCollectionReference.fromSpace(space);
        if (db.collection(collectionReference.getCollectionName()).exists()) {
            //Because Arango doesn't support indexed filtering of array in array search, we expand the (very limited) list of identifiers of the root document and state them explicitly as individual filter elements. This way, the index applies and we profit from more speed.
            NormalizedJsonLd rootDocument = db.collection(collectionReference.getCollectionName()).getDocument(id.toString(), NormalizedJsonLd.class);
            if (rootDocument != null) {
                List<NormalizedJsonLd> result = doGetDocumentsByIdentifiers(rootDocument.allIdentifiersIncludingId(), stage, space);
                if (result != null) {
                    handleAlternativesAndEmbedded(result, stage, alternatives, embedded);
                    return result;
                }
            }
        }
        return Collections.emptyList();
    }

    @ExposesData
    public List<NormalizedJsonLd> getDocumentsByIdentifiers(Set<String> allIdentifiersIncludingId, DataStage stage, SpaceName space, boolean embedded, boolean alternatives) {
        List<NormalizedJsonLd> normalizedJsonLds = doGetDocumentsByIdentifiers(allIdentifiersIncludingId, stage, space);
        handleAlternativesAndEmbedded(normalizedJsonLds, stage, alternatives, embedded);
        return normalizedJsonLds;
    }

    private List<NormalizedJsonLd> doGetDocumentsByIdentifiers(Set<String> allIdentifiersIncludingId, DataStage stage, SpaceName space) {
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
            List<NormalizedJsonLd> result = db.query(aql.build().getValue(), bindVars, new AqlQueryOptions(), NormalizedJsonLd.class).asListRemaining();
            exposeRevision(result);
            return result;
        }
        return null;
    }

    private Set<InstanceId> fetchInvolvedInstances(ScopeElement element, Set<InstanceId> collector) {
        collector.add(new InstanceId(element.getId(), new SpaceName(element.getSpace())));
        if (element.getChildren() != null) {
            element.getChildren().forEach(c -> fetchInvolvedInstances(c, collector));
        }
        return collector;
    }

    @ExposesReleaseStatus
    public ReleaseStatus getReleaseStatus(SpaceName space, UUID id, ReleaseTreeScope treeScope) {
        if(!permissionSvc.hasPermission(authContext.getUserWithRoles(), Functionality.RELEASE_STATUS, space, id)){
            throw new ForbiddenException();
        }
        switch (treeScope) {
            case TOP_INSTANCE_ONLY:
                return getTopInstanceReleaseStatus(space, id);
            case CHILDREN_ONLY:
                //FIXME restrict exposed release status based on permissions.
                ScopeElement scopeForInstance = getScopeForInstance(space, id, DataStage.IN_PROGRESS, false);
                if (scopeForInstance.getChildren() == null || scopeForInstance.getChildren().isEmpty()) {
                    return null;
                }
                Set<InstanceId> instanceIds = fetchInvolvedInstances(scopeForInstance, new HashSet<>());
                //Ignore top instance
                instanceIds.remove(new InstanceId(id, space));
                AQL aql = new AQL();
                aql.addLine(AQL.trust("FOR id IN @ids"));
                Map<String, Object> bindVars = new HashMap<>();
                bindVars.put("ids", instanceIds.stream().map(InstanceId::serialize).collect(Collectors.toList()));
                aql.addLine(AQL.trust("LET doc = DOCUMENT(id)"));
                aql.addLine(AQL.trust("LET status = FIRST((FOR v IN 1..1 INBOUND  doc @@releaseStatusCollection"));
                bindVars.put("@releaseStatusCollection", InternalSpace.RELEASE_STATUS_EDGE_COLLECTION.getCollectionName());
                aql.addLine(AQL.trust("RETURN v.`" + SchemaOrgVocabulary.NAME + "`))"));
                aql.addLine(AQL.trust("RETURN status"));
                ArangoDatabase db = databases.getByStage(DataStage.IN_PROGRESS);
                List<String> status = db.query(aql.build().getValue(), bindVars, String.class).asListRemaining();
                if (status.contains("null") || status.contains(ReleaseStatus.UNRELEASED.name())) {
                    return ReleaseStatus.UNRELEASED;
                } else if (status.contains(ReleaseStatus.HAS_CHANGED.name())) {
                    return ReleaseStatus.HAS_CHANGED;
                } else {
                    return ReleaseStatus.RELEASED;
                }
            default:
                throw new RuntimeException("Release tree scope unknown");
        }
    }

    private ReleaseStatus getTopInstanceReleaseStatus(SpaceName space, UUID id) {
        ArangoDatabase db = databases.getByStage(DataStage.IN_PROGRESS);
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
        List<String> data = db.query(aql, bindVars, new AqlQueryOptions(), String.class).asListRemaining();
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
            Map<String, NormalizedJsonLd> embeddedDocs = Arrays.stream(embeddedDocuments.get(i)).filter(Objects::nonNull).collect(Collectors.toMap(e -> e.id().getId(), e -> e));
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
        ArangoDatabase database = databases.getByStage(stage);
        arangoUtils.getOrCreateArangoCollection(database, InternalSpace.DOCUMENT_ID_EDGE_COLLECTION);
        return database.query(aql.build().getValue(), bindVars, new AqlQueryOptions(), NormalizedJsonLd[].class).asListRemaining();
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

    @ExposesMinimalData
    public Map<UUID, String> getLabelsForInstances(DataStage stage, Set<InstanceId> ids, List<Type> types) {
        if (types == null) {
            throw new IllegalArgumentException("No types are defined - please make sure you provide the list of types");
        }
        Map<String, Object> bindVars = new HashMap<>();
        AQL aql = new AQL();
        aql.addLine(AQL.trust("LET types = {"));
        List<Type> reducedListOfTypes = types.stream().filter(t -> t.getLabelProperty() != null).collect(Collectors.toList());
        for (int i = 0; i < reducedListOfTypes.size(); i++) {
            Type type = reducedListOfTypes.get(i);
            aql.addLine(AQL.trust("\"" + AQL.preventAqlInjection(type.getName()).getValue() + "\": @labelProperty" + i));
            bindVars.put("labelProperty" + i, type.getLabelProperty());
            if (i < reducedListOfTypes.size() - 1) {
                aql.add(AQL.trust(","));
            }
        }
        aql.addLine(AQL.trust("}"));

        aql.addLine(AQL.trust("RETURN MERGE(FOR id IN @ids"));
        bindVars.put("ids", ids.stream().map(InstanceId::serialize).collect(Collectors.toSet()));
        aql.indent().addLine(AQL.trust("LET doc = DOCUMENT(id)"));
        aql.addLine(AQL.trust("FILTER doc != null"));
        aql.addLine(AQL.trust("FILTER doc.`" + JsonLdConsts.TYPE + "`"));
        aql.addLine(AQL.trust("LET labelProperty = FIRST(NOT_NULL(FOR t IN doc.`" + JsonLdConsts.TYPE + "` LET p = types[t] FILTER p!=NULL SORT p asc RETURN p))"));
        aql.addLine(AQL.trust("FILTER labelProperty!= null"));
        aql.addLine(AQL.trust("LET label = doc[labelProperty]"));
        aql.addLine(AQL.trust("RETURN {[ id ] : label})"));

        List<JsonLdDoc> results = databases.getByStage(stage).query(aql.build().getValue(), bindVars, new AqlQueryOptions(), JsonLdDoc.class).asListRemaining();
        if (results.size() == 1) {
            JsonLdDoc map = results.get(0);
            Map<UUID, String> result = new HashMap<>();
            map.keySet().stream().filter(Objects::nonNull).forEach(k -> {
                UUID uuid = InstanceId.deserialize(k).getUuid();
                result.put(uuid, map.getAs(k, String.class));
            });
            return result;
        }
        return Collections.emptyMap();

    }

    @ExposesMinimalData
    //FIXME reduce to minimal data permission
    public ScopeElement getScopeForInstance(SpaceName space, UUID id, DataStage stage, boolean fetchLabels) {
        //get instance
        NormalizedJsonLd instance = getInstance(stage, space, id, false, false, false);
        //get scope relevant queries
        //TODO filter user defined queries (only take client queries into account)
        Stream<NormalizedJsonLd> typeQueries = instance.types().stream().map(type -> getQueriesByRootType(stage, null, null, false, false, type).getData()).flatMap(Collection::stream);
        List<NormalizedJsonLd> results = typeQueries.map(q -> queryController.query(authContext.getUserWithRoles(), new KgQuery(q, stage).setIdRestrictions(Collections.singletonList(new EntityId(id.toString()))), null, null, true).getData()).flatMap(Collection::stream).collect(Collectors.toList());
        return translateResultToScope(results, stage, fetchLabels, instance);
    }

    private ScopeElement handleSubElement(NormalizedJsonLd data, Map<String, Set<ScopeElement>> typeToUUID) {
        String id = data.getAs("id", String.class);
        UUID uuid = idUtils.getUUID(new JsonLdId(id));
        List<ScopeElement> children = data.keySet().stream().filter(k -> k.startsWith("dependency_")).map(k ->
                data.getAsListOf(k, NormalizedJsonLd.class).stream().map(d -> handleSubElement(d, typeToUUID)).collect(Collectors.toList())
        ).flatMap(Collection::stream).collect(Collectors.toList());
        List<String> type = data.getAsListOf("type", String.class);
        ScopeElement element = new ScopeElement(uuid, type, children.isEmpty() ? null : children, data.getAs("internalId", String.class), data.getAs("space", String.class));
        type.forEach(t -> {
            typeToUUID.computeIfAbsent(t, x -> new HashSet<>()).add(element);
        });
        return element;
    }

    private List<ScopeElement> mergeInstancesOnSameLevel(List<ScopeElement> element) {
        if (element != null && !element.isEmpty()) {
            Map<UUID, List<ScopeElement>> groupedById = element.stream().collect(Collectors.groupingBy(ScopeElement::getId));
            List<ScopeElement> result = new ArrayList<>();
            groupedById.values().forEach(c -> {
                if (!c.isEmpty()) {
                    ScopeElement current = null;
                    if (c.size() == 1) {
                        current = c.get(0);
                    } else {
                        ScopeElement merged = new ScopeElement();
                        c.forEach(merged::merge);
                        current = merged;
                    }
                    if (current != null) {
                        result.add(current);
                        current.setChildren(mergeInstancesOnSameLevel(current.getChildren()));
                    }
                }
            });
            return result;
        }
        return null;
    }

    private ScopeElement translateResultToScope(List<NormalizedJsonLd> data, DataStage stage, boolean fetchLabels, NormalizedJsonLd instance) {
        final Map<String, Set<ScopeElement>> typeToUUID = new HashMap<>();
        ScopeElement element;
        if (data == null || data.isEmpty()) {
            element = new ScopeElement(idUtils.getUUID(instance.id()), instance.types(), null, instance.getAs(ArangoVocabulary.ID, String.class), instance.getAs(EBRAINSVocabulary.META_SPACE, String.class));
            instance.types().forEach(t -> typeToUUID.computeIfAbsent(t, x -> new HashSet<>()).add(element));
        } else {
            element = data.stream().map(d -> handleSubElement(d, typeToUUID)).findFirst().orElse(null);
        }
        if (fetchLabels) {
            List<Type> affectedTypes = typesRepo.getTypeInformation(authContext.getUserWithRoles().getClientId(), stage, typeToUUID.keySet().stream().map(Type::new).collect(Collectors.toList()));
            Set<InstanceId> instances = typeToUUID.values().stream().flatMap(Collection::stream).map(s -> InstanceId.deserialize(s.getInternalId())).collect(Collectors.toSet());
            Map<UUID, String> labelsForInstances = getLabelsForInstances(stage, instances, affectedTypes);
            typeToUUID.values().stream().distinct().parallel().flatMap(Collection::stream).forEach(e -> {
                if (e.getLabel() == null) {
                    String label = labelsForInstances.get(e.getId());
                    if (label != null) {
                        e.setLabel(label);
                    }
                }
            });
        }
        if (element == null) {
            return null;
        }
        return mergeInstancesOnSameLevel(Collections.singletonList(element)).get(0);
    }

}

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

package eu.ebrains.kg.graphdb.instances.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.CollectionsReadOptions;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.*;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.Tuple;
import eu.ebrains.kg.commons.exception.AmbiguousException;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.markers.*;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.model.external.types.TypeInformation;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.ArangoRepositoryCommons;
import eu.ebrains.kg.graphdb.commons.controller.GraphDBArangoUtils;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.instances.model.ArangoRelation;
import eu.ebrains.kg.graphdb.queries.controller.QueryController;
import eu.ebrains.kg.graphdb.queries.model.spec.GraphQueryKeys;
import eu.ebrains.kg.graphdb.structure.api.GraphDBTypesAPI;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ArangoRepositoryInstances {

    private final ArangoRepositoryCommons arangoRepositoryCommons;
    private final PermissionsController permissionsController;
    private final Permissions permissions;
    private final AuthContext authContext;
    private final GraphDBArangoUtils graphDBArangoUtils;
    private final QueryController queryController;
    private final ArangoDatabases databases;
    private final IdUtils idUtils;
    private final JsonAdapter jsonAdapter;
    private final GraphDBTypesAPI graphDBTypesAPI;

    public final static int DEFAULT_INCOMING_PAGESIZE = 10;

    public ArangoRepositoryInstances(ArangoRepositoryCommons arangoRepositoryCommons, PermissionsController permissionsController, Permissions permissions, AuthContext authContext, GraphDBArangoUtils graphDBArangoUtils, QueryController queryController, ArangoDatabases databases, IdUtils idUtils, JsonAdapter jsonAdapter, GraphDBTypesAPI graphDBTypesAPI) {
        this.arangoRepositoryCommons = arangoRepositoryCommons;
        this.permissionsController = permissionsController;
        this.permissions = permissions;
        this.authContext = authContext;
        this.graphDBArangoUtils = graphDBArangoUtils;
        this.queryController = queryController;
        this.databases = databases;
        this.idUtils = idUtils;
        this.jsonAdapter = jsonAdapter;
        this.graphDBTypesAPI = graphDBTypesAPI;
    }

    @ExposesMinimalData
    public Paginated<NormalizedJsonLd> getIncomingLinks(DataStage stage, SpaceName space, UUID id, String property, String type, PaginationParam paginationParam) {
        NormalizedJsonLd instanceIncomingLinks = fetchIncomingLinks(Collections.singletonList(ArangoDocumentReference.fromInstanceId(new InstanceId(id, space))), stage, paginationParam.getFrom(), paginationParam.getSize(), property, type);
        if (!CollectionUtils.isEmpty(instanceIncomingLinks)) {
            resolveIncomingLinks(stage, instanceIncomingLinks);
        }
        Object i = instanceIncomingLinks.get(id.toString());
        if (i instanceof Map) {
            Object p = ((Map) i).get(property);
            if (p instanceof Map) {
                Object t = ((Map) p).get(type);
                if (t instanceof Map) {
                    Object data = ((Map) t).get("data");
                    if (data instanceof List) {
                        return new Paginated<>(((List<Map<? extends String, ?>>) data).stream().map(NormalizedJsonLd::new).collect(Collectors.toList()), (long) ((Map) t).get("total"), (long) ((Map) t).get("size"), (long) ((Map) t).get("from"));
                    }
                }
            }
        }
        return new Paginated<>(Collections.emptyList(), 0, 0, 0);
    }

    @ExposesData
    public NormalizedJsonLd getQuery(SpaceName space, UUID id) {
        ArangoDocument document = arangoRepositoryCommons.getDocument(DataStage.IN_PROGRESS, ArangoCollectionReference.fromSpace(space).doc(id));
        if (document == null || !document.getDoc().types().contains(EBRAINSVocabulary.META_QUERY_TYPE)) {
            //If it's not a query, it's not exposed...
            return null;
        }
        //We explicitly do not check for permissions because queries can be read by everyone
        return document.getDoc();
    }


    @ExposesData
    public NormalizedJsonLd getInstance(DataStage stage, SpaceName space, UUID id, boolean embedded, boolean removeInternalProperties, boolean alternatives, boolean incomingLinks, Long incomingLinksPageSize) {
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.MINIMAL_READ, space, id)) {
            throw new ForbiddenException(String.format("You don't have read rights on the instance with the id %s", id));
        }
        ArangoDocument document = arangoRepositoryCommons.getDocument(stage, ArangoCollectionReference.fromSpace(space).doc(id));
        if (document == null) {
            return null;
        }
        List<NormalizedJsonLd> singleDoc = Collections.singletonList(document.getDoc());
        handleAlternativesAndEmbedded(singleDoc, stage, alternatives, embedded);
        exposeRevision(singleDoc);

        if (incomingLinks) {
            ArangoDocumentReference arangoDocumentReference = ArangoDocumentReference.fromInstanceId(new InstanceId(id, space));
            final boolean ignoreIncomingLinks = graphDBTypesAPI.getTypesByName(document.getDoc().types(), stage, space.getName(), false, false).values().stream().filter(t -> t.getData() != null).map(t -> Type.fromPayload(t.getData())).anyMatch(t -> t.getIgnoreIncomingLinks() != null && t.getIgnoreIncomingLinks());
            if (!ignoreIncomingLinks) {
                NormalizedJsonLd instanceIncomingLinks = fetchIncomingLinks(Collections.singletonList(arangoDocumentReference), stage, 0L, incomingLinksPageSize, null, null);
                if (!CollectionUtils.isEmpty(instanceIncomingLinks)) {
                    resolveIncomingLinks(stage, instanceIncomingLinks);
                    NormalizedJsonLd d = document.getDoc();
                    d.put(EBRAINSVocabulary.META_INCOMING_LINKS, instanceIncomingLinks.get(id.toString()));
                }
            }
        }
        if (removeInternalProperties) {
            document.getDoc().removeAllInternalProperties();
        }
        NormalizedJsonLd doc = document.getDoc();
        if (doc != null && !permissions.hasPermission(authContext.getUserWithRoles(), Functionality.READ, space, id)) {
            //The user doesn't have read rights - we need to restrict the information to minimal data
            doc.keepPropertiesOnly(getMinimalFields(stage, doc.types()));
        }
        return doc;
    }

    private NormalizedJsonLd resolveIncomingLinks(DataStage stage, NormalizedJsonLd instanceIncomingLinks) {
        Set<Type> types = new HashSet<>();
        Set<InstanceId> instanceIds = getInstanceIds(instanceIncomingLinks, types);
        if (instanceIds.isEmpty()) {
            //Nothing to do -> we can just return the original document
            return instanceIncomingLinks;
        }
        final Collection<Result<TypeInformation>> typeInformation = graphDBTypesAPI.getTypesByName(types.stream().map(Type::getName).collect(Collectors.toList()), stage, null, true, false).values();
        final Map<String, TypeInformation> extendedTypeInformationByIdentifier = typeInformation.stream().map(Result::getData).filter(Objects::nonNull).collect(Collectors.toMap(TypeInformation::getIdentifier, v -> v));
        Map<UUID, String> labelsForInstances = getLabelsForInstances(stage, instanceIds);
        enrichDocument(instanceIncomingLinks, extendedTypeInformationByIdentifier, labelsForInstances);
        return instanceIncomingLinks;
    }


    private Set<InstanceId> getInstanceIds(NormalizedJsonLd instanceIncomingLinks, Set<Type> types) {
        return instanceIncomingLinks.values()
                .stream()
                .map(i -> (Collection<?>) ((Map<?, ?>) i).values())
                .flatMap(Collection::stream)
                .map(v -> {
                    Map<? extends String, ?> typeMap = (Map<? extends String, ?>) v;
                    typeMap.keySet().forEach(k -> {
                        types.add(new Type(k));
                    });
                    List<NormalizedJsonLd> normalizedJsonLds = new ArrayList<>();
                    for (Object jsonLd : ((Map) v).values()) {
                        if (jsonLd instanceof Map) {
                            Object data = ((Map) jsonLd).get("data");
                            if (data instanceof List) {
                                ((List) data).forEach(d -> {
                                    if (d instanceof Map) {
                                        normalizedJsonLds.add(new NormalizedJsonLd((Map) d));
                                    }
                                });

                            }
                        }
                    }
                    return normalizedJsonLds;
                }).flatMap(Collection::stream).map(normalizedJsonLd ->
                        new InstanceId(idUtils.getUUID(normalizedJsonLd.id()), new SpaceName(normalizedJsonLd.getAs(EBRAINSVocabulary.META_SPACE, String.class)))
                ).collect(Collectors.toSet());
    }

    private void enrichDocument(NormalizedJsonLd instanceIncomingLinks, Map<String, TypeInformation> extendedTypesByIdentifier, Map<UUID, String> labelsForInstances) {
        List<String> typeInformationBlacklist = Arrays.asList(EBRAINSVocabulary.META_PROPERTIES, SchemaOrgVocabulary.IDENTIFIER, SchemaOrgVocabulary.DESCRIPTION, EBRAINSVocabulary.META_SPACES);
        instanceIncomingLinks.keySet().forEach(instance -> {
            Map<String, Map<String, Map<String, Object>>> propertyMap = (Map<String, Map<String, Map<String, Object>>>) instanceIncomingLinks.get(instance);
            propertyMap.keySet().forEach(property -> {
                Map<String, Map<String, Object>> typeMap = propertyMap.get(property);
                typeMap.keySet().forEach(type -> {
                    Map<String, Object> typeDefinition = typeMap.get(type);
                    TypeInformation extendedTypeInfo = extendedTypesByIdentifier.get(type);
                    if (extendedTypeInfo != null) {
                        extendedTypeInfo.keySet().stream().filter(k -> !typeInformationBlacklist.contains(k)).forEach(extendedTypeInfoKey -> typeDefinition.put(extendedTypeInfoKey, extendedTypeInfo.get(extendedTypeInfoKey)));
                        NormalizedJsonLd propertyDefinition = extendedTypeInfo.getAsListOf(EBRAINSVocabulary.META_PROPERTIES, NormalizedJsonLd.class).stream().filter(Objects::nonNull)
                                .filter(f -> f.identifiers().contains(property))
                                .findFirst()
                                .orElse(null);
                        if (propertyDefinition != null) {
                            String nameForReverseLink = propertyDefinition.getAs(EBRAINSVocabulary.META_NAME_REVERSE_LINK, String.class);
                            typeDefinition.put(EBRAINSVocabulary.META_NAME_REVERSE_LINK, nameForReverseLink);
                        }
                    }
                    Map<String, Object> values = typeMap.get(type);
                    if (values != null && values.get("data") instanceof Collection) {
                        ((Collection<?>) values.get("data")).stream()
                                .map(el -> ((Map<String, Object>) el))
                                .forEach(el -> {
                                    NormalizedJsonLd normalizedJsonLd = new NormalizedJsonLd(el);
                                    String label = labelsForInstances.get(idUtils.getUUID(normalizedJsonLd.id()));
                                    el.put(EBRAINSVocabulary.LABEL, label);
                                });
                    }
                });
            });
        });
    }

    private Set<String> getMinimalFields(DataStage stage, List<String> types) {
        Set<String> keepProperties = new HashSet<>(graphDBTypesAPI.getTypesByName(types, stage, null, false, false).values().stream().filter(r -> r.getData() != null).map(r -> r.getData().getAs(EBRAINSVocabulary.META_TYPE_LABEL_PROPERTY, String.class)).filter(Objects::nonNull).collect(Collectors.toSet()));
        keepProperties.add(JsonLdConsts.ID);
        keepProperties.add(IndexedJsonLdDoc.LABEL);
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
            addEmbeddedInstancesToDocument(documents, getEmbeddedDocuments(documents, stage, false));
        }
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
        Map<UUID, Result<NormalizedJsonLd>> usersById = getDocumentsByReferenceList(DataStage.NATIVE, userIdsToResolve, false, false, false, null);
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
                                    //We only expose the necessary subset of user information.
                                    return new ReducedUserInformation(user != null ? user.getAs(SchemaOrgVocabulary.NAME, String.class) : null, user!=null ? user.getAs(SchemaOrgVocabulary.ALTERNATE_NAME, String.class) : null, user != null ? user.getAsListOf(SchemaOrgVocabulary.IDENTIFIER, String.class) : null, id);
                                }).collect(Collectors.toList());
                                alternative.put(EBRAINSVocabulary.META_USER, users);
                                //A special case: if the value has alternatives but the last value is null, we need to set the value explicitly, since it won't be properly stored in the alternatives payload.
                                if (!alternative.containsKey(EBRAINSVocabulary.META_VALUE)) {
                                    alternative.put(EBRAINSVocabulary.META_VALUE, null);
                                }
                            }
                        });
                    }
                }));
    }

    private Map<UUID, Result<NormalizedJsonLd>> getDocumentsByReferenceList(DataStage stage, List<ArangoDocumentReference> documentReferences, boolean embedded, boolean alternatives, boolean incomingLinks, Long incomingLinksPageSize) {
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
        if (!normalizedJsonLds.isEmpty()) {
            handleAlternativesAndEmbedded(normalizedJsonLds, stage, alternatives, embedded);
            exposeRevision(normalizedJsonLds);


            if (incomingLinks) {
                List<String> involvedTypes = normalizedJsonLds.stream().map(JsonLdDoc::types).flatMap(Collection::stream).distinct().collect(Collectors.toList());

                final Set<String> excludedTypes = graphDBTypesAPI.getTypesByName(involvedTypes, stage, null, false, false).values().stream()
                        .map(t -> Type.fromPayload(t.getData())).filter(t -> t.getIgnoreIncomingLinks() != null && t.getIgnoreIncomingLinks()).map(Type::getName).collect(Collectors.toSet());
                List<ArangoDocumentReference> toInspectForIncomingLinks = normalizedJsonLds.stream().filter(n -> n.types().stream().noneMatch(excludedTypes::contains)).map(n -> ArangoDocument.from(n).getId()).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(toInspectForIncomingLinks)) {

                    NormalizedJsonLd instanceIncomingLinks = fetchIncomingLinks(toInspectForIncomingLinks, stage, 0l, incomingLinksPageSize, null, null);
                    if (!CollectionUtils.isEmpty(instanceIncomingLinks)) {
                        resolveIncomingLinks(stage, instanceIncomingLinks);
                        normalizedJsonLds.forEach(d -> {
                            String id = idUtils.getUUID(d.id()).toString();
                            d.put(EBRAINSVocabulary.META_INCOMING_LINKS, instanceIncomingLinks.get(id));
                        });
                    }
                }
            }
            normalizedJsonLds.forEach(NormalizedJsonLd::removeAllInternalProperties);
        }
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
    public Map<UUID, Result<NormalizedJsonLd>> getDocumentsByIdList(DataStage stage, List<InstanceId> instanceIds, boolean embedded, boolean alternatives, boolean incomingLinks, Long incomingLinksPageSize) {
        UserWithRoles userWithRoles = authContext.getUserWithRoles();

        Set<InstanceId> hasReadPermissions = instanceIds.stream().filter(i -> permissions.hasPermission(userWithRoles, permissionsController.getReadFunctionality(stage), i.getSpace(), i.getUuid())).collect(Collectors.toSet());
        Set<InstanceId> hasOnlyMinimalReadPermissions = instanceIds.stream().filter(i -> !hasReadPermissions.contains(i) && permissions.hasPermission(userWithRoles, permissionsController.getMinimalReadFunctionality(stage), i.getSpace(), i.getUuid())).collect(Collectors.toSet());
        Set<InstanceId> hasNoPermissions = instanceIds.stream().filter(i -> !hasReadPermissions.contains(i) && !hasOnlyMinimalReadPermissions.contains(i)).collect(Collectors.toSet());

        Map<UUID, Result<NormalizedJsonLd>> documentsByReferenceList = getDocumentsByReferenceList(stage, hasReadPermissions.stream().map(ArangoDocumentReference::fromInstanceId).collect(Collectors.toList()), embedded, alternatives, incomingLinks, incomingLinksPageSize);
        Map<UUID, Result<NormalizedJsonLd>> documentsByReferenceListWithMinimalReadAccess = getDocumentsByReferenceList(stage, hasOnlyMinimalReadPermissions.stream().map(ArangoDocumentReference::fromInstanceId).collect(Collectors.toList()), false, false, false, null);

        //Reduce the payload to the minimal fields
        documentsByReferenceListWithMinimalReadAccess.values().stream().filter(Objects::nonNull).map(Result::getData).filter(Objects::nonNull).forEach(d -> d.keepPropertiesOnly(getMinimalFields(stage, d.types())));
        documentsByReferenceList.putAll(documentsByReferenceListWithMinimalReadAccess);

        //Define responses for no-permission instances
        hasNoPermissions.forEach(i -> {
            documentsByReferenceList.put(i.getUuid(), Result.nok(HttpStatus.FORBIDDEN.value(), String.format("You don't have rights to read id %s", i.getUuid())));
        });
        return documentsByReferenceList;
    }

    private Paginated<SuggestedLink> getSuggestedLinkById(DataStage stage, InstanceId instanceId, List<UUID> excludeIds) {
        if (excludeIds == null || !excludeIds.contains(instanceId.getUuid())) {
            //It is a lookup for an instance id -> let's do a shortcut.
            NormalizedJsonLd result = getInstance(stage, instanceId.getSpace(), instanceId.getUuid(), false, false, false, false, null);
            if (result != null) {
                final List<String> types = result.types();
                if(!types.isEmpty()) {
                    SuggestedLink l = new SuggestedLink();
                    UUID uuid = idUtils.getUUID(result.id());
                    l.setId(uuid);
                    l.setLabel(result.getAs(IndexedJsonLdDoc.LABEL, String.class));
                    l.setType(types.get(0));
                    l.setSpace(result.getAs(EBRAINSVocabulary.META_SPACE, String.class, null));
                    return new Paginated<>(Collections.singletonList(l), 1, 1, 0);
                }
            }
        }
        return new Paginated<>(Collections.emptyList(), 0, 0, 0);
    }


    @ExposesData
    //FIXME: Do we want to return suggested links for RELEASED stage?
    public Paginated<SuggestedLink> getSuggestionsByTypes(DataStage stage, PaginationParam paginationParam, List<Type> type, String search, List<UUID> excludeIds) {
        // Suggestions are special in terms of permissions: We even allow instances to show up which are in spaces the
        // user doesn't have read access for. This is only acceptable because we're returning a restricted result with
        // minimal information.
         if (search != null) {
            InstanceId instanceId = InstanceId.deserialize(search);
            if (instanceId != null) {
                //This is a shortcut: If the search term is an instance id, we can directly
                return getSuggestedLinkById(stage, instanceId, excludeIds);
            }
        }
        Map<String, Object> bindVars = new HashMap<>();
        AQL aql = new AQL();
        // ATTENTION: We are only allowed to search by "label" fields but not by "searchable" fields if the user has no read rights
        // for those instances since otherwise, information could be extracted by doing searches. We therefore don't provide additional search fields.
        iterateThroughTypeList(type, null, bindVars, aql);
        if (type.size() == 1 && type.get(0).getSpaces().size() == 1) {
            // If there is only one type and one space for this type, we have the chance to optimize the query... Please
            // note that we're not restricting the spaces to the ones the user can read because the suggestions are
            // working with minimal data and are not affected by the read rights.
            aql.indent().addLine(AQL.trust(String.format("FOR v IN @@singleSpace OPTIONS {indexHint: \"%s\"}", ArangoDatabaseProxy.BROWSE_AND_SEARCH_INDEX)));
            aql.addLine(AQL.trust(String.format("FILTER @typeFilter IN v.`%s` AND v.`%s` == null", JsonLdConsts.TYPE, IndexedJsonLdDoc.EMBEDDED)));
            bindVars.put("typeFilter", type.get(0).getName());
            bindVars.put("@singleSpace", ArangoCollectionReference.fromSpace(type.get(0).getSpaces().iterator().next()).getCollectionName());
        } else {
            aql.indent().addLine(AQL.trust("FOR v IN 1..1 OUTBOUND typeDefinition.type @@typeRelationCollection"));
            bindVars.put("@typeRelationCollection", InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName());
        }
        if (!excludeIds.isEmpty()) {
            aql.addLine(AQL.trust("FILTER v." + ArangoVocabulary.KEY + " NOT IN @excludeIds"));
            bindVars.put("excludeIds", excludeIds);
        }
        addSearchFilter(bindVars, aql, search, false);
        aql.addLine(AQL.trust(String.format("SORT v.%s", IndexedJsonLdDoc.LABEL)));
        aql.addPagination(paginationParam);
        aql.addLine(AQL.trust("LET attWithMeta = [{name: \"" + JsonLdConsts.ID + "\", value: v.`" + JsonLdConsts.ID + "`}, {name: \"" + EBRAINSVocabulary.LABEL + "\", value: v." + IndexedJsonLdDoc.LABEL + "},  {name: \"" + EBRAINSVocabulary.META_TYPE + "\", value: typeDefinition.typeName}, {name: \"" + EBRAINSVocabulary.META_SPACE + "\", value: v.`" + EBRAINSVocabulary.META_SPACE + "`}]"));
        aql.addLine(AQL.trust("RETURN ZIP(attWithMeta[*].name, attWithMeta[*].value)"));
        Paginated<NormalizedJsonLd> normalizedJsonLdPaginated = arangoRepositoryCommons.queryDocuments(databases.getByStage(stage), new AQLQuery(aql, bindVars));
        List<SuggestedLink> links = normalizedJsonLdPaginated.getData().stream().map(payload -> {
            SuggestedLink link = new SuggestedLink();
            UUID uuid = idUtils.getUUID(payload.id());
            link.setId(uuid);
            link.setLabel(payload.getAs(EBRAINSVocabulary.LABEL, String.class, uuid != null ? uuid.toString() : null));
            link.setType(payload.getAs(EBRAINSVocabulary.META_TYPE, String.class, null));
            link.setSpace(payload.getAs(EBRAINSVocabulary.META_SPACE, String.class, null));
            return link;
        }).collect(Collectors.toList());
        return new Paginated<>(links, normalizedJsonLdPaginated.getTotalResults(), normalizedJsonLdPaginated.getSize(), normalizedJsonLdPaginated.getFrom());
    }


    private void addSearchFilter(Map<String, Object> bindVars, AQL aql, String search, boolean withSearchableProperties) {
        if (search != null && !search.isBlank()) {
            List<String> searchTerms = Arrays.stream(search.trim().split(" ")).filter(s -> !s.isBlank()).map(s -> "%" + s.replaceAll("%", "") + "%").collect(Collectors.toList());
            if (!searchTerms.isEmpty()) {
                if (withSearchableProperties) {
                    aql.addLine(AQL.trust("LET found = (FOR name IN typeDefinition.searchableProperties FILTER "));
                    for (int i = 0; i < searchTerms.size(); i++) {
                        aql.addLine(AQL.trust("LIKE(v[name], @search" + i + ", true) "));
                        if (i < searchTerms.size() - 1) {
                            aql.add(AQL.trust("AND "));
                        }
                        bindVars.put("search" + i, searchTerms.get(i));
                    }
                    aql.addLine(AQL.trust("RETURN name) "));
                }
                aql.addLine(AQL.trust("FILTER "));
                for (int i = 0; i < searchTerms.size(); i++) {
                    aql.addLine(AQL.trust(String.format("LIKE(v.%s, @search%d, true)%s", IndexedJsonLdDoc.LABEL, i, withSearchableProperties ? " OR" : "")));
                    if (i < searchTerms.size() - 1) {
                        aql.add(AQL.trust("AND "));
                    }
                    bindVars.put("search" + i, searchTerms.get(i));
                }
                if (withSearchableProperties) {
                    aql.addLine(AQL.trust("LENGTH(found)>=1"));
                }
            }
        }
    }

    private void iterateThroughTypeList(List<Type> types, List<String> searchableProperties, Map<String, Object> bindVars, AQL aql) {
        if (types.size() == 0) {
            aql.addLine(AQL.trust("FOR typeDefinition IN []"));
        } else if (types.size() == 1) {
            aql.addLine(AQL.trust("LET typeDefinition = "));
        } else {
            aql.addLine(AQL.trust("FOR typeDefinition IN ["));
        }
        if (types.size() > 0) {
            ArangoCollectionReference typeCollection = ArangoCollectionReference.fromSpace(InternalSpace.TYPE_SPACE);
            bindVars.put("@typeCollection", typeCollection.getCollectionName());
            for (int i = 0; i < types.size(); i++) {
                aql.addLine(AQL.trust(" {typeName: @typeName" + i + ", type: DOCUMENT(@@typeCollection, @documentId" + i + "), searchableProperties : @searchableProperties" + i + "}"));
                bindVars.put("documentId" + i, typeCollection.docWithStableId(types.get(i).getName()).getDocumentId().toString());
                bindVars.put("typeName" + i, types.get(i).getName());
                bindVars.put("searchableProperties" + i, null);
                if (searchableProperties != null && !searchableProperties.isEmpty()) {
                    bindVars.put("searchableProperties" + i, searchableProperties);
                }
                if (i < types.size() - 1) {
                    aql.add(AQL.trust(","));
                }
            }
        }
        if (types.size() > 1) {
            aql.addLine(AQL.trust("]"));
        }
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


    private Tuple<DocumentsByTypeMode, Set<SpaceName>> restrictToSpaces(Type typeWithLabelInfo, DataStage stage, SpaceName spaceFilter) {
        Set<SpaceName> spaces = typeWithLabelInfo.getSpaces();
        if(spaceFilter!=null){
            spaces = spaces.stream().filter(s -> spaceFilter.getName().equals(s.getName())).collect(Collectors.toSet());
        }
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        if (!permissionsController.hasGlobalReadPermissions(userWithRoles, stage)) {
            //We filter out those spaces to which the user doesn't have read access to.
            spaces = permissionsController.removeSpacesWithoutReadAccess(spaces, userWithRoles, stage);
        }
        spaces = spaces.stream().filter(s -> databases.getByStage(stage).collection(ArangoCollectionReference.fromSpace(s).getCollectionName()).exists()).collect(Collectors.toSet());
        if (permissionsController.getInstancesWithExplicitPermission(userWithRoles, stage).isEmpty()) {
            //We can only make use of a simple mode if the user doesn't have explicit instance permissions
            //If so, we fall back to the slightly slower dynamic resolution since this is rather an edge case.
            if (spaces.isEmpty()) {
                return new Tuple<>(DocumentsByTypeMode.EMPTY, spaces);
            } else if (spaces.size() == 1) {
                return new Tuple<>(DocumentsByTypeMode.SIMPLE, spaces);
            }
        }
        return new Tuple<>(DocumentsByTypeMode.DYNAMIC, spaces);
    }

    private enum DocumentsByTypeMode {
        EMPTY, BY_ID, SIMPLE, DYNAMIC
    }

    private Object getParsedFilterValue(String filterValue){

        try{
            return jsonAdapter.fromJson(filterValue, Map.class);
        } catch(RuntimeException runtimeException1){
            try {
                return jsonAdapter.fromJson(filterValue, List.class);
            }
            catch(RuntimeException runtimeException2){
                return filterValue;
            }
        }
    }

    @ExposesData
    public Paginated<NormalizedJsonLd> getDocumentsByTypes(DataStage stage, Type typeWithLabelInfo, SpaceName space, String filterProperty, String filterValue, PaginationParam paginationParam, String search, boolean embedded, boolean alternatives, List<String> searchableProperties) {
        if (typeWithLabelInfo != null) {
            //TODO find label field for type (and client) and filter by search if set.
            ArangoDatabase database = databases.getByStage(stage);
            Map<String, Object> bindVars = new HashMap<>();
            AQL aql = new AQL();
            Tuple<DocumentsByTypeMode, Set<SpaceName>> restrictToSpaces = restrictToSpaces(typeWithLabelInfo, stage, space);
            DocumentsByTypeMode mode = restrictToSpaces.getA();
            if (mode != DocumentsByTypeMode.EMPTY) {
                if (search != null && InstanceId.deserialize(search) != null) {
                    mode = DocumentsByTypeMode.BY_ID;
                }
                Map<String, Object> whitelistFilter = null;
                switch(mode){
                    case BY_ID:
                    case DYNAMIC:
                        whitelistFilter =  permissionsController.whitelistFilterForReadInstances(authContext.getUserWithRoles(), stage);
                        if (whitelistFilter != null) {
                            aql.specifyWhitelist();
                            bindVars.putAll(whitelistFilter);
                        }
                }
                switch(mode){
                    case SIMPLE:
                    case DYNAMIC:
                        iterateThroughTypeList(Collections.singletonList(typeWithLabelInfo), searchableProperties, bindVars, aql);
                }
                switch (mode) {
                    case BY_ID:
                        aql.indent().addLine(AQL.trust("LET v = DOCUMENT(@documentById)"));
                        aql.addLine(AQL.trust(String.format("FILTER @typeFilter IN v.`%s` AND v.`%s` == null", JsonLdConsts.TYPE, IndexedJsonLdDoc.EMBEDDED)));
                        bindVars.put("typeFilter", typeWithLabelInfo.getName());
                        bindVars.put("documentById", search);
                        break;
                    case SIMPLE:
                        aql.indent().addLine(AQL.trust(String.format("FOR v IN @@singleSpace OPTIONS {indexHint: \"%s\"}", ArangoDatabaseProxy.BROWSE_AND_SEARCH_INDEX)));
                        aql.addLine(AQL.trust(String.format("FILTER @typeFilter IN v.`%s` AND v.`%s` == null", JsonLdConsts.TYPE, IndexedJsonLdDoc.EMBEDDED)));
                        bindVars.put("typeFilter", typeWithLabelInfo.getName());
                        bindVars.put("@singleSpace", ArangoCollectionReference.fromSpace(restrictToSpaces.getB().iterator().next()).getCollectionName());
                        if(filterProperty != null && filterValue!=null){
                            aql.addLine(AQL.trust("AND v.@property == @value"));
                            bindVars.put("property", filterProperty);
                            bindVars.put("value", getParsedFilterValue(filterValue));
                        }
                        break;
                    case DYNAMIC:
                        graphDBArangoUtils.getOrCreateArangoCollection(database, InternalSpace.TYPE_EDGE_COLLECTION);
                        aql.indent().addLine(AQL.trust("FOR v IN 1..1 OUTBOUND typeDefinition.type @@typeRelationCollection"));
                        bindVars.put("@typeRelationCollection", InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName());
                        break;
                }
                switch (mode) {
                    case BY_ID:
                    case DYNAMIC:
                        if (whitelistFilter != null) {
                            aql.addDocumentFilterWithWhitelistFilter(AQL.trust("v"));
                        }
                        if (space != null) {
                            aql.addLine(AQL.trust("FILTER v." + ArangoVocabulary.COLLECTION + " == @spaceFilter"));
                            bindVars.put("spaceFilter", ArangoCollectionReference.fromSpace(space).getCollectionName());
                        }
                        if(filterProperty != null && filterValue!=null){
                            aql.addLine(AQL.trust("FILTER v.@property == @value"));
                            bindVars.put("property", filterProperty);
                            bindVars.put("value", getParsedFilterValue(filterValue));
                        }
                        break;
                }
                switch (mode) {
                    case SIMPLE:
                    case DYNAMIC:
                        addSearchFilter(bindVars, aql, search, searchableProperties!=null && !searchableProperties.isEmpty());
                        if(paginationParam.getSize()!=null) {
                            //We only sort if there is pagination involved.
                            aql.addLine(AQL.trust(String.format("SORT v.%s, v.%s ASC", IndexedJsonLdDoc.LABEL, ArangoVocabulary.KEY)));
                        }
                        aql.addPagination(paginationParam);
                        break;
                }
                aql.addLine(AQL.trust("RETURN v"));
                Paginated<NormalizedJsonLd> normalizedJsonLdPaginated = arangoRepositoryCommons.queryDocuments(database, new AQLQuery(aql, bindVars));
                handleAlternativesAndEmbedded(normalizedJsonLdPaginated.getData(), stage, alternatives, embedded);
                exposeRevision(normalizedJsonLdPaginated.getData());
                final SpaceName privateSpace = authContext.getUserWithRolesWithoutTermsCheck().getPrivateSpace();
                normalizedJsonLdPaginated.getData().forEach(r -> {
                    r.removeAllInternalProperties();
                    final String s = r.getAs(EBRAINSVocabulary.META_SPACE, String.class);
                    if(privateSpace.getName().equals(s)){
                        r.put(EBRAINSVocabulary.META_SPACE, SpaceName.PRIVATE_SPACE);
                    }
                });
                return normalizedJsonLdPaginated;
            }
        }
        return new Paginated<>(Collections.emptyList(), 0, 0, 0);
    }

    @ExposesQuery
    public Paginated<NormalizedJsonLd> getQueriesByRootType(DataStage stage, PaginationParam paginationParam, String search, boolean embedded, boolean alternatives, String typeFilter) {
        ArangoDatabase database = databases.getByStage(stage);
        if (database.collection(InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName()).exists()) {
            Map<String, Object> bindVars = new HashMap<>();
            AQL aql = new AQL();
            iterateThroughTypeList(Collections.singletonList(new Type(EBRAINSVocabulary.META_QUERY_TYPE)), null, bindVars, aql);
            aql.indent().addLine(AQL.trust("FOR v IN 1..1 OUTBOUND typeDefinition.type @@typeRelationCollection"));
            if (typeFilter != null && !typeFilter.isBlank()) {
                aql.addLine(AQL.trust("FILTER v.`" + GraphQueryKeys.GRAPH_QUERY_META.getFieldName() + "`.`" + GraphQueryKeys.GRAPH_QUERY_TYPE.getFieldName() + "` == @typeFilter"));
                bindVars.put("typeFilter", typeFilter);
            }
            if (search != null && !search.isBlank()) {
                aql.addLine(AQL.trust("FILTER LIKE(v.`" + GraphQueryKeys.GRAPH_QUERY_META.getFieldName() + "`.`" + GraphQueryKeys.GRAPH_QUERY_NAME.getFieldName() + "`, @search, true)"));
                aql.addLine(AQL.trust("OR LIKE(v.`" + GraphQueryKeys.GRAPH_QUERY_LABEL.getFieldName() + "`, @search, true)"));
                aql.addLine(AQL.trust("OR LIKE(v.`" + GraphQueryKeys.GRAPH_QUERY_DESCRIPTION.getFieldName() + "`, @search, true)"));
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
        if (!permissions.hasPermission(authContext.getUserWithRoles(), permissionsController.getReadFunctionality(stage), space, id)) {
            throw new ForbiddenException();
        }
        //FIXME: Do we have to restrict this to the instances with minimal read access?
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        ArangoDatabase db = databases.getByStage(stage);
        //The edges are injection-safe since they have been checked beforehand - so we can trust these values.
        String edges = String.join(", ", getAllEdgeCollections(db));

        //For now, we're hardcoding the number of investigated levels for simplicity. this could be done differently if we want to make it parametrized
        aql.addLine(AQL.trust("LET doc = DOCUMENT(@id)"));
        bindVars.put("id", String.format("%s/%s", ArangoCollectionReference.fromSpace(space).getCollectionName(), id));

        if (!edges.isEmpty()) {
            aql.addLine(AQL.trust("LET inbnd = (FOR inbnd IN 1..1 INBOUND doc " + edges));
            aql.addLine(AQL.trust("    RETURN { \"id\": inbnd._key, \"name\": inbnd._label, \"types\": inbnd.`@type`, \"space\": inbnd.`" + EBRAINSVocabulary.META_SPACE + "`})"));
            aql.addLine(AQL.trust("LET outbnd = (FOR outbnd IN 1..1 OUTBOUND doc " + edges));
            aql.addLine(AQL.trust("    LET outbnd2 = (FOR outbnd2 IN 1..1 OUTBOUND outbnd " + edges));
            aql.addLine(AQL.trust("    RETURN {\"id\": outbnd2._key, \"name\": outbnd2._label, \"types\": outbnd2.`@type`, \"space\": outbnd2.`" + EBRAINSVocabulary.META_SPACE + "`})"));
            aql.addLine(AQL.trust("    RETURN {\"id\": outbnd._key,  \"name\": outbnd._label, \"outbound\": outbnd2, \"types\": outbnd.`@type`, \"space\": outbnd.`" + EBRAINSVocabulary.META_SPACE + "` })"));
        } else {
            aql.addLine(AQL.trust("LET inbnd = []"));
            aql.addLine(AQL.trust("LET outbnd = []"));
        }
        aql.addLine(AQL.trust("RETURN {\"id\": doc._key, \"name\": doc._label, \"inbound\" : inbnd, \"outbound\": outbnd, \"types\": doc.`@type`, \"space\": doc.`" + EBRAINSVocabulary.META_SPACE + "` }"));

        List<GraphEntity> graphEntities = db.query(aql.build().getValue(), bindVars, new AqlQueryOptions(), GraphEntity.class).asListRemaining();
        if (graphEntities.isEmpty()) {
            return null;
        } else if (graphEntities.size() == 1) {
            return graphEntities.get(0);
        } else {
            throw new AmbiguousException(String.format("Did find multiple instances for the id %s", id));
        }

    }

    @ExposesMinimalData
    public NormalizedJsonLd fetchIncomingLinks(List<ArangoDocumentReference> documents, DataStage stage, Long from, Long pageSize, String restrictToProperty, String restrictToType) {
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        ArangoDatabase db = databases.getByStage(stage);
        Set<String> edgeCollections;
        if (restrictToProperty != null) {
            ArangoCollectionReference ref = ArangoCollectionReference.fromSpace(new SpaceName(restrictToProperty), true);
            if (db.collection(ref.getCollectionName()).exists()) {
                edgeCollections = Collections.singleton(AQL.preventAqlInjection(ref.getCollectionName()).getValue());
            } else {
                return null;
            }
        } else {
            //The edges are injection-safe since they have been checked beforehand - so we can trust these values.
            edgeCollections = getAllEdgeCollections(db);
            if (edgeCollections.isEmpty()) {
                return null;
            }
        }
        String edges = String.join(", ", edgeCollections);
        aql.addLine(AQL.trust("RETURN MERGE(FOR instanceId IN @instanceIds"));
        bindVars.put("instanceIds", documents.stream().map(ArangoDocumentReference::getId).collect(Collectors.toList()));
        aql.addLine(AQL.trust("LET doc = DOCUMENT(instanceId)"));
        aql.addLine(AQL.trust("LET inbnd = UNIQUE("));
        aql.indent().addLine(AQL.trust("FOR inbnd, e IN 1..1 INBOUND doc " + edges));
        if (restrictToType != null) {
            aql.addLine(AQL.trust("FILTER @typeRestriction IN inbnd.`@type`"));
            bindVars.put("typeRestriction", restrictToType);
        }
        aql.addLine(AQL.trust("FILTER inbnd != NULL"));
        aql.addLine(AQL.trust("RETURN {"));
        aql.indent().addLine(AQL.trust("\"" + SchemaOrgVocabulary.IDENTIFIER + "\": e.`_originalLabel`,"));
        aql.addLine(AQL.trust("\"" + JsonLdConsts.ID + "\": inbnd.`@id`,"));
        aql.addLine(AQL.trust("\"" + JsonLdConsts.TYPE + "\": inbnd.`@type`,"));
        aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_SPACE + "\": inbnd.`" + EBRAINSVocabulary.META_SPACE + "`"));
        aql.outdent().outdent().addLine(AQL.trust("})"));

        aql.addLine(AQL.trust("LET groupedByInstances = (FOR i IN inbnd"));
        aql.addLine(AQL.trust("COLLECT identifier = i.`" + SchemaOrgVocabulary.IDENTIFIER + "` INTO instancesByIdentifier"));
        aql.addLine(AQL.trust("LET instancesById = ("));
        aql.addLine(AQL.trust("FOR x IN instancesByIdentifier[*].i"));
        aql.addLine(AQL.trust("COLLECT type = x.`" + JsonLdConsts.TYPE + "` INTO instancesByIdentifierAndType"));
        aql.addLine(AQL.trust("FOR t in type"));
        aql.addLine(AQL.trust("LET instances = (FOR instance IN instancesByIdentifierAndType[*].x SORT instance.`" + JsonLdConsts.ID + "` LIMIT " + (from != null ? from : 0) + ", " + (pageSize != null ? pageSize : DEFAULT_INCOMING_PAGESIZE) + " RETURN KEEP(instance, \"" + JsonLdConsts.ID + "\", \"" + EBRAINSVocabulary.META_SPACE + "\"))"));
        aql.addLine(AQL.trust("RETURN { [t] : {\"data\": instances, \"total\": LENGTH(instancesByIdentifierAndType[*].i),\"size\": LENGTH(instances), \"from\": " + (from != null ? from : 0) + "}})"));
        aql.addLine(AQL.trust("RETURN {"));
        aql.addLine(AQL.trust("[identifier]: MERGE(instancesById)"));
        aql.addLine(AQL.trust("})"));
        aql.addLine(AQL.trust("FILTER groupedByInstances != null"));
        aql.addLine(AQL.trust("RETURN {"));
        aql.indent().addLine(AQL.trust(" [doc._key]: MERGE(groupedByInstances)"));
        aql.outdent().addLine(AQL.trust("})"));

        List<NormalizedJsonLd> instances = db.query(aql.build().getValue(), bindVars, new AqlQueryOptions(), NormalizedJsonLd.class).asListRemaining();
        if (instances.isEmpty()) {
            return null;
        } else if (instances.size() == 1) {
            return instances.get(0);
        } else {
            throw new AmbiguousException("Received unexpected number of results");
        }
    }

    private Set<String> getAllEdgeCollections(ArangoDatabase db) {
        return db.getCollections(new CollectionsReadOptions().excludeSystem(true)).stream().filter(c ->
                //We're only interested in edges
                c.getType() == CollectionType.EDGES &&
                        //We want to exclude meta properties
                        !c.getName().startsWith(ArangoCollectionReference.fromSpace(new SpaceName(EBRAINSVocabulary.META), true).getCollectionName()) &&
                        //And we want to exclude the internal ones...
                        !InternalSpace.INTERNAL_NON_META_EDGES.contains(new ArangoCollectionReference(c.getName(), true))
        ).map(c -> AQL.preventAqlInjection(c.getName()).getValue()).collect(Collectors.toSet());
    }

    private List<NormalizedJsonLd> getDocumentsByRelation(DataStage stage, SpaceName space, UUID id, ArangoRelation relation, boolean incoming, boolean useOriginalTo, boolean embedded, boolean alternatives) {
        List<NormalizedJsonLd> result = graphDBArangoUtils.getDocumentsByRelation(databases.getByStage(stage), space, id, relation, incoming, useOriginalTo);
        handleAlternativesAndEmbedded(result, stage, alternatives, embedded);
        exposeRevision(result);
        return result;
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
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.RELEASE_STATUS, space, id)) {
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
        graphDBArangoUtils.getOrCreateArangoCollection(database, InternalSpace.DOCUMENT_ID_EDGE_COLLECTION);
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
    public Map<UUID, String> getLabelsForInstances(DataStage stage, Set<InstanceId> ids) {
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust(String.format("RETURN MERGE(FOR id IN @ids RETURN { [ id ] : DOCUMENT(id).%s })", IndexedJsonLdDoc.LABEL)));
        bindVars.put("ids", ids != null ? ids.stream().filter(Objects::nonNull).map(InstanceId::serialize).collect(Collectors.toSet()) : null);
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
        NormalizedJsonLd instance = getInstance(stage, space, id, false, false, false, false, null);
        //get scope relevant queries
        //TODO filter user defined queries (only take client queries into account)
        Stream<NormalizedJsonLd> typeQueries = instance.types().stream().map(type -> getQueriesByRootType(stage, null, null, false, false, type).getData()).flatMap(Collection::stream);
        List<NormalizedJsonLd> results = typeQueries.map(q -> {
            QueryResult queryResult = queryController.query(authContext.getUserWithRoles(),
                    new KgQuery(q, stage).setIdRestrictions(
                            Collections.singletonList(id)), null, null, true);
            return queryResult != null && queryResult.getResult() != null ? queryResult.getResult().getData() : null;
        }).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());
        return translateResultToScope(results, stage, fetchLabels, instance);
    }

    private ScopeElement handleSubElement(NormalizedJsonLd data, Map<String, Set<ScopeElement>> typeToUUID) {
        Boolean embedded = data.getAs("embedded", Boolean.class);
        if(embedded!=null && embedded){
            return null;
        }
        String id = data.getAs("id", String.class);
        UUID uuid = idUtils.getUUID(new JsonLdId(id));
        List<ScopeElement> children = data.keySet().stream().filter(k -> k.startsWith("dependency_")).map(k ->
                data.getAsListOf(k, NormalizedJsonLd.class).stream().map(d -> handleSubElement(d, typeToUUID)).filter(Objects::nonNull).collect(Collectors.toList())
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
        List<ScopeElement> elements;
        if (data == null || data.isEmpty()) {
            elements = Collections.singletonList(new ScopeElement(idUtils.getUUID(instance.id()), instance.types(), null, instance.getAs(ArangoVocabulary.ID, String.class), instance.getAs(EBRAINSVocabulary.META_SPACE, String.class)));
        } else {
            elements = data.stream().map(d -> handleSubElement(d, typeToUUID)).filter(Objects::nonNull).collect(Collectors.toList());
        }
        for(ScopeElement el : elements) {
            instance.types().forEach(t -> typeToUUID.computeIfAbsent(t, x -> new HashSet<>()).add(el));
        }
        if (fetchLabels) {
            Set<InstanceId> instances = typeToUUID.values().stream().flatMap(Collection::stream).map(s -> InstanceId.deserialize(s.getInternalId())).collect(Collectors.toSet());
            Map<UUID, String> labelsForInstances = getLabelsForInstances(stage, instances);
            typeToUUID.values().stream().distinct().parallel().flatMap(Collection::stream).forEach(e -> {
                if (e.getLabel() == null) {
                    String label = labelsForInstances.get(e.getId());
                    if (label != null) {
                        e.setLabel(label);
                    }
                }
            });
        }
        return mergeInstancesOnSameLevel(elements).get(0);
    }

}

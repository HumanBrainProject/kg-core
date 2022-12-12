/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import eu.ebrains.kg.arango.commons.aqlbuilder.AQL;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.exception.AmbiguousException;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesMinimalData;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.model.external.types.TypeInformation;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.structure.controller.MetaDataController;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class IncomingLinksRepository extends AbstractRepository{

    public final static int DEFAULT_INCOMING_PAGESIZE = 10;
    //TODO make this configurable

    private final ArangoDatabases databases;
    private final MetaDataController metaDataController;
    private final AuthContext authContext;
    private final IdUtils idUtils;

    public IncomingLinksRepository(ArangoDatabases databases, MetaDataController metaDataController, AuthContext authContext, IdUtils idUtils) {
        this.databases = databases;
        this.metaDataController = metaDataController;
        this.authContext = authContext;
        this.idUtils = idUtils;
    }

    @ExposesMinimalData
    public Paginated<NormalizedJsonLd> getIncomingLinks(DataStage stage, SpaceName space, UUID id, String property, String type, PaginationParam paginationParam, List<NormalizedJsonLd> invitationDocuments) {
        NormalizedJsonLd instanceIncomingLinks = fetchIncomingLinks(Collections.singletonList(ArangoDocumentReference.fromInstanceId(new InstanceId(id, space))), stage, paginationParam.getFrom(), paginationParam.getSize(), property, type);
        if (!CollectionUtils.isEmpty(instanceIncomingLinks)) {
            resolveIncomingLinks(stage, instanceIncomingLinks, invitationDocuments);
        }
        final NormalizedJsonLd byInstanceId = instanceIncomingLinks.getAs(id.toString(), NormalizedJsonLd.class);
        if (byInstanceId != null) {
            final NormalizedJsonLd byProperty = byInstanceId.getAs(property, NormalizedJsonLd.class);
            if (byProperty != null) {
                final NormalizedJsonLd document = byProperty.getAs(type, NormalizedJsonLd.class);
                if (document != null) {
                    return new Paginated<>(document.getAsListOf("data", NormalizedJsonLd.class), document.getAs("total", Long.class, 0L), document.getAs("size", Long.class, 0L), document.getAs("from", Long.class, 0L));
                }
            }
        }
        return new Paginated<>(Collections.emptyList(), 0L, 0, 0);
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
        aql.addLine(AQL.trust("LET inbndRoot = inbnd.`_embedded` ? DOCUMENT(inbnd.`_originalDocument`) : inbnd"));
        aql.addLine(AQL.trust("RETURN {"));
        aql.indent().addLine(AQL.trust("\"" + SchemaOrgVocabulary.IDENTIFIER + "\": e.`_originalLabel`,"));
        aql.addLine(AQL.trust("\"" + JsonLdConsts.ID + "\": inbndRoot.`@id`,"));
        aql.addLine(AQL.trust("\"" + JsonLdConsts.TYPE + "\": inbnd.`@type`,"));
        aql.addLine(AQL.trust("\"" + EBRAINSVocabulary.META_SPACE + "\": inbndRoot.`" + EBRAINSVocabulary.META_SPACE + "`"));
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


    public NormalizedJsonLd resolveIncomingLinks(DataStage stage, NormalizedJsonLd instanceIncomingLinks, List<NormalizedJsonLd> invitationDocuments) {
        Set<Type> types = new HashSet<>();
        Set<InstanceId> instanceIds = getInstanceIds(instanceIncomingLinks, types);
        if (instanceIds.isEmpty()) {
            //Nothing to do -> we can just return the original document
            return instanceIncomingLinks;
        }
        final Collection<Result<TypeInformation>> typeInformation = metaDataController.getTypesByName(types.stream().map(Type::getName).collect(Collectors.toList()), stage, null, true, false, authContext.getUserWithRoles(), authContext.getClientSpace() != null ? authContext.getClientSpace().getName() : null, invitationDocuments).values();
        final Map<String, TypeInformation> extendedTypeInformationByIdentifier = typeInformation.stream().map(Result::getData).filter(Objects::nonNull).collect(Collectors.toMap(TypeInformation::getIdentifier, v -> v));
        Map<UUID, String> labelsForInstances = getLabelsForInstances(stage, instanceIds, databases);
        enrichDocument(instanceIncomingLinks, extendedTypeInformationByIdentifier, labelsForInstances);
        return instanceIncomingLinks;
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
                        if (jsonLd instanceof Map jsonLdMap) {
                            Object data = jsonLdMap.get("data");
                            if (data instanceof List dataList) {
                                dataList.forEach(d -> {
                                    if (d instanceof Map map) {
                                        normalizedJsonLds.add(new NormalizedJsonLd(map));
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



}

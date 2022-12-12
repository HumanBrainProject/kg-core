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
import eu.ebrains.kg.arango.commons.aqlbuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.PrimaryStoreUsers;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.ReducedUserInformation;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.controller.GraphDBArangoUtils;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class EmbeddedAndAlternativesRepository {

    private final PrimaryStoreUsers.Client primaryStoreUsers;
    private final IdUtils idUtils;
    private final ArangoDatabases databases;
    private final GraphDBArangoUtils graphDBArangoUtils;

    public EmbeddedAndAlternativesRepository(PrimaryStoreUsers.Client primaryStoreUsers, IdUtils idUtils, ArangoDatabases databases, GraphDBArangoUtils graphDBArangoUtils) {
        this.primaryStoreUsers = primaryStoreUsers;
        this.idUtils = idUtils;
        this.databases = databases;
        this.graphDBArangoUtils = graphDBArangoUtils;
    }

    void handleAlternativesAndEmbedded(List<NormalizedJsonLd> documents, DataStage stage, boolean alternatives, boolean embedded) {
        if (alternatives) {
            List<NormalizedJsonLd[]> embeddedDocuments = getEmbeddedDocuments(documents, stage, true);
            for (NormalizedJsonLd[] embeddedDocument : embeddedDocuments) {
                Arrays.stream(embeddedDocument).forEach(e -> {
                    e.remove(EBRAINSVocabulary.META_REVISION);
                    //We don't need the space field of embedded instances since it's redundant
                    e.remove(EBRAINSVocabulary.META_SPACE);
                });
            }
            addEmbeddedInstancesToDocument(documents, embeddedDocuments);
            resolveUsersForAlternatives(documents);
        } else {
            documents.forEach(d -> d.remove(EBRAINSVocabulary.META_ALTERNATIVE));
        }
        if (embedded) {
            addEmbeddedInstancesToDocument(documents, getEmbeddedDocuments(documents, stage, false));
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


    private void addEmbeddedInstancesToDocument(List<NormalizedJsonLd> documentsToBeResolved, List<NormalizedJsonLd[]> embeddedDocuments) {
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


    private void resolveUsersForAlternatives(List<NormalizedJsonLd> documents) {
        //Collect all ids to resolve
        final Set<UUID> userIdsToResolve = documents.stream().map(document -> {
            final NormalizedJsonLd alternative = document.getAs(EBRAINSVocabulary.META_ALTERNATIVE, NormalizedJsonLd.class);
            if (alternative != null) {
                return alternative.keySet().stream().filter(key -> !DynamicJson.isInternalKey(key) && !JsonLdConsts.isJsonLdConst(key)).map(key -> {
                    List<NormalizedJsonLd> alt;
                    try {
                        alt = alternative.getAsListOf(key, NormalizedJsonLd.class);
                    } catch (IllegalArgumentException e) {
                        alt = Collections.emptyList();
                    }
                    return alt != null ? alt.stream().map(a -> a.getAsListOf(EBRAINSVocabulary.META_USER, String.class)).flatMap(List::stream).collect(Collectors.toSet()) : Collections.<String>emptySet();
                }).flatMap(Set::stream).map(id -> idUtils.getUUID(new JsonLdId(id))).collect(Collectors.toSet());
            }
            return Collections.<UUID>emptySet();
        }).flatMap(Set::stream).collect(Collectors.toSet());

        final Map<String, ReducedUserInformation> resolvedUsers = primaryStoreUsers.getUsers(userIdsToResolve);
        documents.forEach(document -> {
            final NormalizedJsonLd alternative = document.getAs(EBRAINSVocabulary.META_ALTERNATIVE, NormalizedJsonLd.class);
            if (alternative != null) {
                alternative.keySet().stream().filter(key -> !DynamicJson.isInternalKey(key) && !JsonLdConsts.isJsonLdConst(key)).forEach(key -> {
                    alternative.put(key, alternative.getAsListOf(key, NormalizedJsonLd.class, true).stream().peek(a -> {
                        List<Object> users = a.getAsListOf(EBRAINSVocabulary.META_USER, String.class).stream().map(id -> {
                            UUID uuid = idUtils.getUUID(new JsonLdId(id));
                            ReducedUserInformation userResult = null;
                            if (uuid != null) {
                                userResult = resolvedUsers.get(uuid.toString());
                            }
                            return userResult;
                        }).collect(Collectors.toList());
                        a.put(EBRAINSVocabulary.META_USER, users);
                    }).collect(Collectors.toList()));
                });
                document.put(EBRAINSVocabulary.META_ALTERNATIVE, alternative);
            }
        });
    }


}

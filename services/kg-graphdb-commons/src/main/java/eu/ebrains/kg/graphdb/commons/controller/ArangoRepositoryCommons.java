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

package eu.ebrains.kg.graphdb.commons.controller;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.StreamTransactionEntity;
import com.arangodb.model.*;
import eu.ebrains.kg.arango.commons.aqlbuilder.AQL;
import eu.ebrains.kg.arango.commons.aqlbuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.jsonld.DynamicJson;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.commons.model.ArangoEdge;
import eu.ebrains.kg.graphdb.ingestion.controller.DataController;
import eu.ebrains.kg.graphdb.ingestion.model.*;
import eu.ebrains.kg.graphdb.structure.controller.CacheController;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ArangoRepositoryCommons {

    private final ArangoDatabases databases;

    private final JsonAdapter jsonAdapter;

    private final GraphDBArangoUtils utils;

    private final EntryHookDocuments entryHookDocuments;

    private final CacheController cacheController;

    private final Logger logger = LoggerFactory.getLogger(getClass());


    public ArangoRepositoryCommons(ArangoDatabases databases, JsonAdapter jsonAdapter, GraphDBArangoUtils utils, EntryHookDocuments entryHookDocuments, CacheController cacheController) {
        this.databases = databases;
        this.jsonAdapter = jsonAdapter;
        this.utils = utils;
        this.entryHookDocuments = entryHookDocuments;
        this.cacheController = cacheController;
    }

    private List<ArangoCollectionReference> getAllEdgeCollections(ArangoDatabase db) {
        Collection<CollectionEntity> collections = db.getCollections(new CollectionsReadOptions().excludeSystem(true));
        return collections.stream().filter(c -> c.getType() == CollectionType.EDGES).map(c -> new ArangoCollectionReference(c.getName(), true)).collect(Collectors.toList());
    }

    public List<ArangoDocumentReference> findEdgeBetweenDocuments(ArangoDatabase db, ArangoDocumentReference origin, ArangoDocumentReference target, ArangoCollectionReference collectionReference) {
        AQL aql = new AQL();
        aql.addLine(AQL.trust("FOR doc IN @@collection"));
        aql.addLine(AQL.trust("FILTER doc._from == @origin AND doc._to == @target"));
        aql.addLine(AQL.trust("RETURN doc._id"));
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("@collection", collectionReference.getCollectionName());
        bindVars.put("origin", origin.getId());
        bindVars.put("target", target.getId());
        if (logger.isTraceEnabled()) {
            logger.trace(aql.buildSimpleDebugQuery(bindVars));
        }
        long start = new Date().getTime();
        List<ArangoDocumentReference> result = query(db, aql.build().getValue(), bindVars, new AqlQueryOptions(), String.class).stream().map(s -> ArangoDocumentReference.fromArangoId(s, true)).collect(Collectors.toList());
        logger.debug(String.format("Resolved %d edges between document %s and %s in %dms", result.size(), origin.getId(), target.getId(), new Date().getTime() - start));
        return result;
    }

    public List<ArangoEdge> findUnresolvedEdgesForIds(DataStage stage, Set<String> ids) {
        ArangoDatabase db = databases.getByStage(stage);
        ArangoCollectionReference unresolvedEdges = ArangoCollectionReference.fromSpace(InternalSpace.UNRESOLVED_SPACE);
        if (db.collection(unresolvedEdges.getCollectionName()).exists()) {
            AQL aql = new AQL();
            aql.addLine(AQL.trust("FOR doc IN @@unresolvedEdges"));
            aql.addLine(AQL.trust("FILTER doc." + IndexedJsonLdDoc.ORIGINAL_TO + " IN @ids"));
            aql.addLine(AQL.trust("RETURN doc"));
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("@unresolvedEdges", unresolvedEdges.getCollectionName());
            bindVars.put("ids", ids);
            if (logger.isTraceEnabled()) {
                logger.trace(aql.buildSimpleDebugQuery(bindVars));
            }
            long start = new Date().getTime();
            List<ArangoEdge> result = query(db, aql.build().getValue(), bindVars, new AqlQueryOptions(), ArangoEdge.class);
            logger.debug(String.format("Found %d unresolved edges for ids %s in %dms", result.size(), StringUtils.join(ids, ", "), new Date().getTime() - start));
            return result;
        }
        return Collections.emptyList();
    }

    public List<ArangoEdge> getIncomingRelationsForDocument(DataStage stage, ArangoDocumentReference documentReference) {
        return getIncomingRelationsForDocument(stage, documentReference, null);
    }

    private List<ArangoEdge> getIncomingRelationsForDocument(DataStage stage, ArangoDocumentReference documentReference, Set<String> filterByIds) {
        ArangoDatabase db = databases.getByStage(stage);
        List<ArangoCollectionReference> edgeCollections = getAllEdgeCollections(db);
        if (edgeCollections == null || edgeCollections.isEmpty()) {
            return Collections.emptyList();
        }
        AQL aql = new AQL();
        aql.addLine(AQL.trust("LET doc = DOCUMENT(@document)"));
        aql.addLine(AQL.trust("LET edges = (FOR v, e IN 1..1 INBOUND doc "));
        aql.addLine(AQL.trust(edgeCollections.stream().map(e -> String.format("`%s`", e.getCollectionName())).collect(Collectors.joining(", "))));
        Map<String, Object> bindVars = new HashMap<>();
        if (filterByIds != null) {
            aql.addLine(AQL.trust(" FILTER e." + IndexedJsonLdDoc.ORIGINAL_TO + " IN @ids"));
            bindVars.put("ids", filterByIds);
        }
        aql.addLine(AQL.trust("RETURN e)"));
        aql.addLine(AQL.trust("FOR edge IN edges"));
        aql.addLine(AQL.trust("RETURN edge"));
        bindVars.put("document", documentReference.getId());
        if (logger.isTraceEnabled()) {
            logger.trace(aql.buildSimpleDebugQuery(bindVars));
        }
        long start = new Date().getTime();
        List<ArangoEdge> result = query(db, aql.build().getValue(), bindVars, new AqlQueryOptions(), ArangoEdge.class);
        logger.debug(String.format("Found %d incoming relations for ids %s in %dms", result.size(), documentReference.getId(), new Date().getTime() - start));
        return result;
    }

    private <T> List<T> query(ArangoDatabase db, String query, Map<String, Object> bindVars, AqlQueryOptions options, Class<T> clazz) {
        return db.query(query, bindVars, options, String.class).asListRemaining().stream().map(i -> jsonAdapter.fromJson(i, clazz)).collect(Collectors.toList());
    }


    public ArangoDocument getDocument(DataStage stage, ArangoDocumentReference reference) {
        return ArangoDocument.from(databases.getByStage(stage).collection(reference.getArangoCollectionReference().getCollectionName()).getDocument(reference.getDocumentId().toString(), NormalizedJsonLd.class));
    }

    public boolean doesDocumentExist(DataStage stage, ArangoDocumentReference reference) {
        return databases.getByStage(stage).collection(reference.getArangoCollectionReference().getCollectionName()).documentExists(reference.getDocumentId().toString());
    }


    private Set<ArangoDocumentReference> findArangoReferencesForDocumentId(ArangoDatabase db, UUID documentId) {
        ArangoCollectionReference documentIdSpace = ArangoCollectionReference.fromSpace(InternalSpace.DOCUMENT_ID_SPACE);
        if (db.collection(documentIdSpace.getCollectionName()).exists() && db.collection(InternalSpace.DOCUMENT_ID_EDGE_COLLECTION.getCollectionName()).exists()) {
            AQL aql = new AQL();
            aql.addLine(AQL.trust("LET doc = DOCUMENT(@@collection, @documentId)"));
            aql.addLine(AQL.trust("LET docs = (FOR v,e IN 1..1 OUTBOUND doc @@relation"));
            aql.indent().addLine(AQL.trust("RETURN [v._id, e._id])")).outdent();
            aql.addLine(AQL.trust("LET flattened = FLATTEN(docs)"));
            aql.addLine(AQL.trust("FOR d IN flattened"));
            aql.addLine(AQL.trust("RETURN d"));
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("@collection", documentIdSpace.getCollectionName());
            bindVars.put("documentId", documentId.toString());
            bindVars.put("@relation", InternalSpace.DOCUMENT_ID_EDGE_COLLECTION.getCollectionName());
            if (logger.isTraceEnabled()) {
                logger.trace(aql.buildSimpleDebugQuery(bindVars));
            }
            long start = new Date().getTime();
            List<String> ids = db.query(aql.build().getValue(), bindVars, new AqlQueryOptions(), String.class).asListRemaining();
            logger.debug(String.format("Resolved %d references for document id %s in %dms", ids.size(), documentId, new Date().getTime() - start));
            return ids.stream().filter(Objects::nonNull).map(id -> ArangoDocumentReference.fromArangoId(id, null)).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }


    public void executeTransactional(DataStage stage, List<? extends DBOperation> operations) {
        UUID transactionId = UUID.randomUUID();
        logger.debug(String.format("Executing transaction %s on stage %s ", transactionId, stage.name()));
        executeTransactional(stage, databases.getByStage(stage), operations);
        logger.debug(String.format("Finished transaction %s on stage %s", transactionId, stage.name()));
    }


    private List<CacheEvictionPlan> fetchCacheEvictionPlans(DataStage stage, List<String> ids) {
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR id IN @ids"));
        bindVars.put("ids", ids);
        aql.addLine(AQL.trust("LET doc = DOCUMENT(id)"));
        aql.addLine(AQL.trust(String.format("FILTER doc != NULL && doc.`%s` != NULL && doc.`%s` != NULL && doc.`%s` != []", JsonLdConsts.TYPE, EBRAINSVocabulary.META_SPACE, EBRAINSVocabulary.META_SPACE)));
        aql.addLine(AQL.trust(String.format("RETURN { \"id\": id, \"type\": doc.`%s`,", JsonLdConsts.TYPE)));
        aql.addLine(AQL.trust(String.format("    \"space\": doc.`%s`,", EBRAINSVocabulary.META_SPACE)));
        aql.addLine(AQL.trust(String.format("    \"properties\": (FOR a IN ATTRIBUTES(doc, true) FILTER a NOT LIKE \"%s%%\" AND a NOT LIKE \"@%%\" RETURN a)}", EBRAINSVocabulary.META)));
        return databases.getByStage(stage).query(aql.build().getValue(), bindVars, CacheEvictionPlan.class).asListRemaining();
    }


    public void executeTransactional(DataStage stage, ArangoDatabase db, List<? extends DBOperation> operations) {
        if (operations.isEmpty()) {
            logger.debug("No operations to be executed - we therefore do not do anything");
            return;
        }
        logger.trace("Setting up the transaction...");
        Set<ArangoCollectionReference> collections = new HashSet<>();
        collections.add(InternalSpace.DOCUMENT_ID_EDGE_COLLECTION);
        ArangoCollectionReference documentIdSpaceRef = ArangoCollectionReference.fromSpace(InternalSpace.DOCUMENT_ID_SPACE);
        collections.add(documentIdSpaceRef);
        final ArangoCollectionReference unresolvedSpace = ArangoCollectionReference.fromSpace(InternalSpace.UNRESOLVED_SPACE, true);
        collections.add(unresolvedSpace);

        List<DBOperation> distinctOperations = operations.stream().distinct().collect(Collectors.toList());
        Set<ArangoDocumentReference> deleteIds = distinctOperations.stream().filter(o -> o instanceof DeleteOperation).map(o -> ((DeleteOperation) o).getLifecycleDocumentRef()).collect(Collectors.toSet());
        Set<RemoveReleaseStateOperation> removeReleaseStateOperations = distinctOperations.stream().filter(o -> o instanceof RemoveReleaseStateOperation).map(o -> (RemoveReleaseStateOperation) o).collect(Collectors.toSet());
        List<UpsertOperation> upserts = distinctOperations.stream().filter(o -> o instanceof UpsertOperation).map(o -> (UpsertOperation) o).filter(u -> u.getDocumentReference() != null).filter(u -> u.isOverrideIfExists() || !doesDocumentExist(stage, u.getDocumentReference())).collect(Collectors.toList());
        List<EdgeResolutionOperation> edgeResolutionOperations = distinctOperations.stream().filter(o -> o instanceof EdgeResolutionOperation).map(o -> (EdgeResolutionOperation) o).collect(Collectors.toList());

        //An UPSERT is implemented as a DELETE & INSERT - we therefore need to remove all dependent resources (there can be many) for the original document ID
        Set<ArangoDocumentReference> upsertIdsForDelete = upserts.stream().filter(UpsertOperation::isAttachToOriginalDocument).map(UpsertOperation::getLifecycleDocumentId).filter(u -> !deleteIds.contains(u)).collect(Collectors.toSet());
        Set<ArangoDocumentReference> removedDocuments = new HashSet<>();
        Map<ArangoCollectionReference, List<String>> insertedDocuments = new HashMap<>();
        upsertIdsForDelete.forEach(upsertForDelete -> {
            removedDocuments.addAll(findRemovalOfAllDependenciesForDocumentId(db, upsertForDelete, removedDocuments));
        });
        deleteIds.forEach(delete -> {
            removedDocuments.addAll(findRemovalOfAllDependenciesForDocumentId(db, delete, removedDocuments));
            //When actually deleting something, we also get rid of the document-id hook-document.
            ArangoDocumentReference documentReference = documentIdSpaceRef.doc(delete.getDocumentId());
            if (stage != DataStage.NATIVE) {
                //Since we remove an instance, this also means that incoming links are detached...
                getIncomingRelationsForDocument(stage, delete).stream().filter(incomingRelation -> incomingRelation.getOriginalTo() != null).forEach(edge -> {
                    removedDocuments.add(edge.getId());
                    edge.redefineId(new ArangoDocumentReference(ArangoCollectionReference.fromSpace(InternalSpace.UNRESOLVED_SPACE), edge.getKey()));
                    edge.defineCollectionById();
                    edge.setTo(DataController.UNKNOWN_TARGET);
                    insertedDocuments.computeIfAbsent(unresolvedSpace, x -> new ArrayList<>()).add(jsonAdapter.toJson(edge));
                });
            }
            removedDocuments.add(documentReference);
        });
        removeReleaseStateOperations.forEach(removeReleaseState -> {
            if (removeReleaseState.getDocumentReference() != null) {
                removedDocuments.add(removeReleaseState.getDocumentReference());
            }
        });
        Map<ArangoDocumentReference, ArangoDocument> edgeResolutionDependencies = new HashMap<>();
        edgeResolutionOperations.forEach(edgeResolution -> {
            //Remove documentId link...
            ArangoDocumentReference edgeReference = edgeResolution.getUpdatedEdge().getId();
            List<ArangoDocumentReference> documentIdLinksToUnresolved = findEdgeBetweenDocuments(db, edgeReference, edgeResolution.getUnresolvedEdgeRef(), InternalSpace.DOCUMENT_ID_EDGE_COLLECTION);

            // remove the links to unresolved
            documentIdLinksToUnresolved.stream().distinct().filter(ref -> !removedDocuments.contains(ref)).forEach(removedDocuments::add);

            //... remove the edge document ...
            removedDocuments.add(edgeResolution.getUnresolvedEdgeRef());

            //... create the new edge ...
            insertedDocuments.computeIfAbsent(edgeReference.getArangoCollectionReference(), x -> new ArrayList<>()).add(jsonAdapter.toJson(edgeResolution.getUpdatedEdge().defineCollectionById()));

            //... and attach it to the document id
            ArangoDocumentReference internalDocReference = new ArangoDocumentReference(ArangoCollectionReference.fromSpace(InternalSpace.DOCUMENT_ID_SPACE), edgeResolution.getUpdatedEdge().getOriginalDocument().getDocumentId());
            insertedDocuments.computeIfAbsent(InternalSpace.DOCUMENT_ID_EDGE_COLLECTION, x -> new ArrayList<>()).add(jsonAdapter.toJson(entryHookDocuments.createEdgeFromHookDocument(InternalSpace.DOCUMENT_ID_EDGE_COLLECTION, edgeReference, internalDocReference, null).defineCollectionById()));

            //... finally, update the payload of the related document to the resolved id
            ArangoDocument originalDocument = edgeResolutionDependencies.get(edgeResolution.getUpdatedEdge().getOriginalDocument());
            if (originalDocument == null) {
                originalDocument = getDocument(stage, edgeResolution.getUpdatedEdge().getOriginalDocument());
                edgeResolutionDependencies.put(edgeResolution.getUpdatedEdge().getOriginalDocument(), originalDocument);
            }
            if (originalDocument != null) {
                originalDocument.applyResolvedEdges(Collections.singleton(edgeResolution.getUpdatedEdge()));
            }
//            removedDocuments.add(edgeResolution.getUpdatedEdge().getOriginalDocument());
//            insertedDocuments.computeIfAbsent(edgeResolution.getUpdatedEdge().getOriginalDocument().getArangoCollectionReference(), x -> new ArrayList<>()).add(new JsonAdapter().toJson(originalDocument.getDoc()));
        });

        Map<ArangoDocumentReference, ArangoDocumentReference> documentIdHooks = new HashMap<>();
        upserts.forEach(upsert -> {
            ArangoCollectionReference collection = upsert.getDocumentReference().getArangoCollectionReference();
            ArangoDocument arangoDocument = ArangoDocument.from(upsert.getPayload());
            arangoDocument.asIndexedDoc().setCollection(arangoDocument.getId().getArangoCollectionReference().getCollectionName());
            arangoDocument.asIndexedDoc().updateIdentifiers();
            arangoDocument.setKeyBasedOnId();
            insertedDocuments.computeIfAbsent(collection, x -> new ArrayList<>()).add(jsonAdapter.toJson(upsert.getPayload()));
            if (upsert.isAttachToOriginalDocument()) {
                //Attention: The following method is non-transactional. It's just the hook-document though and therefore acceptable
                ArangoDocumentReference documentIdHook = documentIdHooks.get(upsert.getLifecycleDocumentId());
                if (documentIdHook == null) {
                    documentIdHook = entryHookDocuments.getOrCreateDocumentIdHookDocument(upsert.getLifecycleDocumentId(), db);
                    documentIdHooks.put(upsert.getLifecycleDocumentId(), documentIdHook);
                }
                insertedDocuments.computeIfAbsent(InternalSpace.DOCUMENT_ID_EDGE_COLLECTION, x -> new ArrayList<>()).add(jsonAdapter.toJson(entryHookDocuments.createEdgeFromHookDocument(InternalSpace.DOCUMENT_ID_EDGE_COLLECTION, upsert.getDocumentReference(), documentIdHook, null).defineCollectionById()));
            }
        });


        collections.addAll(removedDocuments.stream().map(ArangoDocumentReference::getArangoCollectionReference).collect(Collectors.toSet()));
        collections.addAll(edgeResolutionDependencies.values().stream().map(d -> d.getOriginalDocument().getArangoCollectionReference()).collect(Collectors.toSet()));
        collections.addAll(insertedDocuments.keySet());

        //Create missing collections...
        collections.forEach(c -> {
            utils.getOrCreateArangoCollection(db, c);
        });

        long startTransactionDate = new Date().getTime();

        StreamTransactionEntity tx = db.beginStreamTransaction(new StreamTransactionOptions().writeCollections(collections.stream().map(ArangoCollectionReference::getCollectionName).toArray(String[]::new)));
        logger.debug(String.format("Starting transaction %s", tx.getId()));
        DocumentDeleteOptions deleteOptions = new DocumentDeleteOptions().streamTransactionId(tx.getId());
        DocumentCreateOptions insertOptions = new DocumentCreateOptions().streamTransactionId(tx.getId());
        DocumentUpdateOptions updateOptions = new DocumentUpdateOptions().streamTransactionId(tx.getId());
        List<String> allIds = null;
        List<CacheEvictionPlan> cacheEvictionPlansBeforeTransaction = null;
        if (stage == DataStage.IN_PROGRESS || stage == DataStage.RELEASED) {
            allIds = Stream.concat(Stream.concat(removedDocuments.stream().map(ArangoDocumentReference::getId), edgeResolutionDependencies.keySet().stream().map(ArangoDocumentReference::getId)), insertedDocuments.values().stream().flatMap(Collection::stream).map(i -> jsonAdapter.fromJson(i, DynamicJson.class).getAs(ArangoVocabulary.ID, String.class))).distinct().collect(Collectors.toList());
            cacheEvictionPlansBeforeTransaction = fetchCacheEvictionPlans(stage, allIds);
        }
        try {
            removedDocuments.stream().collect(Collectors.groupingBy(ArangoDocumentReference::getArangoCollectionReference)).forEach((c, v) -> db.collection(c.getCollectionName()).deleteDocuments(v.stream().map(r -> r.getDocumentId().toString()).collect(Collectors.toSet()), String.class, deleteOptions));
            edgeResolutionDependencies.values().stream().collect(Collectors.groupingBy(i -> i.getId().getArangoCollectionReference())).forEach((c, v) -> db.collection(c.getCollectionName()).updateDocuments(v.stream().map(doc -> jsonAdapter.toJson(doc.getDoc())).collect(Collectors.toList()), updateOptions));
            insertedDocuments.forEach((c, v) -> db.collection(c.getCollectionName()).insertDocuments(v, insertOptions.overwrite(true)));
            db.commitStreamTransaction(tx.getId());
            logger.debug(String.format("Committing transaction %s after %dms", tx.getId(), new Date().getTime() - startTransactionDate));
        } catch (Exception e) {
            logger.debug(String.format("Execution of transaction has failed after %dms. \n\n TRANSACTION: %s\n\n", new Date().getTime() - startTransactionDate, tx.getId()));
            db.abortStreamTransaction(tx.getId());
        }
        if (stage == DataStage.IN_PROGRESS || stage == DataStage.RELEASED) {
            cacheController.evictCacheByPlan(stage, cacheEvictionPlansBeforeTransaction, fetchCacheEvictionPlans(stage, allIds));
        }
    }

    public Set<ArangoDocumentReference> findNativeDependenciesForDocumentId(ArangoDocumentReference documentId) {
        return findArangoReferencesForDocumentId(databases.getByStage(DataStage.NATIVE), documentId.getDocumentId());
    }

    private Set<ArangoDocumentReference> findRemovalOfAllDependenciesForDocumentId(ArangoDatabase db, ArangoDocumentReference delete, Set<ArangoDocumentReference> skipList) {
        return findArangoReferencesForDocumentId(db, delete.getDocumentId()).stream().filter(Objects::nonNull).filter(ref -> !skipList.contains(ref)).collect(Collectors.toSet());
    }

    public <T> void visitDocuments(ArangoDatabase db, AQLQuery aqlQuery, Consumer<T> consumer, Class<T> clazz) {
        AQL aql = aqlQuery.getAql();
        if (logger.isTraceEnabled()) {
            logger.trace(aql.buildSimpleDebugQuery(aqlQuery.getBindVars()));
        }
        String value = aql.build().getValue();
        long launch = new Date().getTime();
        ArangoCursor<T> result = db.query(value, aqlQuery.getBindVars(), aql.getQueryOptions(), clazz);
        logger.debug(String.format("Received %d results from Arango in %dms", result.getCount(), new Date().getTime() - launch));
        while(result.hasNext()){
            consumer.accept(result.next());
        }
        logger.debug(String.format("Done visiting the results after %dms", new Date().getTime() - launch));
    }

}

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

package eu.ebrains.kg.graphdb.commons.controller;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.StreamTransactionEntity;
import com.arangodb.model.*;
import com.google.gson.Gson;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.commons.model.ArangoEdge;
import eu.ebrains.kg.graphdb.commons.model.ArangoInstance;
import eu.ebrains.kg.graphdb.ingestion.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ArangoRepositoryCommons {

    private final ArangoDatabases databases;

    private final Gson gson;

    private final ArangoUtils utils;

    private final EntryHookDocuments entryHookDocuments;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ArangoRepositoryCommons(ArangoDatabases databases, Gson gson, ArangoUtils utils, EntryHookDocuments entryHookDocuments) {
        this.databases = databases;
        this.gson = gson;
        this.utils = utils;
        this.entryHookDocuments = entryHookDocuments;
    }


    private List<ArangoCollectionReference> getAllEdgeCollections(ArangoDatabase db) {
        Collection<CollectionEntity> collections = db.getCollections(new CollectionsReadOptions().excludeSystem(true));
        return collections.stream().filter(c -> c.getType() == CollectionType.EDGES).map(c -> new ArangoCollectionReference(c.getName(), true)).collect(Collectors.toList());
    }

    public List<ArangoDocumentReference> findEdgeBetweenDocuments(ArangoDatabase db, ArangoDocumentReference origin, ArangoDocumentReference target, ArangoCollectionReference collectionReference) {
        String query = "FOR doc IN @@collection\n" +
                "   FILTER doc._from == @origin AND doc._to == @target\n" +
                "   RETURN doc._id";
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("@collection", collectionReference.getCollectionName());
        bindVars.put("origin", origin.getId());
        bindVars.put("target", target.getId());
        return query(db, query, bindVars, new AqlQueryOptions(), String.class).stream().map(s -> ArangoDocumentReference.fromArangoId(s, true)).collect(Collectors.toList());
    }

    public List<ArangoEdge> findUnresolvedEdgesForIds(DataStage stage, Set<String> ids) {
        ArangoDatabase db = databases.getByStage(stage);
        ArangoCollectionReference unresolvedEdges = ArangoCollectionReference.fromSpace(InternalSpace.UNRESOLVED_SPACE);
        if (db.collection(unresolvedEdges.getCollectionName()).exists()) {
            String query = "FOR doc IN `" + unresolvedEdges.getCollectionName() + "`\n" +
                    "   FILTER doc." + IndexedJsonLdDoc.ORIGINAL_TO + " IN @ids\n" +
                    "   RETURN doc";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("ids", ids);
            return query(db, query, bindVars, new AqlQueryOptions(), ArangoEdge.class);
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
        StringBuilder query = new StringBuilder("LET doc = DOCUMENT(@document)\n" +
                "LET edges = (FOR v, e IN 1..1 INBOUND doc ");
        Set<String> collections = edgeCollections.stream().map(e -> "`" + e.getCollectionName() + "`").collect(Collectors.toSet());
        query.append(String.join(", ", collections));
        Map<String, Object> bindVars = new HashMap<>();
        if (filterByIds != null) {
            query.append(" FILTER e." + IndexedJsonLdDoc.ORIGINAL_TO + " IN @ids");
            bindVars.put("ids", filterByIds);
        }
        query.append("        RETURN e)\n");
        query.append("FOR edge IN edges\n");
        query.append("  RETURN edge");
        bindVars.put("document", documentReference.getId());
        return query(db, query.toString(), bindVars, new AqlQueryOptions(), ArangoEdge.class);
    }

    private <T> List<T> query(ArangoDatabase db, String query, Map<String, Object> bindVars, AqlQueryOptions options, Class<T> clazz) {
        return db.query(query, bindVars, options, String.class).asListRemaining().stream().map(i -> gson.fromJson(i, clazz)).collect(Collectors.toList());
    }


    public ArangoDocument getDocument(DataStage stage, ArangoDocumentReference reference) {
        return ArangoDocument.from(databases.getByStage(stage).collection(reference.getArangoCollectionReference().getCollectionName()).getDocument(reference.getDocumentId().toString(), NormalizedJsonLd.class));
    }

    public boolean doesDocumentExist(DataStage stage, ArangoDocumentReference reference) {
        return databases.getByStage(stage).collection(reference.getArangoCollectionReference().getCollectionName()).documentExists(reference.getDocumentId().toString());
    }

    public Set<ArangoInstance> checkExistenceOfInstances(DataStage stage, Collection<ArangoInstance> instances, boolean returnExisting) {
        AQL aql = new AQL();
        String documentList = instances.stream().map(i -> "DOCUMENT(\"" + AQL.preventAqlInjection(i.getId().getId()).getValue() + "\")").collect(Collectors.joining(","));
        aql.addLine(AQL.trust(String.format(String.format("FOR d IN [%s]", documentList))));
        aql.addLine(AQL.trust("FILTER d != null"));
        aql.addLine(AQL.trust("RETURN d._id"));
        List<String> ids = databases.getByStage(stage).query(aql.build().getValue(), new HashMap<>(), new AqlQueryOptions(), String.class).asListRemaining();
        return instances.stream().filter(i -> returnExisting == ids.contains(i.getId().getId())).collect(Collectors.toSet());
    }


    private Set<ArangoDocumentReference> findArangoReferencesForDocumentId(ArangoDatabase db, UUID documentId) {
        ArangoCollectionReference documentIdSpace = ArangoCollectionReference.fromSpace(InternalSpace.DOCUMENT_ID_SPACE);
        if (db.collection(documentIdSpace.getCollectionName()).exists() && db.collection(InternalSpace.DOCUMENT_ID_EDGE_COLLECTION.getCollectionName()).exists()) {
            String aql = "LET doc = DOCUMENT(@@collection, @documentId)\n" +
                    "LET docs = (FOR v,e IN 1..1 OUTBOUND doc @@relation\n" +
                    "    RETURN [v._id, e._id])\n" +
                    "LET flattened = FLATTEN(docs)\n" +
                    "FOR d IN flattened\n" +
                    "  RETURN d";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("@collection", documentIdSpace.getCollectionName());
            bindVars.put("documentId", documentId.toString());
            bindVars.put("@relation", InternalSpace.DOCUMENT_ID_EDGE_COLLECTION.getCollectionName());
            List<String> ids = db.query(aql, bindVars, new AqlQueryOptions(), String.class).asListRemaining();
            return ids.stream().filter(Objects::nonNull).map(id -> ArangoDocumentReference.fromArangoId(id, null)).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    public List<ArangoDocumentReference> getEdgesFrom(DataStage stage, ArangoCollectionReference edgeCollection, ArangoDocumentReference from) {
        ArangoDatabase db = databases.getByStage(stage);
        if (db.collection(edgeCollection.getCollectionName()).exists()) {
            String aql = "FOR doc IN @@edgeCollection" +
                    "FILTER doc._from == @from" +
                    "RETURN doc._id";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("@edgeCollection", edgeCollection.getCollectionName());
            bindVars.put("from", from.getDocumentId());
            List<String> documentIds = db.query(aql, bindVars, new AqlQueryOptions(), String.class).asListRemaining();
            return documentIds.stream().map(id -> ArangoDocumentReference.fromArangoId(id, true)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public void executeTransactionalOnMeta(DataStage stage, List<DBOperation> operations) {
        logger.info(String.format("Executing transaction for metadata on stage %s", stage.name()));
        executeTransactional(stage, databases.getMetaByStage(stage), operations);
    }

    public void executeTransactional(DataStage stage, List<? extends DBOperation> operations) {
        UUID transactionId = UUID.randomUUID();
        logger.debug(String.format("Executing transaction %s on stage %s ", transactionId.toString(), stage.name()));
        executeTransactional(stage, databases.getByStage(stage), operations);
        logger.debug(String.format("Finished transaction %s on stage %s", transactionId.toString(), stage.name()));
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
        collections.add(ArangoCollectionReference.fromSpace(InternalSpace.UNRESOLVED_SPACE, true));

        List<DBOperation> distinctOperations = operations.stream().distinct().collect(Collectors.toList());
        Set<ArangoDocumentReference> deleteIds = distinctOperations.stream().filter(o -> o instanceof DeleteOperation).map(o -> ((DeleteOperation) o).getLifecycleDocumentRef()).collect(Collectors.toSet());
        Set<DeleteInstanceOperation> deleteInstanceOperations = distinctOperations.stream().filter(o -> o instanceof DeleteInstanceOperation).map(o -> (DeleteInstanceOperation) o).collect(Collectors.toSet());
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
            removedDocuments.add(documentReference);
        });
        deleteInstanceOperations.forEach(delete -> {
            if (delete.getDocumentReference() != null) {
                removedDocuments.add(delete.getDocumentReference());
            }
        });
        edgeResolutionOperations.forEach(edgeResolution -> {
            //Remove documentId link...
            ArangoDocumentReference edgeReference = edgeResolution.getUpdatedEdge().getId();
            List<ArangoDocumentReference> documentIdLinksToUnresolved = findEdgeBetweenDocuments(db, edgeReference, edgeResolution.getUnresolvedEdgeRef(), InternalSpace.DOCUMENT_ID_EDGE_COLLECTION);

            // remove the links to unresolved
            documentIdLinksToUnresolved.stream().distinct().filter(ref -> !removedDocuments.contains(ref)).forEach(removedDocuments::add);

            //... remove the edge document ...
            removedDocuments.add(edgeResolution.getUnresolvedEdgeRef());

            //... create the new edge ...
            insertedDocuments.computeIfAbsent(edgeReference.getArangoCollectionReference(), x -> new ArrayList<>()).add(new Gson().toJson(edgeResolution.getUpdatedEdge()));

            //... and attach it to the document id
            insertedDocuments.computeIfAbsent(InternalSpace.DOCUMENT_ID_EDGE_COLLECTION, x -> new ArrayList<>()).add(new Gson().toJson(entryHookDocuments.createEdgeFromHookDocument(InternalSpace.DOCUMENT_ID_EDGE_COLLECTION, edgeReference, edgeResolution.getUpdatedEdge().getOriginalDocument(), null)));
        });

        Map<ArangoDocumentReference, ArangoDocumentReference> documentIdHooks = new HashMap<>();
        upserts.forEach(upsert -> {
            ArangoCollectionReference collection = upsert.getDocumentReference().getArangoCollectionReference();
            ArangoDocument arangoDocument = ArangoDocument.from(upsert.getPayload());
            arangoDocument.asIndexedDoc().setCollection(arangoDocument.getId().getArangoCollectionReference().getCollectionName());
            arangoDocument.getDoc().normalizeTypes();
            arangoDocument.asIndexedDoc().updateIdentifiers();
            arangoDocument.setKeyBasedOnId();
            insertedDocuments.computeIfAbsent(collection, x->new ArrayList<>()).add(new Gson().toJson(upsert.getPayload()));
            if (upsert.isAttachToOriginalDocument()) {
                //Attention: The following method is non-transactional. It's just the hook-document though and therefore acceptable
                ArangoDocumentReference documentIdHook = documentIdHooks.get(upsert.getLifecycleDocumentId());
                if (documentIdHook == null) {
                    documentIdHook = entryHookDocuments.getOrCreateDocumentIdHookDocument(upsert.getLifecycleDocumentId(), db);
                    documentIdHooks.put(upsert.getLifecycleDocumentId(), documentIdHook);
                }
                insertedDocuments.computeIfAbsent(InternalSpace.DOCUMENT_ID_EDGE_COLLECTION, x->new ArrayList<>()).add(new Gson().toJson(entryHookDocuments.createEdgeFromHookDocument(InternalSpace.DOCUMENT_ID_EDGE_COLLECTION, upsert.getDocumentReference(), documentIdHook, null)));
            }
        });

        collections.addAll(removedDocuments.stream().map(ArangoDocumentReference::getArangoCollectionReference).collect(Collectors.toSet()));
        collections.addAll(insertedDocuments.keySet());

        //Create missing collections...
        collections.forEach(c -> {
            utils.getOrCreateArangoCollection(db, c);
        });

        StreamTransactionEntity tx = db.beginStreamTransaction(new StreamTransactionOptions().writeCollections(collections.stream().map(ArangoCollectionReference::getCollectionName).toArray(String[]::new)));
        DocumentDeleteOptions deleteOptions = new DocumentDeleteOptions().streamTransactionId(tx.getId());
        DocumentCreateOptions insertOptions = new DocumentCreateOptions().streamTransactionId(tx.getId());
        try {
            removedDocuments.stream().collect(Collectors.groupingBy(ArangoDocumentReference::getArangoCollectionReference)).forEach((c,v)->db.collection(c.getCollectionName()).deleteDocuments(v.stream().map(r -> r.getDocumentId().toString()).collect(Collectors.toSet()), String.class, deleteOptions));
            insertedDocuments.forEach((c,v)->db.collection(c.getCollectionName()).insertDocuments(v, insertOptions.overwrite(true)));
            db.commitStreamTransaction(tx.getId());
        } catch (Exception e) {
            logger.debug(String.format("Execution of transaction has failed. \n\n TRANSACTION: %s\n\n", tx.getId()));
            db.abortStreamTransaction(tx.getId());
        }
    }

    private Set<ArangoDocumentReference> findRemovalOfAllDependenciesForDocumentId(ArangoDatabase db, ArangoDocumentReference delete, Set<ArangoDocumentReference> skipList) {
        return findArangoReferencesForDocumentId(db, delete.getDocumentId()).stream().filter(Objects::nonNull).filter(ref -> !skipList.contains(ref)).collect(Collectors.toSet());
    }

    public Paginated<NormalizedJsonLd> queryDocuments(ArangoDatabase db, AQLQuery aqlQuery) {
        return queryDocuments(db, aqlQuery, null);
    }


    public <T> Paginated<T> queryDocuments(ArangoDatabase db, AQLQuery aqlQuery, Function<NormalizedJsonLd, T> mapper) {
        AQL aql = aqlQuery.getAql();
        String value = aql.build().getValue();
        long launch = new Date().getTime();
        ArangoCursor<String> result = db.query(value, aqlQuery.getBindVars(), aql.getQueryOptions(), String.class);
        logger.debug(String.format("Received %d results from Arango in %dms", result.getCount(), new Date().getTime() - launch));
        Long totalCount;
        if (aql.getPaginationParam() != null && aql.getPaginationParam().getSize() != null) {
            totalCount = result.getStats().getFullCount();
        } else {
            totalCount = result.getCount() != null ? result.getCount().longValue() : null;
        }
        List<T> mappedResult;
        List<NormalizedJsonLd> normalizedJsonLds = result.asListRemaining().stream().map(i -> gson.fromJson(i, NormalizedJsonLd.class)).collect(Collectors.toList());
        logger.debug(String.format("Done parsing the results after %dms", new Date().getTime() - launch));

        if (mapper != null) {
            mappedResult = normalizedJsonLds.stream().map(mapper).collect(Collectors.toList());
        } else {
            mappedResult = (List<T>) normalizedJsonLds;
        }
        logger.debug(String.format("Done processing the Arango result - received %d results in %dms total", mappedResult.size(), new Date().getTime() - launch));
        if (aql.getPaginationParam() != null && aql.getPaginationParam().getSize() == null && (int) aql.getPaginationParam().getFrom() > 0 && (int) aql.getPaginationParam().getFrom() < mappedResult.size()) {
            List<T> mappedResultWithOffset = mappedResult.subList((int) aql.getPaginationParam().getFrom(), mappedResult.size());
            return new Paginated<>(mappedResultWithOffset, totalCount, mappedResult.size(), aql.getPaginationParam().getFrom());
        }
        return new Paginated<>(mappedResult, totalCount, mappedResult.size(), aql.getPaginationParam() != null ? aql.getPaginationParam().getFrom() : 0);
    }

}

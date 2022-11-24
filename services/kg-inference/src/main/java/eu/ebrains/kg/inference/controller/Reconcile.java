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

package eu.ebrains.kg.inference.controller;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.GraphDBInstances;
import eu.ebrains.kg.commons.api.GraphDBTypes;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.external.types.TypeInformation;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The reconciliation mechanism allows to unify documents talking about the same entity. The tricky part is the detection
 * of which instances are contributing to the same entity. This is solved by known-semantics. The system can think of
 * these information sources:
 * 1. Linking via shared identifiers ({@link eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary#IDENTIFIER})
 * 2. Explicit links (instances are connected via explicit links (e.g. {@link eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary#SAME_AS} or {@link eu.ebrains.kg.commons.semantics.vocabularies.ProvVocabulary#ALTERNATE_OF})
 * <p>
 * The reconciliation of stable links is rather straight forward: If the linking information is available already at the
 * creation time of an instance and is not manipulated anymore, it will just never get its own id in the inferred space.
 * <p>
 * But since this information is encoded in the payloads, it can always be subject of change (in case 1) or depends on the lifecycle of an additional resource (case 2).
 * It can therefore happen, that instances are temporarily reconciled and/or reconciled in a later step.
 * <p>
 * Here is a small documentation of possible cases:
 * <p>
 * SETUP
 * ======
 * bar.identifier="bar"
 * INSERT bar -&gt;Reconcile "bar" (with info of bar)
 * <p>
 * <p>
 * LINK KNOWN AT INSERTION TIME
 * ============================
 * foo.identifier=["foo", "bar"]
 * <p>
 * INSERT foo -&gt; Reconcile "bar" (with info of bar and foo)
 * UPDATE foo
 * -&gt; foo.identifier==["foo", "bar"] -&gt; Reconcile "bar" (with info of bar and foo)
 * -&gt; foo.identifier==["foo"] -&gt; Reconcile "foo" (with info of foo) and "bar" (with info of bar)
 * DELETE foo -&gt; Reconcile bar (with info of bar)
 * <p>
 * MERGE
 * ===============
 * foo.identifier=["foo"]
 * INSERT foo -&gt; Reconcile "foo" (with info of foo)
 * UPDATE foo -&gt; foo.identifier==["foo", "bar"] -&gt; Reconcile "fooBar" (with identifiers &amp; 302 redirects for "foo" and "bar") (MERGE)
 * <p>
 * MERGE &amp; DELETE
 * ==============
 * foo.identifier=["foo"]
 * INSERT foo -&gt; Reconcile "foo" (with info of foo)
 * UPDATE foo -&gt; foo.identifier==["foo", "bar"] -&gt; Reconcile "fooBar" (with identifiers &amp; 302 redirects for "foo" and "bar") (MERGE)
 * DELETE foo -&gt; Reconcile fooBar (with info of bar, answer queries to "fooBar" with 302 redirect to "bar" since there is only one left)
 * <p>
 * Reconciliation is currently only possible within the same space (since it would mess with the access permissions otherwise)
 * <p>
 * Currently, the reconcile mechanism has the following restrictions:
 * - It only supports instances in the same space (materialized merging of instances across spaces messes up access permissions).
 */
@Component
public class Reconcile {

    private final GraphDBInstances.Client graphDBInstances;
    private final GraphDBTypes.Client graphDBTypes;

    private final IdUtils idUtils;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Reconcile(GraphDBInstances.Client graphDBInstances, GraphDBTypes.Client graphDBTypes, IdUtils idUtils) {
        this.graphDBInstances = graphDBInstances;
        this.graphDBTypes  = graphDBTypes;
        this.idUtils = idUtils;
    }

    static class InvolvedPayloads {
        Set<IndexedJsonLdDoc> documents = new HashSet<>();
        Set<IndexedJsonLdDoc> existingInstances = new HashSet<>();
    }


    private InvolvedPayloads findInvolvedDocuments(SpaceName space, UUID id, Set<UUID> handledDocumentIds, Set<UUID> handledInstanceIds, InvolvedPayloads involvedPayloads, Set<UUID> relatedDocumentIds) {
        List<IndexedJsonLdDoc> relatedInstancesByIdentifiers = graphDBInstances.getDocumentWithRelatedInstancesByIdentifiers(space == null ? null : space.getName(), id, DataStage.NATIVE, true, false).stream().map(IndexedJsonLdDoc::from).collect(Collectors.toList());
        involvedPayloads.documents.addAll(relatedInstancesByIdentifiers);
        handledDocumentIds.add(id);
        relatedDocumentIds.addAll(relatedInstancesByIdentifiers.stream().map(IndexedJsonLdDoc::getDocumentId).collect(Collectors.toSet()));
        //Find already existing instances for this document
        List<InferredJsonLdDoc> inferredInstances = graphDBInstances.getDocumentWithIncomingRelatedInstances(space == null ? null : space.getName(), id, DataStage.IN_PROGRESS, InferredJsonLdDoc.INFERENCE_OF, true, false, false).stream().map(InferredJsonLdDoc::from).collect(Collectors.toList());
        if (inferredInstances.size() > 1) {
            throw new IllegalStateException(String.format("There are %d inferred instances for the id %s (%s)- this is not acceptable", inferredInstances.size(), id, inferredInstances.stream().map(i -> i.asIndexed().getDocumentId().toString()).collect(Collectors.joining(", "))));
        } else if (inferredInstances.size() == 1) {
            //If there is an inferred instance available, we also should take its documents into account...
            IndexedJsonLdDoc inferredInstance = inferredInstances.get(0).asIndexed();
            UUID instanceId = idUtils.getUUID(inferredInstance.getDoc().id());
            if (!handledInstanceIds.contains(instanceId)) {
                handledInstanceIds.add(instanceId);
                involvedPayloads.existingInstances.add(inferredInstance);
                relatedDocumentIds.addAll(inferredInstances.get(0).getInferenceOf().stream().map(idUtils::getUUID).filter(Objects::nonNull).filter(uuid -> !handledDocumentIds.contains(uuid)).collect(Collectors.toSet()));
            }
        }
        final Optional<UUID> nextNonProcessedRelatedDocumentId = relatedDocumentIds.stream().filter(d -> !handledDocumentIds.contains(d)).findFirst();
        nextNonProcessedRelatedDocumentId.ifPresent(uuid -> findInvolvedDocuments(space, uuid, handledDocumentIds, handledInstanceIds, involvedPayloads, relatedDocumentIds));
        return involvedPayloads;
    }

    static class ReconcileUnit {
        Set<IndexedJsonLdDoc> documents = new HashSet<>();
    }


    Set<InferredJsonLdDoc> reconcileDocuments(Set<IndexedJsonLdDoc> documents) {
        Set<ReconcileUnit> reconcileUnits = extractReconcileUnits(documents);
        return reconcileUnits.stream().map(unit -> merge(unit.documents)).collect(Collectors.toSet());
    }

    Set<ReconcileUnit> extractReconcileUnits(Set<IndexedJsonLdDoc> documents) {
        Set<Set<String>> identifiers = combineIds(documents);
        Map<String, ReconcileUnit> reconcileUnits = new HashMap<>();
        identifiers.forEach(ids -> {
            ReconcileUnit reconcileUnit = new ReconcileUnit();
            ids.forEach(id -> reconcileUnits.put(id, reconcileUnit));
        });
        documents.forEach(doc -> {
            ReconcileUnit reconcileUnit = reconcileUnits.get(doc.getDoc().allIdentifiersIncludingId().stream().findFirst().orElse(null));
            reconcileUnit.documents.add(doc);
        });
        return new HashSet<>(reconcileUnits.values());
    }

    Set<Set<String>> combineIds(Set<IndexedJsonLdDoc> documents) {
        Set<Set<String>> idCombinations = new HashSet<>();
        documents.forEach(doc -> {
            Set<String> identifiers = doc.getDoc().allIdentifiersIncludingId();
            Set<String> idCombination = new HashSet<>();
            for (String identifier : identifiers) {
                Iterator<Set<String>> iterator = idCombinations.iterator();
                while (iterator.hasNext()) {
                    Set<String> next = iterator.next();
                    if (next.contains(identifier)) {
                        idCombination.addAll(next);
                        iterator.remove();
                        break;
                    }
                }
            }
            idCombination.addAll(identifiers);
            idCombinations.add(idCombination);
        });
        return idCombinations;
    }

    static class InferenceResult {
        Set<IndexedJsonLdDoc> toBeRemoved = new HashSet<>();
        Map<InferredJsonLdDoc, Set<IndexedJsonLdDoc>> toBeMerged = new HashMap<>();
        Map<InferredJsonLdDoc, IndexedJsonLdDoc> toBeUpdated = new HashMap<>();
        Set<InferredJsonLdDoc> toBeInserted = new HashSet<>();
    }

    InferenceResult compareInferredInstances(Set<IndexedJsonLdDoc> existingInstances, Set<InferredJsonLdDoc> newInstances) {
        Map<InferredJsonLdDoc, Set<IndexedJsonLdDoc>> newToExistingMapping = new HashMap<>();
        Set<String> newTypes = new HashSet<>();
        for (InferredJsonLdDoc newInstance : newInstances) {
            newTypes.addAll(newInstance.asIndexed().getDoc().types());
            newToExistingMapping.computeIfAbsent(newInstance, f -> new HashSet<>());
            IndexedJsonLdDoc newInstanceDoc = newInstance.asIndexed();
            Set<String> allIdentifiersIncludingId = newInstanceDoc.getDoc().allIdentifiersIncludingId();
            existingInstances.forEach(existing -> {
                Set<String> identifiers = new HashSet<>(existing.getDoc().allIdentifiersIncludingId());
                identifiers.retainAll(allIdentifiersIncludingId);
                Set<IndexedJsonLdDoc> relatedExistingInstances = newToExistingMapping.get(newInstance);
                if (!identifiers.isEmpty()) {
                    //This existing instance shares identifiers with the newly reconciled
                    relatedExistingInstances.add(existing);
                }
            });
        }

        //The different cases
        InferenceResult inferenceResult = new InferenceResult();

        //Removal
        inferenceResult.toBeRemoved.addAll(existingInstances);
        inferenceResult.toBeRemoved.removeAll(newToExistingMapping.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()));

        Map<String, Result<TypeInformation>> typesInformation = graphDBTypes.getTypesByName(new ArrayList<>(newTypes), DataStage.IN_PROGRESS, null, false, false);
        for (InferredJsonLdDoc nextNew : newToExistingMapping.keySet()) {
            finalizeInferredDocument(nextNew, typesInformation);
            Set<IndexedJsonLdDoc> existing = newToExistingMapping.get(nextNew);
            if (existing.isEmpty()) {
                //Insert
                inferenceResult.toBeInserted.add(nextNew);
            } else if (existing.size() == 1) {
                //Update
                inferenceResult.toBeUpdated.put(nextNew, existing.iterator().next());
            } else {
                //Merge
                inferenceResult.toBeMerged.put(nextNew, existing);
            }
        }
        return inferenceResult;
    }

    void finalizeInferredDocument(InferredJsonLdDoc inferredJsonLdDoc, Map<String, Result<TypeInformation>> typesInformation){
        List<String> types = inferredJsonLdDoc.asIndexed().getDoc().types();
        List<String> distinctLabelProperties = types.stream().map(typesInformation::get).filter(Objects::nonNull).
                map(Result::getData).filter(Objects::nonNull).
                map(t -> t.getAs(EBRAINSVocabulary.META_TYPE_LABEL_PROPERTY, String.class)).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if(!distinctLabelProperties.isEmpty()){
            Object labelValue = inferredJsonLdDoc.asIndexed().getDoc().get(distinctLabelProperties.get(0));
            if(labelValue!=null) {
                inferredJsonLdDoc.asIndexed().getDoc().put(IndexedJsonLdDoc.LABEL, labelValue);
            }
        }
        logger.debug(String.format("Finalize inferred document with types %s", StringUtils.join(", ", types)));
    }

    List<Event> translateInferenceResultToEvents(SpaceName space, InferenceResult inferenceResult) {
        List<Event> result = new ArrayList<>();
        for (InferredJsonLdDoc inferredJsonLdDoc : inferenceResult.toBeInserted) {
            IndexedJsonLdDoc indexedJsonLdDoc = inferredJsonLdDoc.asIndexed();
            JsonLdId id = indexedJsonLdDoc.getDoc().id();
            if (id == null) {
                //If we want to insert a new inferred instance which has not existed before, it can be that there is no ID yet. We therefore provide a new ID for this one.
                id = idUtils.buildAbsoluteUrl(UUID.randomUUID());
                indexedJsonLdDoc.getDoc().setId(id);
            }
            result.add(Event.createUpsertEvent(space, idUtils.getUUID(id), Event.Type.INSERT, indexedJsonLdDoc.getDoc()));
        }
        for (InferredJsonLdDoc inferredJsonLdDoc : inferenceResult.toBeUpdated.keySet()) {
            //An update requires the uuid of the original document
            IndexedJsonLdDoc updateResult = inferredJsonLdDoc.asIndexed();
            NormalizedJsonLd previousInstance = inferenceResult.toBeUpdated.get(inferredJsonLdDoc).getDoc();
            updateResult.getDoc().setId(previousInstance.id());
            //Additionally, we want to ensure that all identifiers are kept (even if they have disappeared in the meantime)
            updateResult.getDoc().addIdentifiers(previousInstance.allIdentifiersIncludingId().toArray(String[]::new));
            result.add(Event.createUpsertEvent(space, idUtils.getUUID(inferenceResult.toBeUpdated.get(inferredJsonLdDoc).getDoc().id()), Event.Type.UPDATE, inferredJsonLdDoc.asIndexed().getDoc()));
        }
        for (IndexedJsonLdDoc indexedJsonLdDoc : inferenceResult.toBeRemoved) {
            result.add(Event.createDeleteEvent(space, idUtils.getUUID(indexedJsonLdDoc.getDoc().id()), idUtils.buildAbsoluteUrl(indexedJsonLdDoc.getDocumentId())));
        }
        for (InferredJsonLdDoc inferredJsonLdDoc : inferenceResult.toBeMerged.keySet()) {
            //Add all identifiers of the previously inferred instances
            Set<IndexedJsonLdDoc> indexedJsonLdDocs = inferenceResult.toBeMerged.get(inferredJsonLdDoc);
            NormalizedJsonLd doc = inferredJsonLdDoc.asIndexed().getDoc();
            UUID newUUID = UUID.randomUUID();
            doc.setId(idUtils.buildAbsoluteUrl(newUUID));
            doc.addIdentifiers(indexedJsonLdDocs.stream().map(i -> i.getDoc().allIdentifiersIncludingId()).flatMap(Collection::stream).distinct().toArray(String[]::new));
            result.add(Event.createUpsertEvent(space, newUUID, Event.Type.INSERT, doc));
        }
        return result;
    }


    public List<Event> reconcile(SpaceName space, UUID id) {
        InvolvedPayloads involvedPayloads = findInvolvedDocuments(space, id, new HashSet<>(), new HashSet<>(), new InvolvedPayloads(), new HashSet<>());
        Set<InferredJsonLdDoc> inferredJsonLdDocs = reconcileDocuments(involvedPayloads.documents);
        //Compare calculated inferred instances to already existing ones and take according action.
        InferenceResult inferenceResult = compareInferredInstances(involvedPayloads.existingInstances, inferredJsonLdDocs);
        return translateInferenceResultToEvents(space, inferenceResult);
    }

    private JsonLdDoc createAlternative(String key, Object value, boolean selected, List<JsonLdId> users) {
        if (!DynamicJson.isInternalKey(key) && !JsonLdConsts.isJsonLdConst(key) && !SchemaOrgVocabulary.IDENTIFIER.equals(key) && !EBRAINSVocabulary.META_USER.equals(key) && !EBRAINSVocabulary.META_SPACE.equals(key) && !EBRAINSVocabulary.META_PROPERTYUPDATES.equals(key)) {
            JsonLdDoc alternative = new JsonLdDoc();
            alternative.put(EBRAINSVocabulary.META_SELECTED, selected);
            //We always save the users of an alternative as string only to prevent links to be created - the resolution happens lazily.
            alternative.put(EBRAINSVocabulary.META_USER, users.stream().filter(Objects::nonNull).map(JsonLdId::getId).collect(Collectors.toList()));
            alternative.put(EBRAINSVocabulary.META_VALUE, value);
            return alternative;
        }
        return null;
    }

    private InferredJsonLdDoc merge(Set<IndexedJsonLdDoc> originalInstances) {
        if (originalInstances != null && !originalInstances.isEmpty()) {
            InferredJsonLdDoc inferredDocument = InferredJsonLdDoc.create();
            Set<String> keys = originalInstances.stream().map(i -> i.getDoc().keySet()).flatMap(Set::stream).filter(k -> !DynamicJson.isInternalKey(k)).collect(Collectors.toSet());
            JsonLdDoc alternatives = new JsonLdDoc();
            inferredDocument.setAlternatives(alternatives);
            for (String key : keys) {
                //We don't need the property update times in inferred -> this is an information for reconciliation only and therefore should only be in NATIVE
                if (!key.equals(EBRAINSVocabulary.META_PROPERTYUPDATES)) {
                    List<IndexedJsonLdDoc> documentsForKey = originalInstances.stream().filter(i ->  i.getDoc().containsKey(key) || i.getDoc().getAs(EBRAINSVocabulary.META_PROPERTYUPDATES, Map.class, Collections.emptyMap()).containsKey(key)).collect(Collectors.toList());
                    if (documentsForKey.size() == 1) {
                        //Single occurrence - the merge is easy. :)
                        NormalizedJsonLd doc = documentsForKey.get(0).getDoc();
                        final Object value = doc.get(key);
                        //We only add the property to the inferred document if it is not-null.
                        if(value!=null){
                            inferredDocument.asIndexed().getDoc().addProperty(key, value);
                        }
                        JsonLdDoc alternative = createAlternative(key, doc.get(key), true, Collections.singletonList(doc.getAs(EBRAINSVocabulary.META_USER, JsonLdId.class)));
                        if (alternative != null) {
                            alternatives.put(key, Collections.singletonList(alternative));
                        }
                    } else if (documentsForKey.size() > 1) {
                        sortByFieldChangeDate(key, documentsForKey);
                        IndexedJsonLdDoc firstDoc = documentsForKey.get(0);
                        switch (key) {
                            case JsonLdConsts.ID:
                                List<JsonLdId> distinctIds = documentsForKey.stream().map(d -> d.getDoc().id()).distinct().collect(Collectors.toList());
                                if(distinctIds.size()==1){
                                    inferredDocument.asIndexed().getDoc().setId(distinctIds.get(0));
                                }
                                else {
                                    //We don't handle the ID merging - if there are conflicting ids, we create a new one - but this is in the responsibility of the event generation process.
                                }
                                break;
                            case SchemaOrgVocabulary.IDENTIFIER:
                                Set<String> identifiers = documentsForKey.stream().map(d -> d.getDoc().identifiers()).flatMap(Collection::stream).collect(Collectors.toSet());
                                inferredDocument.asIndexed().getDoc().put(SchemaOrgVocabulary.IDENTIFIER, identifiers);
                                break;
                            case EBRAINSVocabulary.META_USER:
                                //Users are ignored for the reconciled instance since they can be reconstructed from the alternatives
                                break;
                            default:
                                final Object propertyValue = firstDoc.getDoc().get(key);
                                //We only add the property to the inferred document if it is not-null.
                                if(propertyValue!=null) {
                                    inferredDocument.asIndexed().getDoc().addProperty(key, propertyValue);
                                }
                                Object nullGroup = new Object();
                                Map<Object, List<IndexedJsonLdDoc>> documentsByValue = documentsForKey.stream().collect(Collectors.groupingBy(d -> d.getDoc().getOrDefault(key, nullGroup)));
                                final List<JsonLdDoc> alternativePayloads = documentsByValue.keySet().stream().map(value -> {
                                    List<IndexedJsonLdDoc> docs = documentsByValue.get(value);
                                    return createAlternative(key, value == nullGroup ? null : value, docs.contains(firstDoc), docs.stream().filter(d -> d.getDoc() != null && d.getDoc().getAs(EBRAINSVocabulary.META_USER, NormalizedJsonLd.class) != null).map(doc -> doc.getDoc().getAs(EBRAINSVocabulary.META_USER, NormalizedJsonLd.class).id()).distinct().collect(Collectors.toList()));
                                }).filter(Objects::nonNull).collect(Collectors.toList());
                                if(!CollectionUtils.isEmpty(alternativePayloads)) {
                                    alternatives.put(key, alternativePayloads);
                                }
                                else{
                                    //FIXME do we want to remove this code? It is cleaning up some data and therefore is rather defensive
                                    alternatives.remove(key);
                                }
                                break;
                        }
                    }
                }
            }
            inferredDocument.setInferenceOf(originalInstances.stream().map(i -> idUtils.buildAbsoluteUrl(i.getDocumentId()).getId()).distinct().collect(Collectors.toList()));
            return inferredDocument;
        }
        return null;
    }

    private void sortByFieldChangeDate(String key, List<IndexedJsonLdDoc> documentsForKey) {
        documentsForKey.sort((o1, o2) -> {
            ZonedDateTime dateTime1 = o1 != null && o1.getDoc().fieldUpdateTimes() != null ? o1.getDoc().fieldUpdateTimes().get(key) : null;
            ZonedDateTime dateTime2 = o2 != null && o2.getDoc().fieldUpdateTimes() != null ? o2.getDoc().fieldUpdateTimes().get(key) : null;
            if (dateTime1 != null) {
                return dateTime2 == null || dateTime1.isBefore(dateTime2) ? 1 : dateTime1.equals(dateTime2) ? 0 : -1;
            }
            return dateTime2 == null ? 0 : -1;
        });
    }
}

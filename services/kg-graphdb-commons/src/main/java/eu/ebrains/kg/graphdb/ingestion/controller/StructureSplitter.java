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

package eu.ebrains.kg.graphdb.ingestion.controller;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.TypeUtils;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.commons.model.ArangoEdge;
import eu.ebrains.kg.graphdb.commons.model.ArangoInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StructureSplitter {

    private final IdUtils idUtils;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeUtils typeUtils;

    public StructureSplitter(IdUtils idUtils, TypeUtils typeUtils) {
        this.idUtils = idUtils;
        this.typeUtils = typeUtils;
    }

    public List<ArangoInstance> extractRelations(ArangoDocumentReference documentReference, NormalizedJsonLd payload) {
        //The extraction of relations manipulates the payload - to avoid
        //any side-effects, we operate on a copy of it.
        ArangoDocument arangoDocument = ArangoDocument.from(new NormalizedJsonLd(payload));
        arangoDocument.setReference(documentReference);
        // if is query, we just return the document as we do not split it
        if(payload.types().contains(EBRAINSVocabulary.META_QUERY_TYPE)){
            return Collections.singletonList(arangoDocument);
        }
        return extractNestedInstances(arangoDocument, arangoDocument.getDoc(), documentReference, new Stack<>(), new ArrayList<>());
    }

    private List<ArangoInstance> extractNestedInstances(ArangoDocument parent, JsonLdDoc subTree, ArangoDocumentReference originalDocumentReference, Stack<String> keyStack, List<ArangoInstance> collector) {
        //We need to do this explicitly to ensure the parent has set its id already - otherwise, the preconditions are not met for the embedded extractions.
        if (parent.getDoc().id() == null) {
            parent.getDoc().setId(subTree.id());
        }
        parent.setOriginalDocument(originalDocumentReference);
        collector.add(parent);
        for (String key : subTree.keySet()) {
            if(EBRAINSVocabulary.META_ALTERNATIVE.equals(key)){
                //It's an alternative - we treat it somewhat different. We want to prevent the alternatives to become multiple documents although they are nested
                handleAlternative(parent, subTree, originalDocumentReference, collector);
            }
            else if (!EBRAINSVocabulary.META_PROPERTYUPDATES.equals(key) && !DynamicJson.isInternalKey(key)) {
                keyStack.push(key);
                Object value = subTree.get(key);
                int i = 0;
                if (value instanceof Collection) {
                    List newList = new ArrayList();
                    for (Object individualValue : ((Collection) value)) {
                        ArangoEdge arangoEdge = null;
                        if (individualValue instanceof Map) {
                            arangoEdge = extractEdge(parent, typeUtils.translate(individualValue, NormalizedJsonLd.class), originalDocumentReference, keyStack, i, collector);
                            i++;
                        }
                        if (arangoEdge != null) {
                            newList.add(arangoEdge.getOriginalTo());
                        } else {
                            newList.add(individualValue);
                        }
                    }
                    parent.getDoc().put(key, newList);
                } else {
                    ArangoEdge arangoEdge = null;
                    if (value instanceof JsonLdId || value instanceof Map) {
                        arangoEdge = extractEdge(parent, typeUtils.translate(value, NormalizedJsonLd.class), originalDocumentReference, keyStack, i, collector);
                    }
                    if (arangoEdge != null) {
                        parent.getDoc().addProperty(key, arangoEdge.getOriginalTo());
                    } else {
                        parent.getDoc().addProperty(key, value);
                    }
                }
                keyStack.pop();
            } else if (InferredJsonLdDoc.isInferenceOfKey(key)) {
                List<String> originalTos = (List<String>) parent.getDoc().get(key);
                originalTos.forEach(originalTo -> handleInferenceOfReference(originalDocumentReference, collector, new JsonLdId(originalTo)));
            }
        }
        return collector;
    }



    private void handleAlternative(ArangoDocument parent, JsonLdDoc subTree, ArangoDocumentReference originalDocumentReference, List<ArangoInstance> collector) {
        NormalizedJsonLd alternativePayload = subTree.getAs(EBRAINSVocabulary.META_ALTERNATIVE, NormalizedJsonLd.class);
        ArangoDocument alternative = ArangoDocument.from(alternativePayload);
        alternative.asIndexedDoc().setEmbedded(true);
        alternative.asIndexedDoc().setAlternative(true);
        UUID alternativeDocumentId = UUID.randomUUID();
        JsonLdId alternativeId = idUtils.buildAbsoluteUrl(alternativeDocumentId);
        alternative.getDoc().setId(alternativeId);
        alternative.setReference(parent.getId().getArangoCollectionReference().doc(alternativeDocumentId));
        alternative.setOriginalDocument(originalDocumentReference);
        collector.add(alternative);
        ArangoEdge edge = new ArangoEdge();
        edge.setOriginalTo(alternativeId);
        edge.setTo(parent.getId().getArangoCollectionReference().doc(alternativeDocumentId));
        edge.setOriginalLabel(EBRAINSVocabulary.META_ALTERNATIVE);
        edge.setOriginalDocument(originalDocumentReference);
        edge.setFrom(parent.getId());
        edge.redefineId(ArangoCollectionReference.fromSpace(new SpaceName(EBRAINSVocabulary.META_ALTERNATIVE)).doc(UUID.randomUUID()));
        collector.add(edge);
        subTree.put(EBRAINSVocabulary.META_ALTERNATIVE, alternativeId);
    }

    private void handleInferenceOfReference(ArangoDocumentReference from, List<ArangoInstance> collector, JsonLdId originalTo) {
        ArangoEdge edge = new ArangoEdge();
        edge.redefineId(ArangoCollectionReference.fromSpace(InternalSpace.INFERENCE_OF_SPACE).doc(UUID.randomUUID()));
        edge.setOriginalTo(originalTo);
        edge.setFrom(from);
        collector.add(edge);
    }

    private ArangoEdge extractEdge(ArangoDocument parent, NormalizedJsonLd subTree, ArangoDocumentReference originalDocumentRef, Stack<String> keyStack, int orderNumber, List<ArangoInstance> collector) {
        ArangoEdge edge = new ArangoEdge();
        edge.setOrderNumber(orderNumber);
        UUID embeddedDocumentId = UUID.randomUUID();
        JsonLdId embeddedId = idUtils.buildAbsoluteUrl(embeddedDocumentId);
        JsonLdId id = null;
        try {
            id = subTree.id();
        } catch (IllegalArgumentException e) {
            logger.warn("Found an invalid reference in a document. Skipping it...", e);
            return null;
        }
        if (id == null) {
            //It's not a link but an embedded instance. We need to follow and extract it.
            ArangoDocument embedded = ArangoDocument.create();
            embedded.asIndexedDoc().setEmbedded(true);
            embedded.getDoc().setId(embeddedId);
            embedded.setReference(parent.getId().getArangoCollectionReference().doc(embeddedDocumentId));
            extractNestedInstances(embedded, subTree, originalDocumentRef, keyStack, collector);
            edge.setOriginalTo(embeddedId);
            edge.setTo(originalDocumentRef.getArangoCollectionReference().doc(embeddedDocumentId));
        } else {
            edge.setOriginalTo(id);
            // As part of the inference, we ignore all additional information provided next to the link.
            // If we want to support the inline-creation of new instances, we need to do the splitting the latest on the primary store level.
            // In this case, the information would arrive in inference as individual instances. But be careful, this has severe impact on the life-cycle of the document!
        }
        String relationName = keyStack.peek();
        edge.setOriginalLabel(relationName);
        edge.setOriginalDocument(originalDocumentRef);
        edge.setFrom(parent.getId());
        edge.redefineId(ArangoCollectionReference.fromSpace(new SpaceName(relationName)).doc(UUID.randomUUID()));
        if (edge.getOriginalTo() != null) {
            collector.add(edge);
            return edge;
        }
        return null;
    }

}

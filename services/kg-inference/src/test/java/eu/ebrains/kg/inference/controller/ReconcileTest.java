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

package eu.ebrains.kg.inference.controller;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.InferredJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.inference.serviceCall.GraphDBSvc;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class ReconcileTest {

    IdUtils idUtils;
    Reconcile reconcile;
    @Before
    public void setup(){
        idUtils = new IdUtils("http://foobar/");
        reconcile = new Reconcile(Mockito.mock(GraphDBSvc.class), idUtils);
    }

    private IndexedJsonLdDoc createDoc(String... identifiers){
        IndexedJsonLdDoc doc = IndexedJsonLdDoc.from(new NormalizedJsonLd());
        doc.getDoc().addIdentifiers(identifiers);
        doc.updateIdentifiers();
        return doc;
    }

    @Test
    public void testCombineIds0() {
        List<IndexedJsonLdDoc> docs = Arrays.asList(createDoc("A"), createDoc("B"),
                createDoc("C"));


        Set<Set<String>> ids = reconcile.combineIds(new HashSet<>(docs));

        assertEquals(3, ids.size());
        Set<Set<String>> expected = new HashSet<>();
        expected.add(new HashSet<>(Arrays.asList("A")));
        expected.add(new HashSet<>(Arrays.asList("B")));
        expected.add(new HashSet<>(Arrays.asList("C")));
        assertEquals(expected, ids);

    }

    @Test
    public void testCombineIds1() {
       List<IndexedJsonLdDoc> docs = Arrays.asList(createDoc("A"), createDoc("B"),
                createDoc("C"), createDoc("A", "B"));

        Set<Set<String>> ids = reconcile.combineIds(new HashSet<>(docs));

        assertEquals(2, ids.size());
        Set<Set<String>> expected = new HashSet<>();
        expected.add(new HashSet<>(Arrays.asList("A", "B")));
        expected.add(new HashSet<>(Arrays.asList("C")));
        assertEquals(expected, ids);

    }

    @Test
    public void testCombineIds2() {
        List<IndexedJsonLdDoc> docs = Arrays.asList(createDoc("A"), createDoc("B"),
                createDoc("C"), createDoc("B", "C"), createDoc( "A", "B"));

        Set<Set<String>> ids = reconcile.combineIds(new HashSet<>(docs));

        assertEquals(1, ids.size());
        Set<Set<String>> expected = new HashSet<>();
        expected.add(new HashSet<>(Arrays.asList("A", "B", "C")));
        assertEquals(expected, ids);

    }



    @Test
    public void testExtractReconcileUnits() {
        List<IndexedJsonLdDoc> docs = Arrays.asList(createDoc("A"), createDoc("B"), createDoc("C"), createDoc("D", "B", "C"), createDoc("E","A", "B"));

        Set<Reconcile.ReconcileUnit> reconcileUnits = reconcile.extractReconcileUnits(new HashSet<>(docs));

        assertEquals(1, reconcileUnits.size());
        assertEquals(new HashSet<>(docs), reconcileUnits.iterator().next().documents);

    }


    @Test
    public void testCompareInferredInstancesInsertUpdateRemove() {
        //given
        Set<IndexedJsonLdDoc> existingInstances = new HashSet<>();
        Set<InferredJsonLdDoc> inferredJsonLdDocs = new HashSet<>();

        existingInstances.add(createDoc("A"));
        existingInstances.add(createDoc("B"));

        inferredJsonLdDocs.add(InferredJsonLdDoc.from(createDoc("A")));
        inferredJsonLdDocs.add(InferredJsonLdDoc.from(createDoc("C")));

        //when
        Reconcile.InferenceResult inferenceResult = reconcile.compareInferredInstances(existingInstances, inferredJsonLdDocs);


        //then
        assertNotNull(inferenceResult);
        //We assume that there is one update for "a", the removal of "b" and the insertion of "c"
        assertEquals(1, inferenceResult.toBeInserted.size());
        assertEquals(1, inferenceResult.toBeRemoved.size());
        assertEquals(1, inferenceResult.toBeUpdated.size());

        //Ensure that there is nothing else...
        assertEquals(0, inferenceResult.toBeMerged.size());
    }


    @Test
    public void testCompareInferredInstancesMerge() {
        //given
        Set<IndexedJsonLdDoc> existingInstances = new HashSet<>();
        Set<InferredJsonLdDoc> inferredJsonLdDocs = new HashSet<>();

        existingInstances.add(createDoc("A"));
        existingInstances.add(createDoc("B"));

        inferredJsonLdDocs.add(InferredJsonLdDoc.from(createDoc("X", "A", "B")));

        //when
        Reconcile.InferenceResult inferenceResult = reconcile.compareInferredInstances(existingInstances, inferredJsonLdDocs);

        //then
        assertNotNull(inferenceResult);
        //We assume that there is one merge with two sources ("a" and "b")
        assertEquals(1, inferenceResult.toBeMerged.size());
        assertEquals(2, inferenceResult.toBeMerged.get(inferenceResult.toBeMerged.keySet().iterator().next()).size());

        //Ensure that there is nothing else...
        assertEquals(0, inferenceResult.toBeRemoved.size());
        assertEquals(0, inferenceResult.toBeUpdated.size());
        assertEquals(0, inferenceResult.toBeInserted.size());
    }


    @Test
    public void testTranslateMergeInferenceResultToEvents(){
        //Given
        Space space = new Space("Foobar");
        Reconcile.InferenceResult result = new Reconcile.InferenceResult();
        Set<IndexedJsonLdDoc> existing = new HashSet<>();
        existing.add(createDoc("A", "X"));
        existing.add(createDoc("B"));
        result.toBeMerged.put(InferredJsonLdDoc.from(createDoc( "A", "B")), existing);

        //When
        List<Event> events = reconcile.translateInferenceResultToEvents(space, result);

        //Then
        assertNotNull(events);
        assertEquals(3, events.size());
        for (Event event : events) {
            if(event.getType() == Event.Type.INSERT){
                assertTrue(event.getData().getAllIdentifiersIncludingId().contains("A"));
                assertTrue(event.getData().getAllIdentifiersIncludingId().contains("B"));
                assertTrue(event.getData().getAllIdentifiersIncludingId().contains("X"));
            }
        }

    }
}
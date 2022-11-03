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

package eu.ebrains.kg.inference.controller;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.GraphDBInstances;
import eu.ebrains.kg.commons.api.GraphDBTypes;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.InferredJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ReconcileTest {

    IdUtils idUtils;
    Reconcile reconcile;
    @BeforeEach
    public void setup(){
        idUtils = new IdUtils("http://foobar/");
        reconcile = new Reconcile(Mockito.mock(GraphDBInstances.Client.class), Mockito.mock(GraphDBTypes.Client.class), idUtils);
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




}
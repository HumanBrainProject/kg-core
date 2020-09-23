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

package eu.ebrains.kg.core.api.instances.functionality;

import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.AbstractFunctionalityTest;
import eu.ebrains.kg.testutils.TestDataFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class InstancesFunctionalityTest extends AbstractFunctionalityTest {

    @Autowired
    Instances instances;

    @Override
    protected void authenticate() {
        beAdmin();
    }


    IngestConfiguration defaultIngestConfiguration = new IngestConfiguration().setNormalizePayload(false).setDeferInference(false);
    ResponseConfiguration defaultResponseConfiguration = new ResponseConfiguration().setReturnEmbedded(true).setReturnPermissions(false).setReturnAlternatives(false).setReturnPayload(true);
    PaginationParam defaultPaginationParam = new PaginationParam().setFrom(0).setSize(20l);

    @Test
    public void testCreateInstance() {
        //Given
        JsonLdDoc testData = TestDataFactory.createTestData(smallPayload, 0, true);

        //When
        ResponseEntity<Result<NormalizedJsonLd>> response = instances.createNewInstance(testData, "functionalityTest", defaultResponseConfiguration, defaultIngestConfiguration, null);

        //Then
        assureValidPayloadIncludingId(response);
    }

    @Test
    public void testCreateInstanceWithSpecifiedUUID() {
        //Given
        JsonLdDoc testData = TestDataFactory.createTestData(smallPayload, 0, true);
        UUID clientSpecifiedUUID = UUID.randomUUID();

        //When
        ResponseEntity<Result<NormalizedJsonLd>> response = instances.createNewInstance(testData, clientSpecifiedUUID, "functionalityTest", defaultResponseConfiguration, defaultIngestConfiguration, null);

        //Then
        NormalizedJsonLd document = assureValidPayloadIncludingId(response);
        assertEquals(clientSpecifiedUUID, idUtils.getUUID(document.id()));
    }


    private NormalizedJsonLd createInstanceWithServerDefinedUUID(int iteration) {
        JsonLdDoc originalDoc = TestDataFactory.createTestData(smallPayload, iteration, true);
        ResponseEntity<Result<NormalizedJsonLd>> response = instances.createNewInstance(originalDoc, "functionalityTest", defaultResponseConfiguration, defaultIngestConfiguration, null);
        return response.getBody().getData();
    }

    @Test
    public void testContributeToInstanceFullReplacement() {
        //Given
        NormalizedJsonLd originalInstance = createInstanceWithServerDefinedUUID(0);
        JsonLdDoc update = TestDataFactory.createTestData(smallPayload, 0, true);

        //When
        ResponseEntity<Result<NormalizedJsonLd>> response = instances.contributeToInstanceFullReplacement(update, idUtils.getUUID(originalInstance.id()), false, defaultResponseConfiguration, defaultIngestConfiguration, null);

        //Then
        NormalizedJsonLd document = assureValidPayloadIncludingId(response);
        originalInstance.keySet().stream().filter(k -> k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX)).forEach(k -> {
            assertNotNull(document.get(k));
            assertNotEquals("The dynamic properties should change when doing the update", originalInstance.get(k), document.get(k));
        });
        originalInstance.keySet().stream().filter(k -> !k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX) && !k.startsWith(EBRAINSVocabulary.META)).forEach(k -> {
            assertNotNull(document.get(k));
            assertEquals("The non-dynamic properties should remain the same when doing a contribution", originalInstance.get(k), document.get(k));
        });
    }

    @Test
    public void testContributeToInstancePartialReplacement() {
        //Given
        NormalizedJsonLd originalInstance = createInstanceWithServerDefinedUUID(0);
        JsonLdDoc update = new JsonLdDoc();
        update.put(TestDataFactory.DYNAMIC_FIELD_PREFIX + "0", "foobar");

        //When
        ResponseEntity<Result<NormalizedJsonLd>> response = instances.contributeToInstancePartialReplacement(update, idUtils.getUUID(originalInstance.id()), false, defaultResponseConfiguration, defaultIngestConfiguration, null);

        //Then
        NormalizedJsonLd document = assureValidPayloadIncludingId(response);
        originalInstance.keySet().stream().filter(k -> k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX)).forEach(k -> {
            if (k.equals(TestDataFactory.DYNAMIC_FIELD_PREFIX + "0")) {
                assertNotNull(document.get(k));
                assertNotEquals("The dynamic property should change when doing the update", originalInstance.get(k), document.get(k));
            } else {
                assertNotNull(document.get(k));
                assertEquals("All other dynamic properties should remain the same after a partial update", originalInstance.get(k), document.get(k));
            }
        });
        originalInstance.keySet().stream().filter(k -> !k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX) && !k.startsWith(EBRAINSVocabulary.META)).forEach(k -> {
            assertNotNull(document.get(k));
            assertEquals("The non-dynamic properties should remain the same when doing a contribution", originalInstance.get(k), document.get(k));
        });
    }

    @Test
    public void getInstanceById() {
        //Given
        NormalizedJsonLd originalInstance = createInstanceWithServerDefinedUUID(0);

        //When
        ResponseEntity<Result<NormalizedJsonLd>> instanceById = instances.getInstanceById(idUtils.getUUID(originalInstance.id()), ExposedStage.IN_PROGRESS, defaultResponseConfiguration);

        //Then
        NormalizedJsonLd normalizedJsonLd = assureValidPayloadIncludingId(instanceById);
        assertEquals(originalInstance, normalizedJsonLd);
    }

    @Test
    public void getInstanceScopeSimple() {
        //Given
        NormalizedJsonLd originalInstance = createInstanceWithServerDefinedUUID(0);

        //When
        ResponseEntity<Result<ScopeElement>> scope = instances.getInstanceScope(idUtils.getUUID(originalInstance.id()), ExposedStage.IN_PROGRESS, false);

        //Then
        ScopeElement scopeElement = assureValidPayload(scope);
        assertNotNull(scopeElement.getId());
        assertNotNull(scopeElement.getTypes());
        assertNotNull(scopeElement.getSpace());
        assertNull(scopeElement.getChildren());
    }

    @Test
    public void getInstanceNeighborsSimple() {
        //Given
        NormalizedJsonLd originalInstance = createInstanceWithServerDefinedUUID(0);

        //When
        ResponseEntity<Result<GraphEntity>> neighbors = instances.getNeighbors(idUtils.getUUID(originalInstance.id()), ExposedStage.IN_PROGRESS);

        //Then
        GraphEntity neighborsGraph = assureValidPayload(neighbors);
        assertNotNull(neighborsGraph.getId());
        assertNotNull(neighborsGraph.getTypes());
        assertNotNull(neighborsGraph.getSpace());
        assertTrue(neighborsGraph.getInbound().isEmpty());
        assertTrue(neighborsGraph.getOutbound().isEmpty());
    }


    @Test
    public void getInstances() {
        //Given
        NormalizedJsonLd originalInstanceA = createInstanceWithServerDefinedUUID(0);
        NormalizedJsonLd originalInstanceB = createInstanceWithServerDefinedUUID(1);

        //When
        PaginatedResult<NormalizedJsonLd> instances = this.instances.getInstances(ExposedStage.IN_PROGRESS, originalInstanceA.types().get(0), null, null, defaultResponseConfiguration, defaultPaginationParam);

        //Then
        assertEquals(0, instances.getFrom());
        assertEquals(2, instances.getSize());
        assertEquals(2, instances.getTotal());
        List<NormalizedJsonLd> data = instances.getData();
        assertNotNull(data);
        assertEquals(2, data.size());
        assertTrue(data.contains(originalInstanceA));
        assertTrue(data.contains(originalInstanceB));
    }

    @Test
    public void getInstancesByIds() {
        //Given
        NormalizedJsonLd originalInstanceA = createInstanceWithServerDefinedUUID(0);
        NormalizedJsonLd originalInstanceB = createInstanceWithServerDefinedUUID(1);
        Map<UUID, NormalizedJsonLd> originalInstances = Stream.of(originalInstanceA, originalInstanceB).collect(Collectors.toMap(k -> idUtils.getUUID(k.id()), v -> v));
        List<UUID> ids = Arrays.asList(idUtils.getUUID(originalInstanceA.id()), idUtils.getUUID(originalInstanceB.id()));

        //When
        Result<Map<String, Result<NormalizedJsonLd>>> instancesByIds = this.instances.getInstancesByIds(ids.stream().map(UUID::toString).collect(Collectors.toList()), ExposedStage.IN_PROGRESS, defaultResponseConfiguration);

        //Then
        assertNotNull(instancesByIds);
        assertNotNull(instancesByIds.getData());
        assertEquals(2, instancesByIds.getData().size());
        ids.forEach(id -> {
            assertTrue(instancesByIds.getData().containsKey(id.toString()));
            NormalizedJsonLd original = originalInstances.get(id);
            Result<NormalizedJsonLd> result = instancesByIds.getData().get(id.toString());
            assertNotNull(result.getData());
            assertEquals(result.getData(), original);
        });
    }

    @Test
    public void getInstancesByIdentifiers() {
        //Given
        NormalizedJsonLd originalInstance = createInstanceWithServerDefinedUUID(0);
        JsonLdDoc doc = new JsonLdDoc();
        String identifier = "https://foo/bar";
        doc.addIdentifiers(identifier);
        ResponseEntity<Result<NormalizedJsonLd>> updateResult = instances.contributeToInstancePartialReplacement(doc, idUtils.getUUID(originalInstance.id()), false, defaultResponseConfiguration, defaultIngestConfiguration, null);

        //When
        Result<List<NormalizedJsonLd>> instancesByIdentifiers = instances.getInstancesByIdentifiers(Collections.singletonList(identifier), ExposedStage.IN_PROGRESS, defaultResponseConfiguration);

        //Then
        assertNotNull(instancesByIdentifiers);
        assertNotNull(instancesByIdentifiers.getData());
        assertEquals(1, instancesByIdentifiers.getData().size());
        assertEquals(instancesByIdentifiers.getData().get(0), updateResult.getBody().getData());
    }


    @Test
    public void deleteInstance() {
        //Given
        NormalizedJsonLd originalInstance = createInstanceWithServerDefinedUUID(0);

        //When
        instances.deleteInstance(idUtils.getUUID(originalInstance.id()), null);

        //Then
        ResponseEntity<Result<NormalizedJsonLd>> instanceById = instances.getInstanceById(idUtils.getUUID(originalInstance.id()), ExposedStage.IN_PROGRESS, defaultResponseConfiguration);
        assertEquals("We expect a 404 to be returned from instanceById", HttpStatus.NOT_FOUND, instanceById.getStatusCode());
    }


    @Test
    public void releaseInstance() {
        //Given
        NormalizedJsonLd originalInstance = createInstanceWithServerDefinedUUID(0);

        //When
        instances.releaseInstance(idUtils.getUUID(originalInstance.id()), null);

        //Then
        ResponseEntity<Result<NormalizedJsonLd>> instanceById = instances.getInstanceById(idUtils.getUUID(originalInstance.id()), ExposedStage.RELEASED, defaultResponseConfiguration);
        NormalizedJsonLd releasedInstance = assureValidPayloadIncludingId(instanceById);
        assertEquals(releasedInstance.removeAllFieldsFromNamespace(EBRAINSVocabulary.META), originalInstance.removeAllFieldsFromNamespace(EBRAINSVocabulary.META));
    }

    @Test
    public void unreleaseInstance() {
        //Given
        NormalizedJsonLd originalInstance = createInstanceWithServerDefinedUUID(0);
        instances.releaseInstance(idUtils.getUUID(originalInstance.id()), null);

        //When
        instances.unreleaseInstance(idUtils.getUUID(originalInstance.id()));

        //Then
        ResponseEntity<Result<NormalizedJsonLd>> releasedInstanceById = instances.getInstanceById(idUtils.getUUID(originalInstance.id()), ExposedStage.RELEASED, defaultResponseConfiguration);
        assertEquals("We expect a 404 to be returned from instanceById in released scope", HttpStatus.NOT_FOUND, releasedInstanceById.getStatusCode());

        //Just to be sure - we want to check if the instance is still available in the inferred space.
        ResponseEntity<Result<NormalizedJsonLd>> inferredInstanceById = instances.getInstanceById(idUtils.getUUID(originalInstance.id()), ExposedStage.IN_PROGRESS, defaultResponseConfiguration);
        assureValidPayloadIncludingId(inferredInstanceById);
    }

    @Test
    public void getReleaseStatus() {
        //Given
        NormalizedJsonLd originalInstance = createInstanceWithServerDefinedUUID(0);
        UUID uuid = idUtils.getUUID(originalInstance.id());

        //When
        ReleaseStatus releaseStatusAfterCreation = assureValidPayload(instances.getReleaseStatus(uuid, ReleaseTreeScope.TOP_INSTANCE_ONLY));
        //Then
        assertEquals(ReleaseStatus.UNRELEASED, releaseStatusAfterCreation);

        //When
        instances.releaseInstance(uuid, null);
        ReleaseStatus releaseStatusAfterReleasing = assureValidPayload(instances.getReleaseStatus(uuid, ReleaseTreeScope.TOP_INSTANCE_ONLY));

        //Then
        assertEquals(ReleaseStatus.RELEASED, releaseStatusAfterReleasing);

        //When
        instances.contributeToInstancePartialReplacement(TestDataFactory.createTestData(smallPayload, 0, true), uuid, false, defaultResponseConfiguration, defaultIngestConfiguration, null);
        ReleaseStatus releaseStatusAfterChange = assureValidPayload(instances.getReleaseStatus(uuid, ReleaseTreeScope.TOP_INSTANCE_ONLY));

        //Then
        assertEquals(ReleaseStatus.HAS_CHANGED, releaseStatusAfterChange);

        //When
        instances.unreleaseInstance(uuid);
        ReleaseStatus releaseStatusAfterUnreleasing = assureValidPayload(instances.getReleaseStatus(uuid, ReleaseTreeScope.TOP_INSTANCE_ONLY));
        //Then
        assertEquals(ReleaseStatus.UNRELEASED, releaseStatusAfterUnreleasing);
    }

    @Test
    public void getReleaseStatusByIds() {
        //Given
        NormalizedJsonLd originalInstanceA = createInstanceWithServerDefinedUUID(0);
        UUID uuidA = idUtils.getUUID(originalInstanceA.id());

        NormalizedJsonLd originalInstanceB = createInstanceWithServerDefinedUUID(1);
        UUID uuidB = idUtils.getUUID(originalInstanceB.id());

        //When
        instances.releaseInstance(uuidA, null);

        //Then
        Result<Map<UUID, Result<ReleaseStatus>>> releaseStatusByIds = instances.getReleaseStatusByIds(Arrays.asList(uuidA, uuidB), ReleaseTreeScope.TOP_INSTANCE_ONLY);
        assertNotNull(releaseStatusByIds);
        assertNotNull(releaseStatusByIds.getData());
        assertNotNull(releaseStatusByIds.getData().get(uuidA));
        assertNotNull(releaseStatusByIds.getData().get(uuidB));
        assertEquals(ReleaseStatus.RELEASED, releaseStatusByIds.getData().get(uuidA).getData());
        assertEquals(ReleaseStatus.UNRELEASED, releaseStatusByIds.getData().get(uuidB).getData());
    }

}

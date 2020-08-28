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

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.ReleaseStatus;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.AbstractSystemTest;
import eu.ebrains.kg.testutils.TestDataFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.junit.Assert.*;

public class WorkflowSystemTest extends AbstractSystemTest {

    @Autowired
    private Releases releases;

    @Autowired
    private IdUtils idUtils;


    @Test
    public void testReleaseAndUnreleaseAndReReleaseInstance() throws IOException {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(smallPayload, true, 0, null);
        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);
        JsonLdId id = instance.getBody().getData().getId();
        IndexedJsonLdDoc from = IndexedJsonLdDoc.from(instance.getBody().getData());

        //When
        releases.releaseInstance(idUtils.getUUID(id), from.getRevision());
        ResponseEntity<Result<ReleaseStatus>> releaseStatus = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        //Then
        assertEquals(ReleaseStatus.RELEASED.getReleaseStatus(), releaseStatus.getBody().getData().getReleaseStatus());

        releases.unreleaseInstance(idUtils.getUUID(id));
        ResponseEntity<Result<ReleaseStatus>> releaseStatusAfterUnrelease = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        assertEquals(ReleaseStatus.UNRELEASED.getReleaseStatus(), releaseStatusAfterUnrelease.getBody().getData().getReleaseStatus());

        releases.releaseInstance(idUtils.getUUID(id), from.getRevision());
        ResponseEntity<Result<ReleaseStatus>> releaseStatusRerelease = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);
        assertEquals(ReleaseStatus.RELEASED.getReleaseStatus(), releaseStatusRerelease.getBody().getData().getReleaseStatus());
    }

    @Test
    public void testInsertAndDeleteInstance() throws IOException {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(smallPayload, true, 0, null);
        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);
        JsonLdId id = instance.getBody().getData().getId();

        //When
        ResponseEntity<Result<Void>> resultResponseEntity = instances.deleteInstance(idUtils.getUUID(id), null);

        //Then
        assertEquals(HttpStatus.OK, resultResponseEntity.getStatusCode());

        ResponseEntity<Result<NormalizedJsonLd>> instanceById = instances.getInstanceById(idUtils.getUUID(id), ExposedStage.IN_PROGRESS, DEFAULT_RESPONSE_CONFIG);

        assertEquals(HttpStatus.NOT_FOUND, instanceById.getStatusCode());

    }

    @Test
    public void testInsertAndUpdateInstance() throws IOException {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(smallPayload, true, 0, null);

        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);
        JsonLdId id = instance.getBody().getData().getId();

        //When
        JsonLdDoc doc = new JsonLdDoc();
        doc.addProperty("https://core.kg.ebrains.eu/fooE", "fooEUpdated");
        ResponseEntity<Result<NormalizedJsonLd>> resultResponseEntity = instances.contributeToInstancePartialReplacement(doc, idUtils.getUUID(id), false, DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);

        //Then
        assertEquals("fooEUpdated", resultResponseEntity.getBody().getData().getAs("https://core.kg.ebrains.eu/fooE", String.class));
    }

    @Ignore("Failing")
    @Test
    public void testFullCycle() throws IOException {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(smallPayload, true, 0, null);
        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);
        JsonLdId id = instance.getBody().getData().getId();
        IndexedJsonLdDoc from = IndexedJsonLdDoc.from(instance.getBody().getData());

        //When
        //Update
        JsonLdDoc doc = new JsonLdDoc();
        doc.addProperty("https://core.kg.ebrains.eu/fooE", "fooEUpdated");
        ResponseEntity<Result<NormalizedJsonLd>> resultResponseEntity = instances.contributeToInstancePartialReplacement(doc, idUtils.getUUID(id), false, DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);

        //Then
        assertEquals("fooEUpdated", resultResponseEntity.getBody().getData().getAs("https://core.kg.ebrains.eu/fooE", String.class));

        //When
        //Release
        releases.releaseInstance(idUtils.getUUID(id), from.getRevision());
        ResponseEntity<Result<ReleaseStatus>> releaseStatus = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        //Then
        assertEquals(ReleaseStatus.RELEASED.getReleaseStatus(), releaseStatus.getBody().getData().getReleaseStatus());

        //When
        //Unrelease
        releases.unreleaseInstance(idUtils.getUUID(id));
        ResponseEntity<Result<ReleaseStatus>> releaseStatusAfterUnrelease = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        //Then
        assertEquals(ReleaseStatus.UNRELEASED.getReleaseStatus(), releaseStatusAfterUnrelease.getBody().getData().getReleaseStatus());

        //When
        //Delete
        ResponseEntity<Result<Void>> resultResponseEntityDeleted = instances.deleteInstance(idUtils.getUUID(id), null);
        //Then
        assertEquals(HttpStatus.OK, resultResponseEntityDeleted.getStatusCode());

        ResponseEntity<Result<NormalizedJsonLd>> instanceById = instances.getInstanceById(idUtils.getUUID(id), ExposedStage.IN_PROGRESS, DEFAULT_RESPONSE_CONFIG);

        assertEquals(HttpStatus.NOT_FOUND, instanceById.getStatusCode());
    }

}

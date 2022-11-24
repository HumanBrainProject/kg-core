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

package eu.ebrains.kg.release.api;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.release.controller.Release;
import eu.ebrains.kg.test.TestCategories;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
//FIXME - We need to transfer this to a system test (in the bundle)
@Disabled
@SpringBootTest
@Tag(TestCategories.API)
public class ReleaseTest {

    @Autowired
    Release release;

    SpaceName space = new SpaceName("test");
    UUID docId = UUID.randomUUID();



    @Autowired
    IdUtils idUtils;

//    @Test(expected = IllegalArgumentException.class)
//    public void testIncorrectRevisionProvidedShouldBeThrown(){
//        //Given
//        NormalizedJsonLd jsonLd = new NormalizedJsonLd();
//        jsonLd.setId(idUtils.buildAbsoluteUrl(this.docId));
//        graphDb.upsert(jsonLd, DataStage.IN_PROGRESS, this.docId, space);
//
//        //When
//        release.release(space, this.docId, "1");
//
//    }
//
//
//    @Test
//    public void testGetReleaseStatusIsUnreleasedByDefault(){
//        //Given
//        NormalizedJsonLd jsonLd = new NormalizedJsonLd();
//        jsonLd.setId(idUtils.buildAbsoluteUrl(this.docId));
//        graphDb.upsert(jsonLd, DataStage.IN_PROGRESS, this.docId, space);
//
//        //When
//        ReleaseStatus status = release.getStatus(space, docId, ReleaseTreeScope.TOP_INSTANCE_ONLY);
//
//        //Then
//        assertEquals(ReleaseStatus.UNRELEASED, status);
//    }
//
//    @Test
//    public void testGetReleaseStatusIsReleased(){
//        //Given
//        NormalizedJsonLd jsonLd = new NormalizedJsonLd();
//        jsonLd.setId(idUtils.buildAbsoluteUrl(this.docId));
//        graphDb.upsert(jsonLd, DataStage.IN_PROGRESS, this.docId, space);
//        IndexedJsonLdDoc inferredDoc = graphDb.get(DataStage.IN_PROGRESS, space, docId);
//        release.release(space, docId, inferredDoc.getRevision());
//
//        //When
//        ReleaseStatus status = release.getStatus(space, docId, ReleaseTreeScope.TOP_INSTANCE_ONLY);
//
//        //Then
//        assertEquals(ReleaseStatus.RELEASED, status);
//    }
//
//    @Test
//    public void testGetReleaseStatusIsReReleased(){
//        //Given
//        NormalizedJsonLd jsonLd = new NormalizedJsonLd();
//        jsonLd.setId(idUtils.buildAbsoluteUrl(this.docId));
//        graphDb.upsert(jsonLd, DataStage.IN_PROGRESS, this.docId, space);
//
//        IndexedJsonLdDoc inferredDoc = graphDb.get(DataStage.IN_PROGRESS, space, docId);
//        release.release(space, docId, inferredDoc.getRevision());
//        jsonLd.addProperty("http://schema.org/name", "new name");
//        graphDb.upsert(jsonLd, DataStage.IN_PROGRESS, this.docId, space);
//
//        IndexedJsonLdDoc reinferredDoc = graphDb.get(DataStage.IN_PROGRESS, space, docId);
//        release.release(space, docId, reinferredDoc.getRevision());
//        //When
//        ReleaseStatus status = release.getStatus(space, docId, ReleaseTreeScope.TOP_INSTANCE_ONLY);
//        //Then
//        assertEquals(ReleaseStatus.RELEASED, status);
//    }
//
//
//
//
//    @Test
//    public void testGetReleaseStatusHasChanged(){
//        //Given
//        NormalizedJsonLd jsonLd = new NormalizedJsonLd();
//        jsonLd.setId(idUtils.buildAbsoluteUrl(this.docId));
//        graphDb.upsert(jsonLd, DataStage.IN_PROGRESS, this.docId, space);
//
//        IndexedJsonLdDoc inferredDoc = graphDb.get(DataStage.IN_PROGRESS, space, docId);
//        release.release(space, docId, inferredDoc.getRevision());
//        jsonLd.addProperty("http://schema.org/name", "new name");
//        graphDb.upsert(jsonLd, DataStage.IN_PROGRESS, this.docId, space);
//        //When
//        ReleaseStatus status = release.getStatus(space, docId, ReleaseTreeScope.TOP_INSTANCE_ONLY);
//
//        //Then
//        assertEquals(ReleaseStatus.HAS_CHANGED, status);
//    }
//
//    @Test
//    public void testGetReleaseStatusOfNonExistantDocument(){
//        //Given
//        NormalizedJsonLd jsonLd = new NormalizedJsonLd();
//        jsonLd.setId(idUtils.buildAbsoluteUrl(this.docId));
//        graphDb.upsert(jsonLd, DataStage.IN_PROGRESS, this.docId, space);
//
//        //When
//        ReleaseStatus status = release.getStatus(space, UUID.randomUUID(), ReleaseTreeScope.TOP_INSTANCE_ONLY);
//
//        //Then
//        assertNull(status);
//    }
}

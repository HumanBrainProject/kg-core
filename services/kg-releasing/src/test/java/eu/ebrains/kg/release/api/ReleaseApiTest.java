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

package eu.ebrains.kg.release.api;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.test.APITest;
import org.junit.Ignore;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

//FIXME - We need to transfer this to a system test (in the bundle)
@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest
@Category(APITest.class)
public class ReleaseApiTest {

    @Autowired
    ReleaseAPI releaseApi;


    SpaceName space =new SpaceName("test");
    UUID docId = UUID.randomUUID();


    @Autowired
    IdUtils idUtils;
//
//
//    @Test
//    public void testRelease() {
//        //Given
//        NormalizedJsonLd jsonLd = new NormalizedJsonLd();
//        jsonLd.setId(idUtils.buildAbsoluteUrl(this.docId));
//
//        graphDb.upsert(jsonLd, DataStage.IN_PROGRESS, this.docId, space);
//        IndexedJsonLdDoc inferredDoc = graphDb.get(DataStage.IN_PROGRESS, space, this.docId);
//
//        //When
//        releaseApi.releaseInstance(space.getName(), this.docId, inferredDoc.getRevision());
//
//        //Then
//        IndexedJsonLdDoc releasedDoc = graphDb.get(DataStage.RELEASED, space, this.docId);
//        assertNotNull(releasedDoc);
//    }
//
//    @Test
//    public void testUnrelease() {
//        //Given
//        NormalizedJsonLd jsonLd = new NormalizedJsonLd();
//        jsonLd.setId(idUtils.buildAbsoluteUrl(this.docId));
//        graphDb.upsert(jsonLd, DataStage.RELEASED, this.docId, space);
//        IndexedJsonLdDoc inferredDoc = graphDb.get(DataStage.RELEASED, space, this.docId);
//
//        //When
//        releaseApi.unreleaseInstance(space.getName(), this.docId);
//        //Then
//        IndexedJsonLdDoc unreleaseDoc = graphDb.get(DataStage.RELEASED, space, this.docId);
//        assertNull(unreleaseDoc);
//
//    }
//    @Test
//    public void testReleaseNestedDocuments() {
//        DataStage insertStage = DataStage.IN_PROGRESS;
//        DataStage fetchStage = DataStage.RELEASED;
//        String linkProperty = "https://schema.hbp.eu/Location";
//
//        //Given
//        NormalizedJsonLd jsonLd = new NormalizedJsonLd();
//        NormalizedJsonLd nestedJsonLD = new NormalizedJsonLd();
//        nestedJsonLD.addProperty("http://schema.org/name", "Street");
//        jsonLd.setId(idUtils.buildAbsoluteUrl(this.docId));
//        jsonLd.addProperty(linkProperty, nestedJsonLD);
//        graphDb.upsert(jsonLd, insertStage, this.docId, space);
//
//
//        IndexedJsonLdDoc inferredDoc = graphDb.get(insertStage, space, docId);
//
//        //When
//        releaseApi.releaseInstance(space.getName(), docId, inferredDoc.getRevision());
//        //Then
//        IndexedJsonLdDoc releaseNested = graphDb.get(fetchStage, space, docId);
//
//        assert(releaseNested.getDoc().get(linkProperty) instanceof  Map);
//
//    }

}

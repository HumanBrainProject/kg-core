/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.release.api;

import com.netflix.discovery.EurekaClient;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.docker.SpringDockerComposeRunner;
import eu.ebrains.kg.test.GraphDB4Test;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReleaseApiTest {

    @Autowired
    EurekaClient discoveryClient;

    @Autowired
    ReleaseAPI releaseApi;

    SpringDockerComposeRunner dockerComposeRunner;


    SpaceName space =new SpaceName("test");
    UUID docId = UUID.randomUUID();

    @Before
    public void setup() {
        this.dockerComposeRunner = new SpringDockerComposeRunner(discoveryClient, true, false, Arrays.asList("arango"), "kg-permissions", "kg-primarystore", "kg-graphdb-sync", "kg-indexing", "kg-jsonld");
        dockerComposeRunner.start();
    }

    @Autowired
    GraphDB4Test graphDb;

    @Autowired
    IdUtils idUtils;


    @Test
    public void testRelease() {
        //Given
        NormalizedJsonLd jsonLd = new NormalizedJsonLd();
        jsonLd.setId(idUtils.buildAbsoluteUrl(this.docId));

        graphDb.upsert(jsonLd, DataStage.IN_PROGRESS, this.docId, space);
        IndexedJsonLdDoc inferredDoc = graphDb.get(DataStage.IN_PROGRESS, space, this.docId);

        //When
        releaseApi.releaseInstance(space.getName(), this.docId, inferredDoc.getRevision());

        //Then
        IndexedJsonLdDoc releasedDoc = graphDb.get(DataStage.RELEASED, space, this.docId);
        assertNotNull(releasedDoc);
    }

    @Test
    public void testUnrelease() {
        //Given
        NormalizedJsonLd jsonLd = new NormalizedJsonLd();
        jsonLd.setId(idUtils.buildAbsoluteUrl(this.docId));
        graphDb.upsert(jsonLd, DataStage.RELEASED, this.docId, space);
        IndexedJsonLdDoc inferredDoc = graphDb.get(DataStage.RELEASED, space, this.docId);

        //When
        releaseApi.unreleaseInstance(space.getName(), this.docId);
        //Then
        IndexedJsonLdDoc unreleaseDoc = graphDb.get(DataStage.RELEASED, space, this.docId);
        assertNull(unreleaseDoc);

    }
    @Test
    public void testReleaseNestedDocuments() {
        DataStage insertStage = DataStage.IN_PROGRESS;
        DataStage fetchStage = DataStage.RELEASED;
        String linkProperty = "https://schema.hbp.eu/Location";

        //Given
        NormalizedJsonLd jsonLd = new NormalizedJsonLd();
        NormalizedJsonLd nestedJsonLD = new NormalizedJsonLd();
        nestedJsonLD.addProperty("http://schema.org/name", "Street");
        jsonLd.setId(idUtils.buildAbsoluteUrl(this.docId));
        jsonLd.addProperty(linkProperty, nestedJsonLD);
        graphDb.upsert(jsonLd, insertStage, this.docId, space);


        IndexedJsonLdDoc inferredDoc = graphDb.get(insertStage, space, docId);

        //When
        releaseApi.releaseInstance(space.getName(), docId, inferredDoc.getRevision());
        //Then
        IndexedJsonLdDoc releaseNested = graphDb.get(fetchStage, space, docId);

        assert(releaseNested.getDoc().get(linkProperty) instanceof  Map);

    }

}

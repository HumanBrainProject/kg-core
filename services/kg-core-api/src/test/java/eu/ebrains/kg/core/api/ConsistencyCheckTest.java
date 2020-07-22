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

import com.netflix.discovery.EurekaClient;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.docker.SpringDockerComposeRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd=changeMe", "eu.ebrains.kg.arango.port=9111"})
@WebAppConfiguration
public class ConsistencyCheckTest {
    @Autowired
    EurekaClient discoveryClient;

    @Autowired
    Instances instances;

    @Autowired
    IdUtils idUtils;

    @Autowired
    Releases releases;

    @Autowired
    Types types;

    @Before
    public void setup() {
        new SpringDockerComposeRunner(discoveryClient, Arrays.asList("arango"), "kg-ids", "kg-jsonld", "kg-primarystore", "kg-authentication", "kg-indexing", "kg-graphdb-sync").start();
    }

        PaginationParam EMPTY_PAGINATION = new PaginationParam();
    int createInstances = 10;
    String type = "http://schema.org/Test";

    @Test
    public void testInsert() {
        //Given
        for (int i = 0; i < createInstances; i++) {
            JsonLdDoc doc = new JsonLdDoc();
            doc.addTypes(type);
            doc.addProperty("http://schema.hbp.eu/foo", "instance" + i);
            ResponseEntity<Result<NormalizedJsonLd>> document = instances.createNewInstance(doc, "foo", false, false, false, false, false, null);
            JsonLdId id = document.getBody().getData().getId();
            System.out.println(String.format("Created instance %s", id.getId()));
        }
        Assert.assertEquals(createInstances, getAllInstancesFromLive(ExposedStage.LIVE).size());
        Assert.assertEquals(1, types.getTypes(ExposedStage.LIVE, null, false, EMPTY_PAGINATION).getSize());
        List<NormalizedJsonLd> typeWithProperties = types.getTypes(ExposedStage.LIVE, null, true, EMPTY_PAGINATION).getData();
        Assert.assertEquals(1, typeWithProperties.size());
        NormalizedJsonLd typeWithProperty = typeWithProperties.get(0);
        Assert.assertEquals(type, typeWithProperty.get(SchemaOrgVocabulary.IDENTIFIER));
    }

    private List<NormalizedJsonLd> getAllInstancesFromLive(ExposedStage stage){
        return this.instances.getInstances(stage, type, null, false, false, false, EMPTY_PAGINATION).getData();
    }

    @Test
    public void testUpdate() {
        float updateRatio = 0.5f;
        testInsert();
        int updated = 0;
        for (NormalizedJsonLd instance : getAllInstancesFromLive(ExposedStage.LIVE)) {
            if (updated < createInstances * updateRatio) {
                JsonLdDoc doc = new JsonLdDoc();
                doc.addTypes(type);
                doc.addProperty("http://schema.hbp.eu/foo", "updatedValue" + updated);
                this.instances.contributeToInstancePartialReplacement(doc, idUtils.getUUID(instance.getId()), false, false,  false, false, false, false, null);
                updated++;
            }
        }
        Assert.assertEquals(createInstances, getAllInstancesFromLive(ExposedStage.LIVE).size());
    }

    @Test
    public void testRelease() {
        float releaseRatio = 0.2f;
        testInsert();
        int released = 0;
        for (NormalizedJsonLd instance : getAllInstancesFromLive(ExposedStage.LIVE)) {
            if (released < createInstances * releaseRatio) {
                this.releases.releaseInstance(idUtils.getUUID(instance.getId()), IndexedJsonLdDoc.from(instance).getRevision());
                released++;
            }
        }
        Assert.assertEquals((int)Math.floor(createInstances*releaseRatio), getAllInstancesFromLive(ExposedStage.RELEASED).size());
    }



    @Test
    public void testExplicitDelete() {
        float deleteRatio = 0.2f;
        testInsert();
        int deleted = 0;
        for (NormalizedJsonLd instance : getAllInstancesFromLive(ExposedStage.LIVE)) {
            if (deleted < createInstances * deleteRatio) {
                this.instances.deleteInstance(idUtils.getUUID(instance.getId()), null);
                deleted++;
            }
        }
        Assert.assertEquals(createInstances-(int)Math.floor(createInstances*deleteRatio), getAllInstancesFromLive(ExposedStage.LIVE).size());
    }


    @Test
    public void testImplicitDeleteByTypeRemoval() {
        float updateRatio = 0.2f;
        testInsert();
        int updated = 0;
        for (NormalizedJsonLd instance : getAllInstancesFromLive(ExposedStage.LIVE)) {
            if (updated < createInstances * updateRatio) {
                JsonLdDoc doc = new JsonLdDoc();
                doc.addProperty("http://schema.hbp.eu/foo", "valueWithoutType" + updated);
                this.instances.contributeToInstancePartialReplacement(doc, idUtils.getUUID(instance.getId()), true, false, false, false, false, false, null);
                updated++;
            }
        }
        Assert.assertEquals(createInstances-(int)Math.floor(createInstances*updateRatio), getAllInstancesFromLive(ExposedStage.LIVE).size());
    }

    @Test
    public void testDeleteAllAfterUpdate() {
        testUpdate();
        int deleted = 0;
        for (NormalizedJsonLd instance : getAllInstancesFromLive(ExposedStage.LIVE)) {
            this.instances.deleteInstance(idUtils.getUUID(instance.getId()), null);
        }
        Assert.assertEquals(0, getAllInstancesFromLive(ExposedStage.LIVE).size());
    }
}
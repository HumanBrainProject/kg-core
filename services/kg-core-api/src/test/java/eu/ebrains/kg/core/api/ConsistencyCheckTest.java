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

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.model.external.types.TypeInformation;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.test.APITest;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.List;


@Ignore //TODO fix test
@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@Category(APITest.class)
public class ConsistencyCheckTest {


    @Autowired
    Instances instances;

    @Autowired
    IdUtils idUtils;

    @Autowired
    Types types;

    PaginationParam EMPTY_PAGINATION = new PaginationParam();
    ExtendedResponseConfiguration DEFAULT_RESPONSE_CONFIG = new ExtendedResponseConfiguration();

    int createInstances = 10;
    String type = "http://schema.org/Test";

    @Test
    public void testInsert() {
        //Given
        for (int i = 0; i < createInstances; i++) {
            JsonLdDoc doc = new JsonLdDoc();
            doc.addTypes(type);
            doc.addProperty("http://schema.hbp.eu/foo", "instance" + i);
            ResponseEntity<Result<NormalizedJsonLd>> document = instances.createNewInstance(doc, "foo", DEFAULT_RESPONSE_CONFIG);
            JsonLdId id = document.getBody().getData().id();
            System.out.println(String.format("Created instance %s", id.getId()));
        }
        Assert.assertEquals(createInstances, getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).size());
        Assert.assertEquals(1, types.listTypes(ExposedStage.IN_PROGRESS, null, false, false, EMPTY_PAGINATION).getSize());
        List<TypeInformation> typeWithPropertiesAndCounts = types.listTypes(ExposedStage.IN_PROGRESS, null, true, true, EMPTY_PAGINATION).getData();
        Assert.assertEquals(1, typeWithPropertiesAndCounts.size());
        TypeInformation typeWithProperty = typeWithPropertiesAndCounts.get(0);
        Assert.assertEquals(type, typeWithProperty.get(SchemaOrgVocabulary.IDENTIFIER));
    }

    private List<NormalizedJsonLd> getAllInstancesFromInProgress(ExposedStage stage){
        return this.instances.listInstances(stage, type, null, null, null, null,  new ResponseConfiguration().setReturnAlternatives(false).setReturnPermissions(false).setReturnEmbedded(false), EMPTY_PAGINATION).getData();
    }

    @Test
    public void testUpdate() {
        float updateRatio = 0.5f;
        testInsert();
        int updated = 0;
        for (NormalizedJsonLd instance : getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS)) {
            if (updated < createInstances * updateRatio) {
                JsonLdDoc doc = new JsonLdDoc();
                doc.addTypes(type);
                doc.addProperty("http://schema.hbp.eu/foo", "updatedValue" + updated);
                this.instances.contributeToInstancePartialReplacement(doc, idUtils.getUUID(instance.id()), DEFAULT_RESPONSE_CONFIG);
                updated++;
            }
        }
        Assert.assertEquals(createInstances, getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).size());
    }

    @Test
    public void testRelease() {
        float releaseRatio = 0.2f;
        testInsert();
        int released = 0;
        for (NormalizedJsonLd instance : getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS)) {
            if (released < createInstances * releaseRatio) {
                this.instances.releaseInstance(idUtils.getUUID(instance.id()), IndexedJsonLdDoc.from(instance).getRevision());
                released++;
            }
        }
        Assert.assertEquals((int)Math.floor(createInstances*releaseRatio), getAllInstancesFromInProgress(ExposedStage.RELEASED).size());
    }



    @Test
    public void testExplicitDelete() {
        float deleteRatio = 0.2f;
        testInsert();
        int deleted = 0;
        for (NormalizedJsonLd instance : getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS)) {
            if (deleted < createInstances * deleteRatio) {
                this.instances.deleteInstance(idUtils.getUUID(instance.id()));
                deleted++;
            }
        }
        Assert.assertEquals(createInstances-(int)Math.floor(createInstances*deleteRatio), getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).size());
    }


    @Test
    public void testImplicitDeleteByTypeRemoval() {
        float updateRatio = 0.2f;
        testInsert();
        int updated = 0;
        for (NormalizedJsonLd instance : getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS)) {
            if (updated < createInstances * updateRatio) {
                JsonLdDoc doc = new JsonLdDoc();
                doc.addProperty("http://schema.hbp.eu/foo", "valueWithoutType" + updated);
                this.instances.contributeToInstancePartialReplacement(doc, idUtils.getUUID(instance.id()), DEFAULT_RESPONSE_CONFIG);
                updated++;
            }
        }
        Assert.assertEquals(createInstances-(int)Math.floor(createInstances*updateRatio), getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).size());
    }

    @Test
    public void testDeleteAllAfterUpdate() {
        testUpdate();
        int deleted = 0;
        for (NormalizedJsonLd instance : getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS)) {
            this.instances.deleteInstance(idUtils.getUUID(instance.id()));
        }
        Assert.assertEquals(0, getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).size());
    }
}

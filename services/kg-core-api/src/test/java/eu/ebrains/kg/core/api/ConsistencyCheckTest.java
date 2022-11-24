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

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.ExtendedResponseConfiguration;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.ResponseConfiguration;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.external.types.TypeInformation;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.core.api.v3.InstancesV3;
import eu.ebrains.kg.core.api.v3.TypesV3;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.test.TestCategories;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;


@Disabled //TODO fix test
@SpringBootTest
@WebAppConfiguration
@Tag(TestCategories.API)
class ConsistencyCheckTest {
    @Autowired
    InstancesV3 instances;
    @Autowired
    IdUtils idUtils;
    @Autowired
    TypesV3 types;
    PaginationParam EMPTY_PAGINATION = new PaginationParam();
    ExtendedResponseConfiguration DEFAULT_RESPONSE_CONFIG = new ExtendedResponseConfiguration();
    int createInstances = 10;
    String type = "http://schema.org/Test";

    @Test
    void testInsert() {
        //Given
        for (int i = 0; i < createInstances; i++) {
            JsonLdDoc doc = new JsonLdDoc();
            doc.addTypes(type);
            doc.addProperty("http://schema.hbp.eu/foo", "instance" + i);
            ResponseEntity<Result<NormalizedJsonLd>> document = instances.createNewInstance(doc, "foo", DEFAULT_RESPONSE_CONFIG);
            JsonLdId id = Objects.requireNonNull(document.getBody()).getData().id();
            System.out.printf("Created instance %s%n", id.getId());
        }
        assertEquals(createInstances, getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).size());
        assertEquals(1, types.listTypes(ExposedStage.IN_PROGRESS, null, false, false, EMPTY_PAGINATION).getSize());
        List<TypeInformation> typeWithPropertiesAndCounts = types.listTypes(ExposedStage.IN_PROGRESS, null, true, true, EMPTY_PAGINATION).getData();
        assertEquals(1, typeWithPropertiesAndCounts.size());
        TypeInformation typeWithProperty = typeWithPropertiesAndCounts.get(0);
        assertEquals(type, typeWithProperty.get(SchemaOrgVocabulary.IDENTIFIER));
    }

    private List<NormalizedJsonLd> getAllInstancesFromInProgress(ExposedStage stage){
        return this.instances.listInstances(stage, type, null, null, null, null,  new ResponseConfiguration().setReturnAlternatives(false).setReturnPermissions(false).setReturnEmbedded(false), EMPTY_PAGINATION).getData();
    }

    @Test
    void testUpdate() {
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
        assertEquals(createInstances, getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).size());
    }

    @Test
    void testRelease() {
        float releaseRatio = 0.2f;
        testInsert();
        int released = 0;
        for (NormalizedJsonLd instance : getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS)) {
            if (released < createInstances * releaseRatio) {
                this.instances.releaseInstance(idUtils.getUUID(instance.id()), IndexedJsonLdDoc.from(instance).getRevision());
                released++;
            }
        }
        assertEquals((int)Math.floor(createInstances*releaseRatio), getAllInstancesFromInProgress(ExposedStage.RELEASED).size());
    }



    @Test
    void testExplicitDelete() {
        float deleteRatio = 0.2f;
        testInsert();
        int deleted = 0;
        for (NormalizedJsonLd instance : getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS)) {
            if (deleted < createInstances * deleteRatio) {
                this.instances.deleteInstance(idUtils.getUUID(instance.id()));
                deleted++;
            }
        }
        assertEquals(createInstances-(int)Math.floor(createInstances*deleteRatio), getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).size());
    }


    @Test
    void testImplicitDeleteByTypeRemoval() {
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
        assertEquals(createInstances-(int)Math.floor(createInstances*updateRatio), getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).size());
    }

    @Test
    void testDeleteAllAfterUpdate() {
        testUpdate();
        for (NormalizedJsonLd instance : getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS)) {
            this.instances.deleteInstance(idUtils.getUUID(instance.id()));
        }
        assertEquals(0, getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).size());
    }
}

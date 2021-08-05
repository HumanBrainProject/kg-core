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

package eu.ebrains.kg.systemTest.controller.consistency4artificial;

import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.systemTest.serviceCall.SystemTestToCore;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class InternalArtificialData {

    private final SystemTestToCore coreSvc;

    public InternalArtificialData(SystemTestToCore coreSvc) {
        this.coreSvc = coreSvc;
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());


    public void simpleIngestion() {
        Set<JsonLdId> a = ingestGroup("a", 50);
        Set<JsonLdId> b = ingestGroup("b", 20);
    }


    private Set<JsonLdId> ingestGroup(String groupName, int instances) {
        Set<JsonLdId> group = new HashSet<>();
        logger.info(String.format("Ingesting the group %s ...", groupName));
        for (int i = 0; i < instances; i++) {
            NormalizedJsonLd normalizedJsonLd = new NormalizedJsonLd();
            normalizedJsonLd.addTypes(createType(groupName));
            normalizedJsonLd.put(createProperty(groupName), groupName + i);
            Result<NormalizedJsonLd> createdDocument = coreSvc.createInstance(normalizedJsonLd, new SpaceName("testA"), null, null, false, false);
            group.add(createdDocument.getData().id());
        }
        return group;
    }

    private String createProperty(String s) {

        return "http://bar/" + s;
    }

    private String createType(String s) {
        return "http://foo/" + StringUtils.capitalize(s);
    }
}

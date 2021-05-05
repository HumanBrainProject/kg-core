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

package eu.ebrains.kg.nexusv1.controller;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.nexusv1.models.NexusV1Event;
import eu.ebrains.kg.nexusv1.serviceCall.JsonLdSvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PayloadNormalizer {

    @Value("${eu.ebrains.kg.nexus.endpoint}")
    String nexusEndpoint;

    @Autowired
    IdUtils idUtils;

    @Autowired
    JsonLdSvc jsonLdSvc;


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String[] ignoreInstanceOfSpaceEnding = new String[]{"inferred"};
    private static final String[] removeSpaceEnding = new String[]{"editor", "editorsug"};




    public NormalizedJsonLd normalizePayload(NexusV1Event event) {
        JsonLdDoc payload = event.getEventSourcePayload();
        NormalizedJsonLd normalizedJsonLd = jsonLdSvc.toNormalizedJsonLd(payload);
        //Save original id as identifier (for back-reference)
        if(!normalizedJsonLd.isEmpty()){
            normalizedJsonLd.addIdentifiers(normalizedJsonLd.getId().getId());
        }
        //set original nexus id to payload, so it can be handled appropriately.
        normalizedJsonLd.put(JsonLdConsts.ID, event.getResourceId());
        return normalizedJsonLd;
    }

    public static boolean isIgnored(Space space){
        if(space==null){
            return true;
        }
        for (String s : ignoreInstanceOfSpaceEnding) {
            if(space.getName().endsWith(s)){
                return true;
            }
        }
        return false;
    }

    public static Space normalizeIfSuffixed(Space space){
        for(String s : removeSpaceEnding){
            if(space.getName().endsWith(s)){
                space.setName(space.getName().substring(0, space.getName().length()-s.length()));
            }
        }
        return space;
    }

}

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

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.serviceCall.PrimaryStoreSvc;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping(Version.API)
public class Properties {


    private final PrimaryStoreSvc primaryStoreSvc;
    private final AuthContext authContext;

    public Properties(PrimaryStoreSvc primaryStoreSvc, AuthContext authContext) {
        this.primaryStoreSvc = primaryStoreSvc;
        this.authContext = authContext;
    }

    @PutMapping("/properties")
    public void defineProperty(@RequestBody NormalizedJsonLd payload, @RequestParam("property") String property, @RequestParam(value = "type", required = false) String type, @RequestParam(value = "labelProperty", required = false) Boolean labelProperty) {
        payload.put(EBRAINSVocabulary.META_PROPERTY, new JsonLdId(property));
        if(type==null || type.isBlank()){
            payload.setId(EBRAINSVocabulary.createIdForStructureDefinition("clients", authContext.getUserWithRoles().getClientId(), "properties", property));
            payload.put(JsonLdConsts.TYPE, EBRAINSVocabulary.META_PROPERTY_DEFINITION_TYPE);
        }
        else{
            payload.setId(EBRAINSVocabulary.createIdForStructureDefinition("clients", authContext.getUserWithRoles().getClientId(), "types", type, "properties", property));
            payload.put(JsonLdConsts.TYPE, EBRAINSVocabulary.META_PROPERTY_IN_TYPE_DEFINITION_TYPE);
            payload.put(EBRAINSVocabulary.META_TYPE, new JsonLdId(type));
        }
        if(labelProperty!=null){
            payload.put(EBRAINSVocabulary.META_LABELPROPERTY, labelProperty);
        }
        primaryStoreSvc.postEvent(Event.createUpsertEvent(authContext.getClientSpace(), UUID.nameUUIDFromBytes(payload.getId().getId().getBytes(StandardCharsets.UTF_8)), Event.Type.INSERT, payload), false, authContext.getAuthTokens());
    }
}

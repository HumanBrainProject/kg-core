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
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.core.serviceCall.GraphDB4TypesSvc;
import eu.ebrains.kg.core.serviceCall.PrimaryStoreSvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(Version.API)
public class Types {

    @Autowired
    GraphDB4TypesSvc graphDBSvc;

    @Autowired
    AuthContext authContext;

    @Autowired
    PrimaryStoreSvc primaryStoreSvc;

    @GetMapping("/types")
    public PaginatedResult<NormalizedJsonLd> getTypes(@RequestParam("stage") ExposedStage stage, @RequestParam(value = "workspace", required = false) String space, @RequestParam(value = "withProperties", defaultValue = "false") boolean withProperties, PaginationParam paginationParam) {
        return graphDBSvc.getTypes(stage.getStage(), space != null ? new Space(space) : null, withProperties, paginationParam);
    }

    @PostMapping("/typesByName")
    public Result<Map<String, Result<NormalizedJsonLd>>> getTypesByName(@RequestBody List<String> listOfTypeNames, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "withProperties", defaultValue = "false") boolean withProperties, @RequestParam(value = "workspace", required = false) String space) {
        return Result.ok(graphDBSvc.getTypesByNameList(listOfTypeNames, stage.getStage(), space != null ? new Space(space) : null, withProperties));
    }

    @PutMapping("/typesByName")
    public void defineType(@RequestBody NormalizedJsonLd payload, @RequestParam(value = "name") String name) {
        payload.put(EBRAINSVocabulary.META_TYPE, new JsonLdId(name));
        payload.setId(EBRAINSVocabulary.createIdForStructureDefinition("clients", authContext.getUserWithRoles().getClientId(),"types", name));
        payload.put(JsonLdConsts.TYPE, EBRAINSVocabulary.META_TYPEDEFINITION_TYPE);
        primaryStoreSvc.postEvent(Event.createUpsertEvent(authContext.getClientSpace(), UUID.nameUUIDFromBytes(payload.getId().getId().getBytes(StandardCharsets.UTF_8)), Event.Type.INSERT, payload), false, authContext.getAuthTokens());
    }
}

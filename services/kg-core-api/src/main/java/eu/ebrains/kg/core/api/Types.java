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

import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.core.serviceCall.CoreToPrimaryStore;
import eu.ebrains.kg.core.serviceCall.CoreTypesToGraphDB;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The types API allows to get information about the available types of instances including statistical values
 */
@RestController
@RequestMapping(Version.API)
public class Types {

    private final CoreTypesToGraphDB graphDBSvc;

    private final AuthContext authContext;

    private final CoreToPrimaryStore primaryStoreSvc;

    public Types(CoreTypesToGraphDB graphDBSvc, AuthContext authContext, CoreToPrimaryStore primaryStoreSvc) {
        this.graphDBSvc = graphDBSvc;
        this.authContext = authContext;
        this.primaryStoreSvc = primaryStoreSvc;
    }

    @Operation(summary = "Returns the types available - either with property information or without")
    @GetMapping("/types")
    public PaginatedResult<NormalizedJsonLd> getTypes(@RequestParam("stage") ExposedStage stage, @RequestParam(value = "space", required = false) String space, @RequestParam(value = "withProperties", defaultValue = "false") boolean withProperties, @ParameterObject PaginationParam paginationParam) {
        return PaginatedResult.ok(graphDBSvc.getTypes(stage.getStage(), space != null ? new Space(space) : null, withProperties, paginationParam));
    }

    @Operation(summary = "Returns the types according to the list of names - either with property information or without")
    @PostMapping("/typesByName")
    public Result<Map<String, Result<NormalizedJsonLd>>> getTypesByName(@RequestBody List<String> listOfTypeNames, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "withProperties", defaultValue = "false") boolean withProperties, @RequestParam(value = "space", required = false) String space) {
        return Result.ok(graphDBSvc.getTypesByNameList(listOfTypeNames, stage.getStage(), space != null ? new Space(space) : null, withProperties));
    }

    @Operation(summary = "Define a type")
    @PutMapping("/types")
    public ResponseEntity<Result<Void>> defineType(@RequestBody NormalizedJsonLd payload, @Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)")  @RequestParam(value = "global", required = false) boolean global) {
        Space targetSpace = global ? InternalSpace.GLOBAL_SPEC : authContext.getClientSpace();
        JsonLdId type = payload.getAs(EBRAINSVocabulary.META_TYPE, JsonLdId.class);
        if(type==null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.nok(HttpStatus.BAD_REQUEST.value(), String.format("Property \"%s\" should be specified.", EBRAINSVocabulary.META_TYPE)));
        }
        payload.setId(EBRAINSVocabulary.createIdForStructureDefinition("clients", targetSpace.getName(), "types", type.getId()));
        payload.put(JsonLdConsts.TYPE, EBRAINSVocabulary.META_TYPEDEFINITION_TYPE);
        primaryStoreSvc.postEvent(Event.createUpsertEvent(targetSpace, UUID.nameUUIDFromBytes(payload.id().getId().getBytes(StandardCharsets.UTF_8)), Event.Type.INSERT, payload), false, authContext.getAuthTokens());
        return ResponseEntity.ok(Result.ok());
    }
}

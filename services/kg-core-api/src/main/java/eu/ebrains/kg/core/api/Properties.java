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
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.serviceCall.CoreToPrimaryStore;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The property API allows to add meta information about semantic properties either globally or by type for the requesting client.
 */
@RestController
@RequestMapping(Version.API)
public class Properties {

    private final CoreToPrimaryStore primaryStoreSvc;
    private final AuthContext authContext;

    public Properties(CoreToPrimaryStore primaryStoreSvc, AuthContext authContext) {
        this.primaryStoreSvc = primaryStoreSvc;
        this.authContext = authContext;
    }

    @ApiOperation(value = "Upload a property specification either globally or on a type level for the requesting client")
    @PutMapping("/properties")
    public ResponseEntity<Result<Void>> defineProperty(@RequestBody NormalizedJsonLd payload, @ApiParam("By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)")  @RequestParam(value = "global", required = false) boolean global) {
        if(!payload.containsKey(EBRAINSVocabulary.META_PROPERTY)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.nok(HttpStatus.BAD_REQUEST.value(), String.format("Property \"%s\" should be specified.", EBRAINSVocabulary.META_PROPERTY)));
        }
        Space targetSpace = global ? InternalSpace.GLOBAL_SPEC : authContext.getClientSpace();
        JsonLdId property = payload.getAs(EBRAINSVocabulary.META_PROPERTY, JsonLdId.class);
        List<String> listOfType = payload.getAsListOf(JsonLdConsts.TYPE, String.class);
        if(listOfType.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.nok(HttpStatus.BAD_REQUEST.value(), String.format("Property \"%s\" should be specified.", JsonLdConsts.TYPE)));
        }
        if(listOfType.size() == 1){
            switch (listOfType.get(0)) {
                case EBRAINSVocabulary.META_PROPERTY_DEFINITION_TYPE: {
                    payload.setId(EBRAINSVocabulary.createIdForStructureDefinition("clients", targetSpace.getName(), "properties", property.getId()));
                    break;
                }
                case EBRAINSVocabulary.META_PROPERTY_IN_TYPE_DEFINITION_TYPE: {
                    JsonLdId type = payload.getAs(EBRAINSVocabulary.META_TYPE, JsonLdId.class);
                    if(type == null || type.getId().isBlank()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.nok(HttpStatus.BAD_REQUEST.value(), String.format("Property \"%s\" should be specified. Did you mean to use \"%s\" as \"%s\" ?", EBRAINSVocabulary.META_TYPE, EBRAINSVocabulary.META_PROPERTY_DEFINITION_TYPE, JsonLdConsts.TYPE)));
                    } else {
                        payload.setId(EBRAINSVocabulary.createIdForStructureDefinition("clients", targetSpace.getName(), "types", type.getId(), "properties", property.getId()));
                    }
                    break;
                }
                default: {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.nok(HttpStatus.BAD_REQUEST.value(), String.format("Type \"%s\" is unknown.", listOfType.get(0))));
                }
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.nok(HttpStatus.BAD_REQUEST.value(), String.format("Only one \"%s\" is allowed.", JsonLdConsts.TYPE)));
        }
        primaryStoreSvc.postEvent(Event.createUpsertEvent(targetSpace, UUID.nameUUIDFromBytes(payload.getId().getId().getBytes(StandardCharsets.UTF_8)), Event.Type.INSERT, payload), false, authContext.getAuthTokens());
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

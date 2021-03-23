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

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.PrimaryStoreEvents;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.WritesData;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * The property API allows to add meta information about semantic properties either globally or by type for the requesting client.
 */
@RestController
@RequestMapping(Version.API)
@Admin
public class Properties {

    private final PrimaryStoreEvents.Client primaryStore;
    private final AuthContext authContext;
    private final IdUtils idUtils;

    public Properties(PrimaryStoreEvents.Client primaryStore, AuthContext authContext, IdUtils idUtils) {
        this.primaryStore = primaryStore;
        this.authContext = authContext;
        this.idUtils = idUtils;
    }

    @Operation(summary = "Upload a property specification either globally or for the requesting client")
    @PutMapping("/properties")
    @WritesData
    public ResponseEntity<Result<Void>> defineProperty(@RequestBody NormalizedJsonLd payload, @Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)")  @RequestParam(value = "global", required = false) boolean global, @RequestParam("property") String property) {
        SpaceName targetSpace = global ? InternalSpace.GLOBAL_SPEC : authContext.getClientSpace().getName();
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        payload.setId(EBRAINSVocabulary.createIdForStructureDefinition("clients", targetSpace.getName(), "properties", property));
        payload.put(JsonLdConsts.TYPE, EBRAINSVocabulary.META_PROPERTY_DEFINITION_TYPE);
        payload.put(EBRAINSVocabulary.META_PROPERTY, new JsonLdId(decodedProperty));
        primaryStore.postEvent(Event.createUpsertEvent(targetSpace, UUID.nameUUIDFromBytes(payload.id().getId().getBytes(StandardCharsets.UTF_8)), Event.Type.INSERT, payload), false);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(summary = "Upload a property specification either globally or for the requesting client")
    @DeleteMapping("/properties")
    @WritesData
    public ResponseEntity<Result<Void>> deprecateProperty(@Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)")  @RequestParam(value = "global", required = false) boolean global, @RequestParam("property") String property) {
        SpaceName targetSpace = global ? InternalSpace.GLOBAL_SPEC : authContext.getClientSpace().getName();
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        JsonLdId specId = EBRAINSVocabulary.createIdForStructureDefinition("clients", targetSpace.getName(), "properties", property);
        UUID specUUID = UUID.nameUUIDFromBytes(specId.getId().getBytes(StandardCharsets.UTF_8));
         NormalizedJsonLd payload = new NormalizedJsonLd();
        payload.addTypes(EBRAINSVocabulary.META_PROPERTY_DEFINITION_TYPE);
        payload.put(EBRAINSVocabulary.META_PROPERTY, new JsonLdId(decodedProperty));
        Event deprecateProperty = new Event(targetSpace, specUUID, payload, Event.Type.META_DEPRECATION, new Date());
        primaryStore.postEvent(deprecateProperty, false);
        return ResponseEntity.ok(Result.ok());
    }


    @Operation(summary = "Deprecate a property specification either globally for the requesting client")
    @PutMapping("/propertiesForType")
    @WritesData
    public ResponseEntity<Result<Void>> definePropertyForType(@RequestBody NormalizedJsonLd payload, @Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)")  @RequestParam(value = "global", required = false) boolean global, @RequestParam("property") String property, @RequestParam("type") String type) {
        SpaceName targetSpace = global ? InternalSpace.GLOBAL_SPEC : authContext.getClientSpace().getName();
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        String decodedType = URLDecoder.decode(type, StandardCharsets.UTF_8);
        payload.put(EBRAINSVocabulary.META_PROPERTY, new JsonLdId(decodedProperty));
        payload.put(JsonLdConsts.TYPE, EBRAINSVocabulary.META_PROPERTY_IN_TYPE_DEFINITION_TYPE);
        payload.put(EBRAINSVocabulary.META_TYPE, new JsonLdId(decodedType));
        payload.setId(EBRAINSVocabulary.createIdForStructureDefinition("clients", targetSpace.getName(), "types", decodedType, "properties", decodedProperty));
        primaryStore.postEvent(Event.createUpsertEvent(targetSpace, UUID.nameUUIDFromBytes(payload.id().getId().getBytes(StandardCharsets.UTF_8)), Event.Type.INSERT, payload), false);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(summary = "Deprecate a property specification for a specific type either globally or for the requesting client")
    @DeleteMapping("/propertiesForType")
    @WritesData
    public ResponseEntity<Result<Void>> deprecatePropertyForType(@Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)")  @RequestParam(value = "global", required = false) boolean global, @RequestParam("property") String property, @RequestParam("type") String type) {
        SpaceName targetSpace = global ? InternalSpace.GLOBAL_SPEC : authContext.getClientSpace().getName();
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        String decodedType = URLDecoder.decode(type, StandardCharsets.UTF_8);
        JsonLdId specId = EBRAINSVocabulary.createIdForStructureDefinition("clients", targetSpace.getName(), "types", decodedType, "properties", decodedProperty);
        UUID specUUID = UUID.nameUUIDFromBytes(specId.getId().getBytes(StandardCharsets.UTF_8));
        NormalizedJsonLd payload = new NormalizedJsonLd();
        payload.put(EBRAINSVocabulary.META_PROPERTY, new JsonLdId(decodedProperty));
        payload.put(EBRAINSVocabulary.META_TYPE, new JsonLdId(decodedType));
        payload.addTypes(EBRAINSVocabulary.META_PROPERTY_IN_TYPE_DEFINITION_TYPE);
        Event deprecateProperty = new Event(targetSpace, specUUID, payload, Event.Type.META_DEPRECATION, new Date());
        primaryStore.postEvent(deprecateProperty, false);
        return ResponseEntity.ok(Result.ok());
    }

}

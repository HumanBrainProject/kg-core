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

import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.GraphDBTypes;
import eu.ebrains.kg.commons.api.PrimaryStoreEvents;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.config.openApiGroups.Advanced;
import eu.ebrains.kg.commons.config.openApiGroups.Simple;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesType;
import eu.ebrains.kg.commons.markers.WritesData;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.core.model.ExposedStage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The types API allows to get information about the available types of instances including statistical values
 */
@RestController
@RequestMapping(Version.API)
public class Types {

    private final GraphDBTypes.Client graphDBTypes;
    private final PrimaryStoreEvents.Client primaryStoreEvents;
    private final AuthContext authContext;

    public Types(GraphDBTypes.Client graphDBTypes, PrimaryStoreEvents.Client primaryStoreEvents, AuthContext authContext) {
        this.graphDBTypes = graphDBTypes;
        this.primaryStoreEvents = primaryStoreEvents;
        this.authContext = authContext;
    }

    @Operation(summary = "Returns the types available - either with property information or without")
    @GetMapping("/types")
    @ExposesType
    @Simple
    public PaginatedResult<NormalizedJsonLd> getTypes(@RequestParam("stage") ExposedStage stage, @RequestParam(value = "space", required = false) @Parameter(description = "The space by which the types should be filtered or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space.") String space, @RequestParam(value = "withProperties", defaultValue = "false") boolean withProperties, @RequestParam(value = "withIncomingLinks", defaultValue = "false") boolean withIncomingLinks, @RequestParam(value = "withCounts", defaultValue = "false") @Parameter(description = "Only applies if withProperties is set to true") boolean withCounts, @ParameterObject PaginationParam paginationParam) {
        if(withProperties){
            return PaginatedResult.ok(graphDBTypes.getTypesWithProperties(stage.getStage(), getResolvedSpaceName(space), withCounts, withIncomingLinks, paginationParam));
        }
        else {
            return PaginatedResult.ok(graphDBTypes.getTypes(stage.getStage(), getResolvedSpaceName(space), withIncomingLinks, paginationParam));
        }
    }

    private String getResolvedSpaceName(String space){
        SpaceName spaceName = authContext.resolveSpaceName(space);
        return spaceName!=null ? spaceName.getName() : null;
    }

    @Operation(summary = "Returns the types according to the list of names - either with property information or without")
    @PostMapping("/typesByName")
    @ExposesType
    @Advanced
    public Result<Map<String, Result<NormalizedJsonLd>>> getTypesByName(@RequestBody List<String> listOfTypeNames, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "withProperties", defaultValue = "false") boolean withProperties, @RequestParam(value = "withCounts", defaultValue = "false") @Parameter(description = "Only applies if withProperties is set to true") boolean withCounts, @RequestParam(value = "space", required = false) @Parameter(description = "The space by which the types should be filtered or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space.") String space) {
        SpaceName spaceName = authContext.resolveSpaceName(space);
        if(withProperties){
            //TODO check for withIncomingLinks
            return Result.ok(graphDBTypes.getTypesWithPropertiesByName(listOfTypeNames, stage.getStage(), withCounts, true, getResolvedSpaceName(space)));
        }
        else{
            return Result.ok(graphDBTypes.getTypesByName(listOfTypeNames, stage.getStage(), getResolvedSpaceName(space)));
        }
    }

    @Operation(summary = "Specify a type")
    //In theory, this could also go into /types only. But since Swagger doesn't allow the discrimination of groups with the same path (there is already the same path registered as GET for simple), we want to discriminate it properly
    @PutMapping("/types/specification")
    @WritesData
    @Admin
    public ResponseEntity<Result<Void>> defineType(@RequestBody NormalizedJsonLd payload, @Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)") @RequestParam(value = "global", required = false) boolean global) {
        SpaceName targetSpace = global ? InternalSpace.GLOBAL_SPEC : authContext.getClientSpace().getName();
        JsonLdId type = payload.getAs(EBRAINSVocabulary.META_TYPE, JsonLdId.class);
        if (type == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.nok(HttpStatus.BAD_REQUEST.value(), String.format("Property \"%s\" should be specified.", EBRAINSVocabulary.META_TYPE)));
        }
        payload.setId(EBRAINSVocabulary.createIdForStructureDefinition("clients", targetSpace.getName(), "types", type.getId()));
        payload.put(JsonLdConsts.TYPE, EBRAINSVocabulary.META_TYPEDEFINITION_TYPE);
        primaryStoreEvents.postEvent(Event.createUpsertEvent(targetSpace, UUID.nameUUIDFromBytes(payload.id().getId().getBytes(StandardCharsets.UTF_8)), Event.Type.INSERT, payload), false);
        return ResponseEntity.ok(Result.ok());
    }


    @Operation(summary = "List candidate types for deprecation", description = "Returns a list of types which potentially could be deprecated because they are not in use yet")
    @GetMapping("/types/candidates/forDeprecation")
    @ExposesType
    @Admin
    public Result<List<NormalizedJsonLd>> candidatesForDeprecation() {
        Paginated<NormalizedJsonLd> types = graphDBTypes.getTypesWithProperties(DataStage.IN_PROGRESS, null, true, false, new PaginationParam());
        return Result.ok(types.getData().stream().filter(type -> type.getAs(EBRAINSVocabulary.META_OCCURRENCES, Double.class).intValue() == 0).collect(Collectors.toList()));
    }


    @Operation(summary = "Deprecate a type", description = "Allows to deprecate a specified type but only if there is no data existing (the data would have to be removed first)")
    @DeleteMapping("/types/specification")
    @WritesData
    @Admin
    public ResponseEntity<Result<Void>> deprecateType(@RequestParam(value = "type", required = false) String type) {
        Result<NormalizedJsonLd> typeStats = graphDBTypes.getTypesWithPropertiesByName(Collections.singletonList(type), DataStage.IN_PROGRESS, true, false, null).get(type);
        if (typeStats == null || typeStats.getData() == null) {
            return ResponseEntity.notFound().build();
        } else if (typeStats.getData().getAs(EBRAINSVocabulary.META_OCCURRENCES, Double.class).intValue() == 0) {
            SpaceName spaceName = InternalSpace.GLOBAL_SPEC;
            UUID typeId = UUID.nameUUIDFromBytes(EBRAINSVocabulary.createIdForStructureDefinition("clients", spaceName.getName(), "types", type).getId().getBytes(StandardCharsets.UTF_8));
            NormalizedJsonLd payload = new NormalizedJsonLd();
            payload.addTypes(EBRAINSVocabulary.META_TYPEDEFINITION_TYPE);
            payload.put(EBRAINSVocabulary.META_TYPE, type);
            Event deprecateType = new Event(spaceName, typeId, payload, Event.Type.META_DEPRECATION, new Date());
            primaryStoreEvents.postEvent(deprecateType, false);
            return ResponseEntity.ok(Result.ok());
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.nok(HttpStatus.CONFLICT.value(), "Was not able to deprecate type %s because it occurs %d times in the database"));
        }
    }

    @Operation(summary = "Deprecate candidates", description = "Deprecate all candidates (instances without occurrences) in one go")
    @DeleteMapping("/types/candidates/forDeprecation")
    @WritesData
    @Admin
    public ResponseEntity<List<Result<String>>> deprecateAllCandidates() {
        Result<List<NormalizedJsonLd>> listResult = candidatesForDeprecation();
        if (listResult != null && listResult.getData() != null) {
            List<Result<String>> result = new ArrayList<>();
            listResult.getData().forEach(d -> {
                String identifier = d.getAs(SchemaOrgVocabulary.IDENTIFIER, String.class);
                try {
                    deprecateType(identifier);
                    result.add(Result.ok(identifier));
                } catch (Exception e) {
                    result.add(Result.nok(HttpStatus.CONFLICT.value(), String.format("%s - %s", identifier, e.getMessage())));
                }
            });
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }
}

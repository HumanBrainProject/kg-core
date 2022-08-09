/*
 * Copyright 2022 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.core.api.upcoming;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.GraphDBTypes;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.config.openApiGroups.Advanced;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesType;
import eu.ebrains.kg.commons.markers.WritesData;
import eu.ebrains.kg.commons.model.PaginatedResult;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.external.types.TypeInformation;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.api.common.Types;
import eu.ebrains.kg.core.model.ExposedStage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * The types API allows to get information about the available types of instances including statistical values
 */
@RestController
@RequestMapping(Version.UPCOMING)
@Tag(name="types")
public class TypesV3 extends Types {

    private final GraphDBTypes.Client graphDBTypes;

    public TypesV3(GraphDBTypes.Client graphDBTypes, AuthContext authContext) {
        super(authContext);
        this.graphDBTypes = graphDBTypes;
    }

    @Operation(summary = "Returns the types available - either with property information or without")
    @GetMapping("/types")
    @ExposesType
    @Tag(name = TAG)
    public PaginatedResult<TypeInformation> listTypes(@RequestParam("stage") ExposedStage stage, @RequestParam(value = "space", required = false) @Parameter(description = "The space by which the types should be filtered or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space.") String space, @RequestParam(value = "withProperties", defaultValue = "false") boolean withProperties, @RequestParam(value = "withIncomingLinks", defaultValue = "false") boolean withIncomingLinks, @ParameterObject PaginationParam paginationParam) {
        return PaginatedResult.ok(graphDBTypes.listTypes(stage.getStage(), space, withProperties, withIncomingLinks, paginationParam));
    }


    @Operation(summary = "Returns the types according to the list of names - either with property information or without")
    @PostMapping("/typesByName")
    @ExposesType
    @Advanced
    @Tag(name = TAG)
    public Result<Map<String, Result<TypeInformation>>> getTypesByName(@RequestBody List<String> typeNames, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "withProperties", defaultValue = "false") boolean withProperties, @RequestParam(value = "withIncomingLinks", defaultValue = "false") boolean withIncomingLinks, @RequestParam(value = "space", required = false) @Parameter(description = "The space by which the types should be filtered or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space.") String space) {
        return Result.ok(graphDBTypes.getTypesByName(typeNames, stage.getStage(), space, withProperties, withIncomingLinks));
    }

    @Operation(summary = "Specify a type")
    //In theory, this could also go into /types only. But since Swagger doesn't allow the discrimination of groups with the same path (there is already the same path registered as GET for simple), we want to discriminate it properly
    @PutMapping("/types/specification")
    @WritesData
    @Admin
    @Tag(name = TAG)
    public void createTypeDefinition(@RequestBody NormalizedJsonLd payload, @Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)") @RequestParam(value = "global", required = false) boolean global, @RequestParam("type") String type) {
        JsonLdId typeFromPayload = payload.getAs(EBRAINSVocabulary.META_TYPE, JsonLdId.class);
        String decodedType = URLDecoder.decode(type, StandardCharsets.UTF_8);
        if(typeFromPayload!=null){
            throw new IllegalArgumentException("You are not supposed to provide a @type in the payload of the type specifications to avoid ambiguity");
        }
        graphDBTypes.specifyType(new JsonLdId(decodedType), payload, global);
    }


    @Operation(summary = "Remove a type definition", description = "Allows to deprecate a type specification")
    @DeleteMapping("/types/specification")
    @WritesData
    @Admin
    @Tag(name = TAG)
    public void removeTypeDefinition(@RequestParam(value = "type", required = false) String type,  @RequestParam(value = "global", required = false) boolean global) {
        String decodedType = URLDecoder.decode(type, StandardCharsets.UTF_8);
        graphDBTypes.removeTypeSpecification(new JsonLdId(decodedType), global);
    }
}

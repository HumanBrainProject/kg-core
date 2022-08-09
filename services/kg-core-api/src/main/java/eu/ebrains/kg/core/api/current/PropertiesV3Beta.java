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

package eu.ebrains.kg.core.api.current;

import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.GraphDBTypes;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.WritesData;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.api.common.Properties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * The property API allows to add meta information about semantic properties either globally or by type for the requesting client.
 */
@RestController
@RequestMapping(Version.CURRENT)
@Admin
public class PropertiesV3Beta extends Properties {
    private final GraphDBTypes.Client graphDBTypes;

    public PropertiesV3Beta(GraphDBTypes.Client graphDBTypes) {
        super();
        this.graphDBTypes = graphDBTypes;
    }

    @Operation(summary = "Upload a property specification either globally or for the requesting client")
    @PutMapping("/properties")
    @WritesData
    @Tag(name = TAG)
    public void defineProperty(@RequestBody NormalizedJsonLd payload, @Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)")  @RequestParam(value = "global", required = false) boolean global, @RequestParam("property") String property) {
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        graphDBTypes.specifyProperty(new JsonLdId(decodedProperty), payload, global);
    }

    @Operation(summary = "Remove a property specification either globally or for the requesting client")
    @DeleteMapping("/properties")
    @WritesData
    @Tag(name = TAG)
    public void removeProperty(@Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)")  @RequestParam(value = "global", required = false) boolean global, @RequestParam("property") String property) {
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        graphDBTypes.removePropertySpecification(new JsonLdId(decodedProperty), global);
    }


    @Operation(summary = "Define a property specification either globally for the requesting client")
    @PutMapping("/propertiesForType")
    @WritesData
    @Tag(name = TAG)
    public void definePropertyForType(@RequestBody NormalizedJsonLd payload, @Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)")  @RequestParam(value = "global", required = false) boolean global, @RequestParam("property") String property, @RequestParam("type") String type) {
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        String decodedType = URLDecoder.decode(type, StandardCharsets.UTF_8);
        JsonLdId typeFromPayload = payload.getAs(EBRAINSVocabulary.META_TYPE, JsonLdId.class);
        if(typeFromPayload!=null){
            throw new IllegalArgumentException("You are not supposed to provide a @type in the payload of the type specifications to avoid ambiguity");
        }
        graphDBTypes.addOrUpdatePropertyToType(decodedType, decodedProperty, payload, global);
    }

    @Operation(summary = "Deprecate a property specification for a specific type either globally or for the requesting client")
    @DeleteMapping("/propertiesForType")
    @WritesData
    @Tag(name = TAG)
    public void deprecatePropertyForType(@Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)")  @RequestParam(value = "global", required = false) boolean global, @RequestParam("property") String property, @RequestParam("type") String type) {
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        String decodedType = URLDecoder.decode(type, StandardCharsets.UTF_8);
        graphDBTypes.removePropertyFromType(decodedType, decodedProperty, global);
    }

}

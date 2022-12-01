/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

package eu.ebrains.kg.core.api.v3;

import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.GraphDBTypes;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.WritesData;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * The property API allows to add meta information about semantic properties either globally or by type for the requesting client.
 */
@RestController
@RequestMapping(Version.V3)
public class PropertiesV3 {
    private final GraphDBTypes.Client graphDBTypes;

    public PropertiesV3(GraphDBTypes.Client graphDBTypes) {
        this.graphDBTypes = graphDBTypes;
    }

    @Operation(summary = "Upload a property specification either globally or for the requesting client")
    @PutMapping("/properties")
    @WritesData
    @Admin
    public void defineProperty(@RequestBody NormalizedJsonLd payload, @Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)")  @RequestParam(value = "global", required = false) boolean global, @RequestParam("property") String property) {
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        graphDBTypes.specifyProperty(new JsonLdId(decodedProperty), payload, global);
    }

    @Operation(summary = "Upload a property specification either globally or for the requesting client")
    @DeleteMapping("/properties")
    @WritesData
    @Admin
    public void deprecateProperty(@Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)")  @RequestParam(value = "global", required = false) boolean global, @RequestParam("property") String property) {
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        graphDBTypes.removePropertySpecification(new JsonLdId(decodedProperty), global);
    }


    @Operation(summary = "Define a property specification either globally for the requesting client")
    @PutMapping("/propertiesForType")
    @WritesData
    @Admin
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
    @Admin
    public void deprecatePropertyForType(@Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)")  @RequestParam(value = "global", required = false) boolean global, @RequestParam("property") String property, @RequestParam("type") String type) {
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        String decodedType = URLDecoder.decode(type, StandardCharsets.UTF_8);
        graphDBTypes.removePropertyFromType(decodedType, decodedProperty, global);
    }

}

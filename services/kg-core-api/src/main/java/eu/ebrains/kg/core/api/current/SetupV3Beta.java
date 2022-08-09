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
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.model.TermsOfUse;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.core.api.common.Setup;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(Version.CURRENT +"/setup")
@Admin
public class SetupV3Beta extends Setup {

    private final Authentication.Client authentication;

    public SetupV3Beta(Authentication.Client authentication) {
        super();
        this.authentication = authentication;
    }

    @PutMapping("/termsOfUse")
    @Tag(name = TAG)
    public void registerTermsOfUse(@RequestBody TermsOfUse termsOfUse){
        authentication.registerTermsOfUse(termsOfUse);
    }

    @PatchMapping("/permissions/{role}")
    @Tag(name = TAG)
    public JsonLdDoc updateClaimForRole(@PathVariable("role") RoleMapping role, @RequestParam(value = "space", required = false) String space, @RequestBody Map<?, ?> claimPattern, @RequestParam("remove") boolean removeClaim) {
        return authentication.updateClaimForRole(role, space, claimPattern, removeClaim);
    }

    @GetMapping("/permissions/{role}")
    @Tag(name = TAG)
    public JsonLdDoc getClaimForRole(@PathVariable("role") RoleMapping role, @RequestParam(value = "space", required = false) String space) {
        return authentication.getClaimForRole(role, space);
    }

    @GetMapping("/permissions")
    @Tag(name = TAG)
    public List<JsonLdDoc> getAllRoleDefinitions() {
        return authentication.getAllRoleDefinitions();
    }
}

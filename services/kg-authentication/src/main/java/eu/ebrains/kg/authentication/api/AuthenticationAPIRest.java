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

package eu.ebrains.kg.authentication.api;

import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.model.TermsOfUse;
import eu.ebrains.kg.commons.model.TermsOfUseResult;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RequestMapping("/internal/authentication")
@RestController
@ConditionalOnProperty(value = "eu.ebrains.kg.test", havingValue = "false", matchIfMissing = true)
public class AuthenticationAPIRest implements Authentication {

    private final eu.ebrains.kg.authentication.api.AuthenticationAPI authentication;

    public AuthenticationAPIRest(eu.ebrains.kg.authentication.api.AuthenticationAPI authentication) {
        this.authentication = authentication;
    }

    /**
     * CLIENTS
     **/

    @PostMapping(value = "/clients/{client}/token")
    public ClientAuthToken fetchToken(@PathVariable("client") String clientId, @RequestBody String clientSecret) {
        return authentication.fetchToken(clientId, clientSecret);
    }

    /**
     * USERS
     **/

    @GetMapping(value = "/users/authorization/endpoint", produces = MediaType.TEXT_PLAIN_VALUE)
    public String authEndpoint() {
        return authentication.authEndpoint();
    }

    @GetMapping(value = "/users/authorization/tokenEndpoint", produces = MediaType.TEXT_PLAIN_VALUE)
    public String tokenEndpoint() {
        return authentication.tokenEndpoint();
    }

    @GetMapping(value = "/users/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public User getMyUserInfo() {
        User myUserInfo = authentication.getMyUserInfo();
        if (myUserInfo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return myUserInfo;
    }

    @GetMapping(value = "/termsOfUse", produces = MediaType.APPLICATION_JSON_VALUE)
    public TermsOfUseResult getTermsOfUse() {
        return authentication.getTermsOfUse();
    }

    @PostMapping(value = "/termsOfUse/{version}/accept")
    public void acceptTermsOfUse(@PathVariable("version") String version) {
        authentication.acceptTermsOfUse(version);
    }

    @PostMapping(value = "/termsOfUse")
    public void registerTermsOfUse(@RequestBody TermsOfUse termsOfUse) {
        authentication.registerTermsOfUse(termsOfUse);
    }

    @GetMapping(value = "/users/meWithRoles", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserWithRoles getRoles(@RequestParam(value = "checkForTermsOfUse", defaultValue = "true") @Parameter() boolean checkForTermsOfUse) {
        UserWithRoles roles = authentication.getRoles(checkForTermsOfUse);
        if (roles == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return roles;
    }

    @GetMapping("/users/profiles/{nativeId}")
    public User getOtherUserInfo(@PathVariable("nativeId") String nativeId) {
        User otherUserInfo = authentication.getOtherUserInfo(nativeId);
        if (otherUserInfo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return otherUserInfo;
    }

    @GetMapping("/users/profiles/byAttribute/{attribute}/{value}")
    public List<User> getUsersByAttribute(@PathVariable("attribute") String attribute, @PathVariable("value") String value) {
        List<User> usersByAttribute = authentication.getUsersByAttribute(attribute, value);
        if (usersByAttribute == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return usersByAttribute;
    }


    @PatchMapping("/permissions/{role}")
    @Override
    public JsonLdDoc updateClaimForRole(@PathVariable("role") RoleMapping role, @RequestParam(value = "space", required = false) String space, @RequestBody Map<?, ?> claimPattern, @RequestParam("remove") boolean removeClaim) {
            return authentication.updateClaimForRole(role, space, claimPattern, removeClaim);
    }

    @GetMapping("/permissions/{role}")
    @Override
    public JsonLdDoc getClaimForRole(@PathVariable("role") RoleMapping role, @RequestParam(value = "space", required = false) String space) {
        return authentication.getClaimForRole(role, space);
    }

    @Override
    @GetMapping("/permissions")
    public List<JsonLdDoc> getAllRoleDefinitions() {
        return authentication.getAllRoleDefinitions();
    }
}

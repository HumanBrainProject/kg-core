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

import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.core.serviceCall.CoreToAuthentication;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The user API allows to retrieve information how to access the authentication service and to retrieve information about the users.
 */
@RestController
@RequestMapping(Version.API+"/users")
public class Users {

    private final CoreToAuthentication authenticationSvc;

    public Users(CoreToAuthentication authenticationSvc) {
        this.authenticationSvc = authenticationSvc;
    }

    @ApiOperation("Get the endpoint of the authentication service")
    @GetMapping(value = "/authorization", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<JsonLdDoc> getAuthEndpoint() {
        JsonLdDoc ld = new JsonLdDoc();
        ld.addProperty("endpoint", authenticationSvc.endpoint());
        return Result.ok(ld);
    }

    @ApiOperation("Get the endpoint to retrieve your token (e.g. via client id and client secret)")
    @GetMapping(value = "/authorization/tokenEndpoint", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<JsonLdDoc> getTokenEndpoint() {
        JsonLdDoc ld = new JsonLdDoc();
        ld.addProperty("endpoint", authenticationSvc.tokenEndpoint());
        return Result.ok(ld);
    }

    @ApiOperation("Retrieve user information from the passed token (including detailed information such as e-mail address)")
    @GetMapping("/me")
    public ResponseEntity<Result<User>> profile() {
        User myUserProfile = authenticationSvc.getMyUserProfile();
        return myUserProfile!=null ? ResponseEntity.ok(Result.ok(myUserProfile)) : ResponseEntity.notFound().build();
    }

    @ApiOperation("Retrieve user information based on a keycloak attribute (excluding detailed information such as e-mail address)")
    @GetMapping("/byAttribute/{attribute}/{value}")
    public ResponseEntity<List<User>> getUsersByAttribute(@PathVariable("attribute") String attribute, @PathVariable("value") String value){
        List<User> users = authenticationSvc.getUsersByAttribute(attribute, value);
        return users != null ? ResponseEntity.ok(users) : ResponseEntity.notFound().build();
    }

}

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
import eu.ebrains.kg.core.serviceCall.AdminSvc;
import eu.ebrains.kg.core.serviceCall.Authentication4CoreSvc;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Version.API+"/users")
public class Users {

    private final AdminSvc adminSvc;
    private final Authentication4CoreSvc authenticationSvc;

    public Users(AdminSvc adminSvc, Authentication4CoreSvc authenticationSvc) {
        this.adminSvc = adminSvc;
        this.authenticationSvc = authenticationSvc;
    }

//    @GetMapping("/login")
//    public void login(HttpServletResponse httpServletResponse, @RequestParam("redirect_uri") String redirectUri) {
//        httpServletResponse.setHeader(HttpHeaders.LOCATION, authenticationSvc.login(redirectUri));
//        httpServletResponse.setStatus(HttpStatus.TEMPORARY_REDIRECT.value());
//    }

    @GetMapping(value = "/authorization", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<JsonLdDoc> getAuthEndpoint() {
        JsonLdDoc ld = new JsonLdDoc();
        ld.addProperty("endpoint", authenticationSvc.endpoint());
        return Result.ok(ld);
    }

    @GetMapping(value = "/authorization/tokenEndpoint", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<JsonLdDoc> getTokenEndpoint() {
        JsonLdDoc ld = new JsonLdDoc();
        ld.addProperty("endpoint", authenticationSvc.tokenEndpoint());
        return Result.ok(ld);
    }

    @GetMapping("/me")
    public ResponseEntity<Result<User>> profile() {
        User myUserProfile = authenticationSvc.getMyUserProfile();
        return myUserProfile!=null ? ResponseEntity.ok(Result.ok(myUserProfile)) : ResponseEntity.notFound().build();
    }


    @GetMapping("/byAttribute/{attribute}/{value}")
    public ResponseEntity<List<User>> getUsersByAttribute(@PathVariable("attribute") String attribute, @PathVariable("value") String value){
        List<User> users = authenticationSvc.getUsersByAttribute(attribute, value);
        return users != null ? ResponseEntity.ok(users) : ResponseEntity.notFound().build();
    }

}

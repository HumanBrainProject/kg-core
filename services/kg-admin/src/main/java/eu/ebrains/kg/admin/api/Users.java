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

package eu.ebrains.kg.admin.api;

import com.google.gson.Gson;
import eu.ebrains.kg.admin.controller.UserController;
import eu.ebrains.kg.admin.serviceCall.AuthenticationSvcForAdmin;
import eu.ebrains.kg.commons.model.User;
import io.swagger.annotations.Api;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The users API allows to fetch information about users
 */
@RestController
@RequestMapping("/users")
public class Users {

    private final AuthenticationSvcForAdmin authenticationSvc;

    private final UserController userController;

    private final Gson gson;

    public Users(AuthenticationSvcForAdmin authenticationSvc, UserController userController, Gson gson) {
        this.authenticationSvc = authenticationSvc;
        this.userController = userController;
        this.gson = gson;
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> getUser() {
        User authUserInfo = authenticationSvc.getUser();
        if (authUserInfo.getNativeId() != null) {
            return ResponseEntity.ok(userController.getOrCreateUserInfo(authUserInfo));
        } else {
            throw new RuntimeException(String.format("Was receiving an invalid payload from authentication: %s", gson.toJson(authUserInfo)));
        }
    }

    @GetMapping(value = "/others/{id}")
    public ResponseEntity<User> getUser(@PathVariable("id") String id) {
        String nativeId = userController.getNativeId(id);
        User otherUser = authenticationSvc.getOtherUser(nativeId);
        return otherUser != null ? ResponseEntity.ok(otherUser) : ResponseEntity.notFound().build();
    }
}

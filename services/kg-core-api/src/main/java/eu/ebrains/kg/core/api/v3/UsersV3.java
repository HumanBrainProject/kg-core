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
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.config.openApiGroups.Extra;
import eu.ebrains.kg.commons.config.openApiGroups.Simple;
import eu.ebrains.kg.commons.markers.ExposesUserInfo;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * The user API allows to retrieve information how to access the authentication service and to retrieve information about the users.
 */
@RestController
@RequestMapping(Version.V3 +"/users")
public class UsersV3 {
    private final Authentication.Client authentication;

    public UsersV3(Authentication.Client authentication) {
        this.authentication = authentication;
    }

    @Operation(summary = "Retrieve user information from the passed token (including detailed information such as e-mail address)")
    @GetMapping("/me")
    @ExposesUserInfo
    @Simple
    public ResponseEntity<Result<User>> myUserInfo() {
        User myUserInfo = authentication.getMyUserInfo();
        return myUserInfo!=null ? ResponseEntity.ok(Result.ok(myUserInfo)) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Retrieve the roles for the current user")
    @GetMapping("/me/roles")
    @ExposesUserInfo
    @Extra
    public ResponseEntity<Result<UserWithRoles>> myRoles() {
        final UserWithRoles roles = authentication.getRoles(false);
        return roles!=null ? ResponseEntity.ok(Result.ok(roles)) : ResponseEntity.notFound().build();
    }

}

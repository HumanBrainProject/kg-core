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

import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.api.PrimaryStoreUsers;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesConfigurationInformation;
import eu.ebrains.kg.commons.markers.ExposesUserInfo;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.core.api.common.Users;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * The user API allows to retrieve information how to access the authentication service and to retrieve information about the users.
 */
@RestController
@RequestMapping(Version.UPCOMING +"/users")
public class UsersV3 extends Users {

    private final Authentication.Client authentication;
    private final PrimaryStoreUsers.Client primaryStoreUsers;

    public UsersV3(Authentication.Client authentication, PrimaryStoreUsers.Client primaryStoreUsers) {
        this.authentication = authentication;
        this.primaryStoreUsers = primaryStoreUsers;
    }

    @Operation(summary = "Retrieve user information from the passed token (including detailed information such as e-mail address)")
    @GetMapping("/me")
    @ExposesUserInfo
    @Tag(name = TAG)
    public ResponseEntity<Result<User>> myUserInfo() {
        User myUserInfo = authentication.getMyUserInfo();
        return myUserInfo!=null ? ResponseEntity.ok(Result.ok(myUserInfo)) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Retrieve the roles for the current user")
    @GetMapping("/me/roles")
    @ExposesUserInfo
    @Tag(name = TAG_EXTRA)
    public ResponseEntity<Result<UserWithRoles>> myRoles() {
        final UserWithRoles roles = authentication.getRoles(false);
        return roles!=null ? ResponseEntity.ok(Result.ok(roles)) : ResponseEntity.notFound().build();
    }


    @Operation(summary = "Retrieve a list of users")
    @GetMapping
    @ExposesUserInfo
    @Tag(name = TAG_EXTRA)
    public ResponseEntity<PaginatedResult<NormalizedJsonLd>> getUserList(@ParameterObject PaginationParam paginationParam) {
        Paginated<NormalizedJsonLd> users = this.primaryStoreUsers.getUsers(paginationParam);
        users.getData().forEach(NormalizedJsonLd::removeAllInternalProperties);
        return ResponseEntity.ok(PaginatedResult.ok(users));
    }

    @Operation(summary = "Retrieve a list of users from IAM")
    @GetMapping("/fromIAM")
    @ExposesUserInfo
    @Tag(name = TAG_EXTRA)
    public ResponseEntity<Result<List<ReducedUserInformation>>>findUsers(@RequestParam("search") String search) {
        List<ReducedUserInformation> users = authentication.findUsers(search);
        return users!=null ? ResponseEntity.ok(Result.ok(users)) : ResponseEntity.notFound().build();
    }


    @Operation(summary = "Retrieve a list of users without sensitive information")
    @GetMapping("/limited")
    @ExposesUserInfo
    @Tag(name = TAG_EXTRA)
    public ResponseEntity<PaginatedResult<NormalizedJsonLd>> getUserListLimited(@ParameterObject PaginationParam paginationParam, @RequestParam(value = "id", required = false) String id) {
        Paginated<NormalizedJsonLd> users = this.primaryStoreUsers.getUsersWithLimitedInfo(paginationParam, id);
        users.getData().forEach(NormalizedJsonLd::removeAllInternalProperties);
        return ResponseEntity.ok(PaginatedResult.ok(users));
    }


    @Operation(summary = "Get the current terms of use")
    @GetMapping(value = "/termsOfUse")
    @Tag(name = TAG)
    public ResponseEntity<TermsOfUseResult> getTermsOfUse() {
        return ResponseEntity.ok(authentication.getTermsOfUse());
    }

    @Operation(summary = "Accept the terms of use in the given version")
    @PostMapping(value = "/termsOfUse/{version}/accept")
    @Tag(name = TAG)
    public void acceptTermsOfUse(@PathVariable("version") String version) {
        authentication.acceptTermsOfUse(version);
    }

}

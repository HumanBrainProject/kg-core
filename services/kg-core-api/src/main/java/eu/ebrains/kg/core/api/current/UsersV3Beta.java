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

import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.api.GraphDBInstances;
import eu.ebrains.kg.commons.api.PrimaryStoreEvents;
import eu.ebrains.kg.commons.api.PrimaryStoreUsers;
import eu.ebrains.kg.commons.config.openApiGroups.Advanced;
import eu.ebrains.kg.commons.config.openApiGroups.AnonymousAccess;
import eu.ebrains.kg.commons.config.openApiGroups.Extra;
import eu.ebrains.kg.commons.config.openApiGroups.Simple;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesConfigurationInformation;
import eu.ebrains.kg.commons.markers.ExposesUserInfo;
import eu.ebrains.kg.commons.markers.ExposesUserPicture;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.api.common.Users;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * The user API allows to retrieve information how to access the authentication service and to retrieve information about the users.
 */
@RestController
@RequestMapping(Version.CURRENT +"/users")
public class UsersV3Beta extends Users {

    private final Authentication.Client authentication;
    private final PrimaryStoreUsers.Client primaryStoreUsers;
    private final GraphDBInstances.Client graphDBInstances;
    private final IdUtils idUtils;
    private final PrimaryStoreEvents.Client primaryStoreEvents;

    public UsersV3Beta(Authentication.Client authentication, PrimaryStoreUsers.Client primaryStoreUsers, GraphDBInstances.Client graphDBInstances, IdUtils idUtils, PrimaryStoreEvents.Client primaryStoreEvents) {
        super();
        this.authentication = authentication;
        this.primaryStoreUsers = primaryStoreUsers;
        this.graphDBInstances = graphDBInstances;
        this.idUtils = idUtils;
        this.primaryStoreEvents = primaryStoreEvents;
    }

    @Operation(summary = "Get the endpoint of the authentication service", description = "The new auth/config endpoint will provide the URL to the OIDC configuration. The information about the token endpoint should be taken from there.")
    @GetMapping(value = "/authorization", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExposesConfigurationInformation
    @Deprecated(forRemoval = true)
    @Tag(name = TAG_EXTRA)
    @AnonymousAccess
    public Result<JsonLdDoc> getAuthEndpoint() {
        JsonLdDoc ld = new JsonLdDoc();
        ld.addProperty("endpoint", authentication.authEndpoint());
        return Result.ok(ld);
    }

    @Operation(summary = "Get the endpoint of the openid configuration",  description = "The new auth/config endpoint will provide the URL to the OIDC configuration. The information about the token endpoint should be taken from there.")
    @GetMapping(value = "/authorization/config", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExposesConfigurationInformation
    @Tag(name = TAG)
    @Deprecated(forRemoval = true)
    @AnonymousAccess
    public Result<JsonLdDoc> getOpenIdConfigUrl() {
        JsonLdDoc ld = new JsonLdDoc();
        ld.addProperty("endpoint", authentication.openIdConfigUrl());
        return Result.ok(ld);
    }

    @Operation(summary = "Get the endpoint to retrieve your token (e.g. via client id and client secret)", description = "The new auth/config endpoint will provide the URL to the OIDC configuration. The information about the token endpoint should be taken from there.")
    @GetMapping(value = "/authorization/tokenEndpoint", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExposesConfigurationInformation
    @Deprecated(forRemoval = true)
    @Tag(name = TAG_EXTRA)
    @AnonymousAccess
    public Result<JsonLdDoc> getTokenEndpoint() {
        JsonLdDoc ld = new JsonLdDoc();
        ld.addProperty("endpoint", authentication.tokenEndpoint());
        return Result.ok(ld);
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
    public ResponseEntity<Result<List<ReducedUserInformation>>> findUsers(@RequestParam("search") String search) {
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

    @Operation(summary = "Get a pictures for a list of users (only found ones are returned)", description = "This method is intended for removal since the provision of user avatars is not meant to be part of the KG core functionalities.")
    @PostMapping(value = "/pictures")
    @ExposesUserPicture
    @Tag(name = TAG_EXTRA)
    @Deprecated(forRemoval = true)
    public ResponseEntity<Map<UUID, String>> getUserPictures(@RequestBody List<UUID> userIds) {
        SpaceName targetSpace = InternalSpace.USERS_PICTURE_SPACE;
        Map<UUID, Result<NormalizedJsonLd>> instancesByIds = graphDBInstances.getInstancesByIds(userIds.stream().filter(Objects::nonNull).map(userId -> new InstanceId(createUserPictureId(userId), targetSpace).serialize()).collect(Collectors.toList()), DataStage.IN_PROGRESS, null, false, false, false, null);
        Map<UUID, UUID> userPictureIdToUserId = userIds.stream().collect(Collectors.toMap(this::createUserPictureId, v-> v));
        return ResponseEntity.ok(instancesByIds.keySet().stream().filter(k -> instancesByIds.get(k).getData() != null && instancesByIds.get(k).getData().getAs(EBRAINSVocabulary.META_PICTURE, String.class) != null).collect(Collectors.toMap(userPictureIdToUserId::get, v -> "data:image/jpeg;base64,"+instancesByIds.get(v).getData().getAs(EBRAINSVocabulary.META_PICTURE, String.class))));
    }


    @Operation(summary = "Get a picture for a specific user", description = "This method is intended for removal since the provision of user avatars is not meant to be part of the KG core functionalities.")
    @GetMapping(value = "/{id}/picture")
    @ExposesUserPicture
    @Tag(name = TAG_EXTRA)
    @Deprecated(forRemoval = true)
    public ResponseEntity<String> getUserPicture(@PathVariable("id") UUID userId) {
        SpaceName targetSpace = InternalSpace.USERS_PICTURE_SPACE;
        NormalizedJsonLd instance = graphDBInstances.getInstanceById(targetSpace.getName(), createUserPictureId(userId), DataStage.IN_PROGRESS, false, false, false, null, true);
        if(instance!=null){
            String picture = instance.getAs(EBRAINSVocabulary.META_PICTURE, String.class);
            if(picture!=null){
                return ResponseEntity.ok("data:image/jpeg;base64,"+picture);
            }
        }
        return ResponseEntity.notFound().build();
    }

    private UUID createUserPictureId(UUID userId){
        return IdUtils.createMetaRepresentationUUID(userId + "picture");
    }

    @Operation(summary = "Define a picture for a specific user", description = "This method is intended for removal since the provision of user avatars is not meant to be part of the KG core functionalities.")
    @PutMapping("/{id}/picture")
    @Tag(name = TAG_EXTRA)
    @Deprecated(forRemoval = true)
    public ResponseEntity<Result<Void>> defineUserPicture(@PathVariable("id") UUID userId, @RequestBody String base64encodedImage) {
        SpaceName targetSpace = InternalSpace.USERS_PICTURE_SPACE;
        NormalizedJsonLd doc = new NormalizedJsonLd();
        doc.put(EBRAINSVocabulary.META_PICTURE, base64encodedImage);
        UUID uuid = createUserPictureId(userId);
        doc.setId(idUtils.buildAbsoluteUrl(uuid));
        doc.addTypes(EBRAINSVocabulary.META_USER_PICTURE_TYPE);
        primaryStoreEvents.postEvent(Event.createUpsertEvent(targetSpace, uuid, Event.Type.INSERT, doc));
        return ResponseEntity.ok(Result.ok());
    }

}

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

import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesConfigurationInformation;
import eu.ebrains.kg.commons.markers.ExposesUserInfo;
import eu.ebrains.kg.commons.markers.ExposesUserPicture;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.serviceCall.CoreInstancesToGraphDB;
import eu.ebrains.kg.core.serviceCall.CoreToAuthentication;
import eu.ebrains.kg.core.serviceCall.CoreToPrimaryStore;
import eu.ebrains.kg.core.serviceCall.CoreUsersToGraphDB;
import io.swagger.v3.oas.annotations.Operation;
import org.bouncycastle.util.encoders.Base64;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * The user API allows to retrieve information how to access the authentication service and to retrieve information about the users.
 */
@RestController
@RequestMapping(Version.API+"/users")
public class Users {

    private final CoreToAuthentication authenticationSvc;
    private final CoreUsersToGraphDB coreUsersToGraphDB;
    private final CoreInstancesToGraphDB coreInstancesToGraphDB;
    private final IdUtils idUtils;
    private final CoreToPrimaryStore primaryStoreSvc;
    private final AuthContext authContext;

    public Users(CoreToAuthentication authenticationSvc, CoreUsersToGraphDB coreUsersToGraphDB, CoreInstancesToGraphDB coreInstancesToGraphDB, IdUtils idUtils, CoreToPrimaryStore primaryStoreSvc, AuthContext authContext) {
        this.authenticationSvc = authenticationSvc;
        this.coreUsersToGraphDB = coreUsersToGraphDB;
        this.coreInstancesToGraphDB = coreInstancesToGraphDB;
        this.idUtils = idUtils;
        this.primaryStoreSvc = primaryStoreSvc;
        this.authContext = authContext;
    }

    @Operation(summary = "Get the endpoint of the authentication service")
    @GetMapping(value = "/authorization", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExposesConfigurationInformation
    public Result<JsonLdDoc> getAuthEndpoint() {
        JsonLdDoc ld = new JsonLdDoc();
        ld.addProperty("endpoint", authenticationSvc.endpoint());
        return Result.ok(ld);
    }

    @Operation(summary = "Get the endpoint to retrieve your token (e.g. via client id and client secret)")
    @GetMapping(value = "/authorization/tokenEndpoint", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExposesConfigurationInformation
    public Result<JsonLdDoc> getTokenEndpoint() {
        JsonLdDoc ld = new JsonLdDoc();
        ld.addProperty("endpoint", authenticationSvc.tokenEndpoint());
        return Result.ok(ld);
    }

    @Operation(summary = "Retrieve user information from the passed token (including detailed information such as e-mail address)")
    @GetMapping("/me")
    @ExposesUserInfo
    public ResponseEntity<Result<User>> profile() {
        User myUserProfile = authenticationSvc.getMyUserProfile();
        return myUserProfile!=null ? ResponseEntity.ok(Result.ok(myUserProfile)) : ResponseEntity.notFound().build();
    }


    @Operation(summary = "Retrieve a list of users")
    @GetMapping
    @ExposesUserInfo
    public ResponseEntity<PaginatedResult<NormalizedJsonLd>> getUserList(@ParameterObject PaginationParam paginationParam) {
        Paginated<NormalizedJsonLd> users = coreUsersToGraphDB.getUsers(paginationParam);
        users.getData().forEach(NormalizedJsonLd::removeAllInternalProperties);
        return ResponseEntity.ok(PaginatedResult.ok(users));
    }

    @Operation(summary = "Get a pictures for a list of users (only found ones are returned)")
    @PostMapping(value = "/pictures")
    @ExposesUserPicture
    public ResponseEntity<Map<UUID, String>> getUserPictures(@RequestBody List<UUID> userIds) {
        SpaceName targetSpace = InternalSpace.USERS_PICTURE_SPACE;
        Map<UUID, Result<NormalizedJsonLd>> instancesByIds = coreInstancesToGraphDB.getInstancesByIds(DataStage.IN_PROGRESS, userIds.stream().map(userId -> new InstanceId(createUserPictureId(userId), targetSpace)).collect(Collectors.toList()), false, false);
        Map<UUID, UUID> userPictureIdToUserId = userIds.stream().collect(Collectors.toMap(this::createUserPictureId, v-> v));
        return ResponseEntity.ok(instancesByIds.keySet().stream().filter(k -> instancesByIds.get(k).getData() != null && instancesByIds.get(k).getData().getAs(EBRAINSVocabulary.META_PICTURE, String.class) != null).collect(Collectors.toMap(userPictureIdToUserId::get, v -> "data:image/jpeg;base64,"+instancesByIds.get(v).getData().getAs(EBRAINSVocabulary.META_PICTURE, String.class))));
    }


    @Operation(summary = "Get a picture for a specific user")
    @GetMapping(value = "/{id}/picture")
    @ExposesUserPicture
    public ResponseEntity<String> getUserPicture(@PathVariable("id") UUID userId) {
        SpaceName targetSpace = InternalSpace.USERS_PICTURE_SPACE;
        NormalizedJsonLd instance = coreInstancesToGraphDB.getInstance(DataStage.IN_PROGRESS, new InstanceId(createUserPictureId(userId), targetSpace), false, false);
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

    @Operation(summary = "Define a picture for a specific user")
    @PutMapping("/{id}/picture")
    public ResponseEntity<Result<Void>> defineUserPicture(@PathVariable("id") UUID userId, @RequestBody String base64encodedImage) {
        SpaceName targetSpace = InternalSpace.USERS_PICTURE_SPACE;
        NormalizedJsonLd doc = new NormalizedJsonLd();
        doc.put(EBRAINSVocabulary.META_PICTURE, base64encodedImage);
        UUID uuid = createUserPictureId(userId);
        doc.setId(idUtils.buildAbsoluteUrl(uuid));
        doc.addTypes(EBRAINSVocabulary.META_USER_PICTURE_TYPE);
        primaryStoreSvc.postEvent(Event.createUpsertEvent(targetSpace, uuid, Event.Type.INSERT, doc), false, authContext.getAuthTokens());
        return ResponseEntity.ok(Result.ok());
    }

}

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
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.ReleaseStatus;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.core.serviceCall.CoreToIds;
import eu.ebrains.kg.core.serviceCall.CoreToRelease;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The releasing API allows to release and unrelease instances as well as to fetch the status of instances.
 */
@RestController
@RequestMapping(Version.API+"/releases")
public class Releases {

    private final CoreToRelease releaseSvc;
    private final CoreToIds idsSvc;

    public Releases(CoreToRelease releaseSvc, CoreToIds idsSvc) {
        this.releaseSvc = releaseSvc;
        this.idsSvc = idsSvc;
    }

    //RELEASE instances
    @ApiOperation("Release or re-release an instance")
    @PutMapping
    public ResponseEntity<Void> releaseInstance(@RequestParam("id") UUID id, @RequestParam("revision") String revision) {
        InstanceId instanceId = idsSvc.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        }
        else if(instanceId.isDeprecated()){
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        releaseSvc.releaseInstance(instanceId, revision);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @ApiOperation(value = "Unrelease an instance")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The instance that has been unreleased"), @ApiResponse(code = 404, message = "Instance not found")})
    @DeleteMapping
    public ResponseEntity<Void> unreleaseInstance(@RequestParam("id") UUID id) {
        InstanceId instanceId = idsSvc.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        }
        else if(instanceId.isDeprecated()){
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        releaseSvc.unreleaseInstance(instanceId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @ApiOperation(value = "Get the release status for an instance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The release status of the instance"),
            @ApiResponse(code = 404, message = "Instance not found")})
    @GetMapping(value = "/status")
    public ResponseEntity<Result<ReleaseStatus>> getReleaseStatus(@RequestParam("id") UUID id, @RequestParam("releaseTreeScope") ReleaseTreeScope releaseTreeScope) {
        InstanceId instanceId = idsSvc.resolveId(DataStage.IN_PROGRESS, id);
        if (instanceId == null) {
            return ResponseEntity.notFound().build();
        }
        else if(instanceId.isDeprecated()){
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        ReleaseStatus releaseStatus = releaseSvc.getReleaseStatus(instanceId, releaseTreeScope);
        return ResponseEntity.ok(Result.ok(releaseStatus));
    }

    @ApiOperation(value = "Get the release status for multiple instances")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The release status of the instance"),
            @ApiResponse(code = 404, message = "Instance not found")})
    @PostMapping(value = "/statusByIds")
    public Result<Map<UUID, Result<ReleaseStatus>>> getReleasesStatusByIds(@RequestBody List<UUID> listOfIds, @RequestParam("releaseTreeScope") ReleaseTreeScope releaseTreeScope) {
        List<InstanceId> instanceIds = idsSvc.resolveIdsByUUID(DataStage.IN_PROGRESS, listOfIds, false);
        return Result.ok(instanceIds.stream().filter(instanceId -> !instanceId.isDeprecated()).collect(Collectors.toMap(InstanceId::getUuid, instanceId ->
                Result.ok(releaseSvc.getReleaseStatus(instanceId, releaseTreeScope))
        )));
    }

}

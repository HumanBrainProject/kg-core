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

package eu.ebrains.kg.admin.controller;

import eu.ebrains.kg.admin.serviceCall.AdminToGraphDB;
import eu.ebrains.kg.admin.serviceCall.AdminToPrimaryStore;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.model.User;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class AdminUserController {

    private static Space USERS_SPACE = new Space("users");

    private final AdminToGraphDB graphDBSvc;

    private final AdminToPrimaryStore primaryStoreSvc;

    private final IdUtils idUtils;

    public AdminUserController(AdminToGraphDB graphDBSvc, AdminToPrimaryStore primaryStoreSvc, IdUtils idUtils) {
        this.graphDBSvc = graphDBSvc;
        this.primaryStoreSvc = primaryStoreSvc;
        this.idUtils = idUtils;
    }

    public User getOrCreateUserInfo(User authUserInfo) {
        UUID instanceId = UUID.nameUUIDFromBytes(authUserInfo.getNativeId().getBytes(StandardCharsets.UTF_8));
        NormalizedJsonLd instance = graphDBSvc.getInstance(DataStage.IN_PROGRESS, USERS_SPACE, instanceId, true);
        if (instance == null) {
            authUserInfo.setId(idUtils.buildAbsoluteUrl(instanceId));
            Event event = new Event(USERS_SPACE, instanceId, authUserInfo, Event.Type.INSERT, new Date());
            primaryStoreSvc.postEvent(event, false);
            instance = graphDBSvc.getInstance(DataStage.IN_PROGRESS, USERS_SPACE, instanceId, true);
            if (instance == null) {
                throw new RuntimeException("Failed to get user info after creation");
            }
        }
        instance.removeAllInternalProperties();
        return new User(instance);
    }


    public String getNativeId(String id) {
        if (id != null) {
            try{
                UUID uuid = UUID.fromString(id);
                //It's a kg-id. We need to resolve it to the native authentication system id first...
                NormalizedJsonLd document = graphDBSvc.getInstance(DataStage.IN_PROGRESS, USERS_SPACE, uuid, true);
                if (document != null) {
                    return new User(document).getNativeId();
                } else {
                    return id;
                }
            }
            catch(IllegalArgumentException e){
                //It's not a kg-id
            }
        }
        return id;
    }
}

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

package eu.ebrains.kg.nexusv0.serviceCall;

import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.ServiceCallWithClientSecret;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.exception.AmbiguousException;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class CoreSvc {

    private final ServiceCallWithClientSecret serviceCallWithClientSecret;
    private final static String SERVICE_URL = "http://kg-core-api/"+ Version.API;

    public CoreSvc(ServiceCallWithClientSecret serviceCallWithClientSecret) {
        this.serviceCallWithClientSecret = serviceCallWithClientSecret;
    }

    public void inferInstance(Space space, String identifier){
        serviceCallWithClientSecret.post(String.format("%s/extra/inference/%s?identifier=%s", SERVICE_URL, space.getName(), identifier), null, new AuthTokens(), String.class);
    }
    public void inferDeferred(){
        serviceCallWithClientSecret.post(String.format("%s/extra/inference/deferred", SERVICE_URL), null, new AuthTokens(), String.class);
    }

    public Result<List<NormalizedJsonLd>> getInstancesByIdentifiers(Set<String> identifiers){
        return serviceCallWithClientSecret.post(String.format("%s/instancesByIdentifiers?stage=IN_PROGRESS&returnEmbedded=false", SERVICE_URL), identifiers, new AuthTokens(), ResultOfDocuments.class);
    }

    public Result<?> createInstance(JsonLdDoc payload, Space space, String user, ZonedDateTime dateTime, boolean deferInference){
        return serviceCallWithClientSecret.post(String.format("%s/instances?space=%s&returnPayload=false&returnEmbedded=false&externalUserDefinition=%s&externalEventTime=%s&deferInference=%b", SERVICE_URL, space.getName(), user!=null ? user : "", dateTime!=null ? dateTime.format(DateTimeFormatter.ISO_INSTANT) : "", deferInference), payload, new AuthTokens(), Result.class);
    }

    public Result<NormalizedJsonLd> replaceContribution(JsonLdDoc payload, UUID uuid, String user, ZonedDateTime dateTime, boolean deferInference){
        return serviceCallWithClientSecret.patch(String.format("%s/instances/%s?removeNonDeclaredProperties=true&returnPayload=false&returnEmbedded=false&externalUserDefinition=%s&externalEventTime=%s&undeprecate=true&deferInference=%b", SERVICE_URL, uuid, user!=null ? user : "", dateTime!=null ? dateTime.format(DateTimeFormatter.ISO_INSTANT) : "", deferInference), payload, new AuthTokens(), ResultOfDocument.class);
    }

    public void releaseInstance(UUID uuid, String revision){
        serviceCallWithClientSecret.put(String.format("%s/releases?id=%s&revision=%s", SERVICE_URL, uuid, revision), null, new AuthTokens(), Result.class);
    }

    public void unreleaseInstance(UUID uuid){
        serviceCallWithClientSecret.delete(String.format("%s/releases?id=%s", SERVICE_URL, uuid), new AuthTokens(), Void.class);
    }

    public NormalizedJsonLd toNormalizedJsonLd(JsonLdDoc payload) {
        return serviceCallWithClientSecret.post(String.format("%s/extra/normalizedPayload", SERVICE_URL), payload, new AuthTokens(), NormalizedJsonLd.class);
    }
//    public void deleteInstance(UUID uuid){
//        serviceCall.delete(String.format("%s/instances/%s", SERVICE_URL, uuid), null, authContext.getAuthTokens(), Result.class);
//    }

    public User getUserByMidId(String midId){
        User[] users = serviceCallWithClientSecret.get(String.format("%s/users/byAttribute/mid.id/%s", SERVICE_URL, midId), new AuthTokens(), User[].class);
        if(users == null || users.length==0){
            return null;
        }
        if(users.length>1){
            throw new AmbiguousException(String.format("Found multiple users with the mid.id %s", midId));
        }
        return users[0];
    }
}

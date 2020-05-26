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

package eu.ebrains.kg.core.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.model.ReleaseStatus;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import org.springframework.stereotype.Component;

@Component
public class CoreToRelease {
    private final ServiceCall serviceCall;
    private final AuthContext authContext;

    public CoreToRelease(ServiceCall serviceCall, AuthContext authContext) {
        this.serviceCall = serviceCall;
        this.authContext = authContext;
    }

    private static final String RELEASE_ENDPOINT = "http://kg-releasing/internal/releases/";

    public void releaseInstance(InstanceId instanceId, String revision) {
        serviceCall.put(String.format("%s%s?rev=%s", RELEASE_ENDPOINT, instanceId.serialize(), revision), null, authContext.getAuthTokens(), Void.class);
    }

    public void unreleaseInstance(InstanceId instanceId) {
        serviceCall.delete(String.format("%s%s", RELEASE_ENDPOINT, instanceId.serialize()), authContext.getAuthTokens(), Void.class);
    }

    public ReleaseStatus getReleaseStatus(InstanceId instanceId, ReleaseTreeScope treeScope) {
        return serviceCall.get(String.format("%s%s?releaseTreeScope=%s", RELEASE_ENDPOINT, instanceId.serialize(), treeScope), authContext.getAuthTokens(), ReleaseStatus.class);
    }
}

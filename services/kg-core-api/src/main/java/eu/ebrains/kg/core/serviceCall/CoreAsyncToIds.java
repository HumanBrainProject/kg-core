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

import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class CoreAsyncToIds {

    private final ServiceCall serviceCall;
    private final IdUtils idUtils;

    public CoreAsyncToIds(ServiceCall serviceCall, IdUtils idUtils) {
        this.serviceCall = serviceCall;
        this.idUtils = idUtils;
    }

    private final static String SERVICE_URL = "http://kg-ids/internal";

    public InstanceId resolveId(DataStage stage, UUID id, AuthTokens authTokens) {
        List<InstanceId> documentIds = resolveIdsByUUID(stage, Collections.singletonList(id), false, authTokens);
        if (documentIds != null && documentIds.size() == 1) {
            return documentIds.get(0);
        }
        return null;
    }

    public List<InstanceId> resolveIdsByUUID(DataStage stage, List<UUID> ids, boolean returnUnresolved, AuthTokens authTokens) {
        List<IdWithAlternatives> idWithAlternatives = ids.stream().map(id -> new IdWithAlternatives().setId(id).setAlternatives(Collections.singleton(idUtils.buildAbsoluteUrl(id).getId()))).collect(Collectors.toList());
        return resolveIds(stage, idWithAlternatives, returnUnresolved, authTokens);
    }

    private List<InstanceId> resolveIds(DataStage stage, List<IdWithAlternatives> idWithAlternatives, boolean returnUnresolved, AuthTokens authTokens) {
        List<InstanceId> resultList = new ArrayList<>();
        JsonLdIdMapping[] result = serviceCall.post(String.format("%s/ids/%s/resolved", SERVICE_URL, stage.name()), idWithAlternatives, authTokens, JsonLdIdMapping[].class);
        if (result != null) {
            resultList.addAll(Arrays.stream(result).
                    filter(id -> id.getResolvedIds() != null && id.getResolvedIds().size() == 1).
                    map(id -> {
                        idWithAlternatives.stream().filter(idWithAlternative -> id.getRequestedId().equals(idWithAlternative.getId())).forEach(idWithAlternative -> idWithAlternative.setFound(true));
                        return new InstanceId(idUtils.getUUID(id.getResolvedIds().iterator().next()), id.getSpace(), id.isDeprecated());
                    }).
                    collect(Collectors.toList()));
        }
        if (returnUnresolved) {
            List<InstanceId> unresolvedIds = idWithAlternatives.stream().filter(idWithAlternative -> !idWithAlternative.isFound()).map(idWithAlternative ->
                    {
                        InstanceId instanceId = new InstanceId(idWithAlternative.getId(), null);
                        instanceId.setUnresolved(true);
                        return instanceId;
                    }
            ).collect(Collectors.toList());
            resultList.addAll(unresolvedIds);
        }
        return resultList;
    }
}

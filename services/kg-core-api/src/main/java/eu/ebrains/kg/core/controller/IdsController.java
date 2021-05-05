/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.core.controller;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.Ids;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class IdsController {

    private final Ids.Client api;
    private final IdUtils idUtils;

    public IdsController(Ids.Client idsAPI, IdUtils idUtils) {
        this.api = idsAPI;
        this.idUtils = idUtils;
    }

    public void undeprecateInstance(UUID instance) {
        api.deprecateId(DataStage.IN_PROGRESS, instance, true);
    }

    public InstanceId resolveId(DataStage stage, UUID id) {
        List<InstanceId> documentIds = resolveIdsByUUID(stage, Collections.singletonList(id), false);
        if (documentIds != null && documentIds.size() == 1) {
            return documentIds.get(0);
        }
        return null;
    }

    public List<InstanceId> resolveIds(DataStage stage, IdWithAlternatives id, boolean returnUnresolved) {
        return resolveIds(stage, Collections.singletonList(id), returnUnresolved);
    }

    public List<InstanceId> resolveIdsByUUID(DataStage stage, List<UUID> ids, boolean returnUnresolved) {
        List<IdWithAlternatives> idWithAlternatives = ids.stream().map(id -> new IdWithAlternatives().setId(id).setAlternatives(Collections.singleton(idUtils.buildAbsoluteUrl(id).getId()))).collect(Collectors.toList());
        return resolveIds(stage, idWithAlternatives, returnUnresolved);
    }

    public List<JsonLdIdMapping> resolveIds(DataStage stage, List<IdWithAlternatives> idWithAlternatives){
        return api.resolveId(idWithAlternatives, stage);
    }

    private List<InstanceId> resolveIds(DataStage stage, List<IdWithAlternatives> idWithAlternatives, boolean returnUnresolved) {
        List<InstanceId> resultList = new ArrayList<>();
        List<JsonLdIdMapping> result = resolveIds(stage, idWithAlternatives);
        if (result != null) {
            resultList.addAll(result.stream().
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

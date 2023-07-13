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

package eu.ebrains.kg.core.controller;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.Ids;
import eu.ebrains.kg.commons.exception.AmbiguousException;
import eu.ebrains.kg.commons.exception.CancelProcessException;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class IdsController {

    private final Ids.Client api;
    private final IdUtils idUtils;

    public IdsController(Ids.Client idsAPI, IdUtils idUtils) {
        this.api = idsAPI;
        this.idUtils = idUtils;
    }


    public InstanceId resolveId(DataStage stage, UUID id) {
        if(id!=null) {
            List<InstanceId> documentIds = resolveIdsByUUID(stage, Collections.singletonList(id), false);
            if (documentIds != null && documentIds.size() == 1) {
                return documentIds.get(0);
            }
        }
        return null;
    }

    public void checkIdForExistence(UUID uuid, Set<String> identifiers){
        final InstanceId id = findId(uuid, identifiers);
        if(id!=null){
            final Result<?> nok = Result.nok(HttpStatus.CONFLICT.value(), String.format("The payload you're providing is pointing to the instance %s (either by the %s or the %s field it contains). Please do a PUT or a PATCH to the mentioned id instead.", id.serialize(), JsonLdConsts.ID, SchemaOrgVocabulary.IDENTIFIER), id.getUuid());
            throw new CancelProcessException(nok, HttpStatus.CONFLICT.value());
        }
    }

    public InstanceId findId(UUID uuid, Set<String> identifiers) {
        try {
            return this.api.findInstanceByIdentifiers(uuid, new ArrayList<>(identifiers), DataStage.IN_PROGRESS);
        } catch (AmbiguousException e) {
            final Result<?> nok = Result.nok(HttpStatus.CONFLICT.value(), String.format("The payload you're providing contains a shared identifier of the instances %s. Please merge those instances if they are reflecting the same entity.", e.getMessage()));
            throw new CancelProcessException(nok, HttpStatus.CONFLICT.value());
        }
    }

    public List<InstanceId> resolveIdsByUUID(DataStage stage, List<UUID> ids, boolean returnUnresolved) {
        List<IdWithAlternatives> idWithAlternatives = ids.stream().map(id -> new IdWithAlternatives().setId(id).setAlternatives(Collections.singleton(idUtils.buildAbsoluteUrl(id).getId()))).collect(Collectors.toList());
        return resolveIds(stage, idWithAlternatives, returnUnresolved);
    }

    public Map<UUID, InstanceId> resolveIds(DataStage stage, List<IdWithAlternatives> idWithAlternatives) {
        return api.resolveId(idWithAlternatives, stage);
    }


    private List<InstanceId> resolveIds(DataStage stage, List<IdWithAlternatives> idWithAlternatives, boolean returnUnresolved) {
        List<InstanceId> resultList;
        Map<UUID, InstanceId> result = resolveIds(stage, idWithAlternatives);
        if (result != null) {
            resultList = idWithAlternatives.stream().map(idWithAlternative -> {
                idWithAlternative.setFound(result.containsKey(idWithAlternative.getId()));
                return result.get(idWithAlternative.getId());
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }
        else{
             resultList = new ArrayList<>();
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

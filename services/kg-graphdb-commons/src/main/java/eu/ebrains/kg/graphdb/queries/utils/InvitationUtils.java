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

package eu.ebrains.kg.graphdb.queries.utils;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.Ids;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.internal.spaces.Space;
import eu.ebrains.kg.graphdb.instances.controller.ArangoRepositoryInstances;

import java.util.*;
import java.util.stream.Collectors;

public class InvitationUtils {


    public static List<NormalizedJsonLd> getInvitationDocuments(AuthContext authContext, Ids.Client ids, ArangoRepositoryInstances instancesRepository, IdUtils idUtils){
        final List<UUID> invitations = authContext.getUserWithRolesWithoutTermsCheck().getInvitations();
        final Collection<InstanceId> values = ids.resolveId(invitations.stream().distinct().map(id -> new IdWithAlternatives().setId(id).setAlternatives(Collections.singleton(idUtils.buildAbsoluteUrl(id).getId()))).collect(Collectors.toList()), DataStage.IN_PROGRESS).values();
        final Map<UUID, Result<NormalizedJsonLd>> documentsByIdList = instancesRepository.getDocumentsByIdList(DataStage.IN_PROGRESS, new ArrayList<>(values), null, false, false, false, null);
        return documentsByIdList.values().stream().map(Result::getData).collect(Collectors.toList());
    }

    public static SpaceName getClientSpace(AuthContext authContext) {
        Space space = authContext.getClientSpace();
        return space == null ? null : space.getName();
    }
}

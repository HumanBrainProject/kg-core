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
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.core.controller;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class VirtualSpaceController {

    private final CoreInstanceController instanceController;
    private final AuthContext authContext;

    public VirtualSpaceController(CoreInstanceController instanceController, AuthContext authContext) {
        this.instanceController = instanceController;
        this.authContext = authContext;
    }

    public boolean isVirtualSpace(String spaceName){
        if(spaceName != null) {
            switch (spaceName) {
                case SpaceName.REVIEW_SPACE:
                    return true;
            }
        }
        return false;
    }


    public List<NormalizedJsonLd> getInstancesByInvitation(ResponseConfiguration responseConfiguration, DataStage stage, String type){
        Stream<NormalizedJsonLd> stream = handleInvitations(responseConfiguration, stage);
        if(type!=null){
            stream = stream.filter(d -> d.types().contains(type));
        }
        return stream.collect(Collectors.toList());
    }


    private Stream<NormalizedJsonLd> handleInvitations(ResponseConfiguration responseConfiguration, DataStage stage){
        final ExtendedResponseConfiguration r = new ExtendedResponseConfiguration();
        r.setReturnAlternatives(responseConfiguration.isReturnAlternatives());
        r.setReturnEmbedded(responseConfiguration.isReturnEmbedded());
        r.setReturnPayload(responseConfiguration.isReturnPayload());
        r.setReturnPermissions(responseConfiguration.isReturnPermissions());
        final List<String> invitationIds = authContext.getUserWithRoles().getInvitations().stream().map(UUID::toString).sorted().collect(Collectors.toList());
        final Map<String, Result<NormalizedJsonLd>> instancesByIds = instanceController.getInstancesByIds(invitationIds, stage, r);
        return instancesByIds.values().stream().map(Result::getData);
    }


}

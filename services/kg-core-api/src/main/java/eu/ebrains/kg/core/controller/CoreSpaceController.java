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

package eu.ebrains.kg.core.controller;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.FunctionalityInstance;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.core.serviceCall.CoreSpacesToGraphDB;
import eu.ebrains.kg.core.serviceCall.CoreToPrimaryStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CoreSpaceController {

    private final CoreToPrimaryStore primaryStoreSvc;
    private final CoreSpacesToGraphDB graphDbSvc;
    private final AuthContext authContext;

    public CoreSpaceController(CoreToPrimaryStore primaryStoreSvc, CoreSpacesToGraphDB graphDbSvc, AuthContext authContext) {
        this.primaryStoreSvc = primaryStoreSvc;
        this.authContext = authContext;
        this.graphDbSvc = graphDbSvc;
    }

    public NormalizedJsonLd getSpace(ExposedStage stage, String space, boolean permissions) {
        NormalizedJsonLd sp = graphDbSvc.getSpace(new Space(space), stage.getStage());
        if (sp != null && permissions) {
            UserWithRoles userWithRoles = authContext.getUserWithRoles();
            String spaceIdentifier = sp.getAs(SchemaOrgVocabulary.IDENTIFIER, String.class, null);
            if (spaceIdentifier != null) {
                List<Functionality> applyingFunctionalities = userWithRoles.getPermissions().stream().filter(f -> (f.getFunctionality().getStage() == null || f.getFunctionality().getStage() == stage.getStage()) && f.getFunctionality().getFunctionalityGroup() == Functionality.FunctionalityGroup.INSTANCE && f.appliesTo(new Space(spaceIdentifier), null)).map(FunctionalityInstance::getFunctionality).collect(Collectors.toList());
                sp.put(EBRAINSVocabulary.META_PERMISSIONS, applyingFunctionalities);
            }
        }
        return sp;
    }

}

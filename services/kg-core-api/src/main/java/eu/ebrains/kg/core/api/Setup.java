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

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.model.Credential;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.model.TermsOfUse;
import eu.ebrains.kg.core.controller.CoreSpaceController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Version.API+"/setup")
public class Setup {

    private final CoreSpaceController spaceController;
    private final Authentication.Client authentication;

    public Setup(CoreSpaceController spaceController, Authentication.Client authentication) {
        this.spaceController = spaceController;
        this.authentication = authentication;
    }

    @PutMapping("/database")
    @Admin
    public void setupDatabaseStructures(){
        //Create global spec space
        Space space = new Space(InternalSpace.GLOBAL_SPEC, false, false);
        space.setInternalSpace(true);
        spaceController.createSpaceDefinition(space, true, true);
    }

    @PutMapping("/authentication")
    @Admin
    public void setupAuthentication(@RequestBody Credential credential){
        authentication.setup(credential);
    }

    @PutMapping("/termsOfUse")
    @Admin
    public void registerTermsOfUse(@RequestBody TermsOfUse termsOfUse){
        authentication.registerTermsOfUse(termsOfUse);
    }
}

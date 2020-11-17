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

import eu.ebrains.kg.commons.model.Client;
import eu.ebrains.kg.core.serviceCall.CoreToAuthentication;
import org.springframework.stereotype.Component;

@Component
public class CoreClientController {

    private final CoreSpaceController spaceController;
    private final CoreToAuthentication coreToAuthentication;

    public CoreClientController(CoreSpaceController spaceController, CoreToAuthentication coreToAuthentication) {
        this.spaceController = spaceController;
        this.coreToAuthentication = coreToAuthentication;
    }

    public Client addClient(String id) {
        Client client = new Client(id);
        spaceController.createSpaceDefinition(client.getSpace(), false);
        coreToAuthentication.registerClient(client);
        return client;
    }

    public Client deleteClient(String id) {
        Client client = new Client(id);
        spaceController.removeSpaceDefinition(client.getSpace().getName(), false);
        coreToAuthentication.unregisterClient(client.getName());
        return client;
    }

}

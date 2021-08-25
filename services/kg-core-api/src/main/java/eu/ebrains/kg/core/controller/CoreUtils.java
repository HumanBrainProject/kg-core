/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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

import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.exception.InvalidRequestException;
import eu.ebrains.kg.commons.model.internal.spaces.Space;
import eu.ebrains.kg.commons.model.SpaceName;
import org.springframework.stereotype.Component;

@Component
public class CoreUtils {

    private final AuthContext authContext;

    public CoreUtils(AuthContext authContext) {
        this.authContext = authContext;
    }

    public SpaceName getTargetSpace(boolean global){
        if(global){
            return InternalSpace.GLOBAL_SPEC;
        }
        else{
            final Space clientSpace = authContext.getClientSpace();
            if(clientSpace == null){
                throw new InvalidRequestException("Defining client specific properties without authenticating as a client is not possible.");
            }
            return clientSpace.getName();
        }
    }
}

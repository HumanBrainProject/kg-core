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

package eu.ebrains.kg.commons.permissions.model;

import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permission.Permission;

/**
 * An instance permission allows a user to execute a specific functionality for the given space
 */
public class SpacePermission extends Permission {

    private Space space;

    public SpacePermission(Functionality functionality, Space space){
        super(Level.SPACE, functionality);
        this.space = space;
    }



}

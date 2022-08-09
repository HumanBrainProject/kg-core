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

package eu.ebrains.kg.core.api.common;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.model.SpaceName;

public abstract class Types {

    protected static final String TAG = "types";
    protected static final String TAG_ADV = "types - advanced";
    protected static final String TAG_EXTRA = "xtra - types";

    private final AuthContext authContext;

    public Types(AuthContext authContext) {
        this.authContext = authContext;
    }

    protected String getResolvedSpaceName(String space){
        SpaceName spaceName = authContext.resolveSpaceName(space);
        return spaceName!=null ? spaceName.getName() : null;
    }

}

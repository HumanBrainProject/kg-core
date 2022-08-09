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
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.core.controller.CoreQueryController;

public abstract class Queries {

    protected static final String TAG = "queries";
    protected static final String TAG_ADV = "queries - advanced";
    protected static final String TAG_EXTRA = "xtra - queries";
    protected final CoreQueryController queryController;
    protected final AuthContext authContext;


    public Queries(CoreQueryController queryController, AuthContext authContext) {
        this.queryController = queryController;
        this.authContext = authContext;
    }

    protected void handleResponse(Paginated<NormalizedJsonLd> data){
        final SpaceName privateSpace = authContext.getUserWithRoles().getPrivateSpace();
        data.getData().forEach(d -> d.renameSpace(privateSpace, queryController.isInvited(d)));
    }

}

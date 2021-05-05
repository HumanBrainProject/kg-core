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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.commons.exception;

import eu.ebrains.kg.commons.jsonld.JsonLdId;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AmbiguousIdException extends AmbiguousException {

    public AmbiguousIdException(UUID lookupId, List<JsonLdId> idsFound) {
        super(createMessage(lookupId, idsFound));
    }

    public AmbiguousIdException(UUID lookupId, Throwable cause, List<JsonLdId> idsFound) {
        super(createMessage(lookupId, idsFound), cause);
    }

    private static final String createMessage(UUID lookupId, List<JsonLdId> ids) {
        return String.format("Ambiguous ids found for %s: %s", lookupId, String.join(", ", ids != null ? ids.stream().map(JsonLdId::getId).collect(Collectors.toSet()) : Collections.emptySet()));
    }
}

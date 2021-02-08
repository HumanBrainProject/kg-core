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

package eu.ebrains.kg.ids.api;
import eu.ebrains.kg.commons.exception.AmbiguousIdException;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/ids")
public class IdsAPIRest implements eu.ebrains.kg.commons.api.IdsAPI.Endpoint {

    private final IdsAPI idsAPI;

    public IdsAPIRest(IdsAPI idsAPI) {
        this.idsAPI = idsAPI;
    }

    @PostMapping("/{stage}")
    @Override
    public List<JsonLdId> createOrUpdateId(@RequestBody IdWithAlternatives idWithAlternatives, @PathVariable("stage") DataStage stage) {
        return idsAPI.createOrUpdateId(idWithAlternatives, stage);
    }

    @DeleteMapping("/{stage}/{id}")
    @Override
    public List<JsonLdId> deprecateId(@PathVariable("stage") DataStage stage, @PathVariable("id") UUID id, @RequestParam(value = "revert", required = false, defaultValue = "false") boolean revert) {
        return idsAPI.deprecateId(stage, id, revert);
    }

    @PostMapping(value = "/{stage}/resolved")
    @Override
    public List<JsonLdIdMapping> resolveId(@RequestBody List<IdWithAlternatives> idWithAlternatives, @PathVariable("stage") DataStage stage) throws AmbiguousIdException {
       return idsAPI.resolveId(idWithAlternatives, stage);
    }
}

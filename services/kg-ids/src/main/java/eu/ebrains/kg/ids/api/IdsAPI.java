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

package eu.ebrains.kg.ids.api;

import eu.ebrains.kg.commons.exception.AmbiguousIdException;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.ids.controller.IdRepository;
import eu.ebrains.kg.ids.model.PersistedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RestController
@RequestMapping("/internal/ids")
public class IdsAPI {

    private final IdRepository idRepository;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public IdsAPI(IdRepository idRepository) {
        this.idRepository = idRepository;
    }

    @PostMapping("/{stage}")
    public List<JsonLdId> createOrUpdateId(@RequestBody IdWithAlternatives idWithAlternatives, @PathVariable("stage") DataStage stage) {
        if (idWithAlternatives != null && idWithAlternatives.getId() != null) {
            logger.debug(String.format("Updating id %s%s", idWithAlternatives.getId(), idWithAlternatives.getAlternatives() != null ? "with alternatives " + String.join(", ", idWithAlternatives.getAlternatives()) : ""));
            PersistedId persistedId = new PersistedId();
            persistedId.setId(idWithAlternatives.getId());
            persistedId.setSpace(new Space(idWithAlternatives.getSpace()));
            persistedId.setAlternativeIds(idWithAlternatives.getAlternatives());
            return idRepository.upsert(stage, persistedId);
        }
        throw new IllegalArgumentException("Invalid payload");
    }


    @DeleteMapping("/{stage}/{id}")
    public ResponseEntity<List<JsonLdId>> deprecateId(@PathVariable("stage") DataStage stage, @PathVariable("id") UUID id, @RequestParam(value = "revert", required = false, defaultValue = "false") boolean revert) {
        PersistedId foundId = idRepository.getId(id, stage);
        if(foundId==null){
            return ResponseEntity.notFound().build();
        }
        foundId.setDeprecated(!revert);
        return ResponseEntity.ok(idRepository.upsert(stage, foundId));
    }


    @PostMapping(value = "/{stage}/resolved")
    public List<JsonLdIdMapping> resolveId(@RequestBody List<IdWithAlternatives> idWithAlternatives, @PathVariable("stage") DataStage stage) throws AmbiguousIdException {
        if (idWithAlternatives == null || idWithAlternatives.isEmpty()) {
            return Collections.emptyList();
        }
        return idRepository.resolveIds(stage, idWithAlternatives);
    }
}

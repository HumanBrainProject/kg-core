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

package eu.ebrains.kg.ids.api;
import eu.ebrains.kg.commons.api.Ids;
import eu.ebrains.kg.commons.exception.AmbiguousIdException;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.JsonLdIdMapping;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.IdWithAlternatives;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.ids.controller.IdRepository;
import eu.ebrains.kg.ids.model.PersistedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class IdsAPI implements Ids.Client {

    private final IdRepository idRepository;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public IdsAPI(IdRepository idRepository) {
        this.idRepository = idRepository;
    }

    @Override
    public List<JsonLdId> createOrUpdateId(IdWithAlternatives idWithAlternatives,  DataStage stage) {
        if (idWithAlternatives != null && idWithAlternatives.getId() != null) {
            logger.debug(String.format("Updating id %s%s", idWithAlternatives.getId(), idWithAlternatives.getAlternatives() != null ? "with alternatives " + String.join(", ", idWithAlternatives.getAlternatives()) : ""));
            PersistedId persistedId = new PersistedId();
            persistedId.setUUID(idWithAlternatives.getId());
            persistedId.setSpace(new SpaceName(idWithAlternatives.getSpace()));
            persistedId.setAlternativeIds(idWithAlternatives.getAlternatives());
            return idRepository.upsert(stage, persistedId);
        }
        throw new IllegalArgumentException("Invalid payload");
    }

    @Override
    public List<JsonLdId> deprecateId(DataStage stage, UUID id, boolean revert) {
        PersistedId foundId = idRepository.getId(id, stage);
        if(foundId==null){
            return null;
        }
        foundId.setDeprecated(!revert);
        return idRepository.upsert(stage, foundId);
    }

    @Override
    public List<JsonLdIdMapping> resolveId(List<IdWithAlternatives> idWithAlternatives, DataStage stage) throws AmbiguousIdException {
        if (idWithAlternatives == null || idWithAlternatives.isEmpty()) {
            return Collections.emptyList();
        }
        return idRepository.resolveIds(stage, idWithAlternatives);
    }
}

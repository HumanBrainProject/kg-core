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

package eu.ebrains.kg.query.api;


import com.google.gson.Gson;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.permissions.controller.PermissionSvc;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.query.serviceCall.QueriesToGraphDBAsync;
import eu.ebrains.kg.query.serviceCall.QueriesToGraphDBSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Component
@RestController
@RequestMapping("/internal/queries")
public class QueriesAPI {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Gson gson;

    private final QueriesToGraphDBSync graphDBSyncSvc;

    private final QueriesToGraphDBAsync graphDBAsyncSvc;

    private final PermissionSvc permissionSvc;

    public QueriesAPI(Gson gson, QueriesToGraphDBSync graphDBSyncSvc, QueriesToGraphDBAsync graphDBAsyncSvc, PermissionSvc permissionSvc) {
        this.gson = gson;
        this.graphDBSyncSvc = graphDBSyncSvc;
        this.graphDBAsyncSvc = graphDBAsyncSvc;
        this.permissionSvc = permissionSvc;
    }

    @PostMapping
    public List<NormalizedJsonLd> listResults(@RequestBody NormalizedJsonLd query, @RequestParam("stage") DataStage stage, @RequestParam(value = "synchronous", defaultValue = "false", required = false) boolean synchronous) {
        logger.info("Executing query");
        logger.debug(String.format("Payload: %s", gson.toJson(query)));
        KgQuery kgQuery = new KgQuery(query, stage);
        return synchronous ? graphDBSyncSvc.query(kgQuery) : graphDBAsyncSvc.query(kgQuery);
    }

}

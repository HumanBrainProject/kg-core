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

package eu.ebrains.kg.core.api;

import com.arangodb.ArangoDBException;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.model.Client;
import eu.ebrains.kg.core.controller.CoreClientController;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The spaces API provides information about existing KG spaces
 */
@RestController
@RequestMapping(Version.API + "/clients")
@Admin
public class Clients {

    private final CoreClientController coreClientController;

    public Clients(CoreClientController coreClientController) {
        this.coreClientController = coreClientController;
    }

    @Operation(summary = "Register a client in EBRAINS KG")
    @PutMapping("/{id}")
    public ResponseEntity<Client> addClient(@PathVariable("id") String id) {
        try {
            Client client = coreClientController.addClient(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(client);
        } catch (ArangoDBException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    @Operation(summary = "Remove a registered client")
    @DeleteMapping("/{id}")
    public ResponseEntity<Client> deleteClient(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(coreClientController.deleteClient(id));
        } catch (ArangoDBException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

}

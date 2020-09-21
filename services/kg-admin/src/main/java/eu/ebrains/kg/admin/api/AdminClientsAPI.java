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

package eu.ebrains.kg.admin.api;

import com.arangodb.ArangoDBException;
import eu.ebrains.kg.admin.controller.AdminSpaceController;
import eu.ebrains.kg.admin.serviceCall.AdminToAuthentication;
import eu.ebrains.kg.commons.model.Client;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The clients api manages the registration of clients (including the registration of the required configuration in the authentication service)
 */
@RestController
@RequestMapping("/internal/admin/clients")
public class AdminClientsAPI {

    private final AdminSpaceController spaceController;
    private final AdminToAuthentication authenticationSvc;

    public AdminClientsAPI(AdminSpaceController spaceController, AdminToAuthentication authenticationSvc) {
        this.spaceController = spaceController;
        this.authenticationSvc = authenticationSvc;
    }


    @Operation(summary = "Register a client in EBRAINS KG")
    @PutMapping("/{id}")
    public ResponseEntity<String> addClient(@PathVariable("id") String id) {
        try {
            Client client = new Client(id);
            spaceController.createSpace(client.getSpace(), false);
            authenticationSvc.registerClient(client);
            return ResponseEntity.ok(String.format("Successfully inserted the client with id %s", id));
        } catch (ArangoDBException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }


    @Operation(summary = "Remove a registered client")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteClient(@PathVariable("id") String id) {
        try {
            Client client = new Client(id);
            spaceController.removeSpace(client.getSpace());
            authenticationSvc.unregisterClient(id);
            return ResponseEntity.ok(String.format("Successfully deleted the client with id %s", id));
        } catch (ArangoDBException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

}

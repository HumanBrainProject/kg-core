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
import eu.ebrains.kg.admin.controller.AdminArangoRepository;
import eu.ebrains.kg.admin.controller.AdminSpaceController;
import eu.ebrains.kg.admin.serviceCall.AdminToAuthentication;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.model.Client;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The clients api manages the registration of clients (including the registration of the required configuration in the authentication service)
 */
@RestController
@RequestMapping("/internal/admin/clients")
public class AdminClientsAPI {

    private final AdminArangoRepository repository;
    private final AdminSpaceController spaceController;
    private final AdminToAuthentication authenticationSvc;

    public AdminClientsAPI(AdminArangoRepository repository, AdminSpaceController spaceController, AdminToAuthentication authenticationSvc) {
        this.repository = repository;
        this.spaceController = spaceController;
        this.authenticationSvc = authenticationSvc;
    }


    @ApiOperation("Register a client in EBRAINS KG")
    @PutMapping("/{id}")
    public ResponseEntity<String> addClient(@PathVariable("id") String id) {
        try {
            if(!id.matches("[0-9a-zA-Z\\-]*")){
                throw new IllegalArgumentException("Was trying to register a client with an invalid id. Only alphanumeric characters and dashes are allowed");
            }
            Client client = new Client(id);
            //TODO prevent the registration of clients with existing spaces...
            spaceController.createSpace(client.getSpace());
            authenticationSvc.registerClient(client);
            return ResponseEntity.ok(String.format("Successfully inserted the client with id %s", id));
        } catch (ArangoDBException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

    @ApiOperation("Receive information about a registered client")
    @GetMapping("/{id}")
    public ResponseEntity<Client> getClient(@PathVariable("id") String id) {
        try {
            ArangoCollectionReference arangoCollectionReference = new ArangoCollectionReference("clients", true);
            return ResponseEntity.ok(repository.getEntity(arangoCollectionReference, id, Client.class));
        } catch (ArangoDBException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

    }


    @ApiOperation("Remove a registered client")
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

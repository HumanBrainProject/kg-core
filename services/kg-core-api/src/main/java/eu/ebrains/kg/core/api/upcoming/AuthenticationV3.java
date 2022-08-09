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

package eu.ebrains.kg.core.api.upcoming;

import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.api.PrimaryStoreUsers;
import eu.ebrains.kg.commons.config.openApiGroups.AnonymousAccess;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.markers.ExposesConfigurationInformation;
import eu.ebrains.kg.commons.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * The user API allows to retrieve information how to access the authentication service and to retrieve information about the users.
 */
@RestController
@RequestMapping(Version.UPCOMING +"/auth")
public class AuthenticationV3 extends eu.ebrains.kg.core.api.common.Authentication {

    private final Authentication.Client authentication;
    private final PrimaryStoreUsers.Client primaryStoreUsers;

    public AuthenticationV3(Authentication.Client authentication, PrimaryStoreUsers.Client primaryStoreUsers) {
        this.authentication = authentication;
        this.primaryStoreUsers = primaryStoreUsers;
    }

    @Operation(summary = "Get the endpoint of the openid configuration")
    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExposesConfigurationInformation
    @Tag(name = TAG)
    @AnonymousAccess
    public Result<JsonLdDoc> getOpenIdConfigUrl() {
        JsonLdDoc ld = new JsonLdDoc();
        ld.addProperty("endpoint", authentication.openIdConfigUrl());
        return Result.ok(ld);
    }

}

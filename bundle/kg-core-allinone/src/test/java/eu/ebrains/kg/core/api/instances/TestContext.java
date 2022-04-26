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

package eu.ebrains.kg.core.api.instances;

import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.authentication.api.AuthenticationAPI;
import eu.ebrains.kg.authentication.controller.AuthenticationRepository;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.SetupLogic;
import eu.ebrains.kg.commons.permission.roles.Role;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import org.springframework.cache.CacheManager;

import java.util.*;
import java.util.stream.Collectors;


public class TestContext {
    private final List<ArangoDatabaseProxy> databaseProxies;
    private final AuthenticationAPI authentication;
    private final Collection<List<Role>> roleCollections;
    private final IdUtils idUtils;
    private final CacheManager cacheManager;
    private final List<SetupLogic> setupLogics;
    private final AuthenticationRepository authenticationRepository;

    public TestContext(IdUtils idUtils, List<ArangoDatabaseProxy> databaseProxies, AuthenticationAPI authentication, RoleMapping[] roleMappings, List<SetupLogic> setupLogics, AuthenticationRepository authenticationRepository, CacheManager cacheManager) {
        this(idUtils, databaseProxies, authentication,  Arrays.stream(roleMappings).filter(Objects::nonNull).map(r -> Collections.singletonList(r.toRole(null))).collect(Collectors.toSet()), setupLogics, authenticationRepository, cacheManager);
    }

    public TestContext(IdUtils idUtils, List<ArangoDatabaseProxy> databaseProxies, AuthenticationAPI authentication, Collection<List<Role>> roleCollections, List<SetupLogic> setupLogics, AuthenticationRepository authenticationRepository, CacheManager cacheManager) {
        this.databaseProxies = databaseProxies;
        this.authentication = authentication;
        this.roleCollections = roleCollections;
        this.idUtils = idUtils;
        this.cacheManager = cacheManager;
        this.setupLogics = setupLogics;
        this.authenticationRepository = authenticationRepository;
    }

    public List<ArangoDatabaseProxy> getDatabaseProxies() {
        return databaseProxies;
    }

    public AuthenticationAPI getAuthentication() {
        return authentication;
    }

    public Collection<List<Role>> getRoleCollections() {
        return roleCollections;
    }

    public IdUtils getIdUtils() {
        return idUtils;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public List<SetupLogic> getSetupLogics() {
        return setupLogics;
    }

    public AuthenticationRepository getAuthenticationRepository() {
        return authenticationRepository;
    }
}

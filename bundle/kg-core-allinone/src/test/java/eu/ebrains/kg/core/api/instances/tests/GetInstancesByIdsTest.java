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

package eu.ebrains.kg.core.api.instances.tests;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.authentication.api.AuthenticationAPI;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.model.ExposedStage;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GetInstancesByIdsTest extends AbstractInstanceTest {

    public Result<Map<String, Result<NormalizedJsonLd>>> response;
    public NormalizedJsonLd originalInstanceA;
    public NormalizedJsonLd originalInstanceB;
    public Map<UUID, NormalizedJsonLd> originalInstances;
    public List<UUID> ids;
    public IdUtils idUtils;

    public GetInstancesByIdsTest(ArangoDB.Builder database, AuthenticationAPI authenticationAPI, IdUtils idUtils, Instances instances, RoleMapping[] roles) {
        super(database, authenticationAPI,  instances, roles);
        this.idUtils = idUtils;
    }

    @Override
    protected void setup() {
        originalInstanceA = createInstanceWithServerDefinedUUID(0);
        originalInstanceB = createInstanceWithServerDefinedUUID(1);
        originalInstances = Stream.of(originalInstanceA, originalInstanceB).collect(Collectors.toMap(k -> idUtils.getUUID(k.id()), v -> v));
        ids = Arrays.asList(idUtils.getUUID(originalInstanceA.id()), idUtils.getUUID(originalInstanceB.id()));
    }

    @Override
    protected void run() {
        response = this.instances.getInstancesByIds(ids.stream().map(UUID::toString).collect(Collectors.toList()), ExposedStage.IN_PROGRESS, defaultResponseConfiguration);
    }
}

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

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.GraphDBTypes;
import eu.ebrains.kg.commons.api.PrimaryStoreEvents;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.SpaceName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class PropertiesTest {
    AuthContext authContext = Mockito.mock(AuthContext.class);
    Properties instance = new Properties(Mockito.mock(GraphDBTypes.Client.class), Mockito.mock(PrimaryStoreEvents.Client.class), authContext, Mockito.mock(IdUtils.class));

    @Before
    public void init() {
        Mockito.doReturn(new SpaceName("foobar")).when(authContext).getClientSpace();
    }

    @Test
    public void testPropertyDefinitionOk() {
        NormalizedJsonLd ld = new NormalizedJsonLd();
        ResponseEntity<Result<Void>> resultResponseEntity = instance.defineProperty(ld, false, "http://foobar");
        Assert.assertEquals(HttpStatus.OK, resultResponseEntity.getStatusCode());
    }

}
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

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.serviceCall.CoreToPrimaryStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class PropertiesTest {
    AuthContext authContext = Mockito.mock(AuthContext.class);
    Properties instance = new Properties(Mockito.mock(CoreToPrimaryStore.class), authContext);

    @Before
    public void init() {
        Mockito.doReturn(new Space("foobar")).when(authContext).getClientSpace();
    }

    @Test
    public void testPropertyDefinitionOk() {
        NormalizedJsonLd ld = new NormalizedJsonLd();
        ld.addTypes(EBRAINSVocabulary.META_PROPERTY_DEFINITION_TYPE);
        ld.addProperty(EBRAINSVocabulary.META_PROPERTY, new JsonLdId("http://foobar"));
        ResponseEntity<Result<Void>> resultResponseEntity = instance.defineProperty(ld, false);
        Assert.assertEquals(HttpStatus.OK, resultResponseEntity.getStatusCode());
    }

    @Test
    public void testPropertyDefinitionNotOk() {
        NormalizedJsonLd ld = new NormalizedJsonLd();
        ld.addTypes(EBRAINSVocabulary.META_PROPERTY_DEFINITION_TYPE);
        ResponseEntity<Result<Void>> resultResponseEntity = instance.defineProperty(ld, false);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, resultResponseEntity.getStatusCode());
    }
}
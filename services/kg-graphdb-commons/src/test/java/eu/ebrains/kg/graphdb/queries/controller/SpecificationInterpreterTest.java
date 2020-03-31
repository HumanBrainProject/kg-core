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

package eu.ebrains.kg.graphdb.queries.controller;

import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.graphdb.queries.model.spec.Specification;
import eu.ebrains.kg.test.TestObjectFactory;
import org.junit.Assert;
import org.junit.Test;

public class SpecificationInterpreterTest {

    @Test
    public void testReadSpecification(){
        //Given
        SpecificationInterpreter specificationInterpreter = new SpecificationInterpreter();
        NormalizedJsonLd query = TestObjectFactory.createJsonLd(TestObjectFactory.SIMPSONS, "normalizedQueries/simpsonsFamilyNames.json");

        //When
        Specification specification = specificationInterpreter.readSpecification(query, null);

        //Then
        Assert.assertEquals(2, specification.getProperties().size());
        Assert.assertEquals("https://thesimpsons.com/FamilyMember", specification.getRootType().getName());
    }


}
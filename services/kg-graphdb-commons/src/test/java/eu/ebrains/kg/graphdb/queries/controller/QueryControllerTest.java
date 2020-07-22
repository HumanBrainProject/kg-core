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

import com.netflix.discovery.EurekaClient;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.docker.SpringDockerComposeRunner;
import eu.ebrains.kg.graphdb.ingestion.controller.TodoListProcessor;
import eu.ebrains.kg.test.TestToIds;
import eu.ebrains.kg.test.TestObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd=changeMe", "eu.ebrains.kg.arango.port=9111"})
public class QueryControllerTest {

    @Autowired
    QueryController queryController;

    @Autowired
    EurekaClient discoveryClient;

    @Autowired
    TodoListProcessor todoListProcessor;

    @Autowired
    TestToIds idsSvcForTest;

    @Autowired
    IdUtils idUtils;

    private final Space space = TestObjectFactory.SIMPSONS;
    private final DataStage stage = DataStage.IN_PROGRESS;
    private final ArangoCollectionReference simpsons = ArangoCollectionReference.fromSpace(TestObjectFactory.SIMPSONS);
    private final UserWithRoles userWithRoles = Mockito.mock(UserWithRoles.class);

    @Before
    public void setup() {
        new SpringDockerComposeRunner(discoveryClient, Arrays.asList("arango"), "kg-ids").start();
    }

    @Test
    public void querySimple() {
        //Given
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd("simpsons/homer.json"), stage, null);

        //When
        KgQuery kgQuery = new KgQuery(TestObjectFactory.createJsonLd(space, "normalizedQueries/simpsonsFamilyNames.json"), stage);
        Paginated<NormalizedJsonLd> queryResult = queryController.query(userWithRoles, kgQuery, null, null);

        //Then
        Assert.assertEquals(1, queryResult.getSize());
        Assert.assertEquals(1, queryResult.getTotalResults());
        Assert.assertEquals(1, queryResult.getData().size());
        Assert.assertEquals("Homer", queryResult.getData().get(0).getAs("http://schema.org/givenName", String.class));
        Assert.assertEquals("Simpson", queryResult.getData().get(0).getAs("http://schema.org/familyName", String.class));
    }

    @Test
    public void querySimpleWithPagination() {
        //Given
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd("simpsons/homer.json"), stage, null);
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd("simpsons/maggie.json"), stage, null);
        KgQuery kgQuery = new KgQuery(TestObjectFactory.createJsonLd(space, "normalizedQueries/simpsonsFamilyNames.json"), stage);

        //When
        Paginated<NormalizedJsonLd> queryResultA = queryController.query(userWithRoles, kgQuery, new PaginationParam().setSize(1L), null);
        Paginated<NormalizedJsonLd> queryResultB = queryController.query(userWithRoles, kgQuery, new PaginationParam().setSize(1L).setFrom(1), null);
        Paginated<NormalizedJsonLd> queryResultC = queryController.query(userWithRoles, kgQuery, new PaginationParam().setSize(2L).setFrom(0), null);


        //Then
        Assert.assertEquals(1, queryResultA.getSize());
        Assert.assertEquals(1, queryResultA.getData().size());

        Assert.assertEquals(1, queryResultB.getSize());
        Assert.assertEquals(1, queryResultB.getData().size());

        Assert.assertNotEquals("The results of the queries are the same - this should not be the case, since we've paginated", queryResultA.getData().get(0), queryResultB.getData().get(0));

        Assert.assertEquals(2, queryResultC.getSize());
        Assert.assertEquals(2, queryResultC.getData().size());

        Assert.assertEquals(queryResultA.getData().get(0), queryResultC.getData().get(0));
        Assert.assertEquals(queryResultB.getData().get(0), queryResultC.getData().get(1));
    }

    @Test
    public void queryEmbedded() {
        //Given
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd("simpsons/homer.json"), stage, null);
        KgQuery kgQuery = new KgQuery(TestObjectFactory.createJsonLd(space, "normalizedQueries/homerWithEmbeddedTraversal.json"), stage);

        //When
        Paginated<NormalizedJsonLd> queryResult = queryController.query(userWithRoles, kgQuery, null, null);

        //Then
        Assert.assertEquals(1, queryResult.getSize());
        Assert.assertEquals(1, queryResult.getTotalResults());
        Assert.assertEquals(1, queryResult.getData().size());
        Assert.assertEquals("Homer", queryResult.getData().get(0).getAs("http://schema.org/address", String.class));
        Assert.assertEquals("Simpson", queryResult.getData().get(0).getAs("http://schema.org/streetAddress", String.class));
    }

    @Test
    public void queryMultiLevel() {
        //Given
        prepareHomerMargeAndMaggie();

        KgQuery kgQuery = new KgQuery(TestObjectFactory.createJsonLd("simpsons/normalizedQueries/multiLevelQuery.json"), stage);

        //When
        Paginated<NormalizedJsonLd> queryResult = queryController.query(userWithRoles, kgQuery, null, null);

        //Then
        Assert.assertEquals(3, queryResult.getSize());
    }

    private void prepareHomerMargeAndMaggie() {
        NormalizedJsonLd homer = TestObjectFactory.createJsonLd("simpsons/homer.json");
        ArangoDocumentReference homerDocumentId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), homer, stage, null);
        idsSvcForTest.upsert(stage, new IdWithAlternatives().setId(homerDocumentId.getDocumentId()).setSpace(TestObjectFactory.SIMPSONS.getName()).setAlternatives(homer.getIdentifiers()));
        NormalizedJsonLd maggie = TestObjectFactory.createJsonLd("simpsons/maggie.json");
        ArangoDocumentReference maggieDocumentId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), maggie, stage, null);
        idsSvcForTest.upsert(stage, new IdWithAlternatives().setId(maggieDocumentId.getDocumentId()).setSpace(TestObjectFactory.SIMPSONS.getName()).setAlternatives(maggie.getIdentifiers()));
        NormalizedJsonLd marge = TestObjectFactory.createJsonLd("simpsons/marge.json");
        ArangoDocumentReference margeDocumentId = todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), marge, stage, null);
        idsSvcForTest.upsert(stage, new IdWithAlternatives().setId(margeDocumentId.getDocumentId()).setSpace(TestObjectFactory.SIMPSONS.getName()).setAlternatives(marge.getIdentifiers()));
    }


    @Test
    public void queryMultiLevelNestedWithConcat() {
        //Given
        prepareHomerMargeAndMaggie();

        KgQuery kgQuery = new KgQuery(TestObjectFactory.createJsonLd("simpsons/normalizedQueries/multiLevelQueryNestedWithConcat.json"), stage);

        //When
        Paginated<NormalizedJsonLd> queryResult = queryController.query(userWithRoles, kgQuery, null, null);

        //Then
        Assert.assertEquals(3, queryResult.getSize());
        NormalizedJsonLd homer = queryResult.getData().get(0);
        Assert.assertEquals("Homer", homer.getAs("http://schema.org/givenName", String.class));
        List<NormalizedJsonLd> children = homer.getAsListOf("http://schema.org/children", NormalizedJsonLd.class);
        Assert.assertEquals(1, children.size());
        NormalizedJsonLd maggie = children.get(0);
        Assert.assertEquals("Maggie", maggie.getAs("http://schema.org/givenName", String.class));
        String parentConcatenation = maggie.getAs("http://schema.org/parents", String.class);
        Assert.assertEquals("Homer, Marge", parentConcatenation);
    }


    @Test
    public void queryDynamicFilter() {
        //Given
        prepareHomerMargeAndMaggie();

        KgQuery kgQuery = new KgQuery(TestObjectFactory.createJsonLd("simpsons/normalizedQueries/queryDynamicFilter.json"), stage);
        Map<String, String> filterValues = new HashMap<>();

        //When
        filterValues.put("givenName", "Marge");
        Paginated<NormalizedJsonLd> queryResultWithMargeFilter = queryController.query(userWithRoles, kgQuery, null, filterValues);
        filterValues.put("givenName", "Homer");
        Paginated<NormalizedJsonLd> queryResultWithHomerFilter = queryController.query(userWithRoles, kgQuery, null, filterValues);
        Paginated<NormalizedJsonLd> queryResultWithoutFilter = queryController.query(userWithRoles, kgQuery, null, null);

        //Then
        Assert.assertEquals(1, queryResultWithMargeFilter.getSize());
        Assert.assertEquals(1, queryResultWithHomerFilter.getSize());
        Assert.assertEquals(3, queryResultWithoutFilter.getSize());
        Assert.assertEquals("Marge", queryResultWithMargeFilter.getData().get(0).getAs("http://schema.org/givenName", String.class));
        Assert.assertEquals("Homer", queryResultWithHomerFilter.getData().get(0).getAs("http://schema.org/givenName", String.class));
    }

    @Test
    public void queryDynamicFilterWithFallback() {
        //Given
        prepareHomerMargeAndMaggie();

        KgQuery kgQuery = new KgQuery(TestObjectFactory.createJsonLd("simpsons/normalizedQueries/queryDynamicFilterWithFallback.json"), stage);
        Map<String, String> filterValues = new HashMap<>();

        //When
        filterValues.put("givenName", "Marge");
        Paginated<NormalizedJsonLd> queryResultWithMargeFilter = queryController.query(userWithRoles, kgQuery,  null, filterValues);
        filterValues.put("givenName", "Homer");
        Paginated<NormalizedJsonLd> queryResultWithHomerFilter = queryController.query(userWithRoles, kgQuery, null, filterValues);
        Paginated<NormalizedJsonLd> queryResultWithoutFilter = queryController.query(userWithRoles, kgQuery,  null, null);

        //Then
        Assert.assertEquals(1, queryResultWithMargeFilter.getSize());
        Assert.assertEquals(1, queryResultWithHomerFilter.getSize());
        Assert.assertEquals("The fallback-value is \"Homer\" - if nothing is defined, the results should therefore be filtered by this value", 1, queryResultWithoutFilter.getSize());
        Assert.assertEquals("Marge", queryResultWithMargeFilter.getData().get(0).getAs("http://schema.org/givenName", String.class));
        Assert.assertEquals("Homer", queryResultWithHomerFilter.getData().get(0).getAs("http://schema.org/givenName", String.class));
        Assert.assertEquals("Homer", queryResultWithoutFilter.getData().get(0).getAs("http://schema.org/givenName", String.class));

    }


    @Test
    public void queryMultiLevelNestedWithStaticAndNestedTypeFilter() {
        //Given
        prepareHomerMargeAndMaggie();

        KgQuery kgQuery = new KgQuery(TestObjectFactory.createJsonLd("simpsons/normalizedQueries/multiLevelQueryWithStaticAndNestedTypeFilter.json"), stage);

        //When
        Paginated<NormalizedJsonLd> queryResult = queryController.query(userWithRoles, kgQuery, null, null);

        //Then
        Assert.assertEquals("We only expect Homer to appear due to the static filter", 1, queryResult.getSize());
        NormalizedJsonLd homer = queryResult.getData().get(0);
        List<NormalizedJsonLd> children = homer.getAsListOf("http://schema.org/children", NormalizedJsonLd.class);
        Assert.assertEquals(1, children.size());
        NormalizedJsonLd maggie = children.get(0);
        Assert.assertEquals("Maggie", maggie.getAs("http://schema.org/givenName", String.class));
        List<String> parents = maggie.getAsListOf("http://schema.org/maleParents", String.class);
        Assert.assertEquals("We only expect a single parent since the query includes a type filter for Man only - so it's only Homer", 1, parents.size());
        Assert.assertEquals("Homer", parents.get(0));
    }

}
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
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.graphdb.queries.controller;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.api.Ids;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.graphdb.AbstractGraphTest;
import eu.ebrains.kg.graphdb.ingestion.controller.TodoListProcessor;
import eu.ebrains.kg.test.Simpsons;
import eu.ebrains.kg.test.TestCategories;
import eu.ebrains.kg.test.factory.UserFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag(TestCategories.API)
public class QueryControllerTest extends AbstractGraphTest {

    @Autowired
    QueryController queryController;

    @Autowired
    TodoListProcessor todoListProcessor;

    @Autowired
    Ids.Client ids;

    @Autowired
    IdUtils idUtils;

    @Autowired
    JsonAdapter jsonAdapter;

    private final SpaceName space = Simpsons.SPACE_NAME;
    private final DataStage stage = DataStage.IN_PROGRESS;
    private final ArangoCollectionReference simpsons = ArangoCollectionReference.fromSpace(Simpsons.SPACE_NAME);
    private final UserWithRoles userWithRoles = UserFactory.globalAdmin().getUserWithRoles();

    @Test
    public void querySimple() {
        //Given
       upsert(Simpsons.SPACE_NAME, jsonAdapter.fromJson(Simpsons.Characters.HOMER, NormalizedJsonLd.class), stage);

        //When
        KgQuery kgQuery = new KgQuery(jsonAdapter.fromJson(Simpsons.Queries.FAMILY_NAMES_NORMALIZED, NormalizedJsonLd.class), stage);
        Paginated<NormalizedJsonLd> queryResult = queryController.query(userWithRoles, kgQuery, null, null, false).getResult();

        //Then
        assertEquals(1, queryResult.getSize());
        assertEquals(Long.valueOf(1), queryResult.getTotalResults());
        assertEquals(1, queryResult.getData().size());
        assertEquals("Homer", queryResult.getData().get(0).getAs("http://schema.org/givenName", String.class));
        assertEquals("Simpson", queryResult.getData().get(0).getAs("http://schema.org/familyName", String.class));
    }

    @Test
    public void querySimpleWithPagination() {
        //Given
        upsert(Simpsons.SPACE_NAME, jsonAdapter.fromJson(Simpsons.Characters.HOMER, NormalizedJsonLd.class), stage);
        upsert(Simpsons.SPACE_NAME, jsonAdapter.fromJson(Simpsons.Characters.MAGGIE, NormalizedJsonLd.class), stage);
        KgQuery kgQuery = new KgQuery(jsonAdapter.fromJson(Simpsons.Queries.FAMILY_NAMES_NORMALIZED, NormalizedJsonLd.class), stage);

        //When
        Paginated<NormalizedJsonLd> queryResultA = queryController.query(userWithRoles, kgQuery, new PaginationParam().setSize(1L), null, false).getResult();
        Paginated<NormalizedJsonLd> queryResultB = queryController.query(userWithRoles, kgQuery, new PaginationParam().setSize(1L).setFrom(1), null, false).getResult();
        Paginated<NormalizedJsonLd> queryResultC = queryController.query(userWithRoles, kgQuery, new PaginationParam().setSize(2L).setFrom(0), null, false).getResult();


        //Then
        assertEquals(1, queryResultA.getSize());
        assertEquals(1, queryResultA.getData().size());

        assertEquals(1, queryResultB.getSize());
        assertEquals(1, queryResultB.getData().size());

        assertNotEquals(queryResultA.getData().get(0), queryResultB.getData().get(0), "The results of the queries are the same - this should not be the case, since we've paginated");

        assertEquals(2, queryResultC.getSize());
        assertEquals(2, queryResultC.getData().size());

        assertEquals(queryResultA.getData().get(0), queryResultC.getData().get(0));
        assertEquals(queryResultB.getData().get(0), queryResultC.getData().get(1));
    }

    @Test
    @Disabled("Fix me")
    public void queryEmbedded() {
        //Given
        upsert(Simpsons.SPACE_NAME, jsonAdapter.fromJson(Simpsons.Characters.HOMER, NormalizedJsonLd.class), stage);
        KgQuery kgQuery = new KgQuery(jsonAdapter.fromJson(Simpsons.Queries.HOMER_WITH_EMBEDDED_TRAVERSAL, NormalizedJsonLd.class), stage);

        //When
        Paginated<NormalizedJsonLd> queryResult = queryController.query(userWithRoles, kgQuery, null, null, false).getResult();

        //Then
        assertEquals(1, queryResult.getSize());
        assertEquals(Long.valueOf(1), queryResult.getTotalResults());
        assertEquals(1, queryResult.getData().size());
        assertEquals("Homer", queryResult.getData().get(0).getAs("http://schema.org/address", String.class));
        assertEquals("Simpson", queryResult.getData().get(0).getAs("http://schema.org/streetAddress", String.class));
    }

    @Test
    public void queryMultiLevel() {
        //Given
        prepareHomerMargeAndMaggie();

        KgQuery kgQuery = new KgQuery(jsonAdapter.fromJson(Simpsons.Queries.MULTI_LEVEL_QUERY, NormalizedJsonLd.class), stage);

        //When
        Paginated<NormalizedJsonLd> queryResult = queryController.query(userWithRoles, kgQuery, null, null, false).getResult();

        //Then
        assertEquals(3, queryResult.getSize());
    }

    private void prepareHomerMargeAndMaggie() {
        NormalizedJsonLd homer = jsonAdapter.fromJson(Simpsons.Characters.HOMER, NormalizedJsonLd.class);
        ArangoDocumentReference homerDocumentId = upsert(Simpsons.SPACE_NAME, homer, stage);
        ids.createOrUpdateId(new IdWithAlternatives().setId(homerDocumentId.getDocumentId()).setSpace(Simpsons.SPACE_NAME.getName()).setAlternatives(homer.identifiers()), stage);
        NormalizedJsonLd maggie = jsonAdapter.fromJson(Simpsons.Characters.MAGGIE, NormalizedJsonLd.class);
        ArangoDocumentReference maggieDocumentId = upsert(Simpsons.SPACE_NAME, maggie, stage);
        ids.createOrUpdateId(new IdWithAlternatives().setId(maggieDocumentId.getDocumentId()).setSpace(Simpsons.SPACE_NAME.getName()).setAlternatives(maggie.identifiers()), stage);
        NormalizedJsonLd marge = jsonAdapter.fromJson(Simpsons.Characters.MARGE, NormalizedJsonLd.class);
        ArangoDocumentReference margeDocumentId = upsert(Simpsons.SPACE_NAME, marge, stage);
        ids.createOrUpdateId(new IdWithAlternatives().setId(margeDocumentId.getDocumentId()).setSpace(Simpsons.SPACE_NAME.getName()).setAlternatives(marge.identifiers()), stage);
    }


    @Test
    @Disabled("Fix me")
    public void queryMultiLevelNested() {
        //Given
        prepareHomerMargeAndMaggie();

        KgQuery kgQuery = new KgQuery(jsonAdapter.fromJson(Simpsons.Queries.MULTI_LEVEL_QUERY_NESTED, NormalizedJsonLd.class), stage);

        //When
        Paginated<NormalizedJsonLd> queryResult = queryController.query(userWithRoles, kgQuery, null, null, false).getResult();

        //Then
        assertEquals(3, queryResult.getSize());
        NormalizedJsonLd homer = queryResult.getData().get(0);
        assertEquals("Homer", homer.getAs("http://schema.org/givenName", String.class));
        List<NormalizedJsonLd> children = homer.getAsListOf("http://schema.org/children", NormalizedJsonLd.class);
        assertEquals(1, children.size());
        NormalizedJsonLd maggie = children.get(0);
        assertEquals("Maggie", maggie.getAs("http://schema.org/givenName", String.class));
        String parentConcatenation = maggie.getAs("http://schema.org/parents", String.class);
        assertEquals("Homer, Marge", parentConcatenation);
    }


    @Test
    public void queryDynamicFilter() {
        //Given
        prepareHomerMargeAndMaggie();

        KgQuery kgQuery = new KgQuery(jsonAdapter.fromJson(Simpsons.Queries.QUERY_DYNAMIC_FILTER, NormalizedJsonLd.class), stage);
        Map<String, String> filterValues = new HashMap<>();

        //When
        filterValues.put("givenName", "Marge");
        Paginated<NormalizedJsonLd> queryResultWithMargeFilter = queryController.query(userWithRoles, kgQuery, null, filterValues, false).getResult();
        filterValues.put("givenName", "Homer");
        Paginated<NormalizedJsonLd> queryResultWithHomerFilter = queryController.query(userWithRoles, kgQuery, null, filterValues, false).getResult();
        Paginated<NormalizedJsonLd> queryResultWithoutFilter = queryController.query(userWithRoles, kgQuery, null, null, false).getResult();

        //Then
        assertEquals(1, queryResultWithMargeFilter.getSize());
        assertEquals(1, queryResultWithHomerFilter.getSize());
        assertEquals(3, queryResultWithoutFilter.getSize());
        assertEquals("Marge", queryResultWithMargeFilter.getData().get(0).getAs("http://schema.org/givenName", String.class));
        assertEquals("Homer", queryResultWithHomerFilter.getData().get(0).getAs("http://schema.org/givenName", String.class));
    }

    @Test
    public void queryDynamicFilterWithFallback() {
        //Given
        prepareHomerMargeAndMaggie();

        KgQuery kgQuery = new KgQuery(jsonAdapter.fromJson(Simpsons.Queries.QUERY_DYNAMIC_FILTER_WITH_FALLBACK, NormalizedJsonLd.class), stage);
        Map<String, String> filterValues = new HashMap<>();

        //When
        filterValues.put("givenName", "Marge");
        Paginated<NormalizedJsonLd> queryResultWithMargeFilter = queryController.query(userWithRoles, kgQuery,  null, filterValues, false).getResult();
        filterValues.put("givenName", "Homer");
        Paginated<NormalizedJsonLd> queryResultWithHomerFilter = queryController.query(userWithRoles, kgQuery, null, filterValues, false).getResult();
        Paginated<NormalizedJsonLd> queryResultWithoutFilter = queryController.query(userWithRoles, kgQuery,  null, null, false).getResult();

        //Then
        assertEquals(1, queryResultWithMargeFilter.getSize());
        assertEquals(1, queryResultWithHomerFilter.getSize());
        assertEquals(1, queryResultWithoutFilter.getSize(), "The fallback-value is \"Homer\" - if nothing is defined, the results should therefore be filtered by this value");
        assertEquals("Marge", queryResultWithMargeFilter.getData().get(0).getAs("http://schema.org/givenName", String.class));
        assertEquals("Homer", queryResultWithHomerFilter.getData().get(0).getAs("http://schema.org/givenName", String.class));
        assertEquals("Homer", queryResultWithoutFilter.getData().get(0).getAs("http://schema.org/givenName", String.class));

    }


    @Test
    public void queryMultiLevelNestedWithStaticAndNestedTypeFilter() {
        //Given
        prepareHomerMargeAndMaggie();

        KgQuery kgQuery = new KgQuery(jsonAdapter.fromJson(Simpsons.Queries.MULTI_LEVEL_QUERY_WITH_STATIC_AND_NESTED_TYPE_FILTER, NormalizedJsonLd.class), stage);

        //When
        Paginated<NormalizedJsonLd> queryResult = queryController.query(userWithRoles, kgQuery, null, null, false).getResult();

        //Then
        assertEquals( 1, queryResult.getSize(), "We only expect Homer to appear due to the static filter");
        NormalizedJsonLd homer = queryResult.getData().get(0);
        List<NormalizedJsonLd> children = homer.getAsListOf("http://schema.org/children", NormalizedJsonLd.class);
        assertEquals(1, children.size());
        NormalizedJsonLd maggie = children.get(0);
        assertEquals("Maggie", maggie.getAs("http://schema.org/givenName", String.class));
        List<String> parents = maggie.getAsListOf("http://schema.org/maleParents", String.class);
        assertEquals(1, parents.size(), "We only expect a single parent since the query includes a type filter for Man only - so it's only Homer");
        assertEquals("Homer", parents.get(0));
    }

}
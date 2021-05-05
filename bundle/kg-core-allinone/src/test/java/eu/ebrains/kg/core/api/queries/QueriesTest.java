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

package eu.ebrains.kg.core.api.queries;

import eu.ebrains.kg.commons.Tuple;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.permission.roles.Role;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.api.Queries;
import eu.ebrains.kg.core.api.queries.tests.TestSimpleQueryTest;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.APITest;
import eu.ebrains.kg.testutils.AbstractFunctionalityTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

@Category(APITest.class)
public class QueriesTest extends AbstractFunctionalityTest {

    @Autowired
    Instances instances;

    @Autowired
    Queries queries;

    private static List<Tuple<RoleMapping, RoleMapping>> roleCombinations(List<RoleMapping> roleMappingsA, List<RoleMapping> roleMappingsB) {
        List<Tuple<RoleMapping, RoleMapping>> result = new ArrayList<>();
        for (RoleMapping roleMappingA : roleMappingsA) {
            for (RoleMapping roleMappingB : roleMappingsB) {
                result.add(new Tuple<RoleMapping, RoleMapping>().setA(roleMappingA).setB(roleMappingB));
            }
        }
        return result;
    }

    private static List<Role> roles(RoleMapping roleMappingA, RoleMapping roleMappingB) {
        return Arrays.asList(roleMappingA != null ? roleMappingA.toRole(new SpaceName("a")) : null, roleMappingB != null ? roleMappingB.toRole(new SpaceName("b")) : null);
    }

    public final List<List<Role>> allRoles;
    public final List<List<Role>> rolesAllSpacesInProgress;
    public final List<List<Role>> rolesAllSpacesReleased;
    public final List<List<Role>> rolesSpaceAInProgressOnly;
    public final List<List<Role>> rolesSpaceAReleasedOnly;
    public final List<List<Role>> rolesSpaceAInProgressSpaceBReleasedOnly;
    public final List<List<Role>> rolesSpaceAReleasedSpaceBInProgress;
    public final List<List<Role>> noRights;

    public QueriesTest() {
        List<RoleMapping> noReadRights = Collections.singletonList(null);
        List<RoleMapping> readReleaseOnlyRights = Collections.singletonList(RoleMapping.CONSUMER);
        List<RoleMapping> inProgressRights = Arrays.asList(RoleMapping.getRemainingUserRoles(new RoleMapping[]{null, RoleMapping.CONSUMER}).clone());
        List<RoleMapping> allRoles = Stream.concat(Stream.concat(noReadRights.stream(), readReleaseOnlyRights.stream()), inProgressRights.stream()).collect(Collectors.toList());

        this.allRoles = roleCombinations(allRoles, allRoles).stream().map(r -> roles(r.getA(), r.getB())).collect(Collectors.toList());
        this.rolesAllSpacesInProgress = roleCombinations(inProgressRights, inProgressRights).stream().map(r -> roles(r.getA(), r.getB())).collect(Collectors.toList());
        this.rolesAllSpacesReleased = roleCombinations(readReleaseOnlyRights, readReleaseOnlyRights).stream().map(r -> roles(r.getA(), r.getB())).collect(Collectors.toList());
        this.rolesSpaceAInProgressOnly = roleCombinations(inProgressRights, noReadRights).stream().map(r -> roles(r.getA(), r.getB())).collect(Collectors.toList());
        this.rolesSpaceAReleasedOnly = roleCombinations(readReleaseOnlyRights, noReadRights).stream().map(r -> roles(r.getA(), r.getB())).collect(Collectors.toList());
        this.rolesSpaceAInProgressSpaceBReleasedOnly = roleCombinations(inProgressRights, readReleaseOnlyRights).stream().map(r -> roles(r.getA(), r.getB())).collect(Collectors.toList());
        this.rolesSpaceAReleasedSpaceBInProgress = roleCombinations(readReleaseOnlyRights, inProgressRights).stream().map(r -> roles(r.getA(), r.getB())).collect(Collectors.toList());
        this.noRights = roleCombinations(noReadRights, noReadRights).stream().map(r -> roles(r.getA(), r.getB())).collect(Collectors.toList());
    }

    private List<NormalizedJsonLd> mapToListOfNormalizedJsonLds(List<? extends Map<?, ?>> documents) {
        return documents.stream().map(d -> new NormalizedJsonLd((Map<String, ?>) d)).collect(Collectors.toList());
    }

    private void assertAllComplete(TestSimpleQueryTest test, List<NormalizedJsonLd> normalizedJsonLds) {
        NormalizedJsonLd resultForA = normalizedJsonLds.stream().filter(n -> n.getAs(test.nameOfRoot, String.class).equals(test.instanceA.getAs(SchemaOrgVocabulary.NAME, String.class))).findFirst().orElse(null);
        NormalizedJsonLd resultForARelated = normalizedJsonLds.stream().filter(n -> n.getAs(test.nameOfRoot, String.class).equals(test.instanceArelated.getAs(SchemaOrgVocabulary.NAME, String.class))).findFirst().orElse(null);
        NormalizedJsonLd resultForB = normalizedJsonLds.stream().filter(n -> n.getAs(test.nameOfRoot, String.class).equals(test.instanceB.getAs(SchemaOrgVocabulary.NAME, String.class))).findFirst().orElse(null);
        assertNotNull(resultForA);
        assertNotNull(resultForARelated);
        assertNotNull(resultForB);

        assertEquals(test.instanceArelated.getAs(SchemaOrgVocabulary.NAME, String.class), resultForA.getAs(test.nameOfARel, String.class));
        assertEquals(test.instanceB.getAs(SchemaOrgVocabulary.NAME, String.class), resultForA.getAs(test.nameOfB, String.class));

        assertNull(resultForARelated.getAs(test.nameOfARel, String.class));
        assertNull(resultForARelated.getAs(test.nameOfB, String.class));

        assertNull(resultForB.getAs(test.nameOfARel, String.class));
        assertNull(resultForB.getAs(test.nameOfB, String.class));
    }

    // *******************************
    // Happy cases (complete payloads)
    // *******************************
    @Test
    public void simpleTestQueryInProgressWithAllInProgressRights() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, rolesAllSpacesInProgress, queries, instances, ExposedStage.IN_PROGRESS, idUtils, false);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> results = test.assureValidPayload(test.response);
            assertAllComplete(test, mapToListOfNormalizedJsonLds(results));
        });
    }


    @Test
    public void simpleTestQueryReleasedWithReleasedRights() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, rolesAllSpacesReleased, queries, instances, ExposedStage.RELEASED, idUtils, true);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> results = test.assureValidPayload(test.response);
            assertAllComplete(test, mapToListOfNormalizedJsonLds(results));
        });
    }

    @Test
    public void simpleTestQueryReleasedWithSpaceAInProgressAndSpaceBReleasedRights() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, rolesSpaceAInProgressSpaceBReleasedOnly, queries, instances, ExposedStage.RELEASED, idUtils, true);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> results = test.assureValidPayload(test.response);
            assertAllComplete(test, mapToListOfNormalizedJsonLds(results));
        });
    }

    @Test
    public void simpleTestQueryReleasedWithSpaceAReleasedAndSpaceBInProgressRights() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, rolesSpaceAReleasedSpaceBInProgress, queries, instances, ExposedStage.RELEASED, idUtils, true);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> results = test.assureValidPayload(test.response);
            assertAllComplete(test, mapToListOfNormalizedJsonLds(results));
        });
    }


    @Test
    public void simpleTestQueryReleasedWithInProgressRights() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, rolesAllSpacesInProgress, queries, instances, ExposedStage.RELEASED, idUtils, true);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> results = test.assureValidPayload(test.response);
            assertAllComplete(test, mapToListOfNormalizedJsonLds(results));
            assertTrue("We are asking for the released instances with in progress rights -> this implies access rights for released instances too and we expect the payload to be complete", true);
        });
    }

    // *************************************
    // Partial results due to partial rights
    // *************************************
    //

    private void assertSpaceAOnly(TestSimpleQueryTest test, List<NormalizedJsonLd> normalizedJsonLds) {
        NormalizedJsonLd resultForA = normalizedJsonLds.stream().filter(n -> n.getAs(test.nameOfRoot, String.class).equals(test.instanceA.getAs(SchemaOrgVocabulary.NAME, String.class))).findFirst().orElse(null);
        NormalizedJsonLd resultForARelated = normalizedJsonLds.stream().filter(n -> n.getAs(test.nameOfRoot, String.class).equals(test.instanceArelated.getAs(SchemaOrgVocabulary.NAME, String.class))).findFirst().orElse(null);
        NormalizedJsonLd resultForB = normalizedJsonLds.stream().filter(n -> n.getAs(test.nameOfRoot, String.class).equals(test.instanceB.getAs(SchemaOrgVocabulary.NAME, String.class))).findFirst().orElse(null);
        assertNotNull(resultForA);
        assertNotNull(resultForARelated);
        assertNull("We expect there not to be any result for B because the user can not read it", resultForB);

        assertEquals(test.instanceArelated.getAs(SchemaOrgVocabulary.NAME, String.class), resultForA.getAs(test.nameOfARel, String.class));

        assertNull("Since the user doesn't have read rights in space B, we expect this not to be returned (although it would exist in the database)", resultForA.getAs(test.nameOfB, String.class));

        assertNull(resultForARelated.getAs(test.nameOfARel, String.class));
        assertNull(resultForARelated.getAs(test.nameOfB, String.class));
    }

    private void assertSpaceBOnly(TestSimpleQueryTest test, List<NormalizedJsonLd> normalizedJsonLds) {
        NormalizedJsonLd resultForA = normalizedJsonLds.stream().filter(n -> n.getAs(test.nameOfRoot, String.class).equals(test.instanceA.getAs(SchemaOrgVocabulary.NAME, String.class))).findFirst().orElse(null);
        NormalizedJsonLd resultForARelated = normalizedJsonLds.stream().filter(n -> n.getAs(test.nameOfRoot, String.class).equals(test.instanceArelated.getAs(SchemaOrgVocabulary.NAME, String.class))).findFirst().orElse(null);
        NormalizedJsonLd resultForB = normalizedJsonLds.stream().filter(n -> n.getAs(test.nameOfRoot, String.class).equals(test.instanceB.getAs(SchemaOrgVocabulary.NAME, String.class))).findFirst().orElse(null);
        assertNull(resultForA);
        assertNull(resultForARelated);
        assertNotNull(resultForB);

        assertNull(resultForB.getAs(test.nameOfARel, String.class));
        assertNull(resultForB.getAs(test.nameOfB, String.class));
    }


    @Test
    public void simpleTestQueryInProgressSpaceAInProgressOnly() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, rolesSpaceAInProgressOnly, queries, instances, ExposedStage.IN_PROGRESS, idUtils, false);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> results = test.assureValidPayload(test.response);
            assertSpaceAOnly(test, mapToListOfNormalizedJsonLds(results));
        });
    }

    @Test
    public void simpleTestQueryReleasedSpaceAReleasedOnly() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, rolesSpaceAReleasedOnly, queries, instances, ExposedStage.RELEASED, idUtils, true);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> results = test.assureValidPayload(test.response);
            assertSpaceAOnly(test, mapToListOfNormalizedJsonLds(results));
        });
    }

    @Test
    public void simpleTestQueryReleasedSpaceAInProgressOnly() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, rolesSpaceAInProgressOnly, queries, instances, ExposedStage.RELEASED, idUtils, true);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> results = test.assureValidPayload(test.response);
            assertSpaceAOnly(test, mapToListOfNormalizedJsonLds(results));
        });
    }

    @Test
    public void simpleTestQueryInProgressSpaceAInProgressSpaceBReleasedOnly() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, rolesSpaceAInProgressSpaceBReleasedOnly, queries, instances, ExposedStage.IN_PROGRESS, idUtils, false);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> results = test.assureValidPayload(test.response);
            assertSpaceAOnly(test, mapToListOfNormalizedJsonLds(results));
        });
    }

    @Test
    public void simpleTestQueryInProgressSpaceAReleasedSpaceBInProgress() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, rolesSpaceAReleasedSpaceBInProgress, queries, instances, ExposedStage.IN_PROGRESS, idUtils, false);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> results = test.assureValidPayload(test.response);
            assertSpaceBOnly(test, mapToListOfNormalizedJsonLds(results));
        });
    }


    // *******************************
    // Empty results due to no rights
    // *******************************
    @Test
    public void simpleTestQueryInProgressNoRights() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, noRights, queries, instances, ExposedStage.IN_PROGRESS, idUtils, false);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> normalizedJsonLds = test.assureValidPayload(test.response);
            assertTrue("We expect the response to be empty because we don't have any rights.", normalizedJsonLds.isEmpty());
        });
    }

    @Test
    public void simpleTestQueryInProgressSpaceAReleasedOnly() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, rolesSpaceAReleasedOnly, queries, instances, ExposedStage.IN_PROGRESS, idUtils, false);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> normalizedJsonLds = test.assureValidPayload(test.response);
            assertTrue("We expect the response to be empty because we only have released rights for space a.", normalizedJsonLds.isEmpty());
        });
    }

    @Test
    public void simpleTestQueryInProgressOnlyReleasedRights() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, rolesAllSpacesReleased, queries, instances, ExposedStage.IN_PROGRESS, idUtils, false);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> normalizedJsonLds = test.assureValidPayload(test.response);
            assertTrue("We expect the response to be empty because we query in progress but only have rights for released.", normalizedJsonLds.isEmpty());
        });
    }


    @Test
    public void simpleTestQueryReleasedNoRights() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, noRights, queries, instances, ExposedStage.RELEASED, idUtils, true);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> normalizedJsonLds = test.assureValidPayload(test.response);
            assertTrue("We were querying the released instances but have no rights. So we expect the response to be empty.", normalizedJsonLds.isEmpty());
        });
    }


    // *************************************
    // Empty results due to no data
    // *************************************

    @Test
    public void simpleTestQueryAllReleasedNoData() throws IOException {
        //Given
        TestSimpleQueryTest test = new TestSimpleQueryTest(database, authenticationAPI, allRoles, queries, instances, ExposedStage.RELEASED, idUtils, false);

        //When
        test.execute(() -> {
            //Then
            List<? extends Map<?, ?>> normalizedJsonLds = test.assureValidPayload(test.response);
            assertTrue("We were querying the released instances but no instances have been released. So we expect the response to be empty.", normalizedJsonLds.isEmpty());
        });
    }

}

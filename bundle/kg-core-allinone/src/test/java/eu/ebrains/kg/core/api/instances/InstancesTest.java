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

package eu.ebrains.kg.core.api.instances;

import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.GraphEntity;
import eu.ebrains.kg.commons.model.ReleaseStatus;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.ScopeElement;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.api.instances.tests.*;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.APITest;
import eu.ebrains.kg.testutils.AbstractFunctionalityTest;
import eu.ebrains.kg.testutils.TestDataFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

@Category(APITest.class)
public class InstancesTest extends AbstractFunctionalityTest {

    @Autowired
    Instances instances;

    private static final RoleMapping[] WRITE_ROLES = {RoleMapping.ADMIN, RoleMapping.OWNER, RoleMapping.EDITOR};
    private static final RoleMapping[] NON_WRITE_ROLES = RoleMapping.getRemainingUserRoles(WRITE_ROLES);

    private static final RoleMapping[] READ_IN_PROGRESS_ROLES = {RoleMapping.ADMIN, RoleMapping.OWNER, RoleMapping.EDITOR, RoleMapping.REVIEWER};
    private static final RoleMapping[] NON_READ_IN_PROGRESS_ROLES = RoleMapping.getRemainingUserRoles(READ_IN_PROGRESS_ROLES);

    private static final RoleMapping[] OWNER_ROLES = {RoleMapping.ADMIN, RoleMapping.OWNER};
    private static final RoleMapping[] NON_OWNER_ROLES = RoleMapping.getRemainingUserRoles(OWNER_ROLES);

    private static final RoleMapping[] NON_RELEASE_STATUS_ROLES = {null, RoleMapping.CONSUMER};
    private static final RoleMapping[] RELEASE_STATUS_ROLES = RoleMapping.getRemainingUserRoles(NON_RELEASE_STATUS_ROLES);

    @Test
    public void createInstanceOk() {
        //Given
        CreateInstanceTest test = new CreateInstanceTest(ctx(WRITE_ROLES), instances);

        //When
        test.execute(
                //Then
                () -> test.assureValidPayloadIncludingId(test.response)
        );
    }


    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    public void createInstanceForbidden() {
        //Given
        CreateInstanceTest test = new CreateInstanceTest(ctx(NON_WRITE_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    public void createInstanceWithSpecifiedUUIDOk() {
        //Given
        CreateInstanceWithSpecifiedUUIDTest test = new CreateInstanceWithSpecifiedUUIDTest(ctx(WRITE_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            NormalizedJsonLd document = test.assureValidPayloadIncludingId(test.response);
            assertEquals(test.clientSpecifiedUUID, idUtils.getUUID(document.id()));
        });
    }

    @Test
    public void createInstanceWithSpecifiedUUIDOkForbidden() {
        //Given
        CreateInstanceWithSpecifiedUUIDTest test = new CreateInstanceWithSpecifiedUUIDTest(ctx(NON_WRITE_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    public void createInstanceWithNewSpaceOk() {
        //Given
        CreateInstanceWithNewSpaceTest test = new CreateInstanceWithNewSpaceTest(ctx(OWNER_ROLES), instances);

        //When
        test.execute(
                //Then
                () -> test.assureValidPayloadIncludingId(test.response)
        );
    }

    /**
     * Only an administrator can create an instance in a not-yet existing space (since space creation rights are required)
     */
    @Test
    public void createInstanceWithNewSpaceForbidden() {
        //Given
        CreateInstanceWithNewSpaceTest test = new CreateInstanceWithNewSpaceTest(ctx(NON_OWNER_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    public void contributeToInstanceFullReplacementOk() {
        //Given
        ContributeToInstanceFullReplacementTest test = new ContributeToInstanceFullReplacementTest(ctx(WRITE_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            NormalizedJsonLd document = test.assureValidPayloadIncludingId(test.response);
            test.originalInstance.keySet().stream().filter(k -> k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX)).forEach(k -> {
                assertNotNull(document.get(k));
                assertNotEquals("The dynamic properties should change when doing the update", test.originalInstance.get(k), document.get(k));
            });
            test.originalInstance.keySet().stream().filter(k -> !k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX) && !k.startsWith(EBRAINSVocabulary.META)).forEach(k -> {
                assertNotNull(document.get(k));
                assertEquals("The non-dynamic properties should remain the same when doing a contribution", test.originalInstance.get(k), document.get(k));
            });
        });
    }

    @Test
    public void contributeToInstanceFullReplacementForbidden() {
        //Given
        ContributeToInstanceFullReplacementTest test = new ContributeToInstanceFullReplacementTest(ctx(NON_WRITE_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    public void contributeToInstancePartialReplacementOk() {
        //Given
        ContributeToInstancePartialReplacementTest test = new ContributeToInstancePartialReplacementTest(ctx(WRITE_ROLES), instances);

        //When
        test.execute(() -> {
            NormalizedJsonLd document = test.assureValidPayloadIncludingId(test.response);
            test.originalInstance.keySet().stream().filter(k -> k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX)).forEach(k -> {
                if (k.equals(TestDataFactory.DYNAMIC_FIELD_PREFIX + "0")) {
                    assertNotNull(document.get(k));
                    assertNotEquals("The dynamic property should change when doing the update", test.originalInstance.get(k), document.get(k));
                } else {
                    assertNotNull(document.get(k));
                    assertEquals("All other dynamic properties should remain the same after a partial update", test.originalInstance.get(k), document.get(k));
                }
            });
            test.originalInstance.keySet().stream().filter(k -> !k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX) && !k.startsWith(EBRAINSVocabulary.META)).forEach(k -> {
                assertNotNull(document.get(k));
                assertEquals("The non-dynamic properties should remain the same when doing a contribution", test.originalInstance.get(k), document.get(k));
            });
        });
    }

    @Test
    public void contributeToInstancePartialReplacementForbidden() {
        //Given
        ContributeToInstancePartialReplacementTest test = new ContributeToInstancePartialReplacementTest(ctx(NON_WRITE_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    public void getInstanceByIdOk() {
        //Given
        GetInstanceByIdTest test = new GetInstanceByIdTest(ctx(READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            NormalizedJsonLd normalizedJsonLd = test.assureValidPayloadIncludingId(test.response);
            assertEquals(test.originalInstance, normalizedJsonLd);
        });
    }

    @Ignore("this doesn't return a forbidden exception anymore but rather minimal metadata")
    @Test
    public void getInstanceByIdForbidden() {
        //Given
        GetInstanceByIdTest test = new GetInstanceByIdTest(ctx(NON_READ_IN_PROGRESS_ROLES), instances);

        //TODO update assertion
        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    public void getInstanceScopeSimpleOk() {
        //Given
        GetInstanceScopeSimpleTest test = new GetInstanceScopeSimpleTest(ctx(READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            ScopeElement scopeElement = test.assureValidPayload(test.response);
            assertNotNull(scopeElement.getId());
            assertNotNull(scopeElement.getTypes());
            assertNotNull(scopeElement.getSpace());
            assertNull(scopeElement.getChildren());
        });
    }


    @Ignore("this doesn't return a forbidden exception anymore but rather minimal metadata")
    @Test
    public void getInstanceScopeSimpleForbidden() {
        //Given
        GetInstanceScopeSimpleTest test = new GetInstanceScopeSimpleTest(ctx(NON_READ_IN_PROGRESS_ROLES), instances);
        //TODO update assertion
        test.execute(ForbiddenException.class);
    }

    @Test
    public void getInstanceNeighborsSimpleOk() {

        //Given
        GetInstanceNeighborsSimpleTest test = new GetInstanceNeighborsSimpleTest(ctx(READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(() -> {

            //Then
            GraphEntity neighborsGraph = test.assureValidPayload(test.response);
            assertNotNull(neighborsGraph.getId());
            assertNotNull(neighborsGraph.getTypes());
            assertNotNull(neighborsGraph.getSpace());
            assertTrue(neighborsGraph.getInbound().isEmpty());
            assertTrue(neighborsGraph.getOutbound().isEmpty());

        });

    }

    @Test
    public void getInstanceNeighborsSimpleForbidden() {

        //Given
        GetInstanceNeighborsSimpleTest test = new GetInstanceNeighborsSimpleTest(ctx(NON_READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    public void getInstancesOk() {
        //Given
        GetInstancesTest test = GetInstancesTest.getInstancesWithExistingType(ctx(READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(() -> {

            //Then
            assertEquals(0, test.response.getFrom());
            assertEquals(2, test.response.getSize());
            assertEquals(2, test.response.getTotal());
            List<NormalizedJsonLd> data = test.response.getData();
            assertNotNull(data);
            assertEquals(2, data.size());
            assertTrue(data.contains(test.originalInstanceA));
            assertTrue(data.contains(test.originalInstanceB));

        });
    }

    @Test
    public void getInstancesForbidden() {
        //Given
        GetInstancesTest test = GetInstancesTest.getInstancesWithExistingType(ctx(NON_READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(() -> {
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }

    @Test
    public void getInstancesWithNonExistingType() {
        //Given
        GetInstancesTest test = GetInstancesTest.getInstancesByType(ctx(READ_IN_PROGRESS_ROLES), instances, "http://aTypeThatDoesntExist");

        //When
        test.execute(() -> {
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }

    @Test
    public void getInstancesWithNonExistingTypeInExistingSpace() {
        //Given
        GetInstancesTest test = GetInstancesTest.getInstancesByTypeAndSpace(ctx(READ_IN_PROGRESS_ROLES), instances, "http://aTypeThatDoesntExist", "functionalityTest");

        //When
        test.execute(() -> {
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }

    @Test
    public void getInstancesWithNonExistingTypeInNonExistingSpace() {
        //Given
        GetInstancesTest test = GetInstancesTest.getInstancesByTypeAndSpace(ctx(READ_IN_PROGRESS_ROLES), instances, "http://aTypeThatDoesntExist", "aSpaceThatDoesntExist");

        //When
        test.execute(() -> {
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }


    @Test
    public void getInstancesByIdsOk() {
        //Given
        GetInstancesByIdsTest test = new GetInstancesByIdsTest(ctx(READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(() -> {

            //Then
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertEquals(2, test.response.getData().size());
            test.ids.forEach(id -> {
                assertTrue(test.response.getData().containsKey(id.toString()));
                NormalizedJsonLd original = test.originalInstances.get(id);
                Result<NormalizedJsonLd> result = test.response.getData().get(id.toString());
                assertNotNull(result.getData());
                assertEquals(result.getData(), original);
            });

        });

    }

    @Test
    @Ignore("this doesn't return a forbidden exception anymore but rather minimal metadata")
    public void getInstancesByIdsForbidden() {
        //Given
        GetInstancesByIdsTest test = new GetInstancesByIdsTest(ctx(NON_READ_IN_PROGRESS_ROLES), instances);

        //TODO update assertion

        //When
        test.execute(() -> {
            for (UUID id : test.ids) {
                Result.Error error = test.response.getData().get(id.toString()).getError();
                assertNotNull(error);
                assertEquals(403, error.getCode());
            }
        });
    }

    @Test
    public void getInstancesByIdentifiersOk() {
        //Given
        GetInstancesByIdentifiersTest test = new GetInstancesByIdentifiersTest(ctx(READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(() -> {

            //Then
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertEquals(1, test.response.getData().size());
            assertTrue(test.response.getData().containsKey(test.identifier));
            Result<NormalizedJsonLd> result = test.response.getData().get(test.identifier);
            assertNotNull(result.getData());
            assertEquals(result.getData(), test.updateResult.getBody().getData());

        });

    }


    @Test
    @Ignore("this doesn't return a forbidden exception anymore but rather minimal metadata")
    public void getInstancesByIdentifiersForbidden() {
        //Given
        GetInstancesByIdentifiersTest test = new GetInstancesByIdentifiersTest(ctx(NON_READ_IN_PROGRESS_ROLES), instances);

        //TODO update assertion
        //When
        test.execute(() -> {

            //Then
            Result.Error error = test.response.getData().get(test.identifier).getError();
            assertNotNull(error);
            assertEquals(403, error.getCode());
        });
    }


    @Test
    public void deleteInstanceOk() {
        //Given
        DeleteInstanceTest test = new DeleteInstanceTest(ctx(WRITE_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            ResponseEntity<Result<NormalizedJsonLd>> instanceById = test.fetchInstance();
            assertEquals("We expect a 404 to be returned from instanceById", HttpStatus.NOT_FOUND, instanceById.getStatusCode());
        });
    }

    @Test
    public void deleteInstanceForbidden() {
        //Given
        DeleteInstanceTest test = new DeleteInstanceTest(ctx(NON_WRITE_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    public void releaseInstanceOk() {
        //Given
        ReleaseInstanceTest test = new ReleaseInstanceTest(ctx(OWNER_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            ResponseEntity<Result<NormalizedJsonLd>> instanceById = test.fetchInstance();
            NormalizedJsonLd releasedInstance = test.assureValidPayloadIncludingId(instanceById);
            releasedInstance.removeAllFieldsFromNamespace(EBRAINSVocabulary.META);
            test.originalInstance.removeAllFieldsFromNamespace(EBRAINSVocabulary.META);
            assertEquals(releasedInstance, test.originalInstance);
        });
    }

    @Test
    public void releaseInstanceForbidden() {
        //Given
        ReleaseInstanceTest test = new ReleaseInstanceTest(ctx(NON_OWNER_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    public void unreleaseInstanceOk() {
        //Given
        UnreleaseInstanceTest test = new UnreleaseInstanceTest(ctx(OWNER_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            ResponseEntity<Result<NormalizedJsonLd>> releasedInstanceById = test.fetchInstance(ExposedStage.RELEASED);
            assertEquals("We expect a 404 to be returned from instanceById in released scope", HttpStatus.NOT_FOUND, releasedInstanceById.getStatusCode());

            //Just to be sure - we want to check if the instance is still available in the inferred space.
            ResponseEntity<Result<NormalizedJsonLd>> inferredInstanceById = test.fetchInstance(ExposedStage.IN_PROGRESS);
            test.assureValidPayloadIncludingId(inferredInstanceById);
        });
    }

    @Test
    public void unreleaseInstanceForbidden() {
        //Given
        UnreleaseInstanceTest test = new UnreleaseInstanceTest(ctx(NON_OWNER_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    public void getReleaseStatusOk() {
        //Given
        GetReleaseStatusTest test = new GetReleaseStatusTest(ctx(RELEASE_STATUS_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            assertEquals(ReleaseStatus.UNRELEASED, test.releaseStatusAfterCreation);
            assertEquals(ReleaseStatus.RELEASED, test.releaseStatusAfterReleasing);
            assertEquals(ReleaseStatus.HAS_CHANGED, test.releaseStatusAfterChange);
            assertEquals(ReleaseStatus.UNRELEASED, test.releaseStatusAfterUnreleasing);
        });
    }

    @Test
    public void getReleaseStatusForbidden() {
        //Given
        GetReleaseStatusTest test = new GetReleaseStatusTest(ctx(NON_RELEASE_STATUS_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    public void getReleaseStatusByIdsOk() {
        //Given
        GetReleaseStatusByIdsTest test = new GetReleaseStatusByIdsTest(ctx(RELEASE_STATUS_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertNotNull(test.response.getData().get(test.uuidA));
            assertNotNull(test.response.getData().get(test.uuidB));
            assertEquals(ReleaseStatus.RELEASED, test.response.getData().get(test.uuidA).getData());
            assertEquals(ReleaseStatus.UNRELEASED, test.response.getData().get(test.uuidB).getData());
        });
    }

    @Test
    public void getReleaseStatusByIdsForbidden() {
        //Given
        GetReleaseStatusByIdsTest test = new GetReleaseStatusByIdsTest(ctx(NON_RELEASE_STATUS_ROLES), instances);

        //When, then
        test.execute(ForbiddenException.class);
    }


    @Test
    public void getSuggestions() {
        //Given
        GetSuggestionsTest test = new GetSuggestionsTest(ctx(NON_RELEASE_STATUS_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            assertNotNull(test.response);
        });
    }
}

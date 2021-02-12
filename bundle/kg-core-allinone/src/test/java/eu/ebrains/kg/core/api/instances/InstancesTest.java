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
        CreateInstanceTest test = new CreateInstanceTest(database, authenticationAPI, instances, WRITE_ROLES);

        //When
        test.execute(
                //Then
                () -> test.assureValidPayloadIncludingId(test.response)
        );
    }


    @Test
    public void createInstanceForbidden() {
        //Given
        CreateInstanceTest test = new CreateInstanceTest(database, authenticationAPI, instances, NON_WRITE_ROLES);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    public void createInstanceWithSpecifiedUUIDOk() {
        //Given
        CreateInstanceWithSpecifiedUUIDTest test = new CreateInstanceWithSpecifiedUUIDTest(database, authenticationAPI, instances, WRITE_ROLES);

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
        CreateInstanceWithSpecifiedUUIDTest test = new CreateInstanceWithSpecifiedUUIDTest(database, authenticationAPI, instances, NON_WRITE_ROLES);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    public void createInstanceWithNewSpaceOk() {
        //Given
        CreateInstanceWithNewSpaceTest test = new CreateInstanceWithNewSpaceTest(database, authenticationAPI, instances, new RoleMapping[]{RoleMapping.ADMIN});

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
        CreateInstanceWithNewSpaceTest test = new CreateInstanceWithNewSpaceTest(database, authenticationAPI, instances, RoleMapping.getRemainingUserRoles(new RoleMapping[]{RoleMapping.ADMIN}));

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    public void contributeToInstanceFullReplacementOk() {
        //Given
        ContributeToInstanceFullReplacementTest test = new ContributeToInstanceFullReplacementTest(database, authenticationAPI, idUtils, instances, WRITE_ROLES);

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
        ContributeToInstanceFullReplacementTest test = new ContributeToInstanceFullReplacementTest(database, authenticationAPI, idUtils, instances, NON_WRITE_ROLES);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    public void contributeToInstancePartialReplacementOk() {
        //Given
        ContributeToInstancePartialReplacementTest test = new ContributeToInstancePartialReplacementTest(database, authenticationAPI, idUtils, instances, WRITE_ROLES);

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
        ContributeToInstancePartialReplacementTest test = new ContributeToInstancePartialReplacementTest(database, authenticationAPI, idUtils, instances, NON_WRITE_ROLES);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    public void getInstanceByIdOk() {
        //Given
        GetInstanceByIdTest test = new GetInstanceByIdTest(database, authenticationAPI, idUtils, instances, READ_IN_PROGRESS_ROLES);

        //When
        test.execute(() -> {
            //Then
            NormalizedJsonLd normalizedJsonLd = test.assureValidPayloadIncludingId(test.response);
            assertEquals(test.originalInstance, normalizedJsonLd);
        });
    }

    @Test
    public void getInstanceByIdForbidden() {
        //Given
        GetInstanceByIdTest test = new GetInstanceByIdTest(database, authenticationAPI, idUtils, instances, NON_READ_IN_PROGRESS_ROLES);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    public void getInstanceScopeSimpleOk() {
        //Given
        GetInstanceScopeSimpleTest test = new GetInstanceScopeSimpleTest(database, authenticationAPI, idUtils, instances, READ_IN_PROGRESS_ROLES);

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

    @Test
    public void getInstanceScopeSimpleForbidden() {
        //Given
        GetInstanceScopeSimpleTest test = new GetInstanceScopeSimpleTest(database, authenticationAPI, idUtils, instances, NON_READ_IN_PROGRESS_ROLES);

        test.execute(ForbiddenException.class);
    }

    @Test
    public void getInstanceNeighborsSimpleOk() {

        //Given
        GetInstanceNeighborsSimpleTest test = new GetInstanceNeighborsSimpleTest(database, authenticationAPI, idUtils, instances, READ_IN_PROGRESS_ROLES);

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
        GetInstanceNeighborsSimpleTest test = new GetInstanceNeighborsSimpleTest(database, authenticationAPI, idUtils, instances, NON_READ_IN_PROGRESS_ROLES);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    public void getInstancesOk() {
        //Given
        GetInstancesTest test = new GetInstancesTest(database, authenticationAPI, idUtils, instances, READ_IN_PROGRESS_ROLES);

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
        GetInstancesTest test = new GetInstancesTest(database, authenticationAPI, idUtils, instances, NON_READ_IN_PROGRESS_ROLES);

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
        GetInstancesByIdsTest test = new GetInstancesByIdsTest(database, authenticationAPI, idUtils, instances, READ_IN_PROGRESS_ROLES);

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
    public void getInstancesByIdsForbidden() {
        //Given
        GetInstancesByIdsTest test = new GetInstancesByIdsTest(database, authenticationAPI, idUtils, instances, NON_READ_IN_PROGRESS_ROLES);

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
        GetInstancesByIdentifiersTest test = new GetInstancesByIdentifiersTest(database, authenticationAPI, idUtils, instances, READ_IN_PROGRESS_ROLES);

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
    public void getInstancesByIdentifiersForbidden() {
        //Given
        GetInstancesByIdentifiersTest test = new GetInstancesByIdentifiersTest(database, authenticationAPI, idUtils, instances, NON_READ_IN_PROGRESS_ROLES);

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
        DeleteInstanceTest test = new DeleteInstanceTest(database, authenticationAPI, idUtils, instances, OWNER_ROLES);

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
        DeleteInstanceTest test = new DeleteInstanceTest(database, authenticationAPI, idUtils, instances, NON_OWNER_ROLES);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    public void releaseInstanceOk() {
        //Given
        ReleaseInstanceTest test = new ReleaseInstanceTest(database, authenticationAPI, idUtils, instances, OWNER_ROLES);

        //When
        test.execute(() -> {
            //Then
            ResponseEntity<Result<NormalizedJsonLd>> instanceById = test.fetchInstance();
            NormalizedJsonLd releasedInstance = test.assureValidPayloadIncludingId(instanceById);
            assertEquals(releasedInstance.removeAllFieldsFromNamespace(EBRAINSVocabulary.META), test.originalInstance.removeAllFieldsFromNamespace(EBRAINSVocabulary.META));
        });
    }

    @Test
    public void releaseInstanceForbidden() {
        //Given
        ReleaseInstanceTest test = new ReleaseInstanceTest(database, authenticationAPI, idUtils, instances, NON_OWNER_ROLES);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    public void unreleaseInstanceOk() {
        //Given
        UnreleaseInstanceTest test = new UnreleaseInstanceTest(database, authenticationAPI, idUtils, instances, OWNER_ROLES);

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
        UnreleaseInstanceTest test = new UnreleaseInstanceTest(database, authenticationAPI, idUtils, instances, NON_OWNER_ROLES);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    public void getReleaseStatusOk() {
        //Given
        GetReleaseStatusTest test = new GetReleaseStatusTest(database, authenticationAPI, idUtils, instances, RELEASE_STATUS_ROLES);

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
        GetReleaseStatusTest test = new GetReleaseStatusTest(database, authenticationAPI, idUtils, instances, NON_RELEASE_STATUS_ROLES);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    public void getReleaseStatusByIdsOk() {
        //Given
        GetReleaseStatusByIdsTest test = new GetReleaseStatusByIdsTest(database, authenticationAPI, idUtils, instances, RELEASE_STATUS_ROLES);

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
        GetReleaseStatusByIdsTest test = new GetReleaseStatusByIdsTest(database, authenticationAPI, idUtils, instances, NON_RELEASE_STATUS_ROLES);

        //When
        test.execute(() -> {
            //Then
            for (UUID uuid : Arrays.asList(test.uuidA, test.uuidB)) {
                Result.Error error = test.response.getData().get(uuid).getError();
                assertNotNull(error);
                assertEquals(403, error.getCode());
            }
        });
    }

}

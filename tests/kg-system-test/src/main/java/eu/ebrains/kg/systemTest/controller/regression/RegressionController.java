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

package eu.ebrains.kg.systemTest.controller.regression;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.Tuple;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.Type;
import eu.ebrains.kg.systemTest.controller.TestObjectFactory;
import eu.ebrains.kg.systemTest.serviceCall.SystemTestToCore;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class RegressionController {

    private final SystemTestToCore coreSvc;
    private final TestObjectFactory testObjectFactory;
    private final IdUtils idUtils;

    public RegressionController(SystemTestToCore coreSvc, TestObjectFactory testObjectFactory, IdUtils idUtils) {
        this.coreSvc = coreSvc;
        this.testObjectFactory = testObjectFactory;
        this.idUtils = idUtils;
    }
    private UUID sessionId;

    private long getNumberOfInstancesByTypeAndCheckStatsInstanceConsistency(NormalizedJsonLd payload){
        Type type = new Type(payload.types().stream().findFirst().orElseThrow());
        long fromInstances = coreSvc.getInstances(type, 0, 0, DataStage.IN_PROGRESS).getTotal();
        List<Tuple<Type, Long>> types = coreSvc.getTypes(DataStage.IN_PROGRESS);
        Long fromStats = types.stream().filter(t -> t.getA().equals(type)).findFirst().orElse(new Tuple<Type, Long>().setB(0L)).getB();
        if(fromInstances == fromStats){
            return fromInstances;
        }
        throw new IllegalStateException(String.format("Inconsistency between stats (%d) and instances (%d) detected when doing the count", fromStats, fromInstances));
    }


    public void merge(){
        sessionId = UUID.randomUUID();
        //Create two instances with distinct identifiers
        NormalizedJsonLd simplePayloadA = testObjectFactory.createSimplePayload("A-session-" + sessionId);
        NormalizedJsonLd simplePayloadB = testObjectFactory.createSimplePayload("B-session-" + sessionId);

        long initialNumberOfInstances = getNumberOfInstancesByTypeAndCheckStatsInstanceConsistency(simplePayloadA);
        Result<NormalizedJsonLd> instanceA = coreSvc.createInstance(simplePayloadA, testObjectFactory.getSpaceA(), null, null,true, false);
        Result<NormalizedJsonLd> instanceB = coreSvc.createInstance(simplePayloadB, testObjectFactory.getSpaceA(), null, null, true, false);
        long diffOfNumberOfInstances = getNumberOfInstancesByTypeAndCheckStatsInstanceConsistency(simplePayloadA)-initialNumberOfInstances;

        if(diffOfNumberOfInstances!=2){
            throw new IllegalStateException(String.format("Illegal number of different instances: %d - should be 2", diffOfNumberOfInstances));
        }

        //Add the id of instanceA to the instanceB as identifier to provoke a merge
        simplePayloadB.addIdentifiers(simplePayloadA.id().getId());
        Result<NormalizedJsonLd> newInstance = coreSvc.replaceContribution(simplePayloadB, idUtils.getUUID(instanceB.getData().id()), null, null, true, false);

        long finalDiffOfNumberOfInstances = getNumberOfInstancesByTypeAndCheckStatsInstanceConsistency(simplePayloadA)-initialNumberOfInstances;
        if(finalDiffOfNumberOfInstances!=1){
            throw new IllegalStateException(String.format("Illegal number of different instances: %d - Since the two instances should be merged now, we only should have a single additional instance", finalDiffOfNumberOfInstances));
        }
        Set<String> allIds = new HashSet<>();
        allIds.add(instanceA.getData().id().getId());
        allIds.add(instanceB.getData().id().getId());
        allIds.add(newInstance.getData().id().getId());
        if(allIds.size()!=3){
            throw new IllegalStateException(String.format("Expected 3 ids in total (two for the original ones and another one for the merge... instead: %s", String.join(", ", allIds)));
        }
        if(!newInstance.getData().identifiers().containsAll(allIds)){
            throw new IllegalStateException("The merged instance didn't contain all ids of the original instances.");
        }
    }


    public void dontReconcileSameIdentifiersInDifferentSpaces(){
        sessionId = UUID.randomUUID();
        //Create two instances with the same identifier

        NormalizedJsonLd simplePayloadA = testObjectFactory.createSimplePayload("A-session-" + sessionId);
        simplePayloadA.addIdentifiers("session-"+sessionId);
        NormalizedJsonLd simplePayloadA2 = testObjectFactory.createSimplePayload("A2-session-" + sessionId);
        simplePayloadA2.addIdentifiers("session-"+sessionId);
        NormalizedJsonLd simplePayloadB = testObjectFactory.createSimplePayload("B-session-" + sessionId);
        simplePayloadB.addIdentifiers("session-"+sessionId);

        long initialNumberOfInstances = getNumberOfInstancesByTypeAndCheckStatsInstanceConsistency(simplePayloadA);

        coreSvc.createInstance(simplePayloadA, testObjectFactory.getSpaceA(), null, null,true, false);
        coreSvc.createInstance(simplePayloadB, testObjectFactory.getSpaceB(), null, null, true, false);
        coreSvc.createInstance(simplePayloadA2, testObjectFactory.getSpaceA(), null, null, true, false);

        long diffOfNumberOfInstances = getNumberOfInstancesByTypeAndCheckStatsInstanceConsistency(simplePayloadA)-initialNumberOfInstances;
        if(diffOfNumberOfInstances!=2){
            throw new IllegalStateException("I was expecting 2 instances - one in space A, one in space B - although they share their identifier because only instances in the same space are reconciled.");
        }

    }
}

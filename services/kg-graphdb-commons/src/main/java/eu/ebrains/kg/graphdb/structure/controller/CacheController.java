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

package eu.ebrains.kg.graphdb.structure.controller;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.Triple;
import eu.ebrains.kg.commons.Tuple;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.internal.spaces.Space;
import eu.ebrains.kg.graphdb.ingestion.model.CacheEvictionPlan;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CacheController {

    private final StructureRepository structureRepository;
    private final MetaDataController metaDataController;
    private final AuthContext authContext;

    @PostConstruct
    public void setup(){
        metaDataController.initializeCache();
    }


    public CacheController(StructureRepository structureRepository, MetaDataController metaDataController, AuthContext authContext) {
        this.structureRepository = structureRepository;
        this.metaDataController = metaDataController;
        this.authContext = authContext;
    }

    private Set<String> getDeleteIds(Map<String, CacheEvictionPlan> plansBeforeTransaction, Map<String, CacheEvictionPlan> plansAfterTransaction){
        //A removal of an instance is characterized by an id appearing before the transaction but not afterwards anymore
        return plansBeforeTransaction.keySet().stream().filter(id -> !plansAfterTransaction.containsKey(id)).collect(Collectors.toSet());
    }

    private Set<String> getCreateIds(Map<String, CacheEvictionPlan> plansBeforeTransaction, Map<String, CacheEvictionPlan> plansAfterTransaction){
        //A creation of an instance is characterized by an id appearing after the transaction but not before
        return plansAfterTransaction.keySet().stream().filter(id -> !plansBeforeTransaction.containsKey(id)).collect(Collectors.toSet());
    }

    private Set<String> getUpdateIds(Map<String, CacheEvictionPlan> plansBeforeTransaction, Map<String, CacheEvictionPlan> plansAfterTransaction){
        //An update of an instance is characterized by an id appearing at both times, before and after the transaction
        return plansAfterTransaction.keySet().stream().filter(plansBeforeTransaction::containsKey).collect(Collectors.toSet());
    }


    private Set<SpaceName> findSpacesForCacheEviction(Map<String, CacheEvictionPlan> plansBeforeTransaction, Map<String, CacheEvictionPlan> plansAfterTransaction, Set<String> createIds, Set<String> deleteIds, Set<String> updateIds){
        //We evict all caches for delete operations if the original instance had types
        final Stream<SpaceName> fromDeleteOperations = deleteIds.stream().map(plansBeforeTransaction::get).filter(d -> !CollectionUtils.isEmpty(d.getType())).map(d -> new SpaceName(d.getSpace()));

        //We evict all caches for create operations if the new instance has types
        final Stream<SpaceName> fromCreateOperations = createIds.stream().map(plansAfterTransaction::get).filter(d -> !CollectionUtils.isEmpty(d.getType())).map(d -> new SpaceName(d.getSpace()));

        //For update operations we only want the ones which are having changing types
        final Stream<SpaceName> fromUpdateOperations = updateIds.stream().filter(d -> {
            final CacheEvictionPlan cacheEvictionPlanBefore = plansBeforeTransaction.get(d);
            final CacheEvictionPlan cacheEvictionPlanAfter = plansAfterTransaction.get(d);
            return !CollectionUtils.isEmpty(getChangedTypes(cacheEvictionPlanBefore, cacheEvictionPlanAfter));
        }).map(d -> SpaceName.fromString(plansBeforeTransaction.get(d).getSpace()));

        return Stream.concat(Stream.concat(fromDeleteOperations, fromCreateOperations), fromUpdateOperations).collect(Collectors.toSet());
    }

    private List<String> getChangedTypes(@NonNull CacheEvictionPlan cacheEvictionPlanBefore, @NonNull CacheEvictionPlan cacheEvictionPlanAfter) {
        boolean neverHadTypes = CollectionUtils.isEmpty(cacheEvictionPlanBefore.getType()) && CollectionUtils.isEmpty(cacheEvictionPlanAfter.getType());
        if(neverHadTypes){
            return Collections.emptyList();
        }
        boolean hasTypesNow = CollectionUtils.isEmpty(cacheEvictionPlanBefore.getType()) && !CollectionUtils.isEmpty(cacheEvictionPlanAfter.getType());
        if(hasTypesNow){
            return cacheEvictionPlanAfter.getType();
        }
        boolean hadTypesBefore = !CollectionUtils.isEmpty(cacheEvictionPlanBefore.getType()) && CollectionUtils.isEmpty(cacheEvictionPlanAfter.getType());
        if(hadTypesBefore){
            return cacheEvictionPlanBefore.getType();
        }
        final Stream<String> beforeDiff = cacheEvictionPlanBefore.getType().stream().filter(t -> !cacheEvictionPlanAfter.getType().contains(t));
        final Stream<String> afterDiff = cacheEvictionPlanAfter.getType().stream().filter(t -> !cacheEvictionPlanBefore.getType().contains(t));
        return Stream.concat(beforeDiff, afterDiff).collect(Collectors.toList());
    }

    private Set<Tuple<SpaceName, String>> findSpaceTypesForPropertyEviction(Map<String, CacheEvictionPlan> plansBeforeTransaction, Map<String, CacheEvictionPlan> plansAfterTransaction, Set<String> createIds, Set<String> deleteIds, Set<String> updateIds){
        //We evict all caches for delete operations
        Stream<Tuple<SpaceName, String>> fromDeleteOperations = getAllSpaceTypesForPropertyEviction(plansBeforeTransaction, deleteIds);

        //We evict all caches for create operations
        Stream<Tuple<SpaceName, String>> fromCreateOperations = getAllSpaceTypesForPropertyEviction(plansAfterTransaction, createIds);

        //For update operations we only want the ones which are having changing types and/or properties
        final Stream<Tuple<SpaceName, String>> fromUpdateOperations = updateIds.stream().map(d -> {
            final CacheEvictionPlan planBefore = plansBeforeTransaction.get(d);
            final CacheEvictionPlan planAfter = plansAfterTransaction.get(d);
            final SpaceName spaceName = new SpaceName(planBefore.getSpace());
            final List<String> changedTypes = getChangedTypes(planBefore, planAfter);
            Set<Tuple<SpaceName, String>> collector = new HashSet<>();
            if(changedTypes!=null){
                collector.addAll(changedTypes.stream().map(t -> new Tuple<SpaceName, String>().setA(spaceName).setB(t)).collect(Collectors.toSet()));
            }
            if((planBefore.getProperties() == null && planAfter.getProperties() != null) || !planBefore.getProperties().equals(planAfter.getProperties())){
                Set<String> allTypes = new HashSet<>();
                if(planAfter.getType()!=null){
                    allTypes.addAll(planAfter.getType());
                }
                if(planBefore.getType()!=null){
                    allTypes.addAll(planBefore.getType());
                }
                allTypes.forEach(t -> {
                    collector.add(new Tuple<SpaceName, String>().setA(spaceName).setB(t));
                });
            }
            return collector;
        }).flatMap(Collection::stream);
        return Stream.concat(Stream.concat(fromCreateOperations, fromDeleteOperations), fromUpdateOperations).collect(Collectors.toSet());
    }


    private Set<Triple<SpaceName, String, String>> findSpaceTypePropertiesForTargetTypeEviction(Map<String, CacheEvictionPlan> plansBeforeTransaction, Map<String, CacheEvictionPlan> plansAfterTransaction, Set<String> createIds, Set<String> deleteIds, Set<String> updateIds, List<String> allRelevantEdges){
        //We evict all caches for delete operations
        Stream<Triple<SpaceName, String, String>> fromDeleteOperations = getAllSpaceTypePropertiesForTargetTypeEviction(plansBeforeTransaction, deleteIds, allRelevantEdges);

        //We evict all caches for create operations
        Stream<Triple<SpaceName, String, String>> fromCreateOperations = getAllSpaceTypePropertiesForTargetTypeEviction(plansAfterTransaction, createIds, allRelevantEdges);

        //For update operations we only want the relevant properties - no matter if it was before or after the transaction
        //TODO this could be optimized further -> we only would need to invalidate the cache if the values of the relevant edge properties has changed.
        // This would mean that we need to load more information into the cacheEvictionPlan though which might be a bottleneck itself
        Stream<Triple<SpaceName, String, String>> fromUpdateOperationsBeforeTransactions = getAllSpaceTypePropertiesForTargetTypeEviction(plansBeforeTransaction, updateIds, allRelevantEdges);
        Stream<Triple<SpaceName, String, String>> fromUpdateOperationsAfterTransactions = getAllSpaceTypePropertiesForTargetTypeEviction(plansAfterTransaction, updateIds, allRelevantEdges);
        return Stream.concat(Stream.concat(Stream.concat(fromCreateOperations, fromDeleteOperations), fromUpdateOperationsBeforeTransactions), fromUpdateOperationsAfterTransactions).collect(Collectors.toSet());
    }



    private Stream<Tuple<SpaceName, String>> getAllSpaceTypesForPropertyEviction(Map<String, CacheEvictionPlan> plans, Set<String> ids) {
       return ids.stream().map(p -> {
            final CacheEvictionPlan plan = plans.get(p);
            final SpaceName spaceName = new SpaceName(plan.getSpace());
            return plan.getType().stream().map(t -> new Tuple<SpaceName, String>().setA(spaceName).setB(t)).collect(Collectors.toSet());
        }).flatMap(Collection::stream);
    }

    private Stream<Triple<SpaceName, String, String>> getAllSpaceTypePropertiesForTargetTypeEviction(Map<String, CacheEvictionPlan> plans, Set<String> ids, List<String> allRelevantEdges) {
        return ids.stream().map(p -> {
            final CacheEvictionPlan plan = plans.get(p);
            final SpaceName spaceName = new SpaceName(plan.getSpace());
            return plan.getProperties().stream().filter(property -> allRelevantEdges.contains(new ArangoCollectionReference(property, true).getCollectionName()))
                    .map(property -> plan.getType().stream().map(t -> new Triple<SpaceName, String, String>().setA(spaceName).setB(t).setC(property)).collect(Collectors.toSet())
            ).flatMap(Collection::stream).collect(Collectors.toSet());
            }).flatMap(Collection::stream);
    }

    private boolean hasCreatedOrRemovedSpaces(DataStage stage, Map<String, CacheEvictionPlan> plansBeforeTransaction, Map<String, CacheEvictionPlan> plansAfterTransaction, Set<String> createIds, Set<String> deleteIds){
        final List<Space> spaces = this.metaDataController.getSpaces(stage, authContext.getUserWithRoles());
        final Set<String> reflectedSpaceNames = spaces.stream().filter(Space::isReflected).collect(Collectors.toSet()).stream().map(s -> s.getName().getName()).collect(Collectors.toSet());
        final boolean hasReflectedSpaceOnlyInDelete = deleteIds.stream().map(c -> plansBeforeTransaction.get(c).getSpace()).anyMatch(reflectedSpaceNames::contains);
        if(hasReflectedSpaceOnlyInDelete){
            //We can't tell with the given information if the space is gone after the deletion, but we can restrict it to the spaces being reflected only
            return true;
        }
        final Set<String> existingSpaces = spaces.stream().map(s -> s.getName().getName()).collect(Collectors.toSet());
        return createIds.stream().map(c -> plansAfterTransaction.get(c).getSpace()).filter(c -> !InternalSpace.INTERNAL_SPACENAMES.contains(c)).anyMatch(c -> !existingSpaces.contains(c));
    }


    public void evictCacheByPlan(DataStage stage, List<CacheEvictionPlan> plansBeforeTransaction, List<CacheEvictionPlan> plansAfterTransaction) {
        final Map<String, CacheEvictionPlan> beforeTransactionById = plansBeforeTransaction.stream().collect(Collectors.toMap(CacheEvictionPlan::getId, v -> v));
        final Map<String, CacheEvictionPlan> afterTransactionById = plansAfterTransaction.stream().collect(Collectors.toMap(CacheEvictionPlan::getId, v -> v));


        final Set<String> createIds = getCreateIds(beforeTransactionById, afterTransactionById);
        final Set<String> deleteIds = getDeleteIds(beforeTransactionById, afterTransactionById);
        final Set<String> updateIds = getUpdateIds(beforeTransactionById, afterTransactionById);

        if(hasCreatedOrRemovedSpaces(stage, beforeTransactionById, afterTransactionById, createIds, deleteIds)){
            structureRepository.evictReflectedSpacesCache(stage);
        }

        final Set<SpaceName> spaceTypesForCacheEviction = findSpacesForCacheEviction(beforeTransactionById, afterTransactionById, createIds, deleteIds, updateIds);
        spaceTypesForCacheEviction.forEach(s -> structureRepository.evictTypesInSpaceCache(stage, s));

        final Set<Tuple<SpaceName, String>> spaceTypesForPropertyEviction = findSpaceTypesForPropertyEviction(beforeTransactionById, afterTransactionById, createIds, deleteIds, updateIds);
        spaceTypesForPropertyEviction.forEach(t -> structureRepository.evictPropertiesOfTypeInSpaceCache(stage, t.getA(), t.getB()));

        final List<String> allRelevantEdges = structureRepository.getAllRelevantEdges(stage);
        final Set<Triple<SpaceName, String, String>> spaceTypePropertiesForTargetTypeEviction = findSpaceTypePropertiesForTargetTypeEviction(beforeTransactionById, afterTransactionById, createIds, deleteIds, updateIds, allRelevantEdges);
        spaceTypePropertiesForTargetTypeEviction.forEach(p -> structureRepository.evictTargetTypesCache(stage, p.getA(), p.getB(), p.getC()));
    }
}

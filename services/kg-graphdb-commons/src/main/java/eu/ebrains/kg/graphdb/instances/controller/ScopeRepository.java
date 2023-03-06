/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

package eu.ebrains.kg.graphdb.instances.controller;

import eu.ebrains.kg.arango.commons.aqlbuilder.ArangoVocabulary;
import eu.ebrains.kg.commons.*;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.markers.*;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.model.internal.spaces.Space;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.queries.controller.QueryController;
import eu.ebrains.kg.graphdb.structure.controller.StructureRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ScopeRepository {

    private final InstancesRepository instances;

    private final QueriesRepository queries;

    private final AuthContext authContext;

    private final QueryController queryController;

    private final StructureRepository structureRepository;

    private final IdUtils idUtils;

    public ScopeRepository(InstancesRepository instances, QueriesRepository queries, AuthContext authContext, QueryController queryController, StructureRepository structureRepository, IdUtils idUtils) {
        this.instances = instances;
        this.queries = queries;
        this.authContext = authContext;
        this.queryController = queryController;
        this.structureRepository = structureRepository;
        this.idUtils = idUtils;
    }

    @ExposesMinimalData
    //FIXME reduce to minimal data permission
    public ScopeElement getScopeForInstance(SpaceName space, UUID id, DataStage stage, boolean applyRestrictions) {
        //get instance
        NormalizedJsonLd instance = instances.getInstance(stage, space, id, false, false, false, false, null);
        //get scope relevant queries
        //TODO as a performance optimization, we could try to apply the restrictions already to the queries instead of excluding the instances in a post processing step.
        Stream<NormalizedJsonLd> typeQueries = instance.types().stream().map(type -> queries.getQueriesByRootType(stage, null, null, false, false, type).getData()).flatMap(Collection::stream);
        Set<String> relevantSpaces = structureRepository.getSpaceSpecifications().stream().filter(Space::isScopeRelevant).map(s -> s.getName().getName()).collect(Collectors.toSet());
        List<NormalizedJsonLd> results = typeQueries.filter(q -> relevantSpaces.contains(q.getAs(EBRAINSVocabulary.META_SPACE, String.class))).map(q -> {
            QueryResult queryResult = queryController.query(authContext.getUserWithRoles(),
                    new KgQuery(q, stage).setIdRestrictions(
                            Collections.singletonList(id)), null, null, true);
            return queryResult != null && queryResult.getResult() != null ? queryResult.getResult().getData() : null;
        }).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());
        return translateResultToScope(results, instance, applyRestrictions);
    }

    private List<ScopeElement> handleSubElement(NormalizedJsonLd data, Map<String, Set<ScopeElement>> typeToUUID, boolean applyRestrictions, NormalizedJsonLd root) {
        Boolean embedded = data.getAs("embedded", Boolean.class);
        if (embedded != null && embedded) {
            return null;
        }
        List<String> type = data.getAsListOf("type", String.class);
        boolean isRoot = data.equals(root);
        boolean isNotRootButSameType = !isRoot && root.getAsListOf("type", String.class).stream().anyMatch(type::contains);

        //If the subelement has the same type as root, we stop. Also, if this is the root instance, we don't apply restrictions.
        if (isNotRootButSameType) {
            return null;
        }

        boolean applyRestrictionsForRoot = false;
        boolean skipInstanceByRestriction = false;
        if (applyRestrictions && type.stream().filter(Objects::nonNull).anyMatch(t -> {
            final DynamicJson typeSpecification = structureRepository.getTypeSpecification(t);
            return typeSpecification != null ? typeSpecification.getAs(EBRAINSVocabulary.META_CAN_BE_EXCLUDED_FROM_SCOPE, Boolean.class, Boolean.FALSE) : Boolean.FALSE;
        })) {
            if (isRoot) {
                applyRestrictionsForRoot = true;
            } else {
                skipInstanceByRestriction = true;
            }
        }

        String id = data.getAs("id", String.class);
        UUID uuid = idUtils.getUUID(new JsonLdId(id));
        List<ScopeElement> children;
        if (!applyRestrictionsForRoot) {
            children = data.keySet().stream().filter(k -> k.startsWith("dependency_")).map(k ->
                    data.getAsListOf(k, NormalizedJsonLd.class).stream().map(d -> handleSubElement(d, typeToUUID, applyRestrictions, root)).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList())
            ).flatMap(Collection::stream).distinct().collect(Collectors.toList());
        } else {
            //If the root instance is of a restricted type, we don't add any children but only return the individual instance
            children = null;
        }
        if (skipInstanceByRestriction) {
            //We skip the instance - so we're only aiming for the children
            return children;
        } else {
            ScopeElement element = new ScopeElement(uuid, type, children == null || children.isEmpty() ? null : children, data.getAs("internalId", String.class), data.getAs("space", String.class), data.getAs("label", String.class));
            type.forEach(t -> typeToUUID.computeIfAbsent(t, x -> new HashSet<>()).add(element));
            return Collections.singletonList(element);
        }
    }

    private List<ScopeElement> mergeInstancesOnSameLevel(List<ScopeElement> element) {
        if (element != null && !element.isEmpty()) {
            Map<UUID, List<ScopeElement>> groupedById = element.stream().collect(Collectors.groupingBy(ScopeElement::getId));
            List<ScopeElement> result = new ArrayList<>();
            groupedById.values().forEach(c -> {
                if (!c.isEmpty()) {
                    ScopeElement current;
                    if (c.size() == 1) {
                        current = c.get(0);
                    } else {
                        ScopeElement merged = new ScopeElement();
                        c.forEach(merged::merge);
                        current = merged;
                    }
                    if (current != null) {
                        result.add(current);
                        current.setChildren(mergeInstancesOnSameLevel(current.getChildren()));
                    }
                }
            });
            return result;
        }
        return null;
    }


    private ScopeElement translateResultToScope(List<NormalizedJsonLd> data, NormalizedJsonLd instance, boolean applyRestrictions) {
        final Map<String, Set<ScopeElement>> typeToUUID = new HashMap<>();
        List<ScopeElement> elements;
        if (data == null || data.isEmpty()) {
            elements = Collections.singletonList(new ScopeElement(idUtils.getUUID(instance.id()), instance.types(), null, instance.getAs(ArangoVocabulary.ID, String.class), instance.getAs(EBRAINSVocabulary.META_SPACE, String.class), instance.getAs(IndexedJsonLdDoc.LABEL, String.class)));
        } else {
            elements = data.stream().map(d -> handleSubElement(d, typeToUUID, applyRestrictions, d)).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());
        }
        for (ScopeElement el : elements) {
            instance.types().forEach(t -> typeToUUID.computeIfAbsent(t, x -> new HashSet<>()).add(el));
        }
        final List<ScopeElement> scopeElements = mergeInstancesOnSameLevel(elements);
        return scopeElements != null && scopeElements.size() > 0 ? scopeElements.get(0) : null;
    }

}

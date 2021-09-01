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
import eu.ebrains.kg.commons.Tuple;
import eu.ebrains.kg.commons.jsonld.DynamicJson;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.Type;
import eu.ebrains.kg.commons.model.external.types.*;
import eu.ebrains.kg.commons.model.internal.spaces.Space;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.PermissionsController;
import eu.ebrains.kg.graphdb.instances.api.GraphDBInstancesAPI;
import eu.ebrains.kg.graphdb.structure.model.PropertyOfTypeInSpaceReflection;
import eu.ebrains.kg.graphdb.structure.model.TargetTypeReflection;
import eu.ebrains.kg.graphdb.structure.model.TypeWithInstanceCountReflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MetaDataController {

    private final StructureRepository structureRepository;
    private final PermissionsController permissionsController;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public MetaDataController(StructureRepository structureRepository, PermissionsController permissionsController) {
        this.structureRepository = structureRepository;
        this.permissionsController = permissionsController;
    }

    @Async
    public void initializeCache() {
        logger.info("Initial cache population");
        readMetaDataStructure(DataStage.IN_PROGRESS, null, null, true, true, UserWithRoles.INTERNAL_ADMIN, null, null, null);
        readMetaDataStructure(DataStage.RELEASED, null, null, true, true, UserWithRoles.INTERNAL_ADMIN, null, null, null);

    }


    private void readMetaDataStructureForInvitations(DataStage stage, List<String> typeRestriction, boolean withIncomingLinks, boolean withProperties, Map<String, TypeInformation> typeInformations, Map<String, List<SpaceTypeInformation>> spaceTypeInformationLookup, List<String> allRelevantEdges, SpaceName clientSpace, List<UUID> invitations) {
        //TODO to be implemented

    }


    public List<TypeInformation> readMetaDataStructure(DataStage stage, String spaceRestriction, List<String> typeRestriction, boolean withProperties, boolean withIncomingLinks, UserWithRoles userWithRoles, SpaceName clientSpace, SpaceName privateUserSpace, List<UUID> invitations) {
        Date start = new Date();
        Map<String, TypeInformation> typeInformation = new HashMap<>();
        Map<String, List<SpaceTypeInformation>> spaceTypeInformationLookup = new HashMap<>();
        final List<String> allRelevantEdges = structureRepository.getAllRelevantEdges(stage);
        if (!CollectionUtils.isEmpty(invitations) && (withIncomingLinks || spaceRestriction == null || SpaceName.REVIEW_SPACE.equals(spaceRestriction))) {
            //We have invitations available -> we have to reflect on them explicitly
            readMetaDataStructureForInvitations(stage, typeRestriction, withIncomingLinks, withIncomingLinks || withProperties, typeInformation, spaceTypeInformationLookup, allRelevantEdges, clientSpace, invitations);
        }
        final List<Space> s = getSpaces(stage, userWithRoles);
        String resolvedSpaceRestriction = privateUserSpace != null && SpaceName.PRIVATE_SPACE.equals(spaceRestriction) ? privateUserSpace.getName() : spaceRestriction;
        for (Space space : s) {
            // We need all spaces either if there is no space filter or if we require incoming links (because we need to reflect on the whole structure to capture all).
            // If there is a space filter applied and no requirement for incoming links, we can speed things up.
            if (withIncomingLinks || resolvedSpaceRestriction == null || space.getName().getName().equals(resolvedSpaceRestriction)) {
                //When asking for incoming links we need to fetch the properties as well -> otherwise we won't have the required information available.
                readMetaDataStructureForSpace(stage, typeRestriction, withIncomingLinks, withIncomingLinks || withProperties, typeInformation, spaceTypeInformationLookup, allRelevantEdges, space, clientSpace, privateUserSpace);
            }
        }
        final List<TypeInformation> result = aggregateGlobalInformation(withIncomingLinks || withProperties, typeInformation, spaceRestriction == null);
        if (withIncomingLinks) {
            aggregateIncomingLinks(result);
        }
        List<TypeInformation> resultsRestrictedBySpace = result;
        if (spaceRestriction != null) {
            //if we have a space restriction, we don't want the "spaces" to appear anymore but only take those that are having information about this space.
            resultsRestrictedBySpace = result.stream().map(r -> {
                final SpaceTypeInformation spaceTypeInformation = r.getSpaces().stream().filter(space -> spaceRestriction.equals(space.getSpace())).findAny().orElse(null);
                if (spaceTypeInformation == null) {
                    return null;
                } else {
                    if (withProperties) {
                        r.setProperties(spaceTypeInformation.getProperties());
                    }
                    r.setOccurrences(spaceTypeInformation.getOccurrences());
                    r.clearSpaces();
                }
                return r;
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }
        if (withIncomingLinks && !withProperties) {
            // We needed to reflect on the properties beforehand to evaluate the incoming links.
            // But the end-result shouldn't contain them, so we're going to clear it before returning.
            resultsRestrictedBySpace.forEach(r -> {
                r.clearProperties();
                r.getSpaces().forEach(space -> space.setProperties(null));
            });
        }

        final List<TypeInformation> sortedResult = resultsRestrictedBySpace.stream().sorted(Comparator.comparing(TypeInformation::getName)).collect(Collectors.toList());
        logger.info(String.format("Read meta data structure in %d ms", new Date().getTime() - start.getTime()));
        return sortedResult;
    }

    private void aggregateIncomingLinks(List<TypeInformation> result) {
        Map<String, List<IncomingLink>> incomingLinks = new HashMap<>();
        result.forEach(r -> r.getSpaces().forEach(
                space -> space.getProperties().stream().filter(p -> p.getTargetTypes() != null).forEach(
                        p -> p.getTargetTypes().forEach(
                                targetType -> {
                                    List<IncomingLink> incomingLinksOfTargetType = incomingLinks.computeIfAbsent(targetType.getType(), f -> new ArrayList<>());
                                    final IncomingLink incomingLink = incomingLinksOfTargetType.stream().filter(incomingLinkOfTargetType -> incomingLinkOfTargetType.getIdentifier().equals(p.getIdentifier())).findFirst().orElse(new IncomingLink());
                                    incomingLink.setIdentifier(p.getIdentifier());
                                    List<SourceType> sourceTypes = incomingLink.getSourceTypes();
                                    if (sourceTypes == null) {
                                        sourceTypes = new ArrayList<>();
                                    }
                                    final SourceType sourceType = sourceTypes.stream().filter(source -> source.getType().equals(r.getIdentifier())).findFirst().orElse(new SourceType());
                                    sourceType.setType(r.getIdentifier());
                                    List<SpaceReference> spaceReferences = sourceType.getSpaces();
                                    if (spaceReferences == null) {
                                        spaceReferences = new ArrayList<>();
                                    }
                                    SpaceReference spaceRef = new SpaceReference();
                                    spaceRef.setSpace(space.getSpace());
                                    if (!spaceReferences.contains(spaceRef)) {
                                        spaceReferences.add(spaceRef);
                                    }
                                    sourceType.setSpaces(spaceReferences);
                                    if (!sourceTypes.contains(sourceType)) {
                                        sourceTypes.add(sourceType);
                                    }
                                    sourceTypes.sort(Comparator.comparing(SourceType::getType));
                                    incomingLink.setSourceTypes(sourceTypes);
                                    if (!incomingLinksOfTargetType.contains(incomingLink)) {
                                        incomingLinksOfTargetType.add(incomingLink);
                                    }
                                }))));
        result.forEach(r -> {
            final List<IncomingLink> incomingLinksForType = incomingLinks.get(r.getIdentifier());
            r.setIncomingLinks(incomingLinksForType != null ? incomingLinksForType.stream().sorted(Comparator.comparing(IncomingLink::getIdentifier)).collect(Collectors.toList()) : Collections.emptyList());
        });
    }

    private final static List<String> GLOBAL_PROPERTY_BLACKLIST = Arrays.asList(EBRAINSVocabulary.META_OCCURRENCES, EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES, SchemaOrgVocabulary.IDENTIFIER);

    private List<TypeInformation> aggregateGlobalInformation(boolean withProperties, Map<String, TypeInformation> typeInformation, boolean clearAdditionalInfoForPropertiesInSpaces) {
        return typeInformation.values().stream().peek(t -> {
            t.setOccurrences(t.getSpaces().stream().mapToInt(SpaceTypeInformation::getOccurrences).sum());
            if (withProperties) {
                List<Property> properties = new ArrayList<>();
                t.setProperties(properties);
                final Map<String, List<Property>> propertiesMap = t.getSpaces().stream().map(SpaceTypeInformation::getProperties).flatMap(Collection::stream).collect(Collectors.groupingBy(Property::getIdentifier));
                for (String p : propertiesMap.keySet()) {
                    final List<Property> spaceProperties = propertiesMap.get(p);
                    Property globalProperty = new Property();
                    globalProperty.setIdentifier(p);
                    globalProperty.setOccurrences(spaceProperties.stream().mapToInt(Property::getOccurrences).sum());
                    spaceProperties.forEach(s -> {
                        final Set<String> relevantKeys = s.keySet().stream().filter(k -> !GLOBAL_PROPERTY_BLACKLIST.contains(k)).collect(Collectors.toSet());
                        relevantKeys.forEach(k -> globalProperty.put(k, s.get(k)));
                        if (clearAdditionalInfoForPropertiesInSpaces) {
                            relevantKeys.forEach(s::remove);
                        }
                    });
                    final Map<String, List<TargetType>> targetTypesMap = spaceProperties.stream().map(Property::getTargetTypes).flatMap(Collection::stream).collect(Collectors.groupingBy(TargetType::getType));
                    if (!targetTypesMap.isEmpty()) {
                        List<TargetType> targetTypes = new ArrayList<>();
                        globalProperty.setTargetTypes(targetTypes);
                        for (String type : targetTypesMap.keySet()) {
                            final List<TargetType> targetTypesFromSpace = targetTypesMap.get(type);
                            TargetType target = new TargetType();
                            target.setType(type);
                            List<SpaceReference> spaceReferences = new ArrayList<>();
                            final Map<String, List<SpaceReference>> spaceReferenceBySpace = targetTypesFromSpace.stream().map(TargetType::getSpaces).filter(Objects::nonNull).flatMap(Collection::stream).filter(s -> s.getSpace() != null).collect(Collectors.groupingBy(SpaceReference::getSpace));
                            int totalOccurrences = 0;
                            for (String s : spaceReferenceBySpace.keySet()) {
                                SpaceReference spaceref = new SpaceReference();
                                spaceref.setSpace(s);
                                spaceref.setOccurrences(spaceReferenceBySpace.get(s).stream().mapToInt(SpaceReference::getOccurrences).sum());
                                totalOccurrences += spaceref.getOccurrences();
                                spaceReferences.add(spaceref);
                            }
                            target.setOccurrences(totalOccurrences);
                            if (!spaceReferences.isEmpty()) {
                                target.setSpaces(spaceReferences);
                                target.getSpaces().sort(Comparator.comparing(SpaceReference::getSpace));
                            }
                            targetTypes.add(target);
                        }
                    }
                    properties.add(globalProperty);
                }
                properties.sort(Comparator.comparing(Property::getIdentifier));
            }
        }).collect(Collectors.toList());
    }

    private void readMetaDataStructureForSpace(DataStage stage, List<String> typeRestriction, boolean withIncomingLinks, boolean withProperties, Map<String, TypeInformation> typeInformations, Map<String, List<SpaceTypeInformation>> spaceTypeInformationLookup, List<String> allRelevantEdges, Space space, SpaceName clientSpace, SpaceName privateUserSpace) {
        final List<TypeWithInstanceCountReflection> typeWithInstanceCountReflections = space.isExistsInDB() ? structureRepository.reflectTypesInSpace(stage, space.getName()) : Collections.emptyList();
        final Set<String> reflectedTypes = typeWithInstanceCountReflections.stream().map(TypeWithInstanceCountReflection::getName).filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<String, DynamicJson> typesInSpaceBySpecification = structureRepository.getTypesInSpaceBySpecification(space.getName()).stream().map(t -> new Tuple<String, DynamicJson>().setA(t).setB(structureRepository.getTypeSpecification(t))).filter(t -> t.getB() != null).collect(Collectors.toMap(Tuple::getA, Tuple::getB));
        final Map<String, DynamicJson> clientSpecificTypesInSpaceBySpecification = clientSpace == null ? Collections.emptyMap() : typesInSpaceBySpecification.keySet().stream().map(t -> new Tuple<String, DynamicJson>().setA(t).setB(structureRepository.getClientSpecificTypeSpecification(t, clientSpace))).filter(t -> t.getB() != null).collect(Collectors.toMap(Tuple::getA, Tuple::getB));
        final Stream<TypeWithInstanceCountReflection> allTypes = Stream.concat(typeWithInstanceCountReflections.stream(), typesInSpaceBySpecification.keySet().stream().filter(k -> !reflectedTypes.contains(k)).map(k -> {
            TypeWithInstanceCountReflection r = new TypeWithInstanceCountReflection();
            r.setName(k);
            r.setOccurrences(0);
            return r;
        }));
        final Stream<TypeWithInstanceCountReflection> filteredTypes = typeRestriction != null && !withIncomingLinks ? allTypes.filter(t -> typeRestriction.contains(t.getName())) : allTypes;
        filteredTypes.forEach(type -> {
            final TypeInformation typeInformation = typeInformations.computeIfAbsent(type.getName(), t -> new TypeInformation());
            final DynamicJson specification = typesInSpaceBySpecification.get(type.getName());
            typeInformation.setName(Type.labelFromName(type.getName()));
            typeInformation.setIdentifier(type.getName());
            if (specification != null) {
                specification.keySet().forEach(k -> {
                    if (specification.get(k) != null) {
                        typeInformation.put(k, specification.get(k));
                    }
                });
            }
            final DynamicJson clientSpecificSpecification = clientSpecificTypesInSpaceBySpecification.get(type.getName());
            if (clientSpecificSpecification != null) {
                clientSpecificSpecification.keySet().forEach(k -> {
                    if (clientSpecificSpecification.get(k) != null) {
                        typeInformation.put(k, clientSpecificSpecification.get(k));
                    }
                });
            }
            List<SpaceTypeInformation> spaceTypeInformations = spaceTypeInformationLookup.computeIfAbsent(type.getName(), t -> new ArrayList<>());
            SpaceTypeInformation spaceTypeInformation = new SpaceTypeInformation();
            spaceTypeInformations.add(spaceTypeInformation);
            typeInformation.setSpaces(spaceTypeInformations);
            spaceTypeInformation.setSpace(SpaceName.translateSpace(space.getName().getName(), privateUserSpace));
            spaceTypeInformation.setOccurrences(type.getOccurrences());
            if (withProperties) {
                spaceTypeInformation.setProperties(new ArrayList<>());
                final List<PropertyOfTypeInSpaceReflection> reflectedProperties = space.isExistsInDB() ? structureRepository.reflectPropertiesOfTypeInSpace(stage, space.getName(), type.getName()) : Collections.emptyList();
                final Set<String> reflectedPropertyNames = reflectedProperties.stream().map(PropertyOfTypeInSpaceReflection::getName).collect(Collectors.toSet());
                final Map<String, DynamicJson> propertiesOfTypeBySpecification = structureRepository.getPropertiesOfTypeBySpecification(type.getName()).stream().collect(Collectors.toMap(k -> k.getAs(SchemaOrgVocabulary.IDENTIFIER, String.class), v -> v));
                final Map<String, DynamicJson> clientSpecificPropertiesOfTypeBySpecification = clientSpace == null ? Collections.emptyMap() : structureRepository.getClientSpecificPropertiesOfTypeBySpecification(type.getName(), clientSpace).stream().collect(Collectors.toMap(k -> k.getAs(SchemaOrgVocabulary.IDENTIFIER, String.class), v -> v));

                Stream.concat(reflectedProperties.stream(), propertiesOfTypeBySpecification.keySet().stream().filter(k -> !reflectedPropertyNames.contains(k)).map(k -> {
                    //Create placeholders for those properties that are only existing in the specification
                    PropertyOfTypeInSpaceReflection p = new PropertyOfTypeInSpaceReflection();
                    p.setName(k);
                    p.setOccurrences(0);
                    return p;
                })).forEach(property -> {
                    Property p = new Property();
                    spaceTypeInformation.getProperties().add(p);
                    final DynamicJson globalPropertySpec = structureRepository.getPropertyBySpecification(property.getName());
                    Set<String> targetTypesFromSpec = new HashSet<>();
                    if (globalPropertySpec != null) {
                        globalPropertySpec.keySet().forEach(k -> {
                            //Target types are part of the property specification but will be treated differently
                            if (!EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES.equals(k)) {
                                p.put(k, globalPropertySpec.get(k));
                            }
                        });
                        targetTypesFromSpec.addAll(globalPropertySpec.getAsListOf(EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES, String.class));
                    }
                    if (clientSpace != null) {
                        final DynamicJson clientSpecificPropertySpec = structureRepository.getClientSpecificPropertyBySpecification(property.getName(), clientSpace);
                        if (clientSpecificPropertySpec != null) {
                            clientSpecificPropertySpec.keySet().forEach(k -> {
                                //We don't allow client specific target type definitions.
                                if (!EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES.equals(k)) {
                                    p.put(k, clientSpecificPropertySpec.get(k));
                                }
                            });
                        }
                    }
                    final DynamicJson propertySpecFromRelation = propertiesOfTypeBySpecification.get(property.getName());
                    if (propertySpecFromRelation != null) {
                        propertySpecFromRelation.keySet().forEach(k -> {
                            //Target types are part of the property specification but will be treated differently
                            if (!EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES.equals(k)) {
                                p.put(k, propertySpecFromRelation.get(k));
                            }
                        });
                        targetTypesFromSpec.addAll(propertySpecFromRelation.getAsListOf(EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES, String.class));
                    }
                    final DynamicJson clientSpecificPropertySpecFromRelation = clientSpecificPropertiesOfTypeBySpecification.get(property.getName());
                    if (clientSpecificPropertySpecFromRelation != null) {
                        clientSpecificPropertySpecFromRelation.keySet().forEach(k -> {
                            //We don't allow client specific target type definitions.
                            if (!EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES.equals(k)) {
                                p.put(k, clientSpecificPropertySpecFromRelation.get(k));
                            }
                        });
                    }
                    p.setOccurrences(property.getOccurrences());
                    p.setIdentifier(property.getName());
                    final ArangoCollectionReference edgeCollection = new ArangoCollectionReference(property.getName(), true);
                    if (!targetTypesFromSpec.isEmpty() || allRelevantEdges.contains(edgeCollection.getCollectionName())) {
                        final List<TargetTypeReflection> targetTypeReflections = space.isExistsInDB() && allRelevantEdges.contains(edgeCollection.getCollectionName()) ? structureRepository.reflectTargetTypes(stage, space.getName(), type.getName(), property.getName()) : Collections.emptyList();
                        final List<TargetType> targetTypes = new ArrayList<>();
                        p.setTargetTypes(targetTypes);
                        final Map<String, List<TargetTypeReflection>> targetTypeReflectionsByType = targetTypeReflections.stream().collect(Collectors.groupingBy(TargetTypeReflection::getName));
                        targetTypeReflectionsByType.putAll(targetTypesFromSpec.stream().filter(t -> !targetTypeReflectionsByType.containsKey(t)).map(t -> {
                            TargetTypeReflection r = new TargetTypeReflection();
                            r.setName(t);
                            return r;
                        }).collect(Collectors.toMap(TargetTypeReflection::getName, Collections::singletonList)));
                        for (String targetType : targetTypeReflectionsByType.keySet()) {
                            TargetType t = new TargetType();
                            t.setType(targetType);
                            final List<SpaceReference> spaceReferences = new ArrayList<>();
                            targetTypes.add(t);
                            int totalOccurrences = 0;
                            for (TargetTypeReflection reflection : targetTypeReflectionsByType.get(targetType)) {
                                if (reflection.getSpace() != null) {
                                    SpaceReference spaceReference = new SpaceReference();
                                    spaceReference.setSpace(SpaceName.translateSpace(reflection.getSpace(), privateUserSpace));
                                    spaceReference.setOccurrences(reflection.getOccurrences());
                                    totalOccurrences += reflection.getOccurrences();
                                    spaceReferences.add(spaceReference);
                                }
                            }
                            if (!spaceReferences.isEmpty()) {
                                t.setSpaces(spaceReferences);
                            }
                            t.setOccurrences(totalOccurrences);
                        }
                    }
                });
            }

        });
    }


    public List<Space> getSpaces(DataStage stage, UserWithRoles userWithRoles) {
        final List<SpaceName> reflectedSpaces = this.structureRepository.reflectSpaces(stage);
        final List<Space> spaceSpecifications = this.structureRepository.getSpaceSpecifications();
        final Set<SpaceName> spacesWithSpecifications = spaceSpecifications.stream().map(Space::getName).collect(Collectors.toSet());
        final Stream<Space> allSpaces = Stream.concat(spaceSpecifications.stream().map(s->new Space(s.getName(), s.isAutoRelease(), s.isClientSpace())), reflectedSpaces.stream().filter(s -> !spacesWithSpecifications.contains(s))
                .map(s -> new Space(s, false, false).setReflected(true)))
                .peek(s -> {
                    if (reflectedSpaces.contains(s.getName())) {
                        s.setExistsInDB(true);
                    }
                });
        Set<SpaceName> whitelistedSpaces = permissionsController.whitelistedSpaceReads(userWithRoles);
        List<Space> spaceDefinitions;
        if (whitelistedSpaces != null) {
            spaceDefinitions = allSpaces.filter(s -> whitelistedSpaces.contains(s.getName())).collect(Collectors.toList());
        } else {
            spaceDefinitions = allSpaces.collect(Collectors.toList());
        }
        return spaceDefinitions;
    }


}

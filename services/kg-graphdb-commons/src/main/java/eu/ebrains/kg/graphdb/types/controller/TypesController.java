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

package eu.ebrains.kg.graphdb.types.controller;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.external.types.*;
import eu.ebrains.kg.commons.model.types.Property;
import eu.ebrains.kg.commons.model.types.TargetTypeWithOccurrence;
import eu.ebrains.kg.commons.model.types.TypeWithOccurrencesAndProperties;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TypesController {

    private final DBStructureReflection typeReflection;
    private final Specifications specifications;
    private final AuthContext authContext;

    private final Logger logger = LoggerFactory.getLogger(getClass());


    @PostConstruct
    public void setup(){

    }

    private void populateStructuralCache(){
        logger.info("Now populating structural cache...");
    }



    public TypesController(DBStructureReflection typeReflection, Specifications specifications, AuthContext authContext) {
        this.typeReflection = typeReflection;
        this.specifications = specifications;
        this.authContext = authContext;
    }

    public Paginated<TypeInformation> getSpacesWithTypes(DataStage stage, List<String> typeFilter, PaginationParam paginationParam, boolean withCounts){
        final List<String> allSpaces = typeReflection.getAllSpaceCollections(stage);
        final List<Paginated<TypeInformation>> spaceInfo = allSpaces.stream().map(s -> getTypesForSpace(stage, SpaceName.fromString(s), null, paginationParam, withCounts)).collect(Collectors.toList());
        //TODO combine
        return spaceInfo.get(0);

       // final Stream<SpaceDefinition> spaceWithTypesStream = allSpaces.stream().map(s -> new SpaceDefinition(s, ));
//        final List<TypeWithOccurrencesAndProperties> typesWithOccurrences = spaceWithTypesStream.map(s -> {
//            s.getTypes().forEach(t -> {
//                if (t.getSpaces() == null) {
//                    t.setSpaces(new ArrayList<>());
//                }
//                if (!t.getSpaces().contains(s.getInternalSpaceName())) {
//                    //TODO we need the real space name here...
//                    t.getSpaces().add(s.getInternalSpaceName());
//                }
//            });
//            return s.getTypes();
//        }).flatMap(Collection::stream).collect(Collectors.toList());
//        //TODO pagination
//        return new Paginated<>(typesWithOccurrences, typesWithOccurrences.size(), typesWithOccurrences.size(), 0);
//        return null;
    }

    private List<TypeWithOccurrencesAndProperties> enrichSpacesWithSpecificationOnly(List<TypeWithOccurrencesAndProperties> reflectedTypes, DataStage stage, SpaceName spaceName, boolean withCounts){
        final List<String> specifiedTypesForThisSpace = specifications.getTypesInSpace(stage, spaceName);
        // We exclude the types from
        reflectedTypes.forEach(t -> specifiedTypesForThisSpace.remove(t.getType()));
        return Stream.concat(specifiedTypesForThisSpace.stream().map(s -> {
            TypeWithOccurrencesAndProperties fromSpecOnly = new TypeWithOccurrencesAndProperties();
            fromSpecOnly.setType(s);
            if(withCounts) {
                fromSpecOnly.setOccurrences(0L);
            }
            return fromSpecOnly;
        }), reflectedTypes.stream()).collect(Collectors.toList());
    }

    private Set<String> getPropertySpecsForType(NormalizedJsonLd propertyInTypeSpecification, String type) {
        final String typeEnding = String.format("@%s", type);
        return propertyInTypeSpecification.keySet().stream().filter(k -> k.endsWith(typeEnding)).map(k-> k.substring(0, k.length()-typeEnding.length())).collect(Collectors.toSet());
    }

    private List<IncomingLink> getPropertiesPointingToType(NormalizedJsonLd propertyInTypeSpecifications, String type){
        Map<String, List<String[]>> propertyInTypes = propertyInTypeSpecifications.keySet().stream().map(key ->
        {
            final NormalizedJsonLd propertyInType = propertyInTypeSpecifications.getAs(key, NormalizedJsonLd.class);
            final List<String> targetTypes = propertyInType.getAsListOf(EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES, String.class);
            return targetTypes.contains(type) ? key : null;
        }).filter(Objects::nonNull).map(k -> k.split("@")).collect(Collectors.groupingBy(k -> k[0]));
        return propertyInTypes.keySet().stream().map(k -> {
            IncomingLink incomingLink = new IncomingLink();
            incomingLink.setIdentifier(k);
            incomingLink.setSourceTypes(propertyInTypes.get(k).stream().map(t -> {
                SourceType sourceType = new SourceType();
                sourceType.setType(t[1]);
                return sourceType;
            }).collect(Collectors.toList()));
            return incomingLink;
        }).collect(Collectors.toList());
    }


    public NormalizedJsonLd getTypeSpecifications(DataStage stage, SpaceName clientName){
        return getSpecifications(stage, EBRAINSVocabulary.META_TYPEDEFINITION_TYPE, Collections.singletonList(EBRAINSVocabulary.META_TYPE), clientName);
    }

    public NormalizedJsonLd getPropertySpecifications( DataStage stage, SpaceName clientName){
        return getSpecifications(stage, EBRAINSVocabulary.META_PROPERTY_DEFINITION_TYPE, Collections.singletonList(EBRAINSVocabulary.META_PROPERTY), clientName);
    }

    public NormalizedJsonLd getPropertyInTypeSpecifications( DataStage stage, SpaceName clientName){
        return getSpecifications(stage, EBRAINSVocabulary.META_PROPERTY_IN_TYPE_DEFINITION_TYPE, Arrays.asList(EBRAINSVocabulary.META_PROPERTY, EBRAINSVocabulary.META_TYPE), clientName);
    }

    private NormalizedJsonLd getSpecifications(DataStage stage, String definitionType, List<String> groupingField, SpaceName clientName){
        final NormalizedJsonLd globalSpecifications = specifications.getGlobalSpecifications(stage, definitionType, groupingField);
        if(clientName!=null) {
            final NormalizedJsonLd clientSpecifications = specifications.getClientSpecifications(stage, definitionType, groupingField, clientName);
            //TODO merge
        }
        return globalSpecifications;
    }


    
    public Paginated<TypeInformation> getTypesForSpace(DataStage stage, SpaceName spaceName, List<String> typeFilter, PaginationParam paginationParam, boolean withCounts){
        final String collectionName = ArangoCollectionReference.fromSpace(spaceName).getCollectionName();
        // We need to combine the following configurations:
        SpaceName clientName = null;
        if(authContext.getUserWithRoles().getClientId() != null){
            clientName = authContext.getClientSpace().getName();
        }
        final NormalizedJsonLd typeSpecifications = getTypeSpecifications(stage, clientName);
        final NormalizedJsonLd propertySpecifications = getPropertySpecifications(stage, clientName);
        final NormalizedJsonLd propertyInTypeSpecifications = getPropertyInTypeSpecifications(stage, clientName);
        final List<TypeWithOccurrencesAndProperties> reflectedTypes = reflectTypes(collectionName, typeFilter, stage, withCounts);
        final List<TypeWithOccurrencesAndProperties> types = enrichSpacesWithSpecificationOnly(reflectedTypes, stage, spaceName, withCounts);

        final Map<String, List<IncomingLink>> incomingLinks = new HashMap<>();

        //TODO pagination
        final List<TypeInformation> typeInformationList = types.stream().map(t -> {
            final TypeInformation typeInformation = new TypeInformation();
            final NormalizedJsonLd globalSpec = typeSpecifications.getAs(t.getType(), NormalizedJsonLd.class);
            if(globalSpec!=null){
                typeInformation.putAll(globalSpec);
            }
            typeInformation.setIdentifier(t.getType());
            if(t.getOccurrences()!=null) {
                typeInformation.setOccurrences(t.getOccurrences().intValue());
            }
            if(t.getSpaces()!=null) {
                typeInformation.setSpaces(t.getSpaces().stream().map(space -> {
                    SpaceTypeInformation spaceTypeInformation = new SpaceTypeInformation();
                    spaceTypeInformation.setSpace(space);
                    return spaceTypeInformation;
                }).collect(Collectors.toList()));
            }
            enrichPropertiesWithContractOnly(propertyInTypeSpecifications, t, withCounts);
            //typeInformation.setIncomingLinks(getPropertiesPointingToType(propertyInTypeSpecifications, t.getType()));
            if(t.getProperties()!=null){
                typeInformation.setProperties(t.getProperties().stream().map(property -> {
                    eu.ebrains.kg.commons.model.external.types.Property p = new eu.ebrains.kg.commons.model.external.types.Property();
                    final NormalizedJsonLd propertySpec = propertySpecifications.getAs(property.getProperty(), NormalizedJsonLd.class);
                    if(propertySpec!=null){
                        p.putAll(propertySpec);
                    }
                    final NormalizedJsonLd propertyInTypeSpec = propertyInTypeSpecifications.getAs(String.format("%s@%s", property.getProperty(), t.getType()), NormalizedJsonLd.class);
                    if(propertyInTypeSpec!=null){
                        p.putAll(propertyInTypeSpec);
                    }
                    final List<String> specifiedTargetTypes = p.getAsListOf(EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES, String.class);
                    if(!specifiedTargetTypes.isEmpty() || property.getTargetTypes()!=null){
                        //This is a property with target types
                        if(property.getTargetTypes()==null){
                            property.setTargetTypes(new ArrayList<>());
                        }
                        //We remove all specified target types which are reflected
                        specifiedTargetTypes.removeAll(property.getTargetTypes().stream().map(TargetTypeWithOccurrence::getTargetType).collect(Collectors.toSet()));
                        property.getTargetTypes().addAll(specifiedTargetTypes.stream().map(specifiedTargetType -> {
                            TargetTypeWithOccurrence targetTypeWithOccurrence = new TargetTypeWithOccurrence();
                            targetTypeWithOccurrence.setTargetType(specifiedTargetType);
                            if(withCounts) {
                                targetTypeWithOccurrence.setOccurrences(0L);
                            }
                            return targetTypeWithOccurrence;
                        }).collect(Collectors.toSet()));

                        Map<String, TargetType> targetTypeGrouping = new HashMap<>();

                        p.setTargetTypes(property.getTargetTypes().stream().map(targetType -> {
                            TargetType type = targetTypeGrouping.get(targetType.getTargetType());
                            if(type == null){
                                type = new TargetType();
                                type.setType(targetType.getTargetType());
                                targetTypeGrouping.put(targetType.getTargetType(), type);
                            }
                            List<IncomingLink> incomingLinkList = incomingLinks.computeIfAbsent(type.getType(), k -> new ArrayList<>());
                            IncomingLink incomingLink = incomingLinkList.stream().filter(i -> i.getIdentifier().equals(property.getProperty())).findAny().orElse(null);
                            if(incomingLink == null){
                                incomingLink = new IncomingLink();
                                incomingLink.setIdentifier(property.getProperty());
                                incomingLink.setSourceTypes(new ArrayList<>());
                                incomingLinkList.add(incomingLink);
                            }
                            SourceType sourceType = new SourceType();
                            sourceType.setType(t.getType());
                            incomingLink.getSourceTypes().add(sourceType);

                            if(targetType.getSpace()!=null) {
                                SpaceReference reference = new SpaceReference();
                                reference.setSpace(targetType.getSpace());
                                if(targetType.getOccurrences()!=null) {
                                    reference.setOccurrences(targetType.getOccurrences().intValue());
                                }
                                if(type.getSpaces() == null){
                                    type.setSpaces(new ArrayList<>());
                                }
                                type.getSpaces().add(reference);
                            }
                            if(withCounts) {
                                type.setOccurrences((type.getOccurrences() == null ? 0 : type.getOccurrences().intValue()) + (targetType.getOccurrences() == null ? 0 : targetType.getOccurrences().intValue()));
                            }
                            return type;
                        }).collect(Collectors.toList()));
                    }

                    p.setIdentifier(property.getProperty());
                    if(property.getOccurrences()!=null) {
                        p.setOccurrences(property.getOccurrences().intValue());
                    }
                    return p;
                }).collect(Collectors.toList()));
            }
            return typeInformation;
        }).collect(Collectors.toList());typeInformationList.forEach(t -> {
            final List<IncomingLink> incomingLink = incomingLinks.get(t.getIdentifier());
            if(incomingLink!=null) {
                t.setIncomingLinks(incomingLink);
            }
        });
        return new Paginated<>(typeInformationList, typeInformationList.size(), typeInformationList.size(), 0);
    }

    private void enrichPropertiesWithContractOnly(NormalizedJsonLd propertyInTypeSpecifications, TypeWithOccurrencesAndProperties t, boolean withCounts) {
        Set<String> propertyDefinitions = getPropertySpecsForType(propertyInTypeSpecifications, t.getType());
        if(t.getProperties()==null){
            t.setProperties(new ArrayList<>());
        }
        Set<String> reflectedPropertyNames = t.getProperties().stream().map(Property::getProperty).collect(Collectors.toSet());
        propertyDefinitions.removeAll(reflectedPropertyNames);
        t.getProperties().addAll(propertyDefinitions.stream().map(p -> {
            Property prop = new Property();
            prop.setProperty(p);
            if(withCounts) {
                prop.setOccurrences(0L);
            }
            return prop;
        }).collect(Collectors.toList()));
    }


    private List<TypeWithOccurrencesAndProperties> reflectTypes(String space, List<String> typeFilter, DataStage stage, boolean withCounts){
        final List<TypeWithOccurrencesAndProperties> typesWithOccurrences = withCounts ? typeReflection.getTypesWithOccurrencesAndProperties(space, typeFilter, stage) : typeReflection.getTypesWithProperties(space, typeFilter, stage);
        final List<String> allEdges = typeReflection.getAllRelevantEdges(stage);
        typesWithOccurrences.forEach(t -> t.getProperties().forEach(p -> {
            if(!p.getProperty().startsWith(EBRAINSVocabulary.META)) {
                final String propertyCollectionName = new ArangoCollectionReference(p.getProperty(), true).getCollectionName();
                if (allEdges.contains(propertyCollectionName)) {
                    //This is a relation -> enrich with its target types
                    p.setTargetTypes(typeReflection.getTargetTypeWithOccurrence(space, t.getType(), p.getProperty(), propertyCollectionName, stage, withCounts));
                }
            }
        }));
        return typesWithOccurrences;
    }

}

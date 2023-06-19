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

package eu.ebrains.kg.graphdb.queries.utils;

import eu.ebrains.kg.arango.commons.aqlbuilder.ArangoVocabulary;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.queries.model.spec.SpecProperty;
import eu.ebrains.kg.graphdb.queries.model.spec.SpecTraverse;
import eu.ebrains.kg.graphdb.queries.model.spec.Specification;

import java.util.*;

public class SpecificationToScopeQueryAdapter {
    private final Specification originalSpec;

    private final Map<String, SpecProperty> specPropertiesByConcatenatedPath = new HashMap<>();
    private int propertyCounter = 0;

    public SpecificationToScopeQueryAdapter(Specification originalSpec) {
        this.originalSpec = originalSpec;
    }

    private List<SpecProperty> handleProperty(SpecProperty property){
        //Handle path to property
        List<SpecProperty> subProperties = new ArrayList<>();
        List<SpecProperty> traversalSubProperties = subProperties;
        if(property.needsTraversal()) {
            for (SpecTraverse specTraverse : property.path) {
                if(specTraverse!=property.getLeafPath()) {
                    SpecProperty traversalProperty = new SpecProperty(String.format("dependency_%d", propertyCounter++), new ArrayList<>(), Collections.singletonList(specTraverse), null, false, false, false, false, null, null);
                    traversalProperty.property.add(idProperty());
                    traversalProperty.property.add(typeProperty());
                    traversalProperty.property.add(internalIdProperty());
                    traversalProperty.property.add(spaceProperty());
                    traversalProperty.property.add(embeddedProperty());
                    traversalProperty.property.add(labelProperty());
                    traversalSubProperties.add(traversalProperty);
                    traversalSubProperties = traversalProperty.property;
                }
            }
        }
        //Handle sub properties
        if(property.hasSubProperties()){
            for (SpecProperty subProperty : property.property) {
                traversalSubProperties.addAll(handleProperty(subProperty));
            }
        }
        return subProperties;
    }


    public Specification translate(){
        final List<SpecProperty> root = new ArrayList<>();
        root.add(idProperty());
        root.add(typeProperty());
        root.add(internalIdProperty());
        root.add(spaceProperty());
        root.add(embeddedProperty());
        root.add(labelProperty());
        originalSpec.getProperties().stream().map(this::handleProperty).forEach(root::addAll);
        List<SpecProperty> normalized = normalize(root, "");


        return new Specification(normalized, originalSpec.getDocumentFilter(), originalSpec.getRootType(), originalSpec.getResponseVocab());
    }


    private List<SpecProperty> normalize(List<SpecProperty> properties, String prefix){
        List<SpecProperty> result = new ArrayList<>();
        for (SpecProperty property : properties) {
            final String concatenatedPath = appendToConcatenatedPath(prefix, property);
            if(specPropertiesByConcatenatedPath.containsKey(concatenatedPath)){
                SpecProperty propertyToAttach = specPropertiesByConcatenatedPath.get(concatenatedPath);
                if(property.hasSubProperties()){
                    propertyToAttach.property.addAll(property.property);
                    final List<SpecProperty> normalized = normalize(propertyToAttach.property, concatenatedPath);
                    propertyToAttach.property.clear();
                    propertyToAttach.property.addAll(normalized);
                }
                if(!result.contains(propertyToAttach)) {
                    result.add(propertyToAttach);
                }
            }
            else{
                specPropertiesByConcatenatedPath.put(concatenatedPath, property);
                result.add(property);
            }
        }
        return result;
    }

    private String appendToConcatenatedPath(String path, SpecProperty specProperty){
        final SpecTraverse specTraverse = specProperty.path.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append(path).append('€');
        if(specTraverse.reverse){
            sb.append('-');
        }
        sb.append(specTraverse.pathName);
        return sb.toString();
    }


    private SpecProperty idProperty(){
        return new SpecProperty("id", null, Collections.singletonList(new SpecTraverse(JsonLdConsts.ID, false, null)), null, false, false, false, false, null, SpecProperty.SingleItemStrategy.FIRST);
    }
    private SpecProperty internalIdProperty(){
        return new SpecProperty("internalId", null, Collections.singletonList(new SpecTraverse(ArangoVocabulary.ID, false, null)), null, false, false, false, false, null, SpecProperty.SingleItemStrategy.FIRST);
    }
    private SpecProperty labelProperty(){
        return new SpecProperty("label", null, Collections.singletonList(new SpecTraverse(IndexedJsonLdDoc.LABEL, false, null)), null, false, false, false, false, null, SpecProperty.SingleItemStrategy.FIRST);
    }
    private SpecProperty embeddedProperty(){
        return new SpecProperty("embedded", null, Collections.singletonList(new SpecTraverse(IndexedJsonLdDoc.EMBEDDED, false, null)), null, false, false, false, false, null, SpecProperty.SingleItemStrategy.FIRST);
    }
    private SpecProperty typeProperty(){
        return new SpecProperty("type", null, Collections.singletonList(new SpecTraverse(JsonLdConsts.TYPE, false, null)), null, false, false, false,false, null, SpecProperty.SingleItemStrategy.FIRST);
    }
    private SpecProperty spaceProperty(){
        return new SpecProperty("space", null, Collections.singletonList(new SpecTraverse(EBRAINSVocabulary.META_SPACE, false, null)), null, false, false, false,false, null, SpecProperty.SingleItemStrategy.FIRST);
    }

}

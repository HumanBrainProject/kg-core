/*
 * Copyright 2020 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.graphdb.queries.controller;

import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.graphdb.queries.model.spec.SpecProperty;
import eu.ebrains.kg.graphdb.queries.model.spec.SpecTraverse;
import eu.ebrains.kg.graphdb.queries.model.spec.Specification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpecificationToScopeQueryAdapter {
    private final Specification originalSpec;
    private final List<SpecProperty> root = new ArrayList<>();
    private int propertyCounter = 0;

    public SpecificationToScopeQueryAdapter(Specification originalSpec) {
        this.originalSpec = originalSpec;
    }

    private List<SpecProperty> handleProperty(SpecProperty property){
        //Handle path to property
        List<SpecProperty> subProperties = new ArrayList<>();
        if(property.needsTraversal()) {
            List<SpecProperty> traversalSubProperties = subProperties;
            for (SpecTraverse specTraverse : property.path) {
                if(specTraverse!=property.getLeafPath()) {
                    SpecProperty traversalProperty = new SpecProperty(String.format("dependency_%d", propertyCounter++), new ArrayList<>(), Collections.singletonList(specTraverse), null, false, false, false, false, null, null);
                    traversalProperty.property.add(idProperty());
                    traversalProperty.property.add(typeProperty());
                    traversalProperty.property.add(internalIdProperty());
                    traversalSubProperties.add(traversalProperty);
                    traversalSubProperties = traversalProperty.property;
                }
            }
        }
        //Handle sub properties
        if(property.hasSubProperties()){
            for (SpecProperty subProperty : property.property) {
                subProperties.addAll(handleProperty(subProperty));
            }
        }
        return subProperties;
    }


    public Specification translate(){
        root.add(idProperty());
        root.add(typeProperty());
        root.add(internalIdProperty());
        originalSpec.getProperties().stream().map(this::handleProperty).forEach(root::addAll);
        return new Specification(root, originalSpec.getDocumentFilter(), originalSpec.getRootType());
    }


    private SpecProperty idProperty(){
        return new SpecProperty("id", null, Collections.singletonList(new SpecTraverse(JsonLdConsts.ID, false, null)), null, false, false, false, false, null, SpecProperty.SingleItemStrategy.FIRST);
    }
    private SpecProperty internalIdProperty(){
        return new SpecProperty("internalId", null, Collections.singletonList(new SpecTraverse(ArangoVocabulary.ID, false, null)), null, false, false, false, false, null, SpecProperty.SingleItemStrategy.FIRST);
    }
    private SpecProperty typeProperty(){
        return new SpecProperty("type", null, Collections.singletonList(new SpecTraverse(JsonLdConsts.TYPE, false, null)), null, false, false, false, false, null, SpecProperty.SingleItemStrategy.FIRST);
    }

}

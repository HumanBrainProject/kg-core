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

package eu.ebrains.kg.graphdb.queries.utils;

import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
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
        List<SpecProperty> traversalSubProperties = subProperties;
        if(property.needsTraversal()) {
            for (SpecTraverse specTraverse : property.path) {
                if(specTraverse!=property.getLeafPath()) {
                    SpecProperty traversalProperty = new SpecProperty(String.format("dependency_%d", propertyCounter++), new ArrayList<>(), Collections.singletonList(specTraverse), null, false, false, false, false, false, null, null);
                    traversalProperty.property.add(idProperty());
                    traversalProperty.property.add(typeProperty());
                    traversalProperty.property.add(internalIdProperty());
                    traversalProperty.property.add(spaceProperty());
                    traversalProperty.property.add(embeddedProperty());
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
        root.add(idProperty());
        root.add(typeProperty());
        root.add(internalIdProperty());
        root.add(spaceProperty());
        root.add(embeddedProperty());
        originalSpec.getProperties().stream().map(this::handleProperty).forEach(root::addAll);
        return new Specification(root, originalSpec.getDocumentFilter(), originalSpec.getRootType(), originalSpec.getResponseVocab());
    }


    private SpecProperty idProperty(){
        return new SpecProperty("id", null, Collections.singletonList(new SpecTraverse(JsonLdConsts.ID, false, null)), null, false, false, false, false, false, null, SpecProperty.SingleItemStrategy.FIRST);
    }
    private SpecProperty internalIdProperty(){
        return new SpecProperty("internalId", null, Collections.singletonList(new SpecTraverse(ArangoVocabulary.ID, false, null)), null, false, false, false, false, false, null, SpecProperty.SingleItemStrategy.FIRST);
    }
    private SpecProperty embeddedProperty(){
        return new SpecProperty("embedded", null, Collections.singletonList(new SpecTraverse(IndexedJsonLdDoc.EMBEDDED, false, null)), null, false, false, false, false, false, null, SpecProperty.SingleItemStrategy.FIRST);
    }
    private SpecProperty typeProperty(){
        return new SpecProperty("type", null, Collections.singletonList(new SpecTraverse(JsonLdConsts.TYPE, false, null)), null, false, false, false, false,false, null, SpecProperty.SingleItemStrategy.FIRST);
    }
    private SpecProperty spaceProperty(){
        return new SpecProperty("space", null, Collections.singletonList(new SpecTraverse(EBRAINSVocabulary.META_SPACE, false, null)), null, false, false, false, false,false, null, SpecProperty.SingleItemStrategy.FIRST);
    }

}

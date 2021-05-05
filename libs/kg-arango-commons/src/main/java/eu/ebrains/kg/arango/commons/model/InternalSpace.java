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

package eu.ebrains.kg.arango.commons.model;

import eu.ebrains.kg.commons.jsonld.InferredJsonLdDoc;
import eu.ebrains.kg.commons.model.SpaceName;

import java.util.Arrays;
import java.util.Collection;

public class InternalSpace extends SpaceName {


    public static final SpaceName GLOBAL_SPEC = new SpaceName("global_spec");

    public static final InternalSpace INFERENCE_OF_SPACE = new InternalSpace(InferredJsonLdDoc.INFERENCE_OF);
    public static final InternalSpace UNRESOLVED_SPACE = new InternalSpace("unresolved");
    public static final ArangoCollectionReference PROPERTY_TO_PROPERTY_VALUE_TYPE_EDGE_COLLECTION = new ArangoCollectionReference("property2propertyValueType", true);
    public static final SpaceName PROPERTIES_SPACE = new SpaceName("properties");
    public static final ArangoCollectionReference SPACE_TO_TYPE_EDGE_COLLECTION = new ArangoCollectionReference("space2type", true);
    public static final SpaceName SPACES_SPACE = new SpaceName("spaces");
    public static final SpaceName PROPERTY_VALUE_TYPE_SPACE = new SpaceName("propertyValueTypes");
    public static final ArangoCollectionReference DOCUMENT_RELATION_EDGE_COLLECTION = new ArangoCollectionReference("documentRelation", true);
    public static final SpaceName DOCUMENT_SPACE = new SpaceName("documents");
    public static final ArangoCollectionReference CLIENT_TYPE_PROPERTY_EDGE_COLLECTION = new ArangoCollectionReference("clientTypeProperty", true);
    public static final ArangoCollectionReference CLIENT_TYPE_EDGE_COLLECTION = new ArangoCollectionReference("clientType", true);
    public static final SpaceName CLIENT_SPACE = new SpaceName("clients");
    public static final ArangoCollectionReference DOCUMENT_ID_EDGE_COLLECTION = new ArangoCollectionReference("documentId", true);
    public static final InternalSpace DOCUMENT_ID_SPACE = new InternalSpace("documentIds");
    public static final ArangoCollectionReference RELEASE_STATUS_EDGE_COLLECTION = new ArangoCollectionReference("internalrelease", true);
    public static final InternalSpace RELEASE_STATUS_SPACE = new InternalSpace("releaseStatus");

    public static final ArangoCollectionReference GLOBAL_TYPE_TO_PROPERTY_EDGE_COLLECTION = new ArangoCollectionReference("globaltype2property", true);
    public static final ArangoCollectionReference TYPE_TO_PROPERTY_EDGE_COLLECTION = new ArangoCollectionReference("type2property", true);
    public static final ArangoCollectionReference PROPERTY_TO_TYPE_EDGE_COLLECTION = new ArangoCollectionReference("property2type", true);
    public static final SpaceName TYPES_SPACE = new SpaceName("types");
    public static final SpaceName USERS_SPACE = new SpaceName("users");
    public static final SpaceName USERS_PICTURE_SPACE = new SpaceName("userpictures");

    public static final SpaceName ALTERNATIVES_SPACE = new SpaceName("alternatives");
    public static final ArangoCollectionReference TYPE_EDGE_COLLECTION = new ArangoCollectionReference("internaltype", true);
    public static final InternalSpace TYPE_SPACE = new InternalSpace("types");

    public InternalSpace() {
    }

    public static final Collection<ArangoCollectionReference> INTERNAL_NON_META_EDGES = Arrays.asList(DOCUMENT_ID_EDGE_COLLECTION, ArangoCollectionReference.fromSpace(INFERENCE_OF_SPACE), RELEASE_STATUS_EDGE_COLLECTION, TYPE_EDGE_COLLECTION, ArangoCollectionReference.fromSpace(UNRESOLVED_SPACE));


    public InternalSpace(String name) {
        super(name);
    }

    @Override
    public SpaceName setName(String name) {
        if(name!=null && !name.startsWith("internal")) {
            super.setName("internal" + name);
        }
        else {
            super.setName(name);
        }
        return this;
    }

}


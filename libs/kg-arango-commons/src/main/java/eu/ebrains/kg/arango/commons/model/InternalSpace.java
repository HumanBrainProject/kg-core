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

package eu.ebrains.kg.arango.commons.model;

import eu.ebrains.kg.commons.jsonld.InferredJsonLdDoc;
import eu.ebrains.kg.commons.model.Space;

public class InternalSpace extends Space {

    public static final Space ADMIN = new Space("admin");



    public static final InternalSpace INFERENCE_OF_SPACE = new InternalSpace(InferredJsonLdDoc.INFERENCE_OF);
    public static final InternalSpace UNRESOLVED_SPACE = new InternalSpace("unresolved");
    public static final ArangoCollectionReference PROPERTY_TO_PROPERTY_VALUE_TYPE_EDGE_COLLECTION = new ArangoCollectionReference("property2propertyValueType", true);
    public static final Space PROPERTIES_SPACE = new Space("properties");
    public static final ArangoCollectionReference SPACE_TO_TYPE_EDGE_COLLECTION = new ArangoCollectionReference("space2type", true);
    public static final Space SPACES_SPACE = new Space("spaces");
    public static final Space PROPERTY_VALUE_TYPE_SPACE = new Space("propertyValueTypes");
    public static final ArangoCollectionReference DOCUMENT_RELATION_EDGE_COLLECTION = new ArangoCollectionReference("documentRelation", true);
    public static final Space DOCUMENT_SPACE = new Space("documents");
    public static final ArangoCollectionReference CLIENT_TYPE_PROPERTY_EDGE_COLLECTION = new ArangoCollectionReference("clientTypeProperty", true);
    public static final ArangoCollectionReference CLIENT_TYPE_EDGE_COLLECTION = new ArangoCollectionReference("clientType", true);
    public static final Space CLIENT_SPACE = new Space("clients");
    public static final ArangoCollectionReference DOCUMENT_ID_EDGE_COLLECTION = new ArangoCollectionReference("documentId", true);
    public static final InternalSpace DOCUMENT_ID_SPACE = new InternalSpace("documentIds");
    public static final ArangoCollectionReference RELEASE_STATUS_EDGE_COLLECTION = new ArangoCollectionReference("internalrelease", true);
    public static final InternalSpace RELEASE_STATUS_SPACE = new InternalSpace("releaseStatus");

    public static final ArangoCollectionReference GLOBAL_TYPE_TO_PROPERTY_EDGE_COLLECTION = new ArangoCollectionReference("type2property", true);
    public static final ArangoCollectionReference TYPE_TO_PROPERTY_EDGE_COLLECTION = new ArangoCollectionReference("type2property", true);
    public static final ArangoCollectionReference PROPERTY_TO_TYPE_EDGE_COLLECTION = new ArangoCollectionReference("property2type", true);
    public static final Space TYPES_SPACE = new Space("types");
    public static final Space USERS_SPACE = new Space("users");
    public static final Space ALTERNATIVES_SPACE = new Space("alternatives");

    public InternalSpace() {
    }

    public InternalSpace(String name) {
        super(name);
    }

    @Override
    public void setName(String name) {
        if(name!=null && !name.startsWith("internal")) {
            super.setName("internal" + name);
        }
        else {
            super.setName(name);
        }
    }

}


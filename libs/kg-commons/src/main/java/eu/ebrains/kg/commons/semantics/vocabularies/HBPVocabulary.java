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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.commons.semantics.vocabularies;

public class HBPVocabulary {

    public static final String ALIAS = "hbp";
    public static final String NAMESPACE = "https://schema.hbp.eu/";
    public static final String CLIENT_NAMESPACE = NAMESPACE + "client/";
    public static final String SHORT_NAME = NAMESPACE + "shortName";
    public static final String RELATIVE_URL_OF_INTERNAL_LINK = NAMESPACE + "relativeUrl";

    // FOR LINKING
    public static final String LINKING_INSTANCE_TYPE = NAMESPACE + "LinkingInstance";
    public static final String LINKING_INSTANCE_FROM = LINKING_INSTANCE_TYPE.toLowerCase() + "/from";
    public static final String LINKING_INSTANCE_TO = LINKING_INSTANCE_TYPE.toLowerCase() + "/to";

    // FOR PROVENANCE
    private static final String PROVENANCE = NAMESPACE + "provenance/";
    public static final String PROVENANCE_MODIFIED_AT = PROVENANCE + "modifiedAt";
    public static final String PROVENANCE_CREATED_AT = PROVENANCE + "createdAt";
    public static final String PROVENANCE_LAST_MODIFICATION_USER_ID = PROVENANCE + "lastModificationUserId";
    public static final String PROVENANCE_REVISION = PROVENANCE + "revision";
    public static final String PROVENANCE_IMMEDIATE_INDEX = PROVENANCE + "immediateIndex";
    public static final String PROVENANCE_CREATED_BY = PROVENANCE + "createdBy";

    // FOR RELEASING
    public static final String RELEASE_TYPE = HBPVocabulary.NAMESPACE + "Release";
    public static final String RELEASE_INSTANCE = RELEASE_TYPE.toLowerCase() + "/instance";
    public static final String RELEASE_REVISION = RELEASE_TYPE.toLowerCase() + "/revision";
    public static final String RELEASE_STATE = RELEASE_TYPE.toLowerCase() + "/state";
    public static final String RELEASE_LAST_DATE = RELEASE_TYPE.toLowerCase() + "/lastReleaseAt";
    public static final String RELEASE_FIRST_DATE = RELEASE_TYPE.toLowerCase() + "/firstReleaseAt";
    public static final String RELEASE_LAST_BY = RELEASE_TYPE.toLowerCase() + "/lastReleaseBy";
    public static final String RELEASE_FIRST_BY = RELEASE_TYPE.toLowerCase() + "/firstReleaseBy";

    //FOR INFERENCE
    public final static String INFERENCE_TYPE = HBPVocabulary.NAMESPACE + "Inference";

    /**
     * declares the relationship of e.g. an editor instance which extends another (original) entity
     */
    public final static String INFERENCE_EXTENDS = INFERENCE_TYPE.toLowerCase() + "/extends";
    public final static String INFERENCE_ALTERNATIVES_TYPE = HBPVocabulary.NAMESPACE + "Alternative";
    public final static String INFERENCE_ALTERNATIVES = INFERENCE_TYPE.toLowerCase() + "/alternatives";
    public final static String INFERENCE_ALTERNATIVES_VALUE = INFERENCE_ALTERNATIVES.toLowerCase() + "/value";
    public final static String INFERENCE_ALTERNATIVES_USERIDS = INFERENCE_ALTERNATIVES.toLowerCase() + "/userIds";
    public final static String INFERENCE_ALTERNATIVES_SELECTED = INFERENCE_ALTERNATIVES.toLowerCase() + "/selected";

    //FOR SPATIAL
    public static final String SPATIAL_TYPE = HBPVocabulary.NAMESPACE + "SpatialAnchoring";
    public static final String SPATIAL_NAMESPACE = SPATIAL_TYPE.toLowerCase() + "/";

    public static final String SPATIAL_FORMAT = SPATIAL_NAMESPACE + "format";
    public static final String SPATIAL_COORDINATES = SPATIAL_NAMESPACE + "coordinates";
    public static final String SPATIAL_REFERENCESPACE = SPATIAL_NAMESPACE + "referenceSpace";
    public static final String SPATIAL_LOCATED_INSTANCE = SPATIAL_NAMESPACE + "locatedInstance";

    public static final String SUGGESTION = HBPVocabulary.NAMESPACE + "suggestion";

    public static final String SUGGESTION_OF = SUGGESTION + "/suggestionOf";

    public static final String SUGGESTION_USER_ID = SUGGESTION + "/userId";
    public static final String SUGGESTION_USER = SUGGESTION + "/user";

    public static final String SUGGESTION_STATUS = SUGGESTION + "/status";
    public static final String SUGGESTION_STATUS_CHANGED_BY = SUGGESTION_STATUS + "/updatedBy";


    // FOR UPDATE
    public static final String INTERNAL = HBPVocabulary.NAMESPACE + "internal";
    public static final String INTERNAL_HASHCODE = HBPVocabulary.INTERNAL + "/hashcode";

    // FOR META
    public static final String TYPE = HBPVocabulary.NAMESPACE + "type";
    public static final String TYPES =HBPVocabulary.NAMESPACE + "types";
    public static final String TYPE_FIELDS = HBPVocabulary.TYPES + "/fields";
    public static final String LINKED_TYPES = HBPVocabulary.NAMESPACE + "linkedTypes";
    public static final String NUMBER_OF_OCCURRENCES = HBPVocabulary.NAMESPACE + "numberOfOccurrences";
    public static final String CLIENT_DEFINED_FIELDS = HBPVocabulary.NAMESPACE + "client/types/fields";
}

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

package eu.ebrains.kg.commons.semantics.vocabularies;

import eu.ebrains.kg.commons.jsonld.JsonLdId;

import java.util.Arrays;
import java.util.Objects;

public class EBRAINSVocabulary {
    private EBRAINSVocabulary() {
    }

    public static final String ALIAS = "ebr";
    public static final String NAMESPACE = "https://core.kg.ebrains.eu/vocab/";
    public static final String LABEL = NAMESPACE+"label";

    private static String stripHttp(String name){
        return name.replace("http://", "").replace("https://", "");
    }

    public static JsonLdId createIdForStructureDefinition(String... names){
        StringBuilder sb = new StringBuilder();
        sb.append(NAMESPACE).append("structure");
        Arrays.stream(names).filter(Objects::nonNull).map(EBRAINSVocabulary::stripHttp).forEach(n -> sb.append('/').append(n));
        return new JsonLdId(sb.toString());
    }

    public static final String RESET_VALUE = NAMESPACE+"resetValue";

    public static final String ADDITIONAL_INFO = NAMESPACE+"additionalInformation";

    public static final String META = NAMESPACE+"meta";
    public static final String META_SPACES = META + "spaces";
    public static final String META_USER = META+"user";
    public static final String META_CAN_BE_EXCLUDED_FROM_SCOPE = META+"canBeExcludedFromScope";
    public static final String META_NAME_REVERSE_LINK = META+"nameForReverseLink";
    public static final String META_OCCURRENCES = META + "occurrences";
    public static final String META_INCOMING_LINKS = META + "incomingLinks";
    public static final String META_PROPERTIES = META + "properties";
    public static final String META_PROPERTY = META + "property";
    public static final String META_PROPERTY_SEARCHABLE = META_PROPERTY + "/searchable";
    public static final String META_PICTURE = META + "picture";
    public static final String META_PROPERTY_TARGET_TYPES = META + "targetTypes";
    public static final String META_PROPERTY_SOURCE_TYPES = META + "sourceTypes";
    public static final String META_VALUE_TYPES = META + "valueTypes";
    public static final String META_ALTERNATIVE = META+"alternative";
    public static final String META_SELECTED = META+"selected";
    public static final String META_VALUE = META+"value";
    public static final String META_REVISION = META+"revision";
    public static final String META_TYPE = META+"type";
    public static final String META_TYPES = META+"types";
    public static final String META_TYPE_LABEL_PROPERTY = META_TYPE+"/labelProperty";
    public static final String META_LAST_RELEASED_AT = META+"lastReleasedAt";
    public static final String META_LAST_RELEASED_BY = META+"lastReleasedBy";
    public static final String META_FIRST_RELEASED_AT = META+"firstReleasedAt";
    public static final String META_FIRST_RELEASED_BY = META+"firstReleasedBy";
    public static final String META_SPACE = META + "space";
    public static final String META_INTERNAL_SPACE = META_SPACE + "/internalSpace";
    public static final String META_CLIENT_SPACE = META_SPACE + "/clientSpace";
    public static final String META_AUTORELEASE_SPACE = META_SPACE + "/autorelease";
    public static final String META_DEFER_CACHE_SPACE = META_SPACE + "/deferCache";
    public static final String META_SCOPE_RELEVANT_SPACE = META_SPACE + "/scopeRelevant";
    public static final String META_IGNORE_INCOMING_LINKS=META+"ignoreIncomingLinks";
    public static final String META_FORCED_REMOVAL= META+"forcedRemoval";

    public static final String META_PERMISSIONS = META + "permissions";
    public final static String META_PROPERTYUPDATES = META+"propertyUpdates";

    public static final String META_QUERY_TYPE = META_TYPE+"/Query";


    public static final String META_PROPERTY_IN_TYPE_DEFINITION_TYPE = META+"PropertyInTypeDefinition";


    public final static String META_TYPE_IN_SPACE_DEFINITION_TYPE = META+"TypeInSpaceDefinition";
    public final static String META_SPACEDEFINITION_TYPE = META+"SpaceDefinition";
    public final static String META_TYPEDEFINITION_TYPE = META+"TypeDefinition";
    public static final String META_PROPERTY_DEFINITION_TYPE = META+"PropertyDefinition";

    public final static String META_USER_PICTURE_TYPE = META+"UserPicture";

    public final static String QUERY = NAMESPACE + "query/";

    public final static String QUERY_META = QUERY + "meta";

    public final static String QUERY_STRUCTURE = QUERY + "structure";

    public final static String QUERY_TYPE = QUERY + "type";

    public final static String QUERY_PATH = QUERY + "path";


}

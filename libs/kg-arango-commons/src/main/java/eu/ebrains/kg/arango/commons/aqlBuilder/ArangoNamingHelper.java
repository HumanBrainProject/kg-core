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

package eu.ebrains.kg.arango.commons.aqlBuilder;


import org.apache.commons.codec.digest.DigestUtils;

public class ArangoNamingHelper {


    private static final int MAX_CHARACTERS = 60;
    private static final String VALID_CHARS = "a-z0-9\\-_";

    public static String asArangoKey(String value){
        if(value!=null && value.length()<=MAX_CHARACTERS && value.matches("["+VALID_CHARS+"]*")){
            //Value is valid - no change needed
            return value;
        }
        else if(value!=null){
            return reduceStringToMaxSizeByHashing(replaceSpecialCharacters(removeTrailingHttps(value.toLowerCase())));
        }
        else{
            return null;
        }
    }

    private static String replaceSpecialCharacters(String value) {
        return value != null ? value.replaceAll("\\.", "_").replaceAll("[^"+VALID_CHARS+"]", "-") : null;
    }

    private static String reduceStringToMaxSizeByHashing(String string) {
        return string == null || string.length() <= MAX_CHARACTERS ? string : String.format("#%s", DigestUtils.md5Hex(string));
    }

    private static String removeTrailingHttps(String value){
        return value!=null ? value.toLowerCase().replaceAll("http(s)?://", "") : value;
    }

}

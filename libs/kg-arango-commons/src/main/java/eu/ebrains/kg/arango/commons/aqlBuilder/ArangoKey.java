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

package eu.ebrains.kg.arango.commons.aqlBuilder;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Objects;

/**
 * The arango key is a wrapper which ensures that the passed value is a valid arango key.
 */
public class ArangoKey extends TrustedAqlValue {

    private static final int MAX_CHARACTERS = 60;
    private static final String VALID_CHARS = "a-z0-9_";

    private final String originalValue;

    public ArangoKey(String value) {
        super(asArangoKey(value));
        this.originalValue = value;
    }


    public String getOriginalValue() {
        return originalValue;
    }

    @Override
    public String toString() {
        return getValue();
    }

    private static String asArangoKey(String value){
        if(value!=null && value.length()<=MAX_CHARACTERS && value.matches("["+VALID_CHARS+"]*")){
            //Value is valid - no change needed
            return !value.matches("[a-zA-Z].*") ? "a" + value.toLowerCase() : value.toLowerCase();
        }
        else if(value!=null){
            return reduceStringToMaxSizeByHashing(replaceSpecialCharacters(removeTrailingHttps(value.toLowerCase())));
        }
        else{
            return null;
        }
    }

    private static String replaceSpecialCharacters(String value) {
        return value != null ? value.replaceAll("[^"+VALID_CHARS+"]", "_") : null;
    }

    private static String reduceStringToMaxSizeByHashing(String string) {
        return string == null || string.length() <= MAX_CHARACTERS ? string : String.format("h%s", DigestUtils.md5Hex(string));
    }

    private static String removeTrailingHttps(String value){
        return value!=null ? value.toLowerCase().replaceAll("http(s)?://", "") : value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArangoKey arangoKey = (ArangoKey) o;
        return Objects.equals(originalValue, arangoKey.originalValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalValue);
    }
}

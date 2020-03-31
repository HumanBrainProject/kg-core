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


public class ArangoVocabulary {

    //Arango owned
    public static final String ID = "_id";
    public static final String KEY = "_key";
    public static final String REV = "_rev";

    //General custom
    public static final String COLLECTION = "_collection";

    //Relations
    public static final String FROM = "_from";
    public static final String TO = "_to";
    public static final String ORDER_NUMBER = "_orderNumber";

}

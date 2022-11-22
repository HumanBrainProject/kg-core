/*
 * Copyright 2022 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.test;

public class KGEditor {

    public static final String GIVEN_NAME_IN_PERSON_PROPERY_DEFINITION = """
            {
              "@id": "https://kg.ebrains.eu/testobjects/client/kgeditor/givenNameInPersonProperty",
              "@type": ["https://kg.ebrains.eu/vocab/meta/PropertyInTypeDefinition"],
              "https://kg.ebrains.eu/vocab/meta/property": {"@id":  "http://schema.org/givenName"},
              "https://kg.ebrains.eu/vocab/meta/type": {"@id":  "http://schema.org/Person"},
              "https://kg.ebrains.eu/vocab/meta/property/searchable": true,
              "https://kg.ebrains.eu/vocab/meta/property/widget": "InputText",
              "http://schema.org/name": "Given name in Person for editor"
            }
            """;

    public static final String GIVEN_NAME_PROPERTY_DEFINITION = """
            {
              "@id": "https://kg.ebrains.eu/testobjects/client/kgeditor/givenNameProperty",
              "@type": ["https://kg.ebrains.eu/vocab/meta/PropertyDefinition"],
              "https://kg.ebrains.eu/vocab/meta/property": {"@id": "http://schema.org/givenName"},
              "http://schema.org/name": "Given name for editor"
            }
            """;

    public static final String PERSON_TYPE_DEFINITION = """
            {
              "@id": "https://kg.ebrains.eu/testobjects/client/kgeditor/personType",
              "@type": ["https://kg.ebrains.eu/vocab/meta/TypeDefinition"],
              "https://kg.ebrains.eu/vocab/meta/type": {"@id":  "http://schema.org/Person"},
              "https://kg.ebrains.eu/vocab/meta/visible": true,
              "http://schema.org/name": "Person for editor"
            }
            """;
}

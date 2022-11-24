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

package eu.ebrains.kg.test;

public class GlobalSpecifications {

    private GlobalSpecifications() {
    }

    public static final String CHILDREN_PROPERTY_DEFINITION = """
            {
              "@id": "https://kg.ebrains.eu/testobjects/client/admin/childrenProperty",
              "@type": ["https://kg.ebrains.eu/vocab/meta/PropertyDefinition"],
              "https://kg.ebrains.eu/vocab/meta/property": {"@id": "http://schema.org/children"},
              "https://kg.ebrains.eu/vocab/meta/targetTypes": [
                "http://schema.org/Child"
              ],
              "http://schema.org/name": "Global children"
            }
            """;

    public static final String FAMILY_NAME_PROPERTY_DEFINITION = """
            {
              "@id": "https://kg.ebrains.eu/testobjects/client/admin/familyNameProperty",
              "@type": ["https://kg.ebrains.eu/vocab/meta/PropertyDefinition"],
              "https://kg.ebrains.eu/vocab/meta/property": {"@id": "http://schema.org/familyName"},
              "http://schema.org/name": "Global family name",
              "https://kg.ebrains.eu/vocab/meta/property/label": true
            }
            """;

    public static final String GIVEN_NAME_IN_PERSON_PROPERTY_DEFINITION = """
            {
                        
              "@id": "https://kg.ebrains.eu/testobjects/client/admin/givenNameInPersonProperty",
              "@type": ["https://kg.ebrains.eu/vocab/meta/PropertyInTypeDefinition"],
              "https://kg.ebrains.eu/vocab/meta/property": {"@id":  "http://schema.org/givenName"},
              "https://kg.ebrains.eu/vocab/meta/type": {"@id":  "http://schema.org/Person"},
              "https://kg.ebrains.eu/vocab/meta/property/searchable": true,
              "http://schema.org/name": "Given name of Person in space simpsons"
            }
            """;

    public static final String GIVEN_NAME_PROPERTY_DEFINITION = """
            {
              "@id": "https://kg.ebrains.eu/testobjects/client/admin/givenNameProperty",
              "@type": ["https://kg.ebrains.eu/vocab/meta/PropertyDefinition"],
              "https://kg.ebrains.eu/vocab/meta/property": {"@id": "http://schema.org/givenName"},
              "http://schema.org/name": "Global givenName",
              "https://kg.ebrains.eu/vocab/meta/property/label": true
            }
            """;

    public static final String KG_EDITOR_CLIENT = """
            {
              "@id": "https://kg.ebrains.eu/testobjects/client/kgeditor",
              "@type": ["https://kg.ebrains.eu/vocab/meta/ClientConfiguration"],
              "http://schema.org/identifier": "kgeditor",
              "http://schema.org/name": "KG Editor",
              "http://kg.ebrains.eu/vocab/permissions/synchronous": true
            }
            """;

    public static final String PERSON_TYPE_DEFINITION = """
            {
              "@id": "https://kg.ebrains.eu/testobjects/client/admin/personType",
              "@type": ["https://kg.ebrains.eu/vocab/meta/TypeDefinition"],
              "https://kg.ebrains.eu/vocab/meta/type": {"@id":  "http://schema.org/Person"},
              "http://schema.org/name": "Global Person",
              "https://kg.ebrains.eu/vocab/meta/color": "#B22222"
            }
            """;

    public static final String SCHEMA_ORG_NAME_PROPERTY_DEFINITION = """
            {
              "@id": "https://kg.ebrains.eu/testobjects/client/admin/schemaOrgNameProperty",
              "@type": ["https://kg.ebrains.eu/vocab/meta/PropertyDefinition"],
              "https://kg.ebrains.eu/vocab/meta/property": {"@id": "http://schema.org/name"},
              "https://kg.ebrains.eu/vocab/meta/property/label": true,
              "http://schema.org/name": "Global name"
            }
            """;

}

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

package eu.ebrains.kg.core.api.examples;

public class InstancesExamples {

    private InstancesExamples() {
    }

    public static final String PAYLOAD_MINIMAL = """
            {
              "@type": "https://openminds.ebrains.eu/core/Person"
            }
            """;

    public static final String PAYLOAD_MINIMAL_DESC = "The most minimal payload you could think of only contains an @type.";

    public static final String PAYLOAD_MINIMAL_NAME = "minimalistic";

    public static final String PAYLOAD_WITH_PROPERTY = """
            {
              "@type": "https://openminds.ebrains.eu/core/Person",
              "https://openminds.ebrains.eu/vocab/givenName": "Bob"
            }
            """;

    public static final String PAYLOAD_WITH_PROPERTY_DESC =  "A payload can contain - next to the @type additional properties.";

    public static final String PAYLOAD_WITH_PROPERTY_NAME = "with property";
    public static final String PAYLOAD_WITH_LINK = """
            {
              "@type": "https://openminds.ebrains.eu/core/Person",
              "https://openminds.ebrains.eu/vocab/affiliation": [
                { "@id": "http://someQualifiedIdentifier/ACME_orporation" }
              ]
            }
            """;

    public static final String PAYLOAD_WITH_LINK_DESC = """
            To link to other instances, the JSON-LD notation can be used. Please note that you can use any "@id" or "http://schema.org/identifier" of the targeted resource to link it
            """;

    public static final String PAYLOAD_WITH_LINK_NAME = "with link";
}
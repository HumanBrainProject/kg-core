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

package eu.ebrains.kg.test.simpsons;

public class Things {

    public static final String BIG_BELLY = """
            {
              "http://schema.org/identifier": "http://simpsons/big_belly",
              "@type": [
                "http://www.w3.org/ns/prov#Entity"
              ],
              "http://schema.org/name": "Big belly"
            }
            """;

    public static final String DONUT = """
            {
              "http://schema.org/identifier": "http://simpsons/donut",
              "@type": [
                "http://www.w3.org/ns/prov#Entity"
              ],
              "http://schema.org/name": "Donut",
              "http://schema.org/calories": 452
            }
            """;

}

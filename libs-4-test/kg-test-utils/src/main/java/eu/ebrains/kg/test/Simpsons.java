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

import eu.ebrains.kg.commons.model.SpaceName;

public class Simpsons {

    public static final SpaceName SPACE_NAME = new SpaceName("simpsons");

    public static class Activities {

        public static final String HOMER_EATS_DONUTS = """
            {
              "http://schema.org/identifier": "http://simpsons/homer_eats_donuts",
              "@type": [
                "http://www.w3.org/ns/prov#Activity",
                "https://schema.hbp.eu/Meal"
              ],
              "http://schema.org/name": "Homer ate some donuts",
              "https://schema.hbp.eu/amount": "20",
              "http://www.w3.org/ns/prov#qualifiedAssociation": {
                "@type": "http://www.w3.org/ns/prov#Association",
                "http://www.w3.org/ns/prov#agent": {
                  "@id": "http://simpsons/homer"
                },
                "http://www.w3.org/ns/prov#hadRole": {
                  "@id": "http://schema.hbp.eu/roles/meal/Consumer",
                  "http://schema.org/name": "Consumer"
                },
                "http://www.w3.org/2000/01/rdf-schema#comment": "Homer was super hungry"
              },
              "http://www.w3.org/ns/prov#used": {
                "@id": "http://simpsons/donut"
              },
              "http://www.w3.org/ns/prov#generated": {
                "@id": "http://simpsons/big_belly"
              }
            }
            """;
    }

    public static class Characters {

        public static final String BART = """
            {
              "@type": [
                "http://schema.org/Person",
                "https://thesimpsons.com/FamilyMember"
              ],
              "http://schema.org/identifier": ["http://simpsons/bart", "https://kg.ebrains.eu/api/instances/simpsons/bart"],
              "http://schema.org/familyName": "Simpson",
              "http://schema.org/givenName": "Bart"
            }
            """;

        public static final String BART_2 = """
            {
              "@type": [
                "http://schema.org/Kid"
              ],
              "http://schema.org/identifier": ["http://simpsons/bart2", "http://simpsons/bart"],
              "http://schema.org/age": 10,
              "http://schema.org/givenName": "Bartholomew"
            }
            """;

        public static final String BART_UPDATE = """
            {
              "http://schema.org/identifier": "http://simpsons/bart",
              "@type": [
                "http://schema.org/Person",
                "https://thesimpsons.com/FamilyMember",
                "http://schema.org/Kid"
              ],
              "http://schema.org/familyName": "Simpson",
              "http://schema.org/givenName": "Bart",
              "http://schema.org/sayings": ["¡Ay, caramba!"]
            }
            """;

        public final static String CARL_WITH_EXTERNAL_ID = """
            {
              "http://schema.org/identifier": "http://simpsons/carl",
              "@type": [
                "http://schema.org/Person"
              ],
              "http://schema.org/familyName": "Carlson",
              "http://schema.org/givenName": "Carl",
              "http://schema.org/colleague": {
                "@id": "http://simpsons/homer"
              }
            }
            """;

        public static final String HOMER = """
            {
              "http://schema.org/identifier": "http://simpsons/homer",
              "@type": [
                "http://schema.org/Person",
                "http://schema.org/Man",
                "https://thesimpsons.com/FamilyMember",
                "http://www.w3.org/ns/prov#Person"
              ],
              "http://schema.org/familyName": "Simpson",
              "http://schema.org/givenName": "Homer",
              "http://schema.org/spouse": {
                "@id": "http://simpsons/marge"
              },
              "http://schema.org/children": [
                {
                  "@id": "http://simpsons/bart"
                },
                {
                  "@id": "http://simpsons/lisa"
                },
                {
                  "@id": "http://simpsons/maggie"
                }
              ],
              "http://schema.org/address": {
                  "@type": ["http://schema.org/PostalAddress"],
                  "http://schema.org/streetAddress": "742 Evergreen Terrace",
                  "http://schema.org/addressLocality": "Springfield"
              },
              "http://schema.org/affiliation": {
                "@type": ["http://schema.org/Corporation"],
                "@id": "http://simpsons/corp/powerplant",
                "http://schema.org/name": "Power plant"
              }
                        
            }
            """;

        public static final String MARGE = """
                {
                  "http://schema.org/identifier": "http://simpsons/marge",
                  "@type": [
                    "http://schema.org/Person",
                    "http://schema.org/Woman",
                    "https://thesimpsons.com/FamilyMember"
                  ],
                  "http://schema.org/familyName": "Simpson",
                  "http://schema.org/givenName": "Marge",
                  "http://schema.org/spouse": {
                    "@id": "http://simpsons/homer"
                  },
                  "http://schema.org/children": [
                    {
                      "@id": "http://simpsons/bart"
                    },
                    {
                      "@id": "http://simpsons/lisa"
                    },
                    {
                      "@id": "http://simpsons/maggie"
                    }
                  ]
                }
                """;

        public static final String LISA = """
            {
              "http://schema.org/identifier": "http://simpsons/lisa",
              "@type": [
                "http://schema.org/Person",
                "https://thesimpsons.com/FamilyMember",
                "http://schema.org/Kid"
              ],
              "http://schema.org/familyName": "Simpson",
              "http://schema.org/givenName": "Lisa"
            }
            """;

        public static final String MAGGIE = """
            {
              "http://schema.org/identifier": "http://simpsons/maggie",
              "@type": [
                "http://schema.org/Person",
                "https://thesimpsons.com/FamilyMember",
                "http://schema.org/Kid"
              ],
              "http://schema.org/familyName": "Simpson",
              "http://schema.org/givenName": "Maggie"
            }
            """;

        public static final String MILHOUSE = """
            {
              "@type": [
                "http://schema.org/Person"
              ],
              "http://schema.org/identifier": [
                "http://simpsons/milhouse",
                "http://foobar/milhouse"
              ],
              "http://schema.org/familyName": "van Houten",
              "http://schema.org/givenName": "Milhouse"
            }
            """;


        public static final String MOE_WITH_EMBEDDED_AFFILIATION = """
            {
              "http://schema.org/identifier": "http://simpsons/moe",
              "@type": [
                "http://schema.org/Person"
              ],
              "http://schema.org/familyName": "Szyslak",
              "http://schema.org/givenName": "Moe",
              "http://schema.org/affiliation": {
                "@type": "http://schema.org/Corporation",
                "http://schema.org/name": "Moe's tavern"
              }
            }
            """;
    }

    public static class Things {

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

    public static class Queries {

        public static final String FAMILY_NAMES = """
            {
              "@context": {
                "query": "https://core.kg.ebrains.eu/vocab/query/",
                "schema": "http://schema.org/"
              },
              "query:meta": {
                "query:name": "Simpson Family Members",
                "query:alias": "simpsonsFamilyMembers",
                "query:type": "https://thesimpsons.com/FamilyMember"
              },
              "query:structure": [
                {
                  "query:path": {
                    "@id": "schema:familyName"
                  }
                },
                {
                  "query:path": {
                    "@id": "schema:givenName"
                  }
                }
              ]
            }
            """;

        public static final String HOMER_WITH_EMBEDDED_TRAVERSAL = """
            {
              "https://core.kg.ebrains.eu/vocab/query/meta": {
                "https://core.kg.ebrains.eu/vocab/query/alias": "simpsonsFamilyMembers",
                "https://core.kg.ebrains.eu/vocab/query/name": "Simpson Family Members",
                "https://core.kg.ebrains.eu/vocab/query/type": "https://thesimpsons.com/FamilyMember"
              },
              "https://core.kg.ebrains.eu/vocab/query/structure": [
                {
                  "https://core.kg.ebrains.eu/vocab/query/path": {
                    "@id": "http://schema.org/givenName"
                  }
                },
                {
                  "https://core.kg.ebrains.eu/vocab/query/path": [
                    {
                      "@id": "http://schema.org/address",
                      "https://core.kg.ebrains.eu/vocab/query/typeFilter": [{"@id": "http://schema.org/PostalAddress"}]
                    },
                    {
                      "@id": "http://schema.org/streetAddress"
                    }
                  ],
                  "https://core.kg.ebrains.eu/vocab/query/singleValue": "FIRST",
                  "https://core.kg.ebrains.eu/vocab/query/sort": true
                }
              ]
            }
            """;

        public static final String HOMER_WITH_PARTIALLY_RESOLVED_CHILDREN = """
            {
              "https://core.kg.ebrains.eu/vocab/query/meta": {
                "https://core.kg.ebrains.eu/vocab/query/alias": "simpsonsFamilyMembers",
                "https://core.kg.ebrains.eu/vocab/query/name": "Simpson Family Members",
                "https://core.kg.ebrains.eu/vocab/query/type": "https://thesimpsons.com/FamilyMember"
              },
              "https://core.kg.ebrains.eu/vocab/query/structure": [
                {
                  "https://core.kg.ebrains.eu/vocab/query/path": {
                    "@id": "http://schema.org/givenName"
                  }
                },
                {
                  "https://core.kg.ebrains.eu/vocab/query/propertyName": {"@id":  "http://schema.org/children"},
                  "https://core.kg.ebrains.eu/vocab/query/path": [
                    {
                      "@id": "http://schema.org/children"
                    },
                    {
                      "@id": "http://schema.org/givenName"
                    }
                  ]
                }
              ]
            }
            """;

        public static final String MULTI_LEVEL_QUERY = """
            {
              "https://core.kg.ebrains.eu/vocab/query/meta": {
                "https://core.kg.ebrains.eu/vocab/query/alias": "simpsonsFamilyMembers",
                "https://core.kg.ebrains.eu/vocab/query/name": "Simpson Family Members",
                "https://core.kg.ebrains.eu/vocab/query/type": "https://thesimpsons.com/FamilyMember"
              },
              "https://core.kg.ebrains.eu/vocab/query/structure": [
                {
                  "https://core.kg.ebrains.eu/vocab/query/path": {
                    "@id": "http://schema.org/givenName"
                  }
                },
                {
                  "https://core.kg.ebrains.eu/vocab/query/propertyName": {"@id":  "http://schema.org/parentsOfChildren"},
                  "https://core.kg.ebrains.eu/vocab/query/path": [
                    {
                      "@id": "http://schema.org/children"
                    },
                    {
                      "@id": "http://schema.org/children",
                      "https://core.kg.ebrains.eu/vocab/query/reverse": true
                    },
                    {
                      "@id": "http://schema.org/givenName"
                    }
                  ],
                  "https://core.kg.ebrains.eu/vocab/query/singleValue": "FIRST"
                }
              ]
            }
            """;

        public static final String MULTI_LEVEL_QUERY_NESTED = """
            {
              "https://core.kg.ebrains.eu/vocab/query/meta": {
                "https://core.kg.ebrains.eu/vocab/query/alias": "simpsonsFamilyMembers",
                "https://core.kg.ebrains.eu/vocab/query/name": "Simpson Family Members",
                "https://core.kg.ebrains.eu/vocab/query/type": "https://thesimpsons.com/FamilyMember"
              },
              "https://core.kg.ebrains.eu/vocab/query/structure": [
                {
                  "https://core.kg.ebrains.eu/vocab/query/path": {
                    "@id": "http://schema.org/givenName"
                  }
                },
                {
                  "https://core.kg.ebrains.eu/vocab/query/propertyName": {"@id":  "http://schema.org/children"},
                  "https://core.kg.ebrains.eu/vocab/query/path": [
                    {
                      "@id": "http://schema.org/children"
                    }
                  ],
                  "https://core.kg.ebrains.eu/vocab/query/structure": [
                    {
                      "https://core.kg.ebrains.eu/vocab/query/propertyName": {"@id":  "http://schema.org/parents"},
                      "https://core.kg.ebrains.eu/vocab/query/path": [{
                        "@id": "http://schema.org/children",
                        "https://core.kg.ebrains.eu/vocab/query/reverse": true
                      },
                        {
                          "@id": "http://schema.org/givenName"
                        }
                      ]
                    },
                    {
                      "https://core.kg.ebrains.eu/vocab/query/path": {"@id": "http://schema.org/givenName"}
                    }
                  ]
                }
              ]
            }
            """;

        public static final String MULTI_LEVEL_QUERY_WITH_STATIC_AND_NESTED_TYPE_FILTER = """
            {
              "https://core.kg.ebrains.eu/vocab/query/meta": {
                "https://core.kg.ebrains.eu/vocab/query/alias": "simpsonsFamilyMembers",
                "https://core.kg.ebrains.eu/vocab/query/name": "Simpson Family Members",
                "https://core.kg.ebrains.eu/vocab/query/type": "https://thesimpsons.com/FamilyMember"
              },
              "https://core.kg.ebrains.eu/vocab/query/structure": [
                {
                  "https://core.kg.ebrains.eu/vocab/query/path": {
                    "@id": "http://schema.org/givenName"
                  },
                  "https://core.kg.ebrains.eu/vocab/query/filter": {
                    "https://core.kg.ebrains.eu/vocab/query/op": "EQUALS",
                    "https://core.kg.ebrains.eu/vocab/query/value": "Homer"
                  }
                },
                {
                  "https://core.kg.ebrains.eu/vocab/query/propertyName": {"@id":  "http://schema.org/children"},
                  "https://core.kg.ebrains.eu/vocab/query/path": [
                    {
                      "@id": "http://schema.org/children"
                    }
                  ],
                  "https://core.kg.ebrains.eu/vocab/query/structure": [
                    {
                      "https://core.kg.ebrains.eu/vocab/query/propertyName": {"@id":  "http://schema.org/maleParents"},
                      "https://core.kg.ebrains.eu/vocab/query/path": [{
                        "@id": "http://schema.org/children",
                        "https://core.kg.ebrains.eu/vocab/query/reverse": true,
                        "https://core.kg.ebrains.eu/vocab/query/typeFilter": [{"@id": "http://schema.org/Man"}]
                      },
                        {
                          "@id": "http://schema.org/givenName"
                        }
                      ]
                    },
                    {
                      "https://core.kg.ebrains.eu/vocab/query/path": {"@id": "http://schema.org/givenName"}
                    }
                  ]
                }
              ]
            }
            """;

        public static final String QUERY_DYNAMIC_FILTER = """
            {
              "https://core.kg.ebrains.eu/vocab/query/meta": {
                "https://core.kg.ebrains.eu/vocab/query/alias": "simpsonsFamilyMembers",
                "https://core.kg.ebrains.eu/vocab/query/name": "Simpson Family Members",
                "https://core.kg.ebrains.eu/vocab/query/type": "https://thesimpsons.com/FamilyMember"
              },
              "https://core.kg.ebrains.eu/vocab/query/structure": [
                {
                  "https://core.kg.ebrains.eu/vocab/query/path": {
                    "@id": "http://schema.org/givenName"
                  },
                  "https://core.kg.ebrains.eu/vocab/query/filter": {
                    "https://core.kg.ebrains.eu/vocab/query/op": "EQUALS",
                    "https://core.kg.ebrains.eu/vocab/query/parameter": "givenName"
                  }
                }
              ]
            }
            """;

        public static final String QUERY_DYNAMIC_FILTER_WITH_FALLBACK = """
            {
              "https://core.kg.ebrains.eu/vocab/query/meta": {
                "https://core.kg.ebrains.eu/vocab/query/alias": "simpsonsFamilyMembers",
                "https://core.kg.ebrains.eu/vocab/query/name": "Simpson Family Members",
                "https://core.kg.ebrains.eu/vocab/query/type": "https://thesimpsons.com/FamilyMember"
              },
              "https://core.kg.ebrains.eu/vocab/query/structure": [
                {
                  "https://core.kg.ebrains.eu/vocab/query/path": {
                    "@id": "http://schema.org/givenName"
                  },
                  "https://core.kg.ebrains.eu/vocab/query/filter": {
                    "https://core.kg.ebrains.eu/vocab/query/op": "EQUALS",
                    "https://core.kg.ebrains.eu/vocab/query/parameter": "givenName",
                    "https://core.kg.ebrains.eu/vocab/query/value": "Homer"
                  }
                }
              ]
            }
            """;

        public static final String FAMILY_NAMES_NORMALIZED = """
            {
              "https://core.kg.ebrains.eu/vocab/query/meta": {
                "https://core.kg.ebrains.eu/vocab/query/alias": "simpsonsFamilyMembers",
                "https://core.kg.ebrains.eu/vocab/query/name": "Simpson Family Members",
                "https://core.kg.ebrains.eu/vocab/query/type": "https://thesimpsons.com/FamilyMember"
              },
              "https://core.kg.ebrains.eu/vocab/query/structure": [
                {
                  "https://core.kg.ebrains.eu/vocab/query/path": {
                    "@id": "http://schema.org/familyName"
                  },
                  "https://core.kg.ebrains.eu/vocab/query/singleValue": "FIRST"
                },
                {
                  "https://core.kg.ebrains.eu/vocab/query/path": {
                    "@id": "http://schema.org/givenName"
                  }
                }
              ]
            }
            """;
    }

}

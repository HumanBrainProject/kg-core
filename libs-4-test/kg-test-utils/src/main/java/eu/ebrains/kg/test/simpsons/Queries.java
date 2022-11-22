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

public class Queries {

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

    public static final String MULTI_LEVEL_QUERY_NESTED_WITH_CONCAT = """
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
                      ],
                      "https://core.kg.ebrains.eu/vocab/query/singleValue": "CONCAT",
                      "https://core.kg.ebrains.eu/vocab/query/sort": true
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

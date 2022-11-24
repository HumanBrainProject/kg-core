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
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.graphdb.queries.controller;

import eu.ebrains.kg.arango.commons.aqlbuilder.ArangoKey;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.graphdb.queries.model.spec.Specification;
import eu.ebrains.kg.graphdb.queries.utils.DataQueryBuilder;
import eu.ebrains.kg.test.JsonAdapter4Test;
import eu.ebrains.kg.test.Simpsons;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DataQueryBuilderTest {

    JsonAdapter jsonAdapter = new JsonAdapter4Test();

    @Test
    public void buildSimpsonsFamilyNamesQuery(){
        //Given
        NormalizedJsonLd query = jsonAdapter.fromJson(Simpsons.Queries.FAMILY_NAMES_NORMALIZED, NormalizedJsonLd.class);
        Specification specification = new SpecificationInterpreter().readSpecification(query);

        //When
        AQLQuery aqlQuery = new DataQueryBuilder(specification, null, new HashMap<>(), null, null, Collections.singletonList(ArangoCollectionReference.fromSpace(Simpsons.SPACE_NAME))).build();

        //Then

        String expected = """
                
                LET whitelist=@readAccessBySpace
                LET invitation=@readAccessByInvitation
                FOR root_doc IN 1..1 OUTBOUND DOCUMENT(@@typeCollection, @typeId) @@typeRelation

                FILTER root_doc != NULL
                FILTER root_doc._collection IN whitelist OR HAS(invitation, root_doc._key)
                FILTER TO_ARRAY(@idRestriction) == [] OR root_doc._key IN TO_ARRAY(@idRestriction)
                FILTER root_doc != NULL

                RETURN {
                   "http://schema.org/familyName": root_doc.`http://schema.org/familyName`,\s
                   "http://schema.org/givenName": root_doc.`http://schema.org/givenName`
                }""";
        //Then
        assertEquals(expected, aqlQuery.getAql().build().getValue());
    }

    @Test
    public void buildHomerWithEmbeddedTraversal(){
        //Given
        NormalizedJsonLd query = jsonAdapter.fromJson(Simpsons.Queries.HOMER_WITH_EMBEDDED_TRAVERSAL, NormalizedJsonLd.class);
        Specification specification = new SpecificationInterpreter().readSpecification(query);

        //When
        List<ArangoCollectionReference> existingCollections = Arrays.asList(ArangoCollectionReference.fromSpace(Simpsons.SPACE_NAME), new ArangoCollectionReference(new ArangoKey("http://schema.org/address").getValue(), true));
        AQLQuery aqlQuery = new DataQueryBuilder(specification, null,null,  null, null, existingCollections).build();

        //Then

        String expected = """            
                     
                FOR root_doc IN 1..1 OUTBOUND DOCUMENT(@@typeCollection, @typeId) @@typeRelation
                 
                FILTER TO_ARRAY(@idRestriction) == [] OR root_doc._key IN TO_ARRAY(@idRestriction)
                LET schema_org_streetaddress_1 = FIRST((FOR schema_org_streetaddress_1_sort IN UNIQUE(FLATTEN(FOR schema_org_streetaddress_1_doc\s
                    IN 1..1 OUTBOUND root_doc `schema_org_address`
                    FILTER  "http://schema.org/PostalAddress" IN schema_org_streetaddress_1_doc.`@type`
                FILTER schema_org_streetaddress_1_doc != NULL
                 
                FILTER schema_org_streetaddress_1_doc.`http://schema.org/streetAddress` != NULL
                RETURN DISTINCT schema_org_streetaddress_1_doc.`http://schema.org/streetAddress`
                ))
                 
                   SORT schema_org_streetaddress_1_sort ASC
                   RETURN schema_org_streetaddress_1_sort
                )
                )
                FILTER root_doc != NULL
                SORT schema_org_streetaddress_1
                 ASC
                RETURN {
                   "http://schema.org/givenName": root_doc.`http://schema.org/givenName`,\s
                   "http://schema.org/streetAddress": schema_org_streetaddress_1
                }""";
        //Then
        assertEquals(expected, aqlQuery.getAql().build().getValue());
    }


    @Test
    public void buildHomerWithPartiallyResolvedChildren(){
        //Given
        NormalizedJsonLd query = jsonAdapter.fromJson(Simpsons.Queries.HOMER_WITH_PARTIALLY_RESOLVED_CHILDREN, NormalizedJsonLd.class);
        Specification specification = new SpecificationInterpreter().readSpecification(query);

        //When
        List<ArangoCollectionReference> existingCollections = Arrays.asList(ArangoCollectionReference.fromSpace(Simpsons.SPACE_NAME), new ArangoCollectionReference(new ArangoKey("http://schema.org/children").getValue(), true));
        AQLQuery aqlQuery = new DataQueryBuilder(specification, null, null, null, null, existingCollections).build();

        //Then
        String expected = """

                FOR root_doc IN 1..1 OUTBOUND DOCUMENT(@@typeCollection, @typeId) @@typeRelation

                FILTER TO_ARRAY(@idRestriction) == [] OR root_doc._key IN TO_ARRAY(@idRestriction)
                LET schema_org_children_1 = UNIQUE(FLATTEN(FOR schema_org_children_1_doc\s
                    IN 1..1 OUTBOUND root_doc `schema_org_children`
                FILTER schema_org_children_1_doc != NULL

                FILTER schema_org_children_1_doc.`http://schema.org/givenName` != NULL
                RETURN DISTINCT schema_org_children_1_doc.`http://schema.org/givenName`
                ))
                FILTER root_doc != NULL

                RETURN {
                   "http://schema.org/givenName": root_doc.`http://schema.org/givenName`,\s
                   "http://schema.org/children": schema_org_children_1
                }""";
        //Then
        assertEquals(expected, aqlQuery.getAql().build().getValue());
    }



    @Test
    public void buildHomerWithEmbeddedTraversalMissingTraversalCollection(){
        //Given
        NormalizedJsonLd query = jsonAdapter.fromJson(Simpsons.Queries.HOMER_WITH_EMBEDDED_TRAVERSAL, NormalizedJsonLd.class);
        Specification specification = new SpecificationInterpreter().readSpecification(query);

        //When
        List<ArangoCollectionReference> existingCollections = Collections.singletonList(ArangoCollectionReference.fromSpace(Simpsons.SPACE_NAME));
        AQLQuery aqlQuery = new DataQueryBuilder(specification, null,null,  null, null, existingCollections).build();

        //Then

        String expected = """

                FOR root_doc IN 1..1 OUTBOUND DOCUMENT(@@typeCollection, @typeId) @@typeRelation

                FILTER TO_ARRAY(@idRestriction) == [] OR root_doc._key IN TO_ARRAY(@idRestriction)
                LET schema_org_streetaddress_1 = FIRST((FOR schema_org_streetaddress_1_sort IN UNIQUE(FLATTEN(FOR schema_org_streetaddress_1_doc\s
                    IN []\s
                    FILTER  "http://schema.org/PostalAddress" IN schema_org_streetaddress_1_doc.`@type`
                FILTER schema_org_streetaddress_1_doc != NULL

                FILTER schema_org_streetaddress_1_doc.`http://schema.org/streetAddress` != NULL
                RETURN DISTINCT schema_org_streetaddress_1_doc.`http://schema.org/streetAddress`
                ))

                   SORT schema_org_streetaddress_1_sort ASC
                   RETURN schema_org_streetaddress_1_sort
                )
                )
                FILTER root_doc != NULL
                SORT schema_org_streetaddress_1
                 ASC
                RETURN {
                   "http://schema.org/givenName": root_doc.`http://schema.org/givenName`,\s
                   "http://schema.org/streetAddress": schema_org_streetaddress_1
                }""";
        //Then
        assertEquals(expected, aqlQuery.getAql().build().getValue());
    }

}
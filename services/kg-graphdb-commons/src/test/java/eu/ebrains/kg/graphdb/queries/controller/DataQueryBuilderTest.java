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

import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoKey;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.graphdb.queries.model.spec.Specification;
import eu.ebrains.kg.graphdb.queries.utils.DataQueryBuilder;
import eu.ebrains.kg.test.JsonAdapter4Test;
import eu.ebrains.kg.test.TestObjectFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Ignore //TODO reevaluate if the tests shall be kept
public class DataQueryBuilderTest {

    @Test
    public void buildSimpsonsFamilyNamesQuery(){
        //Given
        NormalizedJsonLd query = TestObjectFactory.createJsonLd(TestObjectFactory.SIMPSONS, "normalizedQueries/simpsonsFamilyNames.json");
        Specification specification = new SpecificationInterpreter().readSpecification(query);

        //When
        AQLQuery aqlQuery = new DataQueryBuilder(specification, null, new HashMap<>(), null, null, Collections.singletonList(ArangoCollectionReference.fromSpace(TestObjectFactory.SIMPSONS))).build();

        //Then

        String expected = "\n" +
                "LET whitelist=@readAccessBySpace\n" +
                "LET invitation=@readAccessByInvitation\n" +
                "FOR root_doc IN 1..1 OUTBOUND DOCUMENT(@@typeCollection, @typeId) @@typeRelation\n" +
                "\n" +
                "FILTER root_doc != NULL\n" +
                "FILTER root_doc._collection IN whitelist OR root_doc.`@id` IN invitation\n" +
                "FILTER @idRestriction == [] OR root_doc._key IN @idRestriction\n" +
                "FILTER root_doc != NULL\n" +
                "\n" +
                "RETURN {\n" +
                "   \"http://schema.org/familyName\": root_doc.`http://schema.org/familyName`, \n" +
                "   \"http://schema.org/givenName\": root_doc.`http://schema.org/givenName`\n" +
                "}";
        //Then
        Assert.assertEquals(expected, aqlQuery.getAql().build().getValue());
    }

    @Test
    public void buildHomerWithEmbeddedTraversal(){
        //Given
        NormalizedJsonLd query = TestObjectFactory.createJsonLd(TestObjectFactory.SIMPSONS, "normalizedQueries/homerWithEmbeddedTraversal.json");
        Specification specification = new SpecificationInterpreter().readSpecification(query);

        //When
        List<ArangoCollectionReference> existingCollections = Arrays.asList(ArangoCollectionReference.fromSpace(TestObjectFactory.SIMPSONS), new ArangoCollectionReference(new ArangoKey("http://schema.org/address").getValue(), true));
        AQLQuery aqlQuery = new DataQueryBuilder(specification, null,null,  null, null, existingCollections).build();

        //Then

        String expected = "\n" +
                "FOR root_doc IN 1..1 OUTBOUND DOCUMENT(@@typeCollection, @typeId) @@typeRelation\n" +
                "\n" +
                "FILTER root_doc != NULL\n" +
                "FILTER root_doc._collection IN whitelist OR root_doc.`@id` IN invitation\n" +
                "FILTER @idRestriction == [] OR root_doc._key IN @idRestriction\n" +
                "LET schema_org_streetaddress = (FOR schema_org_streetaddress_sort IN UNIQUE(FLATTEN(FOR schema_org_streetaddress_doc \n" +
                "    IN 1..1 OUTBOUND root_doc `schema_org_address`\n" +
                "      FILTER schema_org_streetaddress_doc != NULL\n" +
                "       FILTER  \"http://schema.org/PostalAddress\" IN schema_org_streetaddress_doc.`@type`\n" +
                "FILTER schema_org_streetaddress_doc != NULL\n" +
                "\n" +
                "FILTER schema_org_streetaddress_doc.`http://schema.org/streetAddress` != NULL\n" +
                "RETURN DISTINCT schema_org_streetaddress_doc.`http://schema.org/streetAddress`\n" +
                "))\n" +
                "\n" +
                "   SORT schema_org_streetaddress_sort ASC\n" +
                "   RETURN schema_org_streetaddress_sort\n" +
                ")\n" +
                "FILTER root_doc != NULL\n" +
                "SORT schema_org_streetaddress\n" +
                " ASC\n" +
                "RETURN {\n" +
                "   \"http://schema.org/givenName\": root_doc.`http://schema.org/givenName`, \n" +
                "   \"http://schema.org/streetAddress\": schema_org_streetaddress\n" +
                "}";
        //Then
        Assert.assertEquals(expected, aqlQuery.getAql().build().getValue());
    }


    @Test
    public void buildHomerWithPartiallyResolvedChildren(){
        //Given
        NormalizedJsonLd query = TestObjectFactory.createJsonLd(TestObjectFactory.SIMPSONS, "normalizedQueries/homerWithPartiallyResolvedChildren.json");
        Specification specification = new SpecificationInterpreter().readSpecification(query);

        //When
        List<ArangoCollectionReference> existingCollections = Arrays.asList(ArangoCollectionReference.fromSpace(TestObjectFactory.SIMPSONS), new ArangoCollectionReference(new ArangoKey("http://schema.org/children").getValue(), true));
        AQLQuery aqlQuery = new DataQueryBuilder(specification, null, null, null, null, existingCollections).build();

        //Then
        String expected = "\n" +
                "FOR root_doc IN 1..1 OUTBOUND DOCUMENT(@@typeCollection, @typeId) @@typeRelation\n" +
                "\n" +
                "FILTER root_doc != NULL\n" +
                "FILTER @idRestriction == [] OR root_doc._key IN @idRestriction\n" +
                "FILTER root_doc != NULL\n" +
                "\n" +
                "RETURN {\n" +
                "   \"http://schema.org/familyName\": root_doc.`http://schema.org/familyName`, \n" +
                "   \"http://schema.org/givenName\": root_doc.`http://schema.org/givenName`\n" +
                "}";
        //Then
        Assert.assertEquals(expected, aqlQuery.getAql().build().getValue());
    }



    @Test
    public void buildHomerWithEmbeddedTraversalMissingTraversalCollection(){
        //Given
        NormalizedJsonLd query = TestObjectFactory.createJsonLd(TestObjectFactory.SIMPSONS, "normalizedQueries/homerWithEmbeddedTraversal.json");
        Specification specification = new SpecificationInterpreter().readSpecification(query);

        //When
        List<ArangoCollectionReference> existingCollections = Collections.singletonList(ArangoCollectionReference.fromSpace(TestObjectFactory.SIMPSONS));
        AQLQuery aqlQuery = new DataQueryBuilder(specification, null,null,  null, null, existingCollections).build();

        //Then

        String expected = "\n" +
                "FOR root_doc IN 1..1 OUTBOUND DOCUMENT(@@typeCollection, @typeId) @@typeRelation\n" +
                "\n" +
                "FILTER @idRestriction == [] OR root_doc._key IN @idRestriction\n" +
                "LET schema_org_streetaddress = FIRST((FOR schema_org_streetaddress_sort IN UNIQUE(FLATTEN(FOR schema_org_streetaddress_doc \n" +
                "    IN [] \n" +
                "    FILTER  \"http://schema.org/PostalAddress\" IN schema_org_streetaddress_doc.`@type`\n" +
                "FILTER schema_org_streetaddress_doc != NULL\n" +
                "\n" +
                "FILTER schema_org_streetaddress_doc.`http://schema.org/streetAddress` != NULL\n" +
                "RETURN DISTINCT schema_org_streetaddress_doc.`http://schema.org/streetAddress`\n" +
                "))\n" +
                "\n" +
                "   SORT schema_org_streetaddress_sort ASC\n" +
                "   RETURN schema_org_streetaddress_sort\n" +
                ")\n" +
                ")\n" +
                "FILTER root_doc != NULL\n" +
                "SORT schema_org_streetaddress\n" +
                " ASC\n" +
                "RETURN {\n" +
                "   \"http://schema.org/givenName\": root_doc.`http://schema.org/givenName`, \n" +
                "   \"http://schema.org/streetAddress\": schema_org_streetaddress\n" +
                "}";
        //Then
        Assert.assertEquals(expected, aqlQuery.getAql().build().getValue());
    }

    @Test
    public void build() throws URISyntaxException, IOException {
        Path path = Paths.get(Objects.requireNonNull(getClass().getClassLoader()
                .getResource("query.json")).toURI());
        Stream<String> lines = Files.lines(path);
        String data = lines.collect(Collectors.joining("\n"));
        lines.close();

        SpecificationInterpreter interpreter = new SpecificationInterpreter();
        Specification specification = interpreter.readSpecification(new JsonAdapter4Test().fromJson(data, NormalizedJsonLd.class));
        List<ArangoCollectionReference> existingCollections = new ArrayList<>();
        existingCollections.add(new ArangoCollectionReference("docs", false));
        PaginationParam pagination = new PaginationParam();
        pagination.setSize(10L);

        DataQueryBuilder builder = new DataQueryBuilder(specification, pagination, null, null, null, existingCollections);

        AQLQuery aql = builder.build();

        System.out.println(aql.getAql());


    }
}
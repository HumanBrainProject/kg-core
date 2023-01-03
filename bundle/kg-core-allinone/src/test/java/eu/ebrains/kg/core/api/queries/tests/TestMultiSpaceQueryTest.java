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

package eu.ebrains.kg.core.api.queries.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.PaginatedStreamResult;
import eu.ebrains.kg.core.api.AbstractTest;
import eu.ebrains.kg.core.api.instances.TestContext;
import eu.ebrains.kg.core.api.v3.InstancesV3;
import eu.ebrains.kg.core.api.v3.QueriesV3;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.TestDataFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("java:S2187") //We don't add "tests" to these classes because they are test abstractions and are used in other tests
public class TestMultiSpaceQueryTest extends AbstractTest {

    private final QueriesV3 queries;
    private final InstancesV3 instances;
    public NormalizedJsonLd instanceA;
    public NormalizedJsonLd instanceB;
    public ExposedStage stage;

    public JsonLdDoc query;

    private final List<String> restrictToSpaces;

    public PaginatedStreamResult<? extends Map<?,?>> response;

    public String testQuery = """ 
                                {
                                "@context": {
                                     "query": "https://core.kg.ebrains.eu/vocab/query/",
                                     "schema": "http://schema.org/"
                                },
                                "query:meta": {
                                    "query:name": "Test Query",
                                    "query:alias": "testQuery",
                                    "query:type": "$testType"
                                },
                                "query:structure": [
                                    {
                                        "query:path": [
                                            { "@id": "schema:name" }
                                        ]
                                    }
                                ]
                                }""".replace("$testType", TestDataFactory.TEST_TYPE);




    public TestMultiSpaceQueryTest(TestContext testContext, QueriesV3 queries, InstancesV3 instances, ExposedStage stage, List<String> restrictToSpaces)  throws IOException
    {
        super(testContext);
        this.instances = instances;
        this.queries = queries;
        this.query = new JsonLdDoc(new ObjectMapper().readValue(this.testQuery, LinkedHashMap.class));
        this.stage = stage;
        this.restrictToSpaces = restrictToSpaces;
    }

    private NormalizedJsonLd createTestInstance(JsonLdDoc doc, String space){
        return instances.createNewInstance(doc, space, defaultResponseConfiguration).getBody().getData();
    }

    @Override
    protected void setup() {
        JsonLdDoc docA = TestDataFactory.createTestData(smallPayload, "a", true);
        JsonLdDoc docB = TestDataFactory.createTestData(smallPayload, "b", true);
        instanceA = createTestInstance(docA, "a");
        instanceB = createTestInstance(docB, "b");
    }

    @Override
    protected void run() {
         response = queries.runDynamicQuery(this.query, defaultPaginationParam, stage, null, restrictToSpaces, new HashMap<>());
    }
}

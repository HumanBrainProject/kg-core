/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.core.api.queries.tests;

import com.arangodb.ArangoDB;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ebrains.kg.authentication.api.AuthenticationAPI;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.PaginatedResult;
import eu.ebrains.kg.commons.permission.roles.Role;
import eu.ebrains.kg.core.api.AbstractTest;
import eu.ebrains.kg.core.api.Instances;
import eu.ebrains.kg.core.api.Queries;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.TestDataFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestSimpleQueryTest extends AbstractTest {

    private final Queries queries;
    private final Instances instances;
    private final IdUtils idUtils;
    private final boolean release;
    public NormalizedJsonLd instanceA;
    public NormalizedJsonLd instanceArelated;
    public NormalizedJsonLd instanceB;
    public ExposedStage stage;

    public String relationToArelated = "http://test/aRelated";
    public String relationToB = "http://test/b";

    public JsonLdDoc query;

    public PaginatedResult<? extends Map<?,?>> response;
    public final String nameOfRoot = "http://test/nameOfRoot";
    public final String nameOfARel = "http://test/nameOfArelated";
    public final String nameOfB = "http://test/nameOfB";

    public String testQuery = "{\n" +
            "  \"@context\": {\n" +
            "    \"query\": \"https://core.kg.ebrains.eu/vocab/query/\",\n" +
            "    \"schema\": \"http://schema.org/\"\n" +
            "  },\n" +
            "  \"query:meta\": {\n" +
            "    \"query:name\": \"Test Query\",\n" +
            "    \"query:alias\": \"testQuery\",\n" +
            "    \"query:type\": \""+TestDataFactory.TEST_TYPE+"\"\n" +
            "  },\n" +
            "  \"query:structure\": [\n" +
            "    {\n" +
            "      \"query:propertyName\": {\"@id\":  \""+nameOfRoot+"\"},\n"+
            "      \"query:path\": {\n" +
            "        \"@id\": \"schema:name\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"query:propertyName\": {\"@id\":  \""+nameOfARel+"\"},\n"+
            "      \"query:singleValue\": \"FIRST\",\n" +
            "      \"query:path\": [" +
            "      {\n" +
            "        \"@id\": \""+relationToArelated+"\"\n" +
            "      },\n" +
            "      {\n" +
            "\"@id\": \"schema:name\"\n" +
            "      }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"query:propertyName\": {\"@id\":  \""+nameOfB+"\"},\n"+
            "      \"query:singleValue\": \"FIRST\",\n" +
            "      \"query:path\": [" +
            "      {\n" +
            "        \"@id\": \""+relationToB+"\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"@id\": \"schema:name\"\n" +
            "      }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";


    public TestSimpleQueryTest(ArangoDB.Builder database, AuthenticationAPI authenticationAPI, Collection<List<Role>> roles, Queries queries, Instances instances, ExposedStage stage, IdUtils idUtils, boolean release)  throws IOException
    {
        super(database, authenticationAPI,  roles);
        this.instances = instances;
        this.queries = queries;
        this.query = new JsonLdDoc(new ObjectMapper().readValue(this.testQuery, LinkedHashMap.class));
        this.stage = stage;
        this.idUtils = idUtils;
        this.release = release;
    }

    private NormalizedJsonLd createTestInstance(JsonLdDoc doc, String space){
        return instances.createNewInstance(doc, space, defaultResponseConfiguration, defaultIngestConfiguration, null).getBody().getData();
    }

    @Override
    protected void setup() {
        JsonLdDoc docB = TestDataFactory.createTestData(smallPayload, "b", true);
        instanceB = createTestInstance(docB, "b");
        JsonLdDoc docArelated = TestDataFactory.createTestData(smallPayload, "aRelated", true);
        instanceArelated = createTestInstance(docArelated, "a");
        JsonLdDoc docA = TestDataFactory.createTestData(smallPayload, "a", true);
        docA.put(relationToArelated, instanceArelated.id());
        docA.put(relationToB, instanceB.id());
        instanceA = createTestInstance(docA, "a");
        if(release) {
            instances.releaseInstance(idUtils.getUUID(instanceA.id()), null);
            instances.releaseInstance(idUtils.getUUID(instanceB.id()), null);
            instances.releaseInstance(idUtils.getUUID(instanceArelated.id()), null);
        }
    }

    @Override
    protected void run() {
        response = queries.testQuery(this.query, defaultPaginationParam, stage);
    }
}

/*
 * Copyright 2020 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.testutils;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.KgCoreAllInOne;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.model.IngestConfiguration;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.ResponseConfiguration;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
@RunWith(SpringRunner.class)
@SpringBootTest(classes = KgCoreAllInOne.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {"eu.ebrains.kg.core.metadata.synchronous=true", "eu.ebrains.kg.test=true", "opentracing.jaeger.enabled=false", "arangodb.connections.max=1"})
public abstract class AbstractSystemTest {

    protected PaginationParam EMPTY_PAGINATION = new PaginationParam();
    protected ResponseConfiguration DEFAULT_RESPONSE_CONFIG = new ResponseConfiguration();
    protected IngestConfiguration DEFAULT_INGEST_CONFIG = new IngestConfiguration().setNormalizePayload(false);


    protected static final int smallPayload = 5;
    protected static final int averagePayload = 25;
    protected static final int bigPayload = 100;

    protected String type = "https://core.kg.ebrains.eu/TestPayload";

    @MockBean
    protected AuthContext authContext;

    @Autowired
    @Qualifier("arangoBuilderForGraphDB")
    protected ArangoDB.Builder database;

    @Autowired
    protected IdUtils idUtils;

}

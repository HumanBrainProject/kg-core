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

package eu.ebrains.kg.systemTest.config;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArangoConfiguration {

    @Bean
    public ArangoDB.Builder produceArangoDB(
            @Value("${eu.ebrains.kg.arango.host}") String host,
            @Value("${eu.ebrains.kg.arango.port}") Integer port,
            @Value("${eu.ebrains.kg.arango.user}") String user,
            @Value("${eu.ebrains.kg.arango.pwd}") String pwd, @Value("${arangodb.timeout:}")
                    Integer timeout, @Value("${arangodb.connections.max:}")
                    Integer maxConnections) {
        ArangoDB.Builder builder = new ArangoDB.Builder().host(host, port).user(user).password(pwd);
        if (timeout != null) {
            builder.timeout(timeout);
        }
        if (maxConnections != null) {
            builder.maxConnections(maxConnections);
        }
        return builder;
    }

    @Bean
    @Qualifier("events")
    public ArangoDatabaseProxy produceEventsDb(ArangoDB.Builder arangoDB) {
        return new ArangoDatabaseProxy(arangoDB.build(), "kg1-events");
    }

    @Bean
    @Qualifier("liveMeta")
    public ArangoDatabaseProxy produceLiveMetaDB(ArangoDB.Builder arangoDB) {
        return new ArangoDatabaseProxy(arangoDB.build(), "kg1-live-meta");
    }

    @Bean
    @Qualifier("releasedMeta")
    public ArangoDatabaseProxy produceReleaseMetaDb(ArangoDB.Builder arangoDB) {
        return new ArangoDatabaseProxy(arangoDB.build(), "kg1-released-meta");
    }


}

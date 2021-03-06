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

package eu.ebrains.kg.graphdb.commons.controller;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphDBArangoConfiguration {

    @Bean
    @Qualifier("arangoBuilderForGraphDB")
    public ArangoDB.Builder produceGraphDBArangoDB(
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
    @Qualifier("native")
    public ArangoDatabaseProxy produceDefaultDb(@Qualifier("arangoBuilderForGraphDB") ArangoDB.Builder arangoDB) {
        return new ArangoDatabaseProxy(arangoDB.build(), "kg1-native");
    }

    @Bean
    @Qualifier("inProgress")
    public ArangoDatabaseProxy produceInProgressDB(@Qualifier("arangoBuilderForGraphDB") ArangoDB.Builder arangoDB) {
        return new ArangoDatabaseProxy(arangoDB.build(), "kg1-inProgress");
    }

    @Bean
    @Qualifier("released")
    public ArangoDatabaseProxy produceReleasedDb(@Qualifier("arangoBuilderForGraphDB") ArangoDB.Builder arangoDB) {
        return new ArangoDatabaseProxy(arangoDB.build(), "kg1-release");
    }

    @Bean
    @Qualifier("nativeMeta")
    public ArangoDatabaseProxy produceDefaultMetaDb(@Qualifier("arangoBuilderForGraphDB") ArangoDB.Builder arangoDB) {
        return new ArangoDatabaseProxy(arangoDB.build(), "kg1-native-meta");
    }

    @Bean
    @Qualifier("inProgressMeta")
    public ArangoDatabaseProxy produceInProgressMetaDB(@Qualifier("arangoBuilderForGraphDB") ArangoDB.Builder arangoDB) {
        return new ArangoDatabaseProxy(arangoDB.build(), "kg1-inProgress-meta");
    }

    @Bean
    @Qualifier("releasedMeta")
    public ArangoDatabaseProxy produceReleaseMetaDb(@Qualifier("arangoBuilderForGraphDB") ArangoDB.Builder arangoDB) {
        return new ArangoDatabaseProxy(arangoDB.build(), "kg1-released-meta");
    }


}

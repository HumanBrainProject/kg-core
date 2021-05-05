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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.commons.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * The two different webclient beans: once with eureka loadbalancing (internal service calls) and once without it - for external/stable endpoints
 */
@EnableEurekaClient
@Configuration
public class WebClientConf {

    private ExchangeStrategies createExchangeStrategy(ObjectMapper objectMapper){
        return ExchangeStrategies.builder()
                .codecs(config -> {
                    ClientCodecConfigurer.ClientDefaultCodecs clientDefaultCodecs = config.defaultCodecs();
                    clientDefaultCodecs.maxInMemorySize(16 * 1024 * 1024);
                    clientDefaultCodecs.jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                    clientDefaultCodecs.jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
                }).build();
    }

    @Bean
    @Qualifier("loadbalanced")
    @LoadBalanced
    WebClient.Builder webClient(ObjectMapper objectMapper) {
        return WebClient.builder().exchangeStrategies(createExchangeStrategy(objectMapper));
    }

    @Bean
    @Qualifier("direct")
    WebClient.Builder webClientExternal(ObjectMapper objectMapper) {
        return WebClient.builder().exchangeStrategies(createExchangeStrategy(objectMapper));
    }

}

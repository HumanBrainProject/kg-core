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

package eu.ebrains.kg.commons.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerExchangeFilterFunction;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * The two different webclient beans: once with eureka loadbalancing (internal service calls) and once without it - for external/stable endpoints
 */
@EnableEurekaClient
@Configuration
public class WebClientConf {
    @Bean
    @Qualifier("loadbalanced")
    WebClient.Builder webClient(LoadBalancerClient loadBalancerClient) {
        return WebClient.builder().filter(new LoadBalancerExchangeFilterFunction(loadBalancerClient));
    }

    @Bean
    @Qualifier("direct")
    WebClient.Builder webClientExternal() {
        return WebClient.builder();
    }

}

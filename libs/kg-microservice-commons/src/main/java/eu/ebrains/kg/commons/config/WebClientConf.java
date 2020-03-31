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


@EnableEurekaClient
@Configuration
public class WebClientConf {
    @Bean
    @Qualifier("lb")
    WebClient.Builder webClient(LoadBalancerClient loadBalancerClient) {
        return WebClient.builder().filter(new LoadBalancerExchangeFilterFunction(loadBalancerClient));
    }

    @Bean
    @Qualifier("sse")
    WebClient.Builder webClientSSE(LoadBalancerClient loadBalancerClient) {
        return WebClient.builder().filter(new LoadBalancerExchangeFilterFunction(loadBalancerClient));
    }

    @Bean
    @Qualifier("sse-external")
    WebClient.Builder webClientSSEExternal(LoadBalancerClient loadBalancerClient) {
        return WebClient.builder();
    }

    @Bean
    @Qualifier("external")
    WebClient.Builder webClientExternal() {
        return WebClient.builder();
    }

    @Bean
    @Qualifier("internal")
    WebClient.Builder internalWebClient(){
        return WebClient.builder();
    }



}

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

@Component
public class StickyExchangeFilterFunction implements ExchangeFilterFunction {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DiscoveryClient discoveryClient;
    private final LoadBalancerClient loadBalancerClient;

    public StickyExchangeFilterFunction(LoadBalancerClient loadBalancerClient, DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.loadBalancerClient = loadBalancerClient;
    }

    private ServiceInstance chooseServiceInstance(String serviceId) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        if (instances != null && !instances.isEmpty()) {
            instances.sort((ServiceInstance s1, ServiceInstance s2) -> {
                String sortCriteriaS1 = s1 != null ? s1.getHost() + ":" + s1.getPort() : null;
                String sortCriteriaS2 = s2 != null ? s2.getHost() + ":" + s2.getPort() : null;
                return sortCriteriaS1 == null ? sortCriteriaS2 == null ? 0 : -1 : sortCriteriaS2 == null ? 1 : sortCriteriaS1.compareTo(sortCriteriaS2);
            });
            return instances.get(0);
        }
        return null;
    }


    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        URI originalUrl = request.url();
        String serviceId = originalUrl.getHost();
        if (serviceId == null) {
            String msg = String.format(
                    "Request URI does not contain a valid hostname: %s",
                    originalUrl.toString());
            logger.warn(msg);
            return Mono.just(
                    ClientResponse.create(HttpStatus.BAD_REQUEST).body(msg).build());
        }
        ServiceInstance instance = chooseServiceInstance(serviceId);
        if (instance == null) {
            String msg = String.format(
                    "Load balancer does not contain an instance for the service %s",
                    serviceId);
            logger.warn(msg);
            return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(msg).build());
        }
        URI uri = this.loadBalancerClient.reconstructURI(instance, originalUrl);
        ClientRequest newRequest = ClientRequest.method(request.method(), uri)
                .headers(headers -> headers.addAll(request.headers()))
                .cookies(cookies -> cookies.addAll(request.cookies()))
                .attributes(attributes -> attributes.putAll(request.attributes()))
                .body(request.body()).build();
        return next.exchange(newRequest);
    }

}



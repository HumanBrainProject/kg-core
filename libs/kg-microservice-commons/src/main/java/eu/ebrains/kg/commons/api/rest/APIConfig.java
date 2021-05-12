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

package eu.ebrains.kg.commons.api.rest;

import eu.ebrains.kg.commons.AuthTokenContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.api.GraphDBDocuments;
import eu.ebrains.kg.commons.api.Ids;
import eu.ebrains.kg.commons.api.Indexing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class APIConfig {

    @Bean
    @ConditionalOnMissingBean
    public Ids.Client createIdsRestClient(ServiceCall serviceCall, AuthTokenContext authTokenContext){
        return new IdsRestClient(serviceCall, authTokenContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public Authentication.Client createAuthenticationRestClient(ServiceCall serviceCall, AuthTokenContext authTokenContext){
        return new AuthenticationRestClient(serviceCall, authTokenContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public Indexing.Client createIndexingRestClient(ServiceCall serviceCall, AuthTokenContext authTokenContext){
        return new IndexingRestClient(serviceCall, authTokenContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public GraphDBDocuments.Client createGraphDBDocumentsRestClient(ServiceCall serviceCall, AuthTokenContext authTokenContext){
        return new GraphDBDocumentsRestClient(serviceCall, authTokenContext);
    }

}

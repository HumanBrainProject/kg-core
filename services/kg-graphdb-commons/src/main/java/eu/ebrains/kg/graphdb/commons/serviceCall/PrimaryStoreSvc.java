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

package eu.ebrains.kg.graphdb.commons.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.SSEService;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.models.EventStoreSvc;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

@Component("main-event-store")
public class PrimaryStoreSvc  implements EventStoreSvc {

    @Autowired
    ServiceCall serviceCall;

    @Autowired
    AuthContext authContext;


    @Autowired
    SSEService sseService;

    private static final String BASE_URL = "http://kg-primarystore/events";

    public Flux<ServerSentEvent> connectToSSE(String lastEventId, ClientAuthToken authToken){
        return sseService.sse(BASE_URL+"/stream", lastEventId,  null);
    }

    public List<PersistedEvent> getEventsSince(String lastEventId){
        String url = BASE_URL;
        if(lastEventId!=null){
            url = String.format("%s?lastEventId=%s", url, lastEventId);
        }
        return Arrays.asList(serviceCall.get(url, authContext.getAuthTokens(), PersistedEvent[].class));
    }

}

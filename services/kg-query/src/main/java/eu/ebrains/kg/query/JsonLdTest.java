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

package eu.ebrains.kg.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RestController()
@RequestMapping("/test")
public class JsonLdTest {

    @Autowired
    WebClient.Builder webClient;

    @GetMapping
    public void translatePayload(){
        JsonLd ld = new JsonLd();
        JsonLdContext context = new JsonLdContext();
        context.setVocab("http://schema.hbp.eu/");
        ld.setContext(context);
        ld.put("foo", "bar");
        String result = webClient.build().post().uri("http://kg-jsonld/internal/").contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromObject(ld)).retrieve().bodyToMono(String.class).block();
        System.out.println(result);
    }

}

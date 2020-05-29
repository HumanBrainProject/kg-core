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

package eu.ebrains.kg.jsonld.controller;

import com.google.gson.Gson;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Component
public class JsonLDJS {

    private final Context context;
    private final Gson gson;

    public JsonLDJS(Gson gson) throws URISyntaxException, IOException {
        this.gson = gson;
        String library = Files.readString(Paths.get(getClass().getClassLoader().getResource("jsonld.min.js").toURI()));
        this.context = Context.newBuilder("js").allowAllAccess(true).out(System.out).build();
        this.context.eval("js", "const window={};");
        this.context.eval("js", library);
    }

    public CompletableFuture<NormalizedJsonLd> normalize(JsonLdDoc payload) {
        CompletableFuture<NormalizedJsonLd> future = new CompletableFuture<>();
        context.getBindings("js").putMember("payload", gson.toJson(payload));
        Value jsPromise = context.eval("js", "new Promise(r => window.jsonld.expand(JSON.parse(payload)).then(exp => window.jsonld.compact(exp, {}).then(comp => r(JSON.stringify(comp)))));");
        Consumer<Object> javaThen = (value)-> future.complete(gson.fromJson(value.toString(), NormalizedJsonLd.class));
        jsPromise.invokeMember("then", javaThen);
        return future;
    }
}

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
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Component
public class JsonLDJS {

    private final Gson gson;
    private final String library;

    private Resource resources;

    public JsonLDJS(Gson gson, @org.springframework.beans.factory.annotation.Value("classpath:jsonld.min.js") Resource libraryResource) throws URISyntaxException, IOException {
        this.gson = gson;
        try (Reader reader = new InputStreamReader(libraryResource.getInputStream(), StandardCharsets.UTF_8)) {
            library = FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public CompletableFuture<NormalizedJsonLd> normalize(JsonLdDoc payload) {
        CompletableFuture<NormalizedJsonLd> future = new CompletableFuture<>();
        Context context = Context.newBuilder("js").allowAllAccess(true).out(System.out).build();
        context.eval("js", "const window={};");
        context.eval("js", this.library);
        context.getBindings("js").putMember("payload", gson.toJson(payload));
        Value jsPromise = context.eval("js", "new Promise(r => window.jsonld.expand(JSON.parse(payload)).then(exp => window.jsonld.compact(exp, {}).then(comp => r(JSON.stringify(comp)))));");
        Consumer<Object> javaThen = (value)-> future.complete(gson.fromJson(value.toString(), NormalizedJsonLd.class));
        jsPromise.invokeMember("then", javaThen);
        return future;
    }
}

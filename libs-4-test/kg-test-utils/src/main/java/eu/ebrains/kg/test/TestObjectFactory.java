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

package eu.ebrains.kg.test;

import com.google.gson.Gson;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Space;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class TestObjectFactory {
    private static final Gson GSON = new Gson();

    public static final Space SIMPSONS = new Space("simpsons");
    public static final Space ADMIN = new Space("admin");
    public static final Space KGEDITOR = new Space("kgeditor");

    public static NormalizedJsonLd overrideId(NormalizedJsonLd jsonLd, JsonLdId id) {
        jsonLd.addIdentifiers(jsonLd.getId().getId());
        jsonLd.setId(id);
        return jsonLd;
    }

    public static NormalizedJsonLd createJsonLdWithInternalId(Space space, String jsonFile, JsonLdId id) {
        NormalizedJsonLd jsonLd = createJsonLd(space, jsonFile);
        return overrideId(jsonLd, id);
    }

    public static NormalizedJsonLd createJsonLd(Space space, String jsonFile) {
        return createJsonLd(space.getName()+"/"+jsonFile);
    }

    public static NormalizedJsonLd createJsonLd(String jsonFile, JsonLdId id){
        NormalizedJsonLd jsonLd = createJsonLd(jsonFile);
        jsonLd.setId(id);
        return jsonLd;
    }

    public static NormalizedJsonLd createJsonLd(String jsonFile) {
        try {
            Path path = Paths.get(TestObjectFactory.class.getClassLoader().getResource("test/" + jsonFile).toURI());
            String json = Files.lines(path).collect(Collectors.joining("\n"));
            NormalizedJsonLd jsonLd = GSON.fromJson(json, NormalizedJsonLd.class);
            if(jsonLd.getId()!=null) {
                jsonLd.addIdentifiers(jsonLd.getId().getId());
            }
            return jsonLd;
        }
        catch (IOException | URISyntaxException e){
            throw new RuntimeException(e);
        }
    }

}

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

package eu.ebrains.kg.test;


import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.SpaceName;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class TestObjectFactory {
    private static final JsonAdapter JSON = new JsonAdapter4Test();

    public static final SpaceName SIMPSONS = new SpaceName("simpsons");
    public static final SpaceName ADMIN = new SpaceName("admin");
    public static final SpaceName KGEDITOR = new SpaceName("kgeditor");




    public static NormalizedJsonLd overrideId(NormalizedJsonLd jsonLd, JsonLdId id) {
        jsonLd.addIdentifiers(jsonLd.id().getId());
        jsonLd.setId(id);
        return jsonLd;
    }

    public static NormalizedJsonLd createJsonLdWithInternalId(SpaceName space, String jsonFile, JsonLdId id) {
        NormalizedJsonLd jsonLd = createJsonLd(space, jsonFile);
        return overrideId(jsonLd, id);
    }

    public static NormalizedJsonLd createJsonLd(SpaceName space, String jsonFile) {
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
            NormalizedJsonLd jsonLd = JSON.fromJson(json, NormalizedJsonLd.class);
            if(jsonLd.id()!=null) {
                jsonLd.addIdentifiers(jsonLd.id().getId());
            }
            return jsonLd;
        }
        catch (IOException | URISyntaxException e){
            throw new RuntimeException(e);
        }
    }

}

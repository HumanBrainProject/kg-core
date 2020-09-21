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

package eu.ebrains.kg.nexusv0.controller;

import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.nexusv0.serviceCall.CoreSvc;
import eu.ebrains.kg.test.JsonAdapter4Test;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

public class PayloadNormalizerTest {

    PayloadNormalizer payloadNormalizer;

    @Before
    public void setup(){
    }


    @Test
    public void spaceFromUrl(){
        //The nexus endpoint is explicitly set differently than the url endpoint - this allows us to do cross-environment data loads.
        Space spaceFromUrl = new PayloadNormalizer(Mockito.mock(CoreSvc.class), new JsonAdapter4Test()).getSpaceFromUrl("https://nexus.humanbrainproject.org/v0/data/cscs/core/file/v1.0.0/fc0a5034-2aba-4b55-94c8-78e92538ecc9");
        Assert.assertEquals(new Space("cscs").getName(), spaceFromUrl.getName());
    }

    @Test
    public void normalizePayload(){
        String json = "{\"@id\": \"https://nexus-dev.humanbrainproject.org/v0/data/minds/core/dataset/v1.0.0/foobar\", \"foo\": {\"@id\": \"https://nexus-dev.humanbrainproject.org/v0/data/mindseditor/core/dataset/v1.0.0/d59eff08-2369-4bef-8ddf-fa9e083a85aa\"}, \"bar\": {\"@id\": \"https://nexus-dev.humanbrainproject.org/v0/data/mindseditor/core/dataset/v1.0.0/d59eff08-2369-4bef-8ddf-fa9e083a85aa\"}}";
        JsonLdDoc map = new JsonAdapter4Test().fromJson(json, JsonLdDoc.class);
        CoreSvc mock = Mockito.mock(CoreSvc.class);
        Mockito.doReturn(new NormalizedJsonLd(map)).when(mock).toNormalizedJsonLd(Mockito.any());
        payloadNormalizer = new PayloadNormalizer(mock, new JsonAdapter4Test());
        payloadNormalizer.normalizePayload(map, "minds", "core", "dataset", "v1.0.0", "foobar", "https://nexus.humanbrainproject.org/v0/");
        Assert.assertEquals("https://nexus.humanbrainproject.org/v0/data/minds/core/dataset/v1.0.0/foobar",map.get("@id"));
        Assert.assertEquals("https://nexus-dev.humanbrainproject.org/v0/data/mindseditor/core/dataset/v1.0.0/d59eff08-2369-4bef-8ddf-fa9e083a85aa",((Map)map.get("foo")).get("@id"));
    }

    @Test
    public void normalizePayloadWithSuffix(){
        String json = "{\"@id\": \"https://nexus-dev.humanbrainproject.org/v0/data/mindseditor/core/dataset/v1.0.0/foobar\", \"foo\": {\"@id\": \"https://nexus-dev.humanbrainproject.org/v0/data/mindseditor/core/dataset/v1.0.0/d59eff08-2369-4bef-8ddf-fa9e083a85aa\"}, \"bar\": {\"@id\": \"https://nexus-dev.humanbrainproject.org/v0/data/mindseditor/core/dataset/v1.0.0/d59eff08-2369-4bef-8ddf-fa9e083a85aa\"}}";
        JsonLdDoc map = new JsonAdapter4Test().fromJson(json, JsonLdDoc.class);
        CoreSvc mock = Mockito.mock(CoreSvc.class);
        Mockito.doReturn(new NormalizedJsonLd(map)).when(mock).toNormalizedJsonLd(Mockito.any());
        payloadNormalizer = new PayloadNormalizer(mock, new JsonAdapter4Test());
        NormalizedJsonLd normalizedJsonLd = payloadNormalizer.normalizePayload(map, "mindseditor", "core", "dataset", "v1.0.0", "foobar", "https://nexus.humanbrainproject.org/v0/");

        Assert.assertEquals("https://nexus.humanbrainproject.org/v0/data/minds/core/dataset/v1.0.0/foobar",normalizedJsonLd.get("@id"));
        Assert.assertEquals("https://nexus-dev.humanbrainproject.org/v0/data/mindseditor/core/dataset/v1.0.0/d59eff08-2369-4bef-8ddf-fa9e083a85aa",((Map)normalizedJsonLd.get("foo")).get("@id"));
    }

    @Test
    public void normalizeIfSuffixed(){
        Space space = PayloadNormalizer.normalizeIfSuffixed(new Space("mindseditor"));
        Assert.assertEquals(new Space("minds"), space);
    }

}
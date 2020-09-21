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

import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.nexusv0.serviceCall.CoreSvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PayloadNormalizer {

    private final CoreSvc coreSvc;
    private final JsonAdapter json;

    private static final String[] removeSpaceEnding = new String[]{"editor", "editorsug", "inferred"};

    public static boolean isInferred(String organization){
        return organization.endsWith("inferred");
    }

    public PayloadNormalizer(CoreSvc coreSvc, JsonAdapter json) {
        this.coreSvc = coreSvc;
        this.json = json;
    }


    private final Logger logger = LoggerFactory.getLogger(getClass());

    public NormalizedJsonLd normalizePayload(JsonLdDoc payload, String organization, String domain, String schema, String schemaVersion, String id, String nexusEndpoint) {
        logger.debug(String.format("Payload received and going to normalize: \n%s", json.toJson(payload)));
        NormalizedJsonLd normalizedJsonLdDoc = coreSvc.toNormalizedJsonLd(payload);
        //set original nexus id to payload, so it can be handled appropriately.
        String absoluteNexusUrl = getAbsoluteNexusUrl(organization, domain, schema, schemaVersion, id, true, nexusEndpoint);
        //Save original id as identifier (for back-reference) if there is one
        if (normalizedJsonLdDoc.id() != null) {
            normalizedJsonLdDoc.addIdentifiers(normalizedJsonLdDoc.id().getId());
        }
        normalizedJsonLdDoc.put(JsonLdConsts.ID, absoluteNexusUrl);
        //If the payload contains "extends" links, we want to use them as identifiers too.
        List<JsonLdId> extendsLinks = normalizedJsonLdDoc.getAsListOf("https://schema.hbp.eu/inference/extends", JsonLdId.class);
        extendsLinks.stream().map(JsonLdId::getId).forEach(normalizedJsonLdDoc::addIdentifiers);

        //Clear nexus internal keys
        Set<String> toBeRemoved = normalizedJsonLdDoc.keySet().stream().filter(k -> k.startsWith("https://schema.hbp.eu/provenance/") || k.startsWith("https://schema.hbp.eu/inference/")).collect(Collectors.toSet());
        toBeRemoved.forEach(normalizedJsonLdDoc::remove);

        String absoluteNexusUrlWithoutSuffixRemoval = getAbsoluteNexusUrl(organization, domain, schema, schemaVersion, id, false, nexusEndpoint);
        if(!absoluteNexusUrl.equals(absoluteNexusUrlWithoutSuffixRemoval)){
            //this is a suffixed space - let's keep the original identifier as a reference
            normalizedJsonLdDoc.addIdentifiers(absoluteNexusUrlWithoutSuffixRemoval);
        }
        return normalizedJsonLdDoc;
    }

    public static Space normalizeIfSuffixed(Space space) {
        for (String s : removeSpaceEnding) {
            if (space.getName().endsWith(s)) {
                space.setName(space.getName().substring(0, space.getName().length() - s.length()));
            }
        }
        return space;
    }

    public String getAbsoluteNexusUrl(String organization, String domain, String schema, String version, String id, boolean removeSuffix, String nexusEndpoint) {
        return nexusEndpoint + String.format("data/%s/%s/%s/%s/%s", removeSuffix ? normalizeIfSuffixed(new Space(organization)).getName() : organization, domain, schema, version, id);
    }

    public String getRelativeUrl(String absoluteUrl, String nexusEndpoint){
        if(absoluteUrl.startsWith(nexusEndpoint)){
            String withoutEndpoint = absoluteUrl.substring(nexusEndpoint.length());
            if(withoutEndpoint.startsWith("data/")){
                withoutEndpoint = withoutEndpoint.substring("data/".length());
            }
            return withoutEndpoint;
        }
        return null;
    }

    public String translateToCurrentEndpoint(String absoluteUrl, String nexusEndpoint){
        int i = absoluteUrl.indexOf("/v0/data/");
        if(i>-1){
            return nexusEndpoint+"data/"+absoluteUrl.substring(i + "/v0/data/".length());
        }
        return absoluteUrl;
    }

    public Space getSpaceFromUrl(String absoluteUrl){
        int i = absoluteUrl.indexOf("/v0/data/");
        if(i>-1){
            String relative = absoluteUrl.substring(i + "/v0/data/".length());
            String[] split = relative.split("/");
            if(split.length>0){
                return normalizeIfSuffixed(new Space(split[0]));
            }
        }
        return null;
    }



}

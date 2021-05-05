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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.systemTest.serviceCall;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.Tuple;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SystemTestToCore {

    //FIXME fix this implementation to make use of the proper clients.
    private final IdUtils idUtils;
    private final static String SERVICE_URL = "http://kg-core-api/" + Version.API;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public SystemTestToCore(IdUtils idUtils) {
        this.idUtils = idUtils;
    }


    public Result<NormalizedJsonLd> createInstance(JsonLdDoc payload, SpaceName space, String user, ZonedDateTime dateTime, boolean returnPayload, boolean deferInference) {
        //return serviceCallWithClientSecret.post(String.format("%s/instances?space=%s&returnPayload=%b&returnEmbedded=false&externalUserDefinition=%s&externalEventTime=%s&deferInference=%b", SERVICE_URL, space.getName(), returnPayload, user!=null ? user : "", dateTime!=null ? dateTime.format(DateTimeFormatter.ISO_INSTANT) : "", deferInference), payload, new AuthTokens(), ResultOfDocument.class);
        return null;
    }

    public Result<NormalizedJsonLd> replaceContribution(JsonLdDoc payload, UUID uuid, String user, ZonedDateTime dateTime, boolean returnPayload, boolean deferInference) {
        // return serviceCallWithClientSecret.patch(String.format("%s/instances/%s?removeNonDeclaredProperties=true&returnPayload=%b&returnEmbedded=false&externalUserDefinition=%s&externalEventTime=%s&undeprecate=true&deferInference=%b", SERVICE_URL, uuid, returnPayload, user!=null ? user : "", dateTime!=null ? dateTime.format(DateTimeFormatter.ISO_INSTANT) : "", deferInference), payload, new AuthTokens(),  ResultOfDocument.class);
        return null;
    }

    public Map<Type, Map<String, Long>> getTypesFilteredByOccurences(DataStage stage, Integer from, Integer to) {
//        ResultOfDocuments resultOfDocuments = serviceCallWithClientSecret.get(String.format("%s/types?stage=%s&withProperties=true", SERVICE_URL, stage.name()), new AuthTokens(), ResultOfDocuments.class);
//        return resultOfDocuments.getData().stream().filter(d -> {
//            logger.debug(String.format("Investigating properties in type %s", d.getAs(SchemaOrgVocabulary.IDENTIFIER, String.class)));
//            Double occurrences = d.getAs(EBRAINSVocabulary.META_OCCURRENCES, Double.class);
//            List<?> properties = d.getAs(EBRAINSVocabulary.META_PROPERTIES, List.class);
//            return occurrences > from && (to ==null || occurrences < to) && properties.size() > 0;
//        }).collect(Collectors.toMap(
//                el -> new Type(el.getAs(SchemaOrgVocabulary.IDENTIFIER, String.class)),
//                el -> {
//                    Map<String, Long> properties = new HashMap<>();
//                    el.getAsListOf(EBRAINSVocabulary.META_PROPERTIES, Map.class).forEach(property -> {
//                        Double occurrences = (Double) property.get(EBRAINSVocabulary.META_OCCURRENCES);
//                        properties.put((String) property.get(SchemaOrgVocabulary.IDENTIFIER),  occurrences.longValue());
//                    });
//                    return properties;
//                }
//        ));
        return null;
    }

    public Map<Type, Map<String, Long>> getTypesFilteredByOccurencesByType(DataStage stage, List<String> listOfTypeNames) {
//        NormalizedJsonLd typesByName = serviceCallWithClientSecret.post(String.format("%s/typesByName?stage=%s&withProperties=true", SERVICE_URL, stage.name()), listOfTypeNames, new AuthTokens(),  NormalizedJsonLd.class);
//        Map<String, Map<String, ?>> data = (Map<String, Map<String, ?>>) typesByName.get("data");
//        Map<Type, Map<String, Long>> result = new HashMap<>();
//        for (Map.Entry<String, Map<String, ?>> entry : data.entrySet()) {
//            Map d = (Map) entry.getValue().get("data");
//            Map<String, Long> properties = new HashMap<>();
//            if(d != null) {
//                List<Map<String, ?>> pr = (List<Map<String,?>>) d.get(EBRAINSVocabulary.META_PROPERTIES);
//                pr.forEach(property -> {
//                    Double occurrences = (Double) property.get(EBRAINSVocabulary.META_OCCURRENCES);
//                    properties.put((String) property.get(SchemaOrgVocabulary.IDENTIFIER),  occurrences.longValue());
//                });
//                result.put(new Type(entry.getKey()), properties);
//            }
//        }
//        return  result;
        return null;
    }


    public List<Tuple<Type, Long>> getTypes(DataStage stage) {
//        ResultOfDocuments resultOfDocuments = serviceCallWithClientSecret.get(String.format("%s/types?stage=%s", SERVICE_URL, stage.name()), new AuthTokens(),  ResultOfDocuments.class);
//        return resultOfDocuments.getData().stream().map(d -> {
//            String name = d.getAs(SchemaOrgVocabulary.IDENTIFIER, String.class);
//            Double occurrences = d.getAs(EBRAINSVocabulary.META_OCCURRENCES, Double.class);
//            return new Tuple<Type, Long>().setA(new Type(name)).setB(occurrences.longValue());
//        }).collect(Collectors.toList());
        return null;
    }


    public PaginatedResultOfDocuments getInstances(Type type, DataStage stage) {
//        return serviceCallWithClientSecret.get(String.format("%s/instances?type=%s&stage=%s", SERVICE_URL, type.getEncodedName(), stage.name()), new AuthTokens(),  PaginatedResultOfDocuments.class);
        return null;
    }

    public PaginatedResultOfDocuments getInstances(Type type, int size, int from, DataStage stage) {
//        return serviceCallWithClientSecret.get(String.format("%s/instances?type=%s&size=%d&from=%d&stage=%s", SERVICE_URL, type.getEncodedName(), size, from, stage.name()), new AuthTokens(),  PaginatedResultOfDocuments.class);
        return null;
    }

    public NormalizedJsonLd getInstanceById(UUID uuid, DataStage stage) {
//        ResultOfDocument resultOfDocument = serviceCallWithClientSecret.get(String.format("%s/instances/%s?stage=%s", SERVICE_URL, uuid, stage.name()), new AuthTokens(), ResultOfDocument.class);
//        return resultOfDocument != null ? resultOfDocument.getData() : null;
        return null;
    }

    public void inferInstance(SpaceName space, UUID uuid, boolean async) {
//        serviceCallWithClientSecret.post(String.format("%s/inference/%s?identifier=%s&async=%b", SERVICE_URL, space.getName(), idUtils.buildAbsoluteUrl(uuid).getId(), async), null, new AuthTokens(), String.class);
    }
}

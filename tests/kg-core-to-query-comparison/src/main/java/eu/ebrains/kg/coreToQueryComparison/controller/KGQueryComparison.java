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

package eu.ebrains.kg.coreToQueryComparison.controller;

import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.Type;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.coreToQueryComparison.model.ComparisonResult;
import eu.ebrains.kg.coreToQueryComparison.model.NexusSchema;
import eu.ebrains.kg.coreToQueryComparison.serviceCall.CoreSvc;
import eu.ebrains.kg.coreToQueryComparison.serviceCall.OldQuerySvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Writer;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KGQueryComparison {

    private final CoreSvc coreSvc;
    private final OldQuerySvc oldQuerySvc;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public KGQueryComparison(CoreSvc coreSvc, OldQuerySvc oldQuerySvc) {
        this.coreSvc = coreSvc;
        this.oldQuerySvc = oldQuerySvc;
    }

    public List<ComparisonResult<?>> compareTypeWithSchema(ZonedDateTime ignoreCreatedInstancesAfter, Type type, NexusSchema nexusSchema, Integer size, String oidcToken, boolean failingOnly, boolean showInvolvedPayloads, boolean showIds, String space, Writer simpleWriter) throws IOException {
        List<NormalizedJsonLd> instancesFromQueryAPI = this.oldQuerySvc.getInstances(DataStage.LIVE, nexusSchema, size, oidcToken);
        logger.debug(String.format("Found %d instances in query api", instancesFromQueryAPI.size()));
        List<NormalizedJsonLd> instancesFromQueryAPIwithType = instancesFromQueryAPI.stream().filter(i -> i.getTypes().contains(type.getName())).collect(Collectors.toList());
        logger.debug(String.format("Found %d instances in query which do not contain the requested type", instancesFromQueryAPI.size() - instancesFromQueryAPIwithType.size()));
        List<NormalizedJsonLd> instancesTimeRestricted = instancesFromQueryAPIwithType.stream().filter(i -> {
            String createdAt = i.getAs("https://schema.hbp.eu/minds/created_at", String.class);
            if (createdAt == null) {
                createdAt = i.getAs("https://schema.hbp.eu/provenance/createdAt", String.class);
            }
            return createdAt == null || ZonedDateTime.parse(createdAt).isBefore(ignoreCreatedInstancesAfter);
        }).collect(Collectors.toList());
        logger.info(String.format("Found %d instances within the requested timespan", instancesTimeRestricted.size()));
        List<ComparisonResult<?>> results = new ArrayList<>();
        Map<String, NormalizedJsonLd> instancesById = instancesTimeRestricted.stream().collect(Collectors.toMap(e -> e.getId().getId(), e -> e));
        Set<String> idsNotInCore = new HashSet<>();
        Set<String> oneEquivalent = new HashSet<>();
        Set<String> multiCoreRepresentations = new HashSet<>();
        Set<String> handledCoreIds = new HashSet<>();
        Set<String> coreIdsUnifyingMultipleQueryInstances = new HashSet<>();
        Set<String> differentPayload = new HashSet<>();
        Set<String> differentPayloadButUpdatedAfterwards = new HashSet<>();
        int count = 1;
        Set<String> ids = instancesById.keySet();
        for (String id : ids) {
            if (count % 100 == 0) {
                logger.info(String.format("%d of %d instances of type %s processed", count, ids.size(), type.getName()));
            }
            count++;
            Result<List<NormalizedJsonLd>> fromCore = this.coreSvc.getInstancesByIdentifier(id);
            logger.debug(String.format("Found %d instances in core corresponding to to the id %s", fromCore.getData().size(), id));
            List<NormalizedJsonLd> payloadsFromCore = fromCore.getData().stream().filter(Objects::nonNull).collect(Collectors.toList());
            switch (payloadsFromCore.size()) {
                case 0:
                    idsNotInCore.add(id);
                    break;
                case 1:
                    oneEquivalent.add(id);
                    ComparisonResult<NormalizedJsonLd> result = new ComparisonResult<>();
                    if (showInvolvedPayloads) {
                        result.setExpectedValue(payloadsFromCore.get(0));
                        result.setActualValue(instancesById.get(id));
                    }
                    Set<String> failingProperties = compareCoreAndQueryPayload(payloadsFromCore.get(0), instancesById.get(id));
                    result.setCorrect(failingProperties.isEmpty());
                    Map<String, Set<String>> extraInfo = new HashMap<>();
                    extraInfo.put("failingProperties", failingProperties);
                    result.setExtraInformation(extraInfo);
                    result.setMessage(String.format("Instance differs for %s", id));
                    if (!failingOnly || !result.isCorrect()) {
                        results.add(result);
                    }
                    if (!result.isCorrect()) {
                        differentPayload.add(id);
                        ZonedDateTime modifiedAt = ZonedDateTime.parse(instancesById.get(id).getAs("https://schema.hbp.eu/provenance/modifiedAt", String.class));
                        if (modifiedAt.isAfter(ignoreCreatedInstancesAfter)) {
                            differentPayloadButUpdatedAfterwards.add(id);
                        }

                    }
                    break;
                default:
                    multiCoreRepresentations.add(id);
            }
            coreIdsUnifyingMultipleQueryInstances.addAll(payloadsFromCore.stream().map(r -> r.getId().getId()).filter(handledCoreIds::contains).collect(Collectors.toSet()));
            handledCoreIds.addAll(payloadsFromCore.stream().map(r -> r.getId().getId()).collect(Collectors.toSet()));
        }
        results.add(createListComparison("Instances in query but not in core", idsNotInCore, 0, failingOnly, showIds));
        //results.add(createListComparison("Instances in query having one equivalent in core", oneEquivalent, instancesById.size(), failingOnly));
        results.add(createListComparison("Instances in query having multiple representations in core", multiCoreRepresentations, 0, failingOnly, showIds));
        results.add(createListComparison("Instances in core unifying multiple query instances", coreIdsUnifyingMultipleQueryInstances, 0, failingOnly, showIds));
        results.add(createListComparison("Instances with different payloads", differentPayload, 0, failingOnly, showIds));
        Set<String> unmodifiedDifferentPayloads = new HashSet<>(differentPayload);
        unmodifiedDifferentPayloads.removeAll(differentPayloadButUpdatedAfterwards);
        results.add(createListComparison("Instances with different payloads without those which have been updated after requested time", unmodifiedDifferentPayloads, 0, failingOnly, showIds));

        Set<String> allCoreIds = coreSvc.getInstances(type, DataStage.LIVE).getData().stream().filter(i -> space == null || space.equals(i.getAs(EBRAINSVocabulary.META_SPACE, String.class))).map(i -> i.getId().getId()).collect(Collectors.toSet());
        if (simpleWriter != null) {
            simpleWriter.write(String.format(",%d", instancesFromQueryAPI.size()));
            simpleWriter.write(String.format(",%d", instancesTimeRestricted.size()));
            simpleWriter.write(String.format(",%d", allCoreIds.size()));
            simpleWriter.write(String.format(",%d", idsNotInCore.size()));
            simpleWriter.write(String.format(",%d", multiCoreRepresentations.size()));
            simpleWriter.write(String.format(",%d", coreIdsUnifyingMultipleQueryInstances.size()));
            simpleWriter.write(String.format(",%d", differentPayload.size()));
            simpleWriter.write(String.format(",%d", unmodifiedDifferentPayloads.size()));
        }
        allCoreIds.removeAll(handledCoreIds);
        if (size == null) {
            results.add(createListComparison("Ids in core which have not been handled", allCoreIds, 0, failingOnly, showIds));
            if(simpleWriter!=null) {
                simpleWriter.write(String.format(",%d", allCoreIds.size()));
            }
        } else if(simpleWriter!=null){
            simpleWriter.write(String.format(",%d", allCoreIds.size()));
        }
        return results.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private ComparisonResult<Integer> createListComparison(String label, Set<String> ids, int expectedCount, boolean failingOnly, boolean showIds) {
        ComparisonResult<Integer> result = new ComparisonResult<>();
        result.setMessage(label);
        result.setExpectedValue(expectedCount);
        result.setActualValue(ids.size());
        if (showIds) {
            result.setExtraInformation(idMap("ids", ids));
        }
        result.setCorrect(result.getExpectedValue() == result.getActualValue());
        return !failingOnly || !result.isCorrect() ? result : null;
    }

    private boolean ignoreKey(String key) {
        if (key.startsWith("https://schema.hbp.eu/provenance/")) {
            return true;
        }
        switch (key) {
            case "https://schema.hbp.eu/inference/alternatives":
            case "https://schema.hbp.eu/relativeUrl":
                return true;
        }
        return false;
    }


    private void compareProperties(NormalizedJsonLd corePayload, NormalizedJsonLd queryPayload, boolean embedded, Set<String> failingProperties, String path) {

        for (String key : queryPayload.keySet()) {
            if (!embedded && JsonLdConsts.ID.equals(key)) {
                //the id has to be part of the identifiers

                if (!corePayload.getIdentifiers().contains(queryPayload.get(key))) {
                    logger.debug(String.format("The core instance doesn't contain the identifier %s", queryPayload.get(key)));
                    failingProperties.add(path + "/" + key);
                }
            } else if (JsonLdConsts.TYPE.equals(key)) {
                //We ignore type declarations in embedded instances
                if (!embedded) {
                    //All types have to be part of the core instance (maybe there is more)
                    HashSet<String> types = new HashSet<>(queryPayload.getAsListOf(key, String.class));
                    types.remove("https://schema.hbp.eu/Inference");
                    types.removeAll(corePayload.getTypes());
                    if (types.size() > 0) {
                        logger.debug(String.format("The core instance doesn't contain all types. Missing: %s", String.join(", ", types)));
                        failingProperties.add(path + "/" + key);
                    }
                }
            } else if (SchemaOrgVocabulary.IDENTIFIER.equals(key) && !embedded) {
                HashSet<String> identifiers = new HashSet<>(queryPayload.getAsListOf(key, String.class));
                identifiers.removeAll(corePayload.getIdentifiers());
                if (identifiers.size() > 0) {
                    logger.warn(String.format("The core instance doesn't contain all identifiers. Missing: %s", String.join(", ", identifiers)));
                    failingProperties.add(path + "/" + key);
                }
            } else if (!ignoreKey(key)) {
                List<Object> fromQuery = queryPayload.getAsListOf(key, Object.class);
                List<Object> fromCore = corePayload.getAsListOf(key, Object.class);
                if (fromQuery.size() != fromCore.size()) {
                    logger.debug(String.format("The property %s has not the same number of values: \n\n Core: \n%d\n\n Query: %d", key, fromCore.size(), fromQuery.size()));
                    failingProperties.add(path + "/" + key);
                } else {
                    for (int i = 0; i < fromQuery.size(); i++) {
                        Object fromQueryElement = fromQuery.get(i);
                        Object fromCoreElement = fromCore.get(i);
                        if (fromQueryElement instanceof Map) {
                            if (fromCoreElement instanceof Map) {
                                compareProperties(new NormalizedJsonLd((Map<String, Object>) fromCoreElement), new NormalizedJsonLd((Map<String, Object>) fromQueryElement), true, failingProperties, path + "/" + key + "[" + i + "]");
                            } else {
                                logger.debug(String.format("The %d value of the property %s has not the same type: \n\n Core: \n%s\n\n Query: %s", i, key, fromCoreElement, fromQueryElement));
                                failingProperties.add(path + "/" + key + "[" + i + "]");
                            }
                        } else {
                            if ((fromCoreElement != null && !fromCoreElement.equals(fromQueryElement)) || (fromCoreElement == null && fromQueryElement != null)) {
                                if (!(JsonLdConsts.ID.equals(key) && String.valueOf(fromCoreElement).trim().equals("/") && String.valueOf(fromQueryElement).trim().equals(""))) {
                                    logger.debug(String.format("The %d value of the property %s is not the same: \n\n Core: \n%s\n\n Query: %s", i, key, fromCoreElement, fromQueryElement));
                                    //There is a difference
                                    failingProperties.add(path + "/" + key);
                                } else {
                                    logger.debug(String.format("The %d value of the property %s has an invalid value for the reference - we skip it though since it's the same on both payloads", i, key));

                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Set<String> compareCoreAndQueryPayload(NormalizedJsonLd corePayload, NormalizedJsonLd queryPayload) {
        Set<String> failingProperties = new HashSet<>();
        compareProperties(corePayload, queryPayload, false, failingProperties, "");
        return failingProperties;
    }

    private Map<String, Set<String>> idMap(String key, Set<String> ids) {
        Map<String, Set<String>> idMap = new HashMap<>();
        idMap.put(key, ids);
        return idMap;
    }


}

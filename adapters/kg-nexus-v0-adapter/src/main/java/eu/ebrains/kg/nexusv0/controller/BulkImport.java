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

import com.google.gson.Gson;
import eu.ebrains.kg.commons.exception.InstanceNotFoundException;
import eu.ebrains.kg.commons.jsonld.JsonLdConsts;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.nexusv0.serviceCall.CoreSvc;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class BulkImport {

    private final Gson gson;

    private final PayloadNormalizer payloadNormalizer;

    private final CoreSvc coreSvc;

    private final NexusV0Importer nexusV0Importer;

    public BulkImport(Gson gson, PayloadNormalizer payloadNormalizer, CoreSvc coreSvc, NexusV0Importer nexusV0Importer) {
        this.gson = gson;
        this.payloadNormalizer = payloadNormalizer;
        this.coreSvc = coreSvc;
        this.nexusV0Importer = nexusV0Importer;
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Async
    public void bulkImport(File tempfile, String nexusEndpoint) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(tempfile))) {
            int cnt = 0;
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] values = line.split(",");
                    String uuid = values[1];
                    String payload = new String(Hex.decodeHex(values[2].substring(2)), StandardCharsets.UTF_8);
                    logger.debug(String.format("Importing event %d via bulk", ++cnt));
                    if (cnt % 1000 == 0) {
                        logger.info(String.format("Importing event %d via bulk", cnt));
                    }
                    processEventWithRetry(payload, uuid, 0, nexusEndpoint);
                } catch (DecoderException e) {
                    logger.error("Unrecoverable error", e);
                }
            }
        }
        tempfile.delete();
    }


    private void processEventWithRetry(String payload, String uuid, int retry, String nexusEndpoint) {
        if (uuid.startsWith("instance-")) {
            uuid = uuid.substring("instance-".length());
        }
        try {
            String[] id = uuid.split("/");
            JsonLdDoc json = gson.fromJson(payload, JsonLdDoc.class);
            String author = (String) ((Map) ((Map) json.get("meta")).get("author")).get("id");
            String instant = (String) ((Map) json.get("meta")).get("instant");
            nexusV0Importer.insertOrUpdateEvent(json.getAs("value", JsonLdDoc.class), id[0], id[1], id[2], id[3], instant, author, id[4], true, nexusEndpoint);
        } catch (WebClientResponseException ex) {
            if (ex.getRawStatusCode() >= 400 && ex.getRawStatusCode() < 500) {
                if (ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                    retry(payload, uuid, retry, nexusEndpoint, ex);
                } else {
                    logger.error("Unrecoverable error", ex);
                }
            } else {
                retry(payload, uuid, retry,nexusEndpoint,  ex);
            }
        } catch (InstanceNotFoundException e){
            //Every other exception is unrecoverable
            logger.warn("Instance not found ", e);
        }
        catch (Exception ex) {
            //Every other exception is unrecoverable
            logger.error("Unrecoverable error", ex);
        }

    }

    private void retry(String payload, String uuid, int retry, String nexusEndpoint, Exception ex) {
        if (retry < 6) {
            int retryInSecs = retry * retry;
            logger.warn(String.format("Execution was unsuccessful, retrying in %d seconds", retryInSecs), ex);
            try {
                Thread.sleep(retryInSecs * 1000);
            } catch (InterruptedException ie) {
                logger.error("Retry was interrupted", ex);
            }
            processEventWithRetry(payload, uuid,  ++retry, nexusEndpoint);
        } else {
            logger.error("Potentially recoverable error was not successful (out of retries)");
        }
    }

    public void inferDeferred() {
        coreSvc.inferDeferred();
    }


    private Set<String> getTypes(Object types) {
        if (types instanceof Collection) {
            return new HashSet<>((Collection) types);
        } else if (types instanceof String) {
            return Collections.singleton((String) types);
        }
        return Collections.emptySet();
    }

    private static List<String> SUPPORTED_EVENTS = Arrays.asList("InstanceCreated", "InstanceUpdated", "InstanceDeprecated");

    public void reduceEvents(InputStream in, OutputStream out, String nexusEndpoint) throws IOException, DecoderException {
        File tempFile = File.createTempFile("eventReducer", null);
        tempFile.deleteOnExit();
        Map<String, Set<String>> idLookup = readFileExtractLookupMapAndStoreLocally(in, tempFile, nexusEndpoint);
        doReduceEvents(tempFile, idLookup, out, nexusEndpoint);
        boolean deleted = tempFile.delete();
    }

    private void doReduceEvents(File tempFile, Map<String, Set<String>> idLookup, OutputStream out, String nexusEndpoint) throws IOException, DecoderException {
        Set<String> deprecatedInstances = new HashSet<>();
        Set<String> handledInstances = new HashSet<>();
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out)); BufferedReader br = new BufferedReader(new FileReader(tempFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                String uuid = values[1];
                String payload = new String(Hex.decodeHex(values[2].substring(2)), StandardCharsets.UTF_8);
                Map<?, ?> json = gson.fromJson(payload, Map.class);
                Object type = json.get("type");
                if (type != null && SUPPORTED_EVENTS.contains(type.toString())) {
                    if (type.equals("InstanceDeprecated")) {
                        deprecatedInstances.addAll(idLookup.get(uuid));
                    } else if (deprecatedInstances.contains(uuid)) {
                        handledInstances.add(uuid);
                    } else if (!handledInstances.contains(uuid)) {
                        Object value = json.get("value");
                        if (value instanceof Map) {
                            Map v = (Map) value;
                            Set<String> types = getTypes(v.get(JsonLdConsts.TYPE));
                            if (types.contains(NexusV0Importer.RELEASING_TYPE)) {
                                Object releasedInstance = v.get("https://schema.hbp.eu/release/instance");
                                if (releasedInstance instanceof Map) {
                                    Object released = ((Map) releasedInstance).get(JsonLdConsts.ID);
                                    if (released instanceof String) {
                                        String relInst = "instance-" + payloadNormalizer.getRelativeUrl((String) released, nexusEndpoint);
                                        //If there is a release, we need to keep the event release and the previous event for this instance
                                        bw.write(line);
                                        bw.write('\n');
                                        handledInstances.remove(relInst);
                                    }
                                }
                            } else {
                                bw.write(line);
                                bw.write('\n');
                            }
                            handledInstances.add(uuid);
                        }
                    }
                }
            }
        }
    }

    private Map<String, Set<String>> readFileExtractLookupMapAndStoreLocally(InputStream stream, File tempFile, String nexusEndpoint) throws IOException, DecoderException {
        Map<String, Set<String>> idLookup = new HashMap<>();
        try (FileWriter fw = new FileWriter(tempFile); BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                String uuid = values[1];
                String payload = new String(Hex.decodeHex(values[2].substring(2)), StandardCharsets.UTF_8);
                idLookup.computeIfAbsent(uuid, k -> new HashSet<>());
                idLookup.get(uuid).add(uuid);
                Map<?, ?> json = gson.fromJson(payload, Map.class);
                Object type = json.get("type");
                if (type != null && SUPPORTED_EVENTS.contains(type.toString())) {
                    Object value = json.get("value");
                    if (value instanceof Map) {
                        Map v = (Map) value;
                        Object extendedOrInferredInstance = v.get("https://schema.hbp.eu/inference/extends");
                        if (extendedOrInferredInstance == null) {
                            extendedOrInferredInstance = v.get("https://schema.hbp.eu/inference/inferenceOf");
                        }
                        if (extendedOrInferredInstance instanceof Map) {
                            Map e = (Map) extendedOrInferredInstance;
                            Object id = e.get(JsonLdConsts.ID);
                            if (id instanceof String) {
                                String relativeUrl = payloadNormalizer.getRelativeUrl((String) id, nexusEndpoint);
                                if (relativeUrl != null) {
                                    relativeUrl = "instance-" + relativeUrl;
                                    idLookup.computeIfAbsent(relativeUrl, k -> new HashSet<>());
                                    idLookup.get(relativeUrl).add(uuid);
                                }
                            }
                        }
                    }
                    fw.write(line);
                    fw.write('\n');
                }
            }
        }
        return idLookup;
    }


}

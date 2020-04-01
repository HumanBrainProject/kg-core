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

package eu.ebrains.kg.coreToQueryComparison.api;

import com.google.gson.Gson;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Type;
import eu.ebrains.kg.coreToQueryComparison.controller.KGQueryComparison;
import eu.ebrains.kg.coreToQueryComparison.model.ComparisonResult;
import eu.ebrains.kg.coreToQueryComparison.model.NexusSchema;
import eu.ebrains.kg.coreToQueryComparison.serviceCall.OldQuerySvc;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tests/consistency")
public class CompareToKGQuery {

    private final KGQueryComparison kgQueryComparison;
    private final OldQuerySvc querySvc;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public CompareToKGQuery(KGQueryComparison kgQueryComparison, OldQuerySvc querySvc) {
        this.kgQueryComparison = kgQueryComparison;
        this.querySvc = querySvc;
    }

    @GetMapping(value = "kgquery", produces = "application/json")
    public void deepCompareTypesWithSchemas(HttpServletResponse response, @RequestParam(value = "size", required = false) Integer size, @RequestParam("oidcToken") String oidcToken, @RequestParam(value = "failingOnly", required = false) boolean failingOnly, @RequestParam(value = "showInvolvedPayloads", required = false) boolean showPayloads, @RequestParam(value = "showIds", required = false) boolean showIds, @RequestParam(value = "space", required = false) String space) throws IOException {
        ZonedDateTime dateOfLastEvent = ZonedDateTime.of(2020, 3, 17, 7, 55, 0, 0, ZoneId.of("Z"));
        //ZonedDateTime dateOfLastEvent = ZonedDateTime.now();
        List<NormalizedJsonLd> schemas = querySvc.getSchemas();
        List<NexusSchema> nexusSchemas = schemas.stream().map(s -> {
            String id = s.getAs("id", String.class);
            String[] split = id.split("/");
            if (split.length != 4) {
                throw new IllegalStateException("Found an invalid schema declaration: " + id);
            }
            return new NexusSchema(split[0], split[1], split[2], split[3]);
        }).filter(s -> space == null || s.getOrg().equals(space)).collect(Collectors.toList());
        Integer sizeByType = size == null ? null : (int) (Math.floor(size.doubleValue() / nexusSchemas.size()));
        Gson gson = new Gson();
        Path compareToKgQuery = Paths.get("/tmp", "compareToKgQuery", space, new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()));
        if(!Files.exists(compareToKgQuery)) {
            Files.createDirectories(compareToKgQuery);
        }
        logger.info(String.format("Logging results to %s", compareToKgQuery.toString()));
        try {
            Path simple = compareToKgQuery.resolve("simple.csv");
            Path full = compareToKgQuery.resolve("full.json");
            try (Writer writer = new BufferedWriter(new FileWriter(full.toFile())); Writer simpleWriter = new FileWriter(simple.toFile())) {
                simpleWriter.write("schema,type,instancesInQuery,instancesInQueryInTime,instancesInCore,instancesInQueryButNotInCore,instancesWithMultipleRepresentationsInCore,mergedInstancesInCore,instancesWithDifferentPayloads,instancesWithDifferentPayloadsIgnoringChanged,instancesInCoreButNotInQuery");
                writer.write("{");
                for (int i = 0; i < nexusSchemas.size(); i++) {
                    simpleWriter.write("\n");
                    if (i > 0) {
                        writer.write(",");
                    }
                    NexusSchema nexusSchema = nexusSchemas.get(i);
                    Type typeByConvention = new Type(String.format("https://schema.hbp.eu/%s/%s", nexusSchema.getOrg(), StringUtils.capitalize(nexusSchema.getSchema())));
                    logger.info(String.format("Comparing schema %s with type %s", nexusSchema.getConcatenatedRepresentation(), typeByConvention.getName()));
                    simpleWriter.write(String.format("\"%s\",\"%s\"", nexusSchema.getConcatenatedRepresentation(), typeByConvention.getName()));
                    try {
                        List<ComparisonResult<?>> comparisonResults = kgQueryComparison.compareTypeWithSchema(dateOfLastEvent, typeByConvention, nexusSchema, sizeByType, oidcToken, failingOnly, showPayloads, showIds, nexusSchema.getOrg(), simpleWriter);
                        writer.write("\"" + typeByConvention.getName() + "\": " + gson.toJson(comparisonResults));
                    } catch (Exception e) {
                        simpleWriter.write(String.format("\"failed: %s\"", e.getMessage().replaceAll("\"", "'")));
                        writer.write("\"" + typeByConvention.getName() + "\": \"failed: " + e.getMessage() + "\"");
                    }
                }
                writer.write("}");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @GetMapping("kgquery/{org}/{domain}/{schema}/{version}")
    public List<ComparisonResult<?>> compareInstances(@RequestParam String type, @PathVariable("org") String org, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String version, @RequestParam(value = "size", required = false) Integer size, @RequestParam("oidcToken") String oidcToken, @RequestParam(value = "failingOnly", required = false) boolean failingOnly, @RequestParam(value = "showInvolvedPayloads", required = false) boolean showPayloads, @RequestParam(value = "showIds", required = false) boolean showIds) throws IOException {
        return kgQueryComparison.compareTypeWithSchema(ZonedDateTime.now(), new Type(type), new NexusSchema(org, domain, schema, version), size, oidcToken, failingOnly, showPayloads, showIds, org, null);
    }


//

}

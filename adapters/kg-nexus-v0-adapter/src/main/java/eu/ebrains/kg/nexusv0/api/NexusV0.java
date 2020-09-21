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

package eu.ebrains.kg.nexusv0.api;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.nexusv0.controller.BulkImport;
import eu.ebrains.kg.nexusv0.controller.NexusV0Importer;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.commons.codec.DecoderException;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/nexus/v0")
public class NexusV0 {

    private final NexusV0Importer nexusV0Importer;

    private final BulkImport bulk;

    private final String nexusEndpoint;

    public NexusV0(NexusV0Importer nexusV0Importer, BulkImport bulk, @Value("${eu.ebrains.kg.nexus.endpoint}") String nexusEndpoint) {
        this.nexusV0Importer = nexusV0Importer;
        this.bulk = bulk;
        this.nexusEndpoint = nexusEndpoint;
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());


    @Operation(summary = "Index the creation of a new instance")
    @PostMapping(value = "/{org}/{domain}/{schema}/{version}/{id}", consumes = {JsonLdDoc.APPLICATION_JSON, JsonLdDoc.APPLICATION_LD_JSON})
    public Void addInstance(@RequestBody JsonLdDoc payload, @PathVariable("org") String organization, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String schemaVersion, @PathVariable("id") String id, @RequestParam(value = "authorId", required = false) String authorId, @RequestParam(value = "eventDateTime", required = false) String timestamp) {
        nexusV0Importer.insertOrUpdateEvent(payload, organization, domain, schema, schemaVersion, timestamp, authorId, id, false, nexusEndpoint);
        return null;
    }

    @Operation(summary = "Index the update of an existing instance in a specific revision")
    @PutMapping(value = "/{org}/{domain}/{schema}/{version}/{id}/{rev}", consumes = {JsonLdDoc.APPLICATION_JSON, JsonLdDoc.APPLICATION_LD_JSON})
    public Void updateInstance(@RequestBody JsonLdDoc payload, @PathVariable("org") String organization, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String schemaVersion, @PathVariable("id") String id, @PathVariable("rev") Integer rev, @RequestParam(value = "authorId", required = false) String authorId, @RequestParam(value = "eventDateTime", required = false) String timestamp) {
        nexusV0Importer.insertOrUpdateEvent(payload, organization, domain, schema, schemaVersion, timestamp, authorId, id, false, nexusEndpoint);
        return null;
    }


    @Operation(summary = "Index the deletion of an existing instance")
    @DeleteMapping(value = "/{org}/{domain}/{schema}/{version}/{id}")
    public Void deleteInstance(@PathVariable("org") String organization, @PathVariable("domain") String domain, @PathVariable("schema") String schema, @PathVariable("version") String schemaVersion, @PathVariable("id") String id, @RequestParam(value = "rev", required = false) Integer rev, @RequestParam(value = "authorId", required = false) String authorId, @RequestParam(value = "eventDateTime", required = false) String timestamp) {
        nexusV0Importer.deleteInstance(organization, domain, schema, schemaVersion, id, rev, authorId, timestamp, nexusEndpoint);
        return null;
    }

    @PostMapping(value = "/bulk/normalize")
    public void normalizeBulk(@RequestParam("file") MultipartFile file, HttpServletResponse response) throws IOException, DecoderException {
        response.addHeader("Content-disposition", "attachment;filename="+file.getOriginalFilename()+"normalized.csv");
        response.setContentType("text/csv");
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()))) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");
                    String uuid = values[0];
                    Date timestamp = new Date(Uuids.unixTimestamp(UUID.fromString(values[3])));
                    String payload = values[5];
                    bw.write(String.valueOf(timestamp.getTime()));
                    bw.write(',');
                    bw.write(uuid);
                    bw.write(',');
                    bw.write(payload);
                    bw.write('\n');
                }
            }
        }
    }

    @PostMapping(value = "/bulk/reduceEvents")
    public void reduceEvents(@RequestParam("file") MultipartFile file, HttpServletResponse response, @RequestParam(value = "nexusEndpoint", required = false) String differentNexusEndpoint) throws IOException, DecoderException {
        response.addHeader("Content-disposition", "attachment;filename="+file.getOriginalFilename()+"reduced.csv");
        response.setContentType("text/csv");
        bulk.reduceEvents(file.getInputStream(), response.getOutputStream(), differentNexusEndpoint == null ? nexusEndpoint : differentNexusEndpoint);
    }

    @Operation(summary = "Index the creation of a new instance")
    @PostMapping("/bulk/import")
    public Void bulkImport(@RequestParam("file") MultipartFile file, @RequestParam(value = "nexusEndpoint", required = false) String differentNexusEndpoint) throws IOException {
        File tempfile = File.createTempFile("bulk", "");
        tempfile.deleteOnExit();
        try(FileOutputStream tempFile = new FileOutputStream(tempfile)) {
            IOUtils.copy(file.getInputStream(), tempFile);
        }
        logger.info(String.format("temporarily stored bulk file in %s", tempfile.getAbsolutePath()));
        bulk.bulkImport(tempfile, differentNexusEndpoint == null ? nexusEndpoint : differentNexusEndpoint);
        return null;
    }


}

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

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.core.controller.QueryController;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.core.serviceCall.IdsSvc;
import eu.ebrains.kg.core.serviceCall.JsonLdSvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(Version.API+"/queries")
public class Queries {

    @Autowired
    IdUtils idUtils;

    @Autowired
    QueryController queryController;

    @Autowired
    JsonLdSvc jsonLdSvc;

    @Autowired
    IdsSvc idsSvc;

    @GetMapping
    public PaginatedResult<NormalizedJsonLd> listQueriesPerRootType(PaginationParam paginationParam, @RequestParam(value = "type", required = false) String rootType, @RequestParam(value = "search", required = false) String search) {
        if(rootType != null){
            return PaginatedResult.ok(queryController.listQueriesPerRootType(search, new Type(rootType), paginationParam));
        } else {
            return PaginatedResult.ok(queryController.listQueries(search, paginationParam));
        }
    }

    @PostMapping
    public PaginatedResult<NormalizedJsonLd> testQuery(@RequestBody JsonLdDoc query,  PaginationParam paginationParam, @RequestParam("stage") ExposedStage stage) {
        NormalizedJsonLd normalizedJsonLd = jsonLdSvc.toNormalizedJsonLd(query);
        return PaginatedResult.ok(queryController.executeQuery(new KgQuery(normalizedJsonLd, stage.getStage()), paginationParam));
    }


    @GetMapping("/{queryId}")
    public Result<NormalizedJsonLd> getQuerySpecification(@PathVariable("queryId") UUID queryId, @RequestParam("space") String space) {
        KgQuery kgQuery = queryController.fetchQueryById(queryId, new Space(space), DataStage.LIVE);
        return Result.ok(kgQuery.getPayload());
    }

    @DeleteMapping("/{queryId}")
    @Deprecated
    public void removeQuery(@PathVariable("queryId") UUID queryId) {
//    queryController.deleteQuery()
    }

    @PutMapping("/{queryId}")
    public ResponseEntity<Result<NormalizedJsonLd>> createOrUpdateQuery(@RequestBody JsonLdDoc query, @PathVariable(value = "queryId") UUID queryId, @RequestParam("space") String space) {
        NormalizedJsonLd normalizedJsonLd = jsonLdSvc.toNormalizedJsonLd(query);
        normalizedJsonLd.addTypes(KgQuery.getKgQueryType());
        Space querySpace = new Space(space);
        InstanceId resolveId = idsSvc.resolveId(DataStage.LIVE, queryId);
        if(resolveId != null){
            return queryController.updateQuery(normalizedJsonLd, resolveId);
        }
        return queryController.createNewQuery(normalizedJsonLd, queryId, querySpace);
    }

    @GetMapping("/{queryId}/instances")
    public PaginatedResult<NormalizedJsonLd> executeQueryById(@PathVariable("queryId") UUID queryId, PaginationParam paginationParam,@RequestParam("space") String space, @RequestParam("stage") ExposedStage stage) {
        KgQuery query = queryController.fetchQueryById(queryId, new Space(space), stage.getStage());
        return PaginatedResult.ok(queryController.executeQuery(query, paginationParam));
    }

    @GetMapping("/{queryId}/meta")
    @Deprecated
    public Result<NormalizedJsonLd> getMetaInformation(@PathVariable("queryId") String queryId) {
        return null;
    }




//    private List<NormalizedJsonLd> mockGetBookmarks() {
//        /* TODO: Replace Mockup data with real data */
//        NormalizedJsonLd instance = new NormalizedJsonLd();
//        instance.addProperty("id", (new DocumentId(new Space("user"), UUID.randomUUID())).toString());
//        instance.addProperty("label", "my favorites 1");
//
//        List<NormalizedJsonLd> list = new ArrayList<>();
//        NormalizedJsonLd ld = new NormalizedJsonLd();
//        ld.setId(idUtils.buildAbsoluteUrl(new DocumentId(new Space("Live"), UUID.randomUUID())));
//
//        NormalizedJsonLd ld1 = new NormalizedJsonLd();
//        ld1.setId(idUtils.buildAbsoluteUrl(new DocumentId(new Space("Live"), UUID.randomUUID())));
//        list.add(ld);
//        list.add(ld1);
//        instance.addProperty("list", list);
//
//
//        NormalizedJsonLd instance1 = new NormalizedJsonLd();
//        instance1.addProperty("id", (new DocumentId(new Space("user"), UUID.randomUUID())).toString());
//        instance1.addProperty("label", "my favorites 2");
//
//        List<NormalizedJsonLd> list1 = new ArrayList<>();
//        NormalizedJsonLd ld2 = new NormalizedJsonLd();
//        ld2.setId(idUtils.buildAbsoluteUrl(new DocumentId(new Space("Live"), UUID.randomUUID())));
//        NormalizedJsonLd ld3 = new NormalizedJsonLd();
//        ld3.setId(idUtils.buildAbsoluteUrl(new DocumentId(new Space("Live"), UUID.randomUUID())));
//        list1.add(ld2);
//        list1.add(ld3);
//        instance1.addProperty("list", list1);
//
//        List<NormalizedJsonLd> instances = new ArrayList<>();
//        instances.add(instance);
//        instances.add(instance1);
//        return instances;
//    }

//    private List<NormalizedJsonLd> generateMockupData() {
//        NormalizedJsonLd instance = new NormalizedJsonLd();
//
//        Map<String, Object> field = new HashMap<>();
//        field.put(HBPVocabulary.NAMESPACE + "label", "Name");
//        field.put(HBPVocabulary.CLIENT_NAMESPACE + "kg-editor/widgetType", "InputText");
//        field.put(HBPVocabulary.NAMESPACE + "value", "Efremov, Velizar");
//        instance.addProperty(SchemaOrgVocabulary.NAME, field);
//
//        Map<String, Object> field1 = new HashMap<>();
//        field1.put(HBPVocabulary.NAMESPACE + "label", "ShortName");
//        field1.put(HBPVocabulary.NAMESPACE + "type", "InputText");
//        field1.put(HBPVocabulary.NAMESPACE + "value", "Efremov, V");
//        instance.addProperty(HBPVocabulary.NAMESPACE + "shortName", field1);
//        instance.setId(idUtils.buildAbsoluteUrl(new DocumentId(new Space("Live"), UUID.randomUUID())));
//        instance.addTypes(SchemaOrgVocabulary.NAMESPACE + "Person");
//
//        NormalizedJsonLd instance1 = new NormalizedJsonLd();
//        Map<String, Object> field2 = new HashMap<>();
//        field2.put(HBPVocabulary.NAMESPACE + "label", "Name");
//        field2.put(HBPVocabulary.NAMESPACE + "value", "Extraction of Parvalbumin positive cells from an Allen mouse brain in situ hybridisation experiment");
//        instance1.addProperty(SchemaOrgVocabulary.NAME, field2);
//
//        Map<String, Object> field3 = new HashMap<>();
//        field3.put(HBPVocabulary.NAMESPACE + "label", "Description");
//        field3.put(HBPVocabulary.CLIENT_NAMESPACE + "kg-editor/widgetType", "TextArea");
//        field3.put(HBPVocabulary.NAMESPACE + "markdown", true);
//        field3.put(HBPVocabulary.NAMESPACE + "value", "Aim of project: To assign coordinates and quantify parvalbumin positive cells extracted from a series of brain section images obtained from the Allen Brain ISH repository. ↵↵Study design: The image series was exported from the Allen Mouse Brain Atlas Data Portal, http://mouse.brain-map.org/experiment/show/75457579 ( © 2004 Allen Institute for Brain Science. Allen Mouse Brain Atlas. Available from: mouse.brain-map.org). The series included images of 20 sagittal mouse brain sections from the left hemisphere labelled for parvalbumin by in situ hybridisation (Specimen 06-0419, Probe RP_060523_03_E07; Image credit: Allen Institute). Positive cells were identified by segmentation with the ilastik software. The section images were registered to reference atlas space with the QuickNII software and custom atlas maps exported. Extracted features were quantified, localised and assigned coordinates with the Nutil software. ↵↵Study output: ↵20 segmentation images ↵20 customised atlas maps↵20 atlas maps with superimposed colour coded features↵A .json file with coordinates for visualising the  parvalbumin positive cells in atlas space (Allen mouse brain Common Coordinate framework version 3).↵2 reports (.docx) with quantifications per atlas region (1 report is for the whole brain, the other per section). ↵↵Reference: ↵Allen Mouse Brain Atlas. Lein, E.S. et al. (2007) Genome-wide atlas of gene expression in the adult mouse brain, Nature 445: 168-176. doi:10.1038/nature05453."
//        );
//        instance1.addProperty(SchemaOrgVocabulary.DESCRIPTION, field3);
//        instance1.setId(idUtils.buildAbsoluteUrl(new DocumentId(new Space("Live"), UUID.randomUUID())));
//        instance1.addTypes(SchemaOrgVocabulary.NAMESPACE + "Dataset");
//
//
//        return Arrays.asList(instance, instance1);
//    }
}

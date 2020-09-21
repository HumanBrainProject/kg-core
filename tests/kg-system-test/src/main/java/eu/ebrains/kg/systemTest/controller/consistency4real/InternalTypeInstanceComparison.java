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

package eu.ebrains.kg.systemTest.controller.consistency4real;

import com.arangodb.model.AqlQueryOptions;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.Tuple;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.systemTest.model.ComparisonResult;
import eu.ebrains.kg.systemTest.serviceCall.SystemTestToCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InternalTypeInstanceComparison {

    private final JsonAdapter jsonAdapter;

    private final SystemTestToCore coreSvc;

    private final IdUtils idUtils;

    private final ArangoDatabaseProxy inProgressMeta;
    private final ArangoDatabaseProxy releasedMeta;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public InternalTypeInstanceComparison(JsonAdapter jsonAdapter, SystemTestToCore coreSvc, IdUtils idUtils, @Qualifier("inProgressMetaTest") ArangoDatabaseProxy inProgressMeta, @Qualifier("releasedMetaTest") ArangoDatabaseProxy releasedMeta) {
        this.jsonAdapter = jsonAdapter;
        this.coreSvc = coreSvc;
        this.idUtils = idUtils;
        this.inProgressMeta = inProgressMeta;
        this.releasedMeta = releasedMeta;
    }

    public Map<String, ComparisonResult<Long>> compareTypesWithInstances(DataStage stage, boolean failingOnly, boolean analyzeFailing) {
        List<Tuple<Type, Long>> types = coreSvc.getTypes(stage);
        Map<String, ComparisonResult<Long>> result = new HashMap<>();
        for (Tuple<Type, Long> type : types) {
            logger.info(String.format("Now investigating on type %s", type.getA().getName()));
            PaginatedResult<NormalizedJsonLd> instances = coreSvc.getInstances(type.getA(), 0, 0, stage);
            long totalFromInstances = instances.getTotal();
            long totalFromTypes = type.getB();
            ComparisonResult<Long> r = new ComparisonResult<>();
            r.setActualValue(totalFromInstances);
            r.setExpectedValue(totalFromTypes);
            r.setCorrect(totalFromInstances == totalFromTypes);
            if (!failingOnly || !r.isCorrect()) {
                result.put(type.getA().getName(), r);
            }
            if (!r.isCorrect() && analyzeFailing) {
                Map<String, Set<UUID>> stringSetMap = analyzeType(stage, type.getA());
                r.setExtraInformation(stringSetMap);

            }
        }
        return result;
    }


    public Map<String, Set<UUID>> selfHeal(DataStage stage, Type type){
        Map<String, Set<UUID>> conflicts = analyzeType(stage, type);
        conflicts.values().stream().flatMap(Collection::stream).forEach(uuid -> {
            NormalizedJsonLd instanceById = coreSvc.getInstanceById(uuid, stage);
            if(instanceById!=null) {
                String space = instanceById.getAs(EBRAINSVocabulary.META_SPACE, String.class);
                logger.info(String.format("Trying to re-infer the instance %s in space %s", uuid, space));
                coreSvc.inferInstance(new Space(space), uuid, false);
            }
            else{
                logger.error(String.format("Was not able to find the instance %s", uuid));
            }
        });
        return analyzeType(stage, type);
    }

    public Map<String, Set<UUID>> analyzeType(DataStage stage, Type type) {
        logger.info(String.format("Now analyzing the type %s", type.getName()));
        PaginatedResultOfDocuments instances = coreSvc.getInstances(type, stage);
        Set<UUID> idsFromInstances = instances.getData().stream().map(i -> idUtils.getUUID(i.id())).collect(Collectors.toSet());
        Set<UUID> idsFromTypeRelation = getIdsFromMetaTypeRelation(stage, type);
        Set<UUID> idsOnlyInInstances = new HashSet<>(idsFromInstances);
        idsOnlyInInstances.removeAll(idsFromTypeRelation);
        Set<UUID> idsOnlyInType = new HashSet<>(idsFromTypeRelation);
        idsOnlyInType.removeAll(idsFromInstances);
        Map<String, Set<UUID>> invalidDocs = new HashMap<>();
        invalidDocs.put("onlyInInstances", idsOnlyInInstances);
        invalidDocs.put("onlyInTypes", idsOnlyInType);
        logger.info(String.format("Found differences for type %s: %s", type.getName(), jsonAdapter.toJson(invalidDocs)));
        return invalidDocs;
    }


    private Set<UUID> getIdsFromMetaTypeRelation(DataStage stage, Type type) {
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("LET typeDocs = FIRST(FOR type IN "+ArangoCollectionReference.fromSpace(InternalSpace.TYPES_SPACE).getCollectionName()));
        aql.addLine(AQL.trust("FILTER type.`" + SchemaOrgVocabulary.IDENTIFIER + "`==@type"));
        bindVars.put("type", type.getName());
        aql.addLine(AQL.trust("LET spaceType = FLATTEN(FOR spaceType, spaceTypeEdge IN 1..1 INBOUND type "+InternalSpace.SPACE_TO_TYPE_EDGE_COLLECTION.getCollectionName()));
        aql.addLine(AQL.trust("LET relatedDocs = (FOR docs, docsEdge IN INBOUND spaceTypeEdge "+InternalSpace.DOCUMENT_RELATION_EDGE_COLLECTION.getCollectionName()));
        aql.addLine(AQL.trust("RETURN docsEdge."+ IndexedJsonLdDoc.ORIGINAL_DOCUMENT+")"));
        aql.addLine(AQL.trust(" RETURN relatedDocs)"));
        aql.addLine(AQL.trust("RETURN spaceType)"));
        aql.addLine(AQL.trust("FOR typeDoc IN typeDocs"));
        aql.addLine(AQL.trust("RETURN typeDoc"));
        ArangoDatabaseProxy proxy;
        switch(stage){
            case IN_PROGRESS:
                proxy = inProgressMeta;
                break;
            case RELEASED:
                proxy = releasedMeta;
                break;
            default:
                throw new IllegalArgumentException(String.format("Was not able to find meta database for stage %s", stage.name()));
        }
        return proxy.getOrCreate().query(aql.build().getValue(), bindVars, new AqlQueryOptions(), String.class).asListRemaining().stream().map(id -> UUID.fromString(id.split("/")[1])).collect(Collectors.toSet());
    }

}

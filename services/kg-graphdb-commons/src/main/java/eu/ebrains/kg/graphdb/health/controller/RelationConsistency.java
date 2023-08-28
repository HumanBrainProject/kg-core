/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

package eu.ebrains.kg.graphdb.health.controller;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import eu.ebrains.kg.arango.commons.aqlbuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RelationConsistency {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final List<String> EXCLUDED_SPACES = List.of("files"); //FIXME get rid of this hardcoded value
    private static final List<String> EXCLUDED_PROPERTIES = List.of("https://core.kg.ebrains.eu/vocab/meta/user", "https://core.kg.ebrains.eu/vocab/query/structure");

    private final IdUtils idUtils;

    public RelationConsistency(IdUtils idUtils) {
        this.idUtils = idUtils;
    }

    public Map<String, List<String>> checkRelationConsistency(ArangoDatabase database, CollectionEntity collection) {
        if(EXCLUDED_SPACES.contains(collection.getName())){
            logger.info("Skipping relation consistency check for collection {} because it's flagged as excluded", collection.getName());
            return null;
        }
        else {
            Map<String, List<String>> resultCollector = new HashMap<>();
            logger.info("Checking relation consistency for collection {} in database {}", collection.getName(), database.getInfo().getName());
            int currentPage = -1;
            List<RelationFromPayload> result;
            while (!(result = retrieveRelationsFromPayload(database, collection.getName(), ++currentPage)).isEmpty()) {
                logger.info("Page {} for collection {} in database {}", currentPage, collection.getName(), database.getInfo().getName());
                result.forEach(r -> {
                    r.getRefs().forEach(propertyRelation -> {
                        final String sourceDocument = String.format("%s/%s", r.collection, r.id);
                        final Map<String, Object> bindVars = Map.of("@collection", ArangoCollectionReference.fromSpace(new SpaceName(propertyRelation.getProperty()), true).getCollectionName(),
                                "documentId", sourceDocument);
                        try (final ArangoCursor<NormalizedJsonLd> query = database.query("FOR r IN @@collection FILTER r._from == @documentId RETURN r", bindVars, NormalizedJsonLd.class)) {
                            final List<NormalizedJsonLd> relationsFromEdge = query.asListRemaining();
                            compareRelationsFromPayloadWithThoseFromEdge(r.id, sourceDocument, propertyRelation.getProperty(), propertyRelation.getInstances(), relationsFromEdge, resultCollector);
                        } catch (ArangoDBException | IOException e) {
                            final String message = String.format("Was not able to read the edges of %s - %s", propertyRelation.property, e.getMessage());
                            resultCollector.computeIfAbsent(r.id, x -> new ArrayList<>()).add(message);
                            logger.error(message, e);
                        }
                    });
                });
            }
            return resultCollector;
        }
    }


    public static class PropertyRelation {
        private String property;
        private List<String> instances;

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public List<String> getInstances() {
            return instances;
        }

        public void setInstances(List<String> instances) {
            this.instances = instances;
        }
    }

    public static class RelationFromPayload {
        private String id;

        private String collection;
        private List<PropertyRelation> refs;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<PropertyRelation> getRefs() {
            return refs;
        }

        public void setRefs(List<PropertyRelation> refs) {
            this.refs = refs;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }
    }


    private void compareRelationsFromPayloadWithThoseFromEdge(String id, String sourceIdWithCollection, String propertyName, List<String> fromPayload, List<NormalizedJsonLd> fromEdge, Map<String, List<String>> resultCollector) {
        if (!EXCLUDED_PROPERTIES.contains(propertyName)) {
            Set<String> handledEdges = new HashSet<>();
            fromPayload.forEach(payload -> {
                final UUID targetUUID = idUtils.getUUID(new JsonLdId(payload));
                final Set<NormalizedJsonLd> foundRelationsFromEdge = fromEdge.stream().filter(e -> {
                    final String to = e.getAs(ArangoVocabulary.TO, String.class);
                    return to != null && to.endsWith(String.format("/%s", targetUUID));
                }).collect(Collectors.toSet());
                if (foundRelationsFromEdge.isEmpty()) {
                    resultCollector.computeIfAbsent(id, x -> new ArrayList<>()).add(String.format("Edge not found for property %s (targeting %s)", propertyName, payload));
                    logger.error(String.format("%s - Edge not found for property %s (targeting %s)", id, propertyName, payload));
                } else if (foundRelationsFromEdge.size() > 1) {
                    resultCollector.computeIfAbsent(id, x -> new ArrayList<>()).add(String.format("Too many edges found for property %s (targeting %s)", propertyName, payload));
                    logger.error(String.format("%s - Too many edges found for property %s (targeting %s)", id, propertyName, payload));
                } else {
                    final ArangoDocument edge = ArangoDocument.from(foundRelationsFromEdge.iterator().next());
                    handledEdges.add(edge.getId().getId());
                    final ArangoDocumentReference originalDocument = edge.getOriginalDocument();
                    if (originalDocument == null) {
                        resultCollector.computeIfAbsent(id, x -> new ArrayList<>()).add(String.format("Found an edge (%s) without original document", edge.getId()));
                        logger.error(String.format("%s - Found an edge (%s) without original document", id, edge.getId()));
                    } else if (!originalDocument.getId().equals(sourceIdWithCollection)) {
                        resultCollector.computeIfAbsent(id, x -> new ArrayList<>()).add(String.format("Found edge (%s) with inconsistent original document (%s instead of %s)", edge.getId(), edge.getOriginalDocument(), sourceIdWithCollection));
                        logger.error(String.format("%s - Found edge (%s) with inconsistent original document (%s instead of %s)", id, edge.getId(), edge.getOriginalDocument(), sourceIdWithCollection));
                    }
                }
            });
            fromEdge.stream().map(ArangoDocument::from).filter(e -> !handledEdges.contains(e.getId().getId())).forEach(e -> {
                resultCollector.computeIfAbsent(id, x -> new ArrayList<>()).add(String.format("Found edge (%s) which is not reflected in its document", e.getId().getId()));
                logger.error(String.format("%s - Found edge (%s) which is not reflected in its document", id, e.getId().getId()));
            });
        }
    }

    private static final int PAGE_SIZE = 2000;

    private List<RelationFromPayload> retrieveRelationsFromPayload(ArangoDatabase database, String collection, int currentPage) {
        try (final ArangoCursor<RelationFromPayload> cursor = database.query(String.format("""
                FOR d in @@collection
                    FILTER d.`_alternative` == NULL or d.`_alternative` == false
                    LET refs = (FOR a in ATTRIBUTES(d)
                        FILTER STARTS_WITH(a, "_") == false
                        LET onlyObjects = (FOR i in TO_ARRAY(d[a]) FILTER IS_OBJECT(i) AND HAS(i, "@id") RETURN i["@id"])
                        FILTER onlyObjects != []
                        RETURN {
                            "property": a,
                            "instances" : onlyObjects
                        })
                    FILTER refs != []
                    LIMIT %d,%d
                    RETURN {
                        "id": d._key,
                        "collection": d._collection,
                        "refs" : refs
                    }
                """, currentPage * PAGE_SIZE, PAGE_SIZE), Map.of("@collection", collection), RelationFromPayload.class)) {
            return cursor.asListRemaining();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

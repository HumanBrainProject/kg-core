/*
 * Copyright 2022 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.arango.commons;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.model.AQLQuery;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginatedStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ArangoQueries {

    private ArangoQueries() {
    }

    private final static Logger logger = LoggerFactory.getLogger(ArangoQueries.class);


    public static <T> PaginatedStream<T> queryDocuments(ArangoDatabase db, AQLQuery aqlQuery, Function<NormalizedJsonLd, T> mapper, Double maxMemoryForQuery) {
        AQL aql = aqlQuery.getAql();
        if (logger.isTraceEnabled()) {
            logger.trace(aql.buildSimpleDebugQuery(aqlQuery.getBindVars()));
        }
        String value = aql.build().getValue();
        long launch = new Date().getTime();
        if(maxMemoryForQuery!=null){
            aql.getQueryOptions().memoryLimit(maxMemoryForQuery.longValue());
        }
        aql.getQueryOptions().count(true);
        ArangoCursor<NormalizedJsonLd> result = db.query(value, aqlQuery.getBindVars(), aql.getQueryOptions(), NormalizedJsonLd.class);
        logger.debug("Received {} results from Arango in {}ms", result.getCount(), new Date().getTime() - launch);
        Long count = result.getCount() != null ? result.getCount().longValue() : null;
        Long totalCount;
        if (aql.getPaginationParam() != null && aql.getPaginationParam().getSize() != null) {
            totalCount = result.getStats().getFullCount();
        } else {
            totalCount = count;
        }
        logger.debug("Start parsing the results after {}ms", new Date().getTime() - launch);
        final Stream<NormalizedJsonLd> stream = StreamSupport.stream(result.spliterator(), false);
        Stream<T> resultStream = stream.map(Objects.requireNonNullElseGet(mapper, () -> s -> (T) s));
        logger.debug("Done processing the Arango result - received {} results in {}ms total", count, new Date().getTime() - launch);
        if (aql.getPaginationParam() != null && aql.getPaginationParam().getSize() == null && (int) aql.getPaginationParam().getFrom() > 0 && count != null && (int) aql.getPaginationParam().getFrom() < count) {
            //Arango doesn't allow to request from a specific offset to infinite. To achieve this, we load everything and we cut the additional instances in Java
            resultStream = resultStream.skip((int) aql.getPaginationParam().getFrom());
            return new PaginatedStream<>(resultStream, totalCount, count-aql.getPaginationParam().getFrom(), aql.getPaginationParam() != null ? aql.getPaginationParam().getFrom() : 0);
        }
        return new PaginatedStream<>(resultStream, totalCount, count != null ? count : -1L, aql.getPaginationParam() != null ? aql.getPaginationParam().getFrom() : 0);
    }

    public static Paginated<NormalizedJsonLd> queryDocuments(ArangoDatabase db, AQLQuery aqlQuery, Double maxMemoryForQuery) {
        return new Paginated<>(queryDocuments(db, aqlQuery, null, maxMemoryForQuery));
    }

    public static PaginatedStream<NormalizedJsonLd> queryDocumentsAsStream(ArangoDatabase db, AQLQuery aqlQuery, Double maxMemoryForQuery) {
        return queryDocuments(db, aqlQuery, null, maxMemoryForQuery);
    }
}

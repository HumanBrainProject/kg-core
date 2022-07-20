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

package eu.ebrains.kg.commons.model;

import java.util.stream.Stream;

public class PaginatedStream<T> {

    private Stream<T> stream;
    private Long totalResults;
    private long size;
    private long from;


    public PaginatedStream() {
    }

    public PaginatedStream(Stream<T> stream, Long totalResults, long size, long from) {
        this.stream = stream;
        this.totalResults = totalResults;
        this.size = size;
        this.from = from;
    }

    public Stream<T> getStream() {
        return stream;
    }

    public Long getTotalResults() {
        return totalResults;
    }

    public long getSize() {
        return size;
    }

    public long getFrom() {
        return from;
    }

}


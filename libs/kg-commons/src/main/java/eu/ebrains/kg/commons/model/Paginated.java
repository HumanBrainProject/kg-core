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

package eu.ebrains.kg.commons.model;

import java.util.ArrayList;
import java.util.List;

public class Paginated<T> {

    private List<T> data;
    private long totalResults;
    private long size;
    private long from;


    public Paginated() {
        this.data = new ArrayList<>();
    }

    public Paginated(List<T> data, long totalResults, long size, long from) {
        this.data = data;
        this.totalResults = totalResults;
        this.size = size;
        this.from = from;
    }

    public List<T> getData() {
        return data;
    }

    public long getTotalResults() {
        return totalResults;
    }

    public long getSize() {
        return size;
    }

    public long getFrom() {
        return from;
    }

}


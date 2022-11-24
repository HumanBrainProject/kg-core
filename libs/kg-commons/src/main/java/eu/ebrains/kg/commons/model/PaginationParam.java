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

package eu.ebrains.kg.commons.model;

import eu.ebrains.kg.commons.model.internal.spaces.Space;

import java.util.Collections;
import java.util.List;

public class PaginationParam {
    private long from;
    private Long size;

    private boolean returnTotalResults;

    public long getFrom() {
        return from;
    }

    public PaginationParam setFrom(long from) {
        this.from = from;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public PaginationParam setSize(Long size) {
        this.size = size;
        return this;
    }

    public boolean isReturnTotalResults() {
        return returnTotalResults;
    }

    public PaginationParam setReturnTotalResults(boolean returnTotalResults) {
        this.returnTotalResults = returnTotalResults;
        return this;
    }

    public static <T> Paginated<T> paginate(List<T> source, PaginationParam paginationParam){
        if(paginationParam!=null) {
            if(paginationParam.getFrom()>source.size()-1){
                return new Paginated<>(Collections.emptyList(), (long) source.size(), 0, paginationParam.getFrom());
            }
            int upper = paginationParam.getSize() == null ? source.size() : Math.min(source.size(), (int) (paginationParam.getFrom() + paginationParam.getSize()));
            final List<T> result = source.subList((int)paginationParam.getFrom(), upper);
            return new Paginated<>(result, (long)source.size(), result.size(), paginationParam.getFrom());
        }
        return new Paginated<>(source, (long)source.size(), source.size(), 0);
    }

}

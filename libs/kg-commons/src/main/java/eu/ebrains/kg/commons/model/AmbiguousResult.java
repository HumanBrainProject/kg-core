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

import java.util.Collection;

public class AmbiguousResult<T> extends Result<T> {

    private Collection<T> ambiguousData;

    public static <T> Result<T> ok(Collection<T> data) {
        return ok(data, null);
    }

    public static <T> Result<T> ok(Collection<T> data, String message) {
        if(data==null || data.isEmpty()){
            return null;
        }
        else if(data.size()==1){
            return Result.ok(data.iterator().next(), message);
        }
        else {
            AmbiguousResult<T> result = new AmbiguousResult<>();
            result.ambiguousData = data;
            if (message != null) {
                result.message = message;
            }
            return result;
        }
    }

    @Override
    public T getData() {
        return data;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

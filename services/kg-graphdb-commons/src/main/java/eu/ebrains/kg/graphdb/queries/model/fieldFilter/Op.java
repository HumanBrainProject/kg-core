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

package eu.ebrains.kg.graphdb.queries.model.fieldFilter;

public enum Op {

    STARTS_WITH("starts with", ExampleValues.SIMPLE_STRING_EXAMPLE, false), ENDS_WITH("ends with", ExampleValues.SIMPLE_STRING_EXAMPLE, false), CONTAINS("contains", ExampleValues.SIMPLE_STRING_EXAMPLE, false), EQUALS("equals", ExampleValues.SIMPLE_STRING_EXAMPLE, false), REGEX("regular expression", ExampleValues.REGEX_EXAMPLE, false), MBB("minimal bounding box", ExampleValues.MBB_EXAMPLE, true), TYPE("type", null, true), ID("id", null, true);

    private final String name;
    private final String example;
    private boolean instanceFilter;

    Op(String name, String example, boolean instanceFilter){
          this.name = name;
          this.example = example;
          this.instanceFilter = instanceFilter;
    }

    public static Op valueCaseInsensitive(String value){
        if(value!=null) {
            for (Op op : values()) {
                if (op.toString().toLowerCase().equals(value.toLowerCase())) {
                    return op;
                }
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public String getExample() {
        return example;
    }

    public boolean isInstanceFilter() {
        return instanceFilter;
    }
}
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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import eu.ebrains.kg.commons.serializer.SpaceNameDeserializer;

import java.io.Serializable;
import java.util.Objects;

@JsonSerialize(using = ToStringSerializer.class)
@JsonDeserialize(using = SpaceNameDeserializer.class)
public class SpaceName implements Serializable {

    public static final String PRIVATE_SPACE = "myspace";
    public static final String REVIEW_SPACE = "review";

    private String name;

    //For serialization
    public SpaceName(){
    }

    public SpaceName(String name){
        this.setName(name);
    }

    public SpaceName setName(String name) {
        this.name = normalizeName(name);
        return this;
    }

    public static SpaceName fromString(String name){
        return name!=null && !name.isBlank() ? new SpaceName(name): null;
    }

    private String normalizeName(String name) {
        return name!=null ? name.toLowerCase().replace("_", "-") : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpaceName spaceName = (SpaceName) o;
        return Objects.equals(name, spaceName.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }


    public static String translateSpace(String spaceName, SpaceName privateUserSpace){
        if(privateUserSpace!=null && privateUserSpace.getName().equals(spaceName)){
            return SpaceName.PRIVATE_SPACE;
        }
        return spaceName;
    }

    public static SpaceName getInternalSpaceName(String originalSpace, SpaceName privateUserSpace) {
        return getInternalSpaceName(SpaceName.fromString(originalSpace), privateUserSpace);
    }

    public static SpaceName getInternalSpaceName(SpaceName originalSpace, SpaceName privateUserSpace){
        if(originalSpace!=null && originalSpace.getName()!=null && originalSpace.getName().equals(SpaceName.PRIVATE_SPACE)){
            return privateUserSpace;
        }
        return originalSpace;
    }

    public boolean isWildcard(){
        return getName()!=null && getName().endsWith("*");
    }

    public boolean matchesWildcard(SpaceName space){
        return isWildcard() && space!=null && space.getName()!=null && space.getName().startsWith(getName().substring(0, getName().length()-1));
    }
}

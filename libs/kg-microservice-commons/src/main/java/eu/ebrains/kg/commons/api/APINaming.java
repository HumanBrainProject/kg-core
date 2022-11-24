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

package eu.ebrains.kg.commons.api;

import io.swagger.v3.oas.models.tags.Tag;

import java.util.Arrays;
import java.util.List;

public class APINaming {

    public static final String BASIC = "1 Basic";
    public static final String ADVANCED = "2 Advanced";
    public static final String ADMIN = "3 Admin";


    public static List<Tag> orderedTags(){
        return Arrays.asList(new Tag().name(APINaming.BASIC).description("The most used end-points to interact with the EBRAINS KG"),
                new Tag().name(APINaming.ADVANCED).description("Some advanced endpoints allowing you e.g. to apply bulk manipulations and run operations which are going beyond standard meta-data management"),
                new Tag().name(APINaming.ADMIN).description("Endpoints for the administration of the EBRAINS KG. Please note, that you won't be able to invoke those endpoints unless you are a global administrator."));
    }
}

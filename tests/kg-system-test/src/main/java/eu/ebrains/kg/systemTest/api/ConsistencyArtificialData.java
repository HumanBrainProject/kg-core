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
 * Human Brain Project, funded from the European Union’s Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.systemTest.api;

import eu.ebrains.kg.systemTest.controller.consistency4artificial.InternalArtificialData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tests/consistencyOfArtificialData")
public class ConsistencyArtificialData {

    //TODO translate those tests into tests running on kg-core-all-in-one bundle without the web layer.

    private final InternalArtificialData internalArtificialData;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ConsistencyArtificialData(InternalArtificialData internalArtificialData) {
        this.internalArtificialData = internalArtificialData;
    }

    @GetMapping("artificialData")
    public void executeArtificialDataTest(){
       internalArtificialData.simpleIngestion();
    }

}

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

package eu.ebrains.kg.systemTest.api;

import eu.ebrains.kg.systemTest.controller.regression.RegressionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tests/regression")
public class Regression {

    private final RegressionController regression;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Regression(RegressionController regression) {
        this.regression = regression;
    }

    @GetMapping("merge")
    public void merge(){
        regression.merge();
    }


    @GetMapping("dontReconcileSameIdentifiersInDifferentSpaces")
    public void dontReconcileSameIdentifiersInDifferentSpaces(){
        regression.dontReconcileSameIdentifiersInDifferentSpaces();
    }


}

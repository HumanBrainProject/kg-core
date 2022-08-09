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

package eu.ebrains.kg.core.api.common;

import eu.ebrains.kg.commons.api.JsonLd;
import eu.ebrains.kg.commons.exception.InvalidRequestException;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.ExtendedResponseConfiguration;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.core.controller.CoreInstanceController;
import eu.ebrains.kg.core.controller.IdsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.UUID;

public abstract class JsonLD {

    protected static final String TAG = "jsonld";
    protected static final String TAG_ADV = "jsonld - advanced";
    protected static final String TAG_EXTRA = "xtra - jsonld";
    protected  final Logger logger = LoggerFactory.getLogger(getClass());

    public JsonLD() {

    }

}

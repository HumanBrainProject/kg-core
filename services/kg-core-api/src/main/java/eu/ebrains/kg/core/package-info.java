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

/**
 * The Core API is the publicly exposed interface (mapped via reverse proxy) and therefore the central entry point of all "core"-external clients.
 *
 * <p><b>The components' main concerns</b></p>
 * <ul>
 *     <li>Normalization of incoming payloads: Whenever a JSON-LD payload is passed via the API (e.g. ingestion, contribution, query), this component ensures that these payloads are normalized for further processing inside the core environment</li>
 *     <li>Filtering of outgoing payloads: Ensures that internal properties and/or filtered elements are removed from the response.</li>
 *     <li>Initial authentication: A first check of authentication (e.g. if the user &amp; client tokens are valid) to fail fast</li>
 *     <li>Orchestration of API calls: One of the main purposes of this API is to orchestrate the API calls to the various services involved in the exposed functionality.</li>
 * </ul>
 */
package eu.ebrains.kg.core;

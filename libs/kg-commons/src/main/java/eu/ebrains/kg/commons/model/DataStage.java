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

public enum DataStage {

    /**
     * The native space is the space containing the individual instances in their original state. No additional logic is applied.
     **/
    NATIVE,
    /**
     * The in_progress space is built based on the native space by applying inference logic (e.g. merging instances which contribute to the same entity)
     **/
    IN_PROGRESS,
    /**
     * The released space contains instances looking similar to the inferred, but "copied-away" in the specific released revision. Typically, data is moved from the inferred to the released stage when its content has been validated.
     */
    RELEASED

}

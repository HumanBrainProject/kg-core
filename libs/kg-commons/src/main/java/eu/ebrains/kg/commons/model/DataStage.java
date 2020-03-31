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

package eu.ebrains.kg.commons.model;

public enum DataStage {

    /**
     * The native space is the space containing the individual instances in their original state. No additional logic is applied.
     **/
    NATIVE,
    /**
     * The live space is built based on the native space by applying inference logic (e.g. merging instances which contribute to the same entity)
     **/
    LIVE,
    /**
     * The released space contains instances looking similar to the inferred, but "copied-away" in the specific released revision. Typically, data is moved from the inferred to the released stage when its content has been validated.
     */
    RELEASED;

}

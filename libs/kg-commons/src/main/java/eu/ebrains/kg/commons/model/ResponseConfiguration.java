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

package eu.ebrains.kg.commons.model;

public class ResponseConfiguration {

    private boolean returnPayload = true;
    private boolean returnPermissions = false;
    private boolean returnAlternatives = false;
    private boolean returnEmbedded = true;
    private boolean sortByLabel = true;

    public boolean isSortByLabel() {
        return sortByLabel;
    }

    public void setSortByLabel(boolean sortByLabel) {
        this.sortByLabel = sortByLabel;
    }

    public boolean isReturnPayload() {
        return returnPayload;
    }

    public ResponseConfiguration setReturnPayload(boolean returnPayload) {
        this.returnPayload = returnPayload;
        return this;
    }

    public boolean isReturnPermissions() {
        return returnPermissions;
    }

    public ResponseConfiguration setReturnPermissions(boolean returnPermissions) {
        this.returnPermissions = returnPermissions;
        return this;
    }

    public boolean isReturnAlternatives() {
        return returnAlternatives;
    }

    public ResponseConfiguration setReturnAlternatives(boolean returnAlternatives) {
        this.returnAlternatives = returnAlternatives;
        return this;
    }

    public boolean isReturnEmbedded() {
        return returnEmbedded;
    }

    public ResponseConfiguration setReturnEmbedded(boolean returnEmbedded) {
        this.returnEmbedded = returnEmbedded;
        return this;
    }

}

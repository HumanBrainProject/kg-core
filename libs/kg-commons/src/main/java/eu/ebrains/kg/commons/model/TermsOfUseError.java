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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "error", "howToFix" })
public class TermsOfUseError extends TermsOfUse {
    @SuppressWarnings("java:S1068") //we keep the property to ensure it's properly serialized
    private final String error;
    @SuppressWarnings("java:S1068") //we keep the property to ensure it's properly serialized
    private final String howToFix;

    public TermsOfUseError(TermsOfUse termsOfUse) {
        super(termsOfUse.getVersion(), termsOfUse.getData());
        this.error = String.format("You have not accepted the latest version (%s) of the terms of use", termsOfUse.getVersion());
        this.howToFix = String.format("By calling the POST endpoint of users/termsOfUse/%s/accept you give your consent to the terms of use and can make use of the API.", termsOfUse.getVersion());
    }

}

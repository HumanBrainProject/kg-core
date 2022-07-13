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

package eu.ebrains.kg.commons.api;

import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.model.ReducedUserInformation;
import eu.ebrains.kg.commons.model.TermsOfUse;
import eu.ebrains.kg.commons.model.TermsOfUseResult;
import eu.ebrains.kg.commons.model.User;
import eu.ebrains.kg.commons.models.UserWithRoles;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;

import java.util.List;
import java.util.Map;

public interface Authentication {

    interface Client extends Authentication {}

    ClientAuthToken fetchToken(String clientId, String clientSecret);

    String openIdConfigUrl();

    String authEndpoint();

    String tokenEndpoint();

    User getMyUserInfo();

    UserWithRoles getRoles(boolean checkForTermsOfUse);

    List<ReducedUserInformation> findUsers(String name);

    User getOtherUserInfo(String nativeId);

    List<User> getUsersByAttribute(String attribute, String value);

    TermsOfUseResult getTermsOfUse();

    void acceptTermsOfUse(String version);

    void registerTermsOfUse(TermsOfUse version);

    JsonLdDoc updateClaimForRole(RoleMapping role, String space, Map<?, ?> claimPattern, boolean removeClaim);

    JsonLdDoc getClaimForRole(RoleMapping role, String space);

    List<JsonLdDoc> getAllRoleDefinitions();

}

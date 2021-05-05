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

import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.semantics.vocabularies.HBPVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;

public class User extends NormalizedJsonLd {
    public User() {
    }

    public User(NormalizedJsonLd normalizedJsonLd) {
        super(normalizedJsonLd);
    }

    public User(String userName, String displayName, String email, String givenName, String familyName, String nativeId) {
        addProperty(SchemaOrgVocabulary.NAMESPACE + "alternateName", userName);
        addProperty(SchemaOrgVocabulary.NAMESPACE + "name", displayName);
        addProperty(SchemaOrgVocabulary.NAMESPACE + "email", email);
        addProperty(SchemaOrgVocabulary.NAMESPACE + "givenName", givenName);
        addProperty(SchemaOrgVocabulary.NAMESPACE + "familyName", familyName);
        addProperty(HBPVocabulary.NAMESPACE + "users/nativeId", nativeId);
        addIdentifiers(nativeId);
    }

    public String getUserName() {
        return getAs(SchemaOrgVocabulary.NAMESPACE + "alternateName", String.class);
    }

    public String getDisplayName() {
        return getAs(SchemaOrgVocabulary.NAMESPACE + "name", String.class);
    }

    public String getEmail() {
        return getAs(SchemaOrgVocabulary.NAMESPACE + "email", String.class);
    }

    public String getGivenName() {
        return getAs(SchemaOrgVocabulary.NAMESPACE + "givenName", String.class);
    }

    public String getFamilyName() {
        return getAs(SchemaOrgVocabulary.NAMESPACE + "familyName", String.class);
    }

    public String getNativeId() {
        return getAs(HBPVocabulary.NAMESPACE + "users/nativeId", String.class);
    }

    public boolean isEqual(User user){
        return user!=null && isSame(getUserName(), user.getUserName()) && isSame(getDisplayName(), user.getDisplayName()) &&
                isSame(getEmail(), user.getEmail()) && isSame(getGivenName(), user.getGivenName()) &&
                isSame(getFamilyName(), user.getFamilyName());
    }

    private boolean isSame(Object o1, Object o2){
        return o1==null && o2==null || (o1!=null && o1.equals(o2));
    }

    public boolean isServiceAccountForClient(String clientId){
        return getUserName() !=null && getUserName().equals(String.format("service-account-%s", clientId));
    }
}

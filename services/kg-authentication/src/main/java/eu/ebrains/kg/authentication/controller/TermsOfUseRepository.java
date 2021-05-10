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

package eu.ebrains.kg.authentication.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.DocumentCreateOptions;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.authentication.model.AcceptedTermsOfUse;
import eu.ebrains.kg.authentication.model.ArangoTermsOfUse;
import eu.ebrains.kg.authentication.model.TermsOfUseAcceptance;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.model.TermsOfUse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

@Component
public class TermsOfUseRepository {
    private final ArangoDatabaseProxy arangoDatabase;
    private final JsonAdapter jsonAdapter;

    private ArangoCollection getTermsOfUseCollection() {
        ArangoDatabase database = arangoDatabase.getOrCreate();
        ArangoCollection collection = database.collection("termsOfUse");
        if (!collection.exists()) {
            collection.create();
        }
        return collection;
    }

    private ArangoCollection getUsersCollection() {
        ArangoDatabase database = arangoDatabase.getOrCreate();
        ArangoCollection collection = database.collection("users");
        if (!collection.exists()) {
            collection.create();
        }
        return collection;
    }

    public TermsOfUse findTermsOfUseToAccept(String userId){
        TermsOfUseAcceptance termsOfUse = jsonAdapter.fromJson(getUsersCollection().getDocument(userId, String.class), TermsOfUseAcceptance.class);
        TermsOfUse currentTermsOfUse = getCurrentTermsOfUse();
        if(currentTermsOfUse!=null) {
            String currentVersion = currentTermsOfUse.getVersion();
            if (termsOfUse != null && termsOfUse.getAcceptedTermsOfUse().stream().anyMatch(t -> t.getVersion().equals(currentVersion))) {
                return null;
            }
        }
        return currentTermsOfUse;
    }

    public TermsOfUse getCurrentTermsOfUse() {
        return getTermsOfUseCollection().getDocument("current", TermsOfUse.class);
    }

    public void acceptTermsOfUse(String version, String userId){
        TermsOfUseAcceptance userAcceptance = getUsersCollection().getDocument(userId, TermsOfUseAcceptance.class);
        if(userAcceptance == null){
            userAcceptance = new TermsOfUseAcceptance(userId, userId, new ArrayList<>());
        }
        userAcceptance.getAcceptedTermsOfUse().add(new AcceptedTermsOfUse(version, new Date()));
        getUsersCollection().insertDocument(jsonAdapter.toJson(userAcceptance), new DocumentCreateOptions().overwrite(true).silent(true));
    }

    public void setCurrentTermsOfUse(TermsOfUse termsOfUse) {
        if(termsOfUse==null || termsOfUse.getData() == null || termsOfUse.getVersion() == null){
            throw new IllegalArgumentException("Was receiving an invalid terms of use specification");
        }
        ArangoTermsOfUse versioned = new ArangoTermsOfUse(termsOfUse.getVersion(), termsOfUse.getData(), termsOfUse.getVersion());
        ArangoTermsOfUse current = new ArangoTermsOfUse(termsOfUse.getVersion(), termsOfUse.getData(), "current");
        getTermsOfUseCollection().insertDocuments(Arrays.asList(jsonAdapter.toJson(versioned), jsonAdapter.toJson(current)), new DocumentCreateOptions().overwrite(true).silent(true));
    }

    public TermsOfUseRepository(@Qualifier("termsOfUseDB") ArangoDatabaseProxy arangoDatabase, JsonAdapter jsonAdapter, IdUtils idUtils) {
        this.arangoDatabase = arangoDatabase;
        this.jsonAdapter = jsonAdapter;
    }
}
